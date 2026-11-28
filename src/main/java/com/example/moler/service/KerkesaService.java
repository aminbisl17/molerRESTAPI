package com.example.moler.service;

import com.example.moler.dto.Kerkesa.AprovoKerkesenRequest;
import com.example.moler.dto.Kerkesa.KerkesaResponse;
import com.example.moler.dto.Kerkesa.KrijoKerkesenRequest;
import com.example.moler.dto.Kerkesa.PerditesoKerkesenRequest;
import com.example.moler.dto.Kerkesa.RefuzoKerkesenRequest;
import com.example.moler.dto.Notification.Marresi;
import com.example.moler.exception.ResourceNotFoundException;
import com.example.moler.model.Kerkesa;
import com.example.moler.model.menaxheri;
import com.example.moler.model.puntori;
import com.example.moler.repository.KerkesaRepository;
import com.example.moler.repository.MenaxheriRepository;
import com.example.moler.repository.PuntoriRepository;
import com.example.moler.service.NjoftimService.Perdoruesi;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Requests (kerkesat) of workers and managers.
 * - Workers see and manage only their own requests.
 * - Managers see all requests of their tenant and approve / decline them.
 * Decision dates are set by the SQL Server trigger, never from Java.
 */
@Service
public class KerkesaService {

    private static final Logger log = LoggerFactory.getLogger(KerkesaService.class);

    private static final String NE_PRITJE = "w";
    private static final String E_APROVUAR = "a";
    private static final String E_REFUZUAR = "d";

    private static final Set<String> STATUSET = Set.of(NE_PRITJE, E_APROVUAR, E_REFUZUAR);

    // Newest first; id breaks ties for requests created in the same instant
    private static final Sort RENDITJA =
            Sort.by(Sort.Order.desc("dataKrijimit"), Sort.Order.desc("id"));

    private final KerkesaRepository kerkesaRepository;
    private final PuntoriRepository puntoriRepository;
    private final MenaxheriRepository menaxheriRepository;
    private final NotificationService notificationService;

    @PersistenceContext
    private EntityManager entityManager;

    public KerkesaService(
            KerkesaRepository kerkesaRepository,
            PuntoriRepository puntoriRepository,
            MenaxheriRepository menaxheriRepository,
            NotificationService notificationService
    ) {
        this.kerkesaRepository = kerkesaRepository;
        this.puntoriRepository = puntoriRepository;
        this.menaxheriRepository = menaxheriRepository;
        this.notificationService = notificationService;
    }

    @Transactional
    public KerkesaResponse krijo(Perdoruesi p, KrijoKerkesenRequest request) {

        Kerkesa k = new Kerkesa();

        k.setTitulli(teksti(request.titulli(), "Titulli", 255));
        k.setPershkrimi(teksti(request.pershkrimi(), "Pershkrimi", null));
        k.setStatusi(NE_PRITJE);

        // The owner is always the logged-in user
        if (p.eshteMenaxher()) {
            menaxheri m = menaxheriRepository.findById(p.id())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Menaxheri me ID " + p.id() + " nuk ekziston."
                    ));
            k.setMenaxheri(m);
        } else {
            puntori pu = puntoriRepository.findById(p.id())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Puntori me ID " + p.id() + " nuk ekziston."
                    ));
            k.setPuntori(pu);
        }

        k = kerkesaRepository.saveAndFlush(k);

        // Read data_krijimit (database default)
        entityManager.refresh(k);

        njoftoMenaxheretPerKerkeseTeRe(k, p);

        return toResponse(k);
    }

    @Transactional(readOnly = true)
    public List<KerkesaResponse> listo(Perdoruesi p, String statusi) {

        String filtri = statusi == null || statusi.isBlank()
                ? null
                : validoStatusin(statusi);

        List<Kerkesa> kerkesat;

        if (p.eshteMenaxher()) {
            kerkesat = filtri == null
                    ? kerkesaRepository.findAll(RENDITJA)
                    : kerkesaRepository.findByStatusi(filtri, RENDITJA);
        } else {
            kerkesat = filtri == null
                    ? kerkesaRepository.findByPuntori_Id(p.id(), RENDITJA)
                    : kerkesaRepository.findByPuntori_IdAndStatusi(p.id(), filtri, RENDITJA);
        }

        return kerkesat.stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public KerkesaResponse merr(Perdoruesi p, Long id) {
        return toResponse(gjej(p, id));
    }

    @Transactional
    public KerkesaResponse perditeso(Perdoruesi p, Long id, PerditesoKerkesenRequest request) {

        Kerkesa k = gjej(p, id);

        // Only the creator edits the content, and only while it is waiting
        if (!eshteKrijuesi(p, k)) {
            throw new AccessDeniedException(
                    "Mund te ndryshoni vetem kerkesat tuaja."
            );
        }

        if (!NE_PRITJE.equals(k.getStatusi())) {
            throw new IllegalArgumentException(
                    "Kerkesa nuk mund te ndryshohet pasi eshte aprovuar ose refuzuar."
            );
        }

        if (request.titulli() != null) {
            k.setTitulli(teksti(request.titulli(), "Titulli", 255));
        }

        if (request.pershkrimi() != null) {
            k.setPershkrimi(teksti(request.pershkrimi(), "Pershkrimi", null));
        }

        kerkesaRepository.flush();

        return toResponse(k);
    }

    @Transactional
    public KerkesaResponse aprovo(Perdoruesi p, Long id, AprovoKerkesenRequest request) {

        Kerkesa k = gjejPerVendim(p, id);

        k.setStatusi(E_APROVUAR);
        k.setPershkrimiAprovimit(
                opsional(request == null ? null : request.pershkrimiAprovimit())
        );

        KerkesaResponse response = ruajVendimin(k);

        njoftoKrijuesin(
                k,
                "kerkesa_e_aprovuar",
                "Kerkesa juaj u aprovua",
                "Kerkesa '" + k.getTitulli() + "' u aprovua.",
                k.getPershkrimiAprovimit()
        );

        return response;
    }

    @Transactional
    public KerkesaResponse refuzo(Perdoruesi p, Long id, RefuzoKerkesenRequest request) {

        Kerkesa k = gjejPerVendim(p, id);

        k.setStatusi(E_REFUZUAR);
        k.setPershkrimiAnulimit(
                opsional(request == null ? null : request.pershkrimiAnulimit())
        );

        KerkesaResponse response = ruajVendimin(k);

        njoftoKrijuesin(
                k,
                "kerkesa_e_refuzuar",
                "Kerkesa juaj u refuzua",
                "Kerkesa '" + k.getTitulli() + "' u refuzua.",
                k.getPershkrimiAnulimit()
        );

        return response;
    }

    @Transactional
    public void fshi(Perdoruesi p, Long id) {

        Kerkesa k = gjej(p, id);

        // Managers can delete any request of the tenant.
        // Workers can delete only their own requests that are still waiting.
        if (!p.eshteMenaxher() && !NE_PRITJE.equals(k.getStatusi())) {
            throw new IllegalArgumentException(
                    "Kerkesa nuk mund te fshihet pasi eshte aprovuar ose refuzuar."
            );
        }

        kerkesaRepository.delete(k);
    }

    // ---------- notifications ----------
    // NotificationService sends after commit; a notification problem never
    // fails or rolls back the request action.

    /** New request: every manager except the creator. */
    private void njoftoMenaxheretPerKerkeseTeRe(Kerkesa k, Perdoruesi krijuesi) {
        try {
            List<Marresi> menaxheret = menaxheriRepository.findAllIds()
                    .stream()
                    .filter(id -> !(krijuesi.eshteMenaxher() && id.equals(krijuesi.id())))
                    .map(Marresi::menaxher)
                    .toList();

            if (menaxheret.isEmpty()) {
                return;
            }

            String emri = k.getPuntori() != null
                    ? k.getPuntori().getEmri() + " " + k.getPuntori().getMbiemri()
                    : k.getMenaxheri().getEmri() + " " + k.getMenaxheri().getMbiemri();

            notificationService.dergo(
                    menaxheret,
                    "Kerkese e re",
                    shkurto(emri + " dergoi kerkesen '" + k.getTitulli() + "'.", MAX_MESAZHI),
                    Map.of(
                            "tipi", "kerkesa_e_re",
                            "idKerkeses", k.getId()
                    )
            );
        } catch (Exception e) {
            log.warn("Could not notify managers about request {}: {}", k.getId(), e.getMessage());
        }
    }

    /** Decision: the creator of the request, with the optional note appended. */
    private void njoftoKrijuesin(
            Kerkesa k,
            String tipi,
            String titulli,
            String mesazhi,
            String shenimi
    ) {
        try {
            Marresi marresi = k.getPuntori() != null
                    ? Marresi.puntor(k.getPuntori().getId())
                    : Marresi.menaxher(k.getMenaxheri().getId());

            String teksti = shenimi == null ? mesazhi : mesazhi + " " + shenimi;

            notificationService.dergo(
                    List.of(marresi),
                    titulli,
                    shkurto(teksti, MAX_MESAZHI),
                    Map.of(
                            "tipi", tipi,
                            "idKerkeses", k.getId()
                    )
            );
        } catch (Exception e) {
            log.warn("Could not notify creator of request {}: {}", k.getId(), e.getMessage());
        }
    }

    // Notification body limit (njoftimet.mesazhi)
    private static final int MAX_MESAZHI = 500;

    private String shkurto(String teksti, int max) {
        return teksti.length() > max ? teksti.substring(0, max - 3) + "..." : teksti;
    }

    // ---------- helpers ----------

    /** A worker asking for someone else's request gets "not found", never "forbidden". */
    private Kerkesa gjej(Perdoruesi p, Long id) {
        return (p.eshteMenaxher()
                ? kerkesaRepository.findById(id)
                : kerkesaRepository.findByIdAndPuntori_Id(id, p.id()))
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Kerkesa me ID " + id + " nuk ekziston."
                ));
    }

    /** Only waiting requests can be decided, and a manager cannot decide their own. */
    private Kerkesa gjejPerVendim(Perdoruesi p, Long id) {

        Kerkesa k = gjej(p, id);

        if (eshteKrijuesi(p, k)) {
            throw new AccessDeniedException(
                    "Nuk mund te aprovoni ose refuzoni kerkesen tuaj."
            );
        }

        if (!NE_PRITJE.equals(k.getStatusi())) {
            throw new IllegalArgumentException(
                    "Kerkesa eshte aprovuar ose refuzuar tashme."
            );
        }

        return k;
    }

    /** Saves the decision, then reloads so the trigger's date is in the response. */
    private KerkesaResponse ruajVendimin(Kerkesa k) {
        kerkesaRepository.saveAndFlush(k);
        entityManager.refresh(k);
        return toResponse(k);
    }

    private boolean eshteKrijuesi(Perdoruesi p, Kerkesa k) {
        // getId() on a lazy proxy does not hit the database
        return p.eshteMenaxher()
                ? k.getMenaxheri() != null && k.getMenaxheri().getId().equals(p.id())
                : k.getPuntori() != null && k.getPuntori().getId().equals(p.id());
    }

    private String validoStatusin(String statusi) {

        String s = statusi.trim().toLowerCase();

        if (!STATUSET.contains(s)) {
            throw new IllegalArgumentException(
                    "Statusi i kerkeses nuk eshte i vlefshem. Lejohen: w, a, d."
            );
        }

        return s;
    }

    private String teksti(String value, String fusha, Integer maxGjatesia) {

        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fusha + " eshte i detyrueshem.");
        }

        String t = value.trim();

        if (maxGjatesia != null && t.length() > maxGjatesia) {
            throw new IllegalArgumentException(
                    fusha + " nuk mund te kaloje " + maxGjatesia + " karaktere."
            );
        }

        return t;
    }

    private String opsional(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private KerkesaResponse toResponse(Kerkesa k) {

        KerkesaResponse.Krijuesi krijuesi = null;

        if (k.getPuntori() != null) {
            krijuesi = new KerkesaResponse.Krijuesi(
                    k.getPuntori().getId(),
                    k.getPuntori().getEmri(),
                    k.getPuntori().getMbiemri(),
                    "PUNTOR"
            );
        } else if (k.getMenaxheri() != null) {
            krijuesi = new KerkesaResponse.Krijuesi(
                    k.getMenaxheri().getId(),
                    k.getMenaxheri().getEmri(),
                    k.getMenaxheri().getMbiemri(),
                    "MENAXHER"
            );
        }

        return new KerkesaResponse(
                k.getId(),
                k.getTitulli(),
                k.getPershkrimi(),
                k.getStatusi(),
                k.getDataKrijimit(),
                k.getDataAprovimit(),
                k.getDataRefuzimit(),
                k.getPershkrimiAprovimit(),
                k.getPershkrimiAnulimit(),
                krijuesi
        );
    }
}
