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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Requests (kerkesat).
 * - Workers create a request and send it to a manager of their choice; they see,
 *   edit and delete only their own requests (edit / delete only while waiting).
 * - Managers cannot create requests; they see only the requests sent to them
 *   and approve / decline them.
 * Role checks (who may call which endpoint) are done in KerkesaController.
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

    // Notification body limit (njoftimet.mesazhi)
    private static final int MAX_MESAZHI = 500;

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

    // ---------- worker ----------

    /** Worker creates a request and sends it to the chosen manager. */
    @Transactional
    public KerkesaResponse krijo(Long idPuntorit, KrijoKerkesenRequest request) {

        String titulli = teksti(request.titulli(), "Titulli", 255);
        // Optional: missing or blank is stored as null
        String pershkrimi = opsional(request.pershkrimi());

        if (request.idMenaxherit() == null) {
            throw new IllegalArgumentException("Menaxheri eshte i detyrueshem.");
        }

        puntori pu = puntoriRepository.findById(idPuntorit)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Puntori me ID " + idPuntorit + " nuk ekziston."
                ));

        Kerkesa k = new Kerkesa();

        k.setPuntori(pu);
        k.setMenaxheri(gjejMenaxherin(request.idMenaxherit()));
        k.setTitulli(titulli);
        k.setPershkrimi(pershkrimi);
        k.setStatusi(NE_PRITJE);

        k = kerkesaRepository.saveAndFlush(k);

        // Read data_krijimit (database default)
        entityManager.refresh(k);

        njoftoMenaxherin(k);

        return toResponse(k);
    }

    /** Worker edits their own waiting request, optionally sending it to another manager. */
    @Transactional
    public KerkesaResponse perditeso(Long idPuntorit, Long id, PerditesoKerkesenRequest request) {

        Kerkesa k = gjejTePuntorit(idPuntorit, id);

        if (!NE_PRITJE.equals(k.getStatusi())) {
            throw new IllegalArgumentException(
                    "Kerkesa nuk mund te ndryshohet pasi eshte aprovuar ose refuzuar."
            );
        }

        if (request.titulli() != null) {
            k.setTitulli(teksti(request.titulli(), "Titulli", 255));
        }

        // Not sent = unchanged; sent as "" = removed
        if (request.pershkrimi() != null) {
            k.setPershkrimi(opsional(request.pershkrimi()));
        }

        boolean menaxherITjeter = request.idMenaxherit() != null
                && (k.getMenaxheri() == null
                    || !request.idMenaxherit().equals(k.getMenaxheri().getId()));

        if (menaxherITjeter) {
            k.setMenaxheri(gjejMenaxherin(request.idMenaxherit()));
        }

        kerkesaRepository.flush();

        // The new manager has not seen this request yet
        if (menaxherITjeter) {
            njoftoMenaxherin(k);
        }

        return toResponse(k);
    }

    /** Worker deletes their own request while it is still waiting. */
    @Transactional
    public void fshi(Long idPuntorit, Long id) {

        Kerkesa k = gjejTePuntorit(idPuntorit, id);

        if (!NE_PRITJE.equals(k.getStatusi())) {
            throw new IllegalArgumentException(
                    "Kerkesa nuk mund te fshihet pasi eshte aprovuar ose refuzuar."
            );
        }

        kerkesaRepository.delete(k);
    }

    /** Managers a worker can send a request to. */
    @Transactional(readOnly = true)
    public List<KerkesaResponse.Personi> menaxheretPerZgjedhje() {
        return menaxheriRepository.findAll()
                .stream()
                .map(m -> new KerkesaResponse.Personi(m.getId(), m.getEmri(), m.getMbiemri()))
                .toList();
    }

    // ---------- worker + manager ----------

    /** Worker: own requests. Manager: requests sent to them. */
    @Transactional(readOnly = true)
    public List<KerkesaResponse> listo(Perdoruesi p, String statusi) {

        String filtri = statusi == null || statusi.isBlank()
                ? null
                : validoStatusin(statusi);

        List<Kerkesa> kerkesat;

        if (p.eshteMenaxher()) {
            kerkesat = filtri == null
                    ? kerkesaRepository.findByMenaxheri_Id(p.id(), RENDITJA)
                    : kerkesaRepository.findByMenaxheri_IdAndStatusi(p.id(), filtri, RENDITJA);
        } else {
            kerkesat = filtri == null
                    ? kerkesaRepository.findByPuntori_Id(p.id(), RENDITJA)
                    : kerkesaRepository.findByPuntori_IdAndStatusi(p.id(), filtri, RENDITJA);
        }

        return kerkesat.stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public KerkesaResponse merr(Perdoruesi p, Long id) {
        return toResponse(p.eshteMenaxher()
                ? gjejTeMenaxherit(p.id(), id)
                : gjejTePuntorit(p.id(), id));
    }

    // ---------- manager ----------

    @Transactional
    public KerkesaResponse aprovo(Long idMenaxherit, Long id, AprovoKerkesenRequest request) {

        Kerkesa k = gjejPerVendim(idMenaxherit, id);

        k.setStatusi(E_APROVUAR);
        k.setPershkrimiAprovimit(
                opsional(request == null ? null : request.pershkrimiAprovimit())
        );

        KerkesaResponse response = ruajVendimin(k);

        njoftoPuntorin(
                k,
                "kerkesa_e_aprovuar",
                "Kerkesa juaj u aprovua",
                "Kerkesa '" + k.getTitulli() + "' u aprovua.",
                k.getPershkrimiAprovimit()
        );

        return response;
    }

    @Transactional
    public KerkesaResponse refuzo(Long idMenaxherit, Long id, RefuzoKerkesenRequest request) {

        Kerkesa k = gjejPerVendim(idMenaxherit, id);

        k.setStatusi(E_REFUZUAR);
        k.setPershkrimiAnulimit(
                opsional(request == null ? null : request.pershkrimiAnulimit())
        );

        KerkesaResponse response = ruajVendimin(k);

        njoftoPuntorin(
                k,
                "kerkesa_e_refuzuar",
                "Kerkesa juaj u refuzua",
                "Kerkesa '" + k.getTitulli() + "' u refuzua.",
                k.getPershkrimiAnulimit()
        );

        return response;
    }

    // ---------- notifications ----------
    // NotificationService sends after commit; a notification problem never
    // fails or rolls back the request action.

    /** New (or re-sent) request: only the chosen manager. */
    private void njoftoMenaxherin(Kerkesa k) {
        try {
            String emri = k.getPuntori().getEmri() + " " + k.getPuntori().getMbiemri();

            notificationService.dergo(
                    List.of(Marresi.menaxher(k.getMenaxheri().getId())),
                    "Kerkese e re",
                    shkurto(emri + " ju dergoi kerkesen '" + k.getTitulli() + "'."),
                    Map.of(
                            "tipi", "kerkesa_e_re",
                            "idKerkeses", k.getId()
                    )
            );
        } catch (Exception e) {
            log.warn("Could not notify manager about request {}: {}", k.getId(), e.getMessage());
        }
    }

    /** Decision: the worker who created the request, with the optional note appended. */
    private void njoftoPuntorin(
            Kerkesa k,
            String tipi,
            String titulli,
            String mesazhi,
            String shenimi
    ) {
        if (k.getPuntori() == null) {
            return;
        }

        try {
            String teksti = shenimi == null ? mesazhi : mesazhi + " " + shenimi;

            notificationService.dergo(
                    List.of(Marresi.puntor(k.getPuntori().getId())),
                    titulli,
                    shkurto(teksti),
                    Map.of(
                            "tipi", tipi,
                            "idKerkeses", k.getId()
                    )
            );
        } catch (Exception e) {
            log.warn("Could not notify worker about request {}: {}", k.getId(), e.getMessage());
        }
    }

    private String shkurto(String teksti) {
        return teksti.length() > MAX_MESAZHI
                ? teksti.substring(0, MAX_MESAZHI - 3) + "..."
                : teksti;
    }

    // ---------- helpers ----------
    // Someone else's request is reported as "not found", never as "forbidden".

    private Kerkesa gjejTePuntorit(Long idPuntorit, Long id) {
        return kerkesaRepository.findByIdAndPuntori_Id(id, idPuntorit)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Kerkesa me ID " + id + " nuk ekziston."
                ));
    }

    private Kerkesa gjejTeMenaxherit(Long idMenaxherit, Long id) {
        return kerkesaRepository.findByIdAndMenaxheri_Id(id, idMenaxherit)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Kerkesa me ID " + id + " nuk ekziston."
                ));
    }

    /** Only waiting requests sent to this manager can be decided. */
    private Kerkesa gjejPerVendim(Long idMenaxherit, Long id) {

        Kerkesa k = gjejTeMenaxherit(idMenaxherit, id);

        if (!NE_PRITJE.equals(k.getStatusi())) {
            throw new IllegalArgumentException(
                    "Kerkesa eshte aprovuar ose refuzuar tashme."
            );
        }

        return k;
    }

    private menaxheri gjejMenaxherin(Long idMenaxherit) {
        return menaxheriRepository.findById(idMenaxherit)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Menaxheri me ID " + idMenaxherit + " nuk ekziston."
                ));
    }

    /** Saves the decision, then reloads so the trigger's date is in the response. */
    private KerkesaResponse ruajVendimin(Kerkesa k) {
        kerkesaRepository.saveAndFlush(k);
        entityManager.refresh(k);
        return toResponse(k);
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

        KerkesaResponse.Personi puntori = k.getPuntori() == null
                ? null
                : new KerkesaResponse.Personi(
                        k.getPuntori().getId(),
                        k.getPuntori().getEmri(),
                        k.getPuntori().getMbiemri()
                );

        KerkesaResponse.Personi menaxheri = k.getMenaxheri() == null
                ? null
                : new KerkesaResponse.Personi(
                        k.getMenaxheri().getId(),
                        k.getMenaxheri().getEmri(),
                        k.getMenaxheri().getMbiemri()
                );

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
                puntori,
                menaxheri
        );
    }
}
