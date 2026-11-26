package com.example.moler.service;

import com.example.moler.dto.Prezenca.AplikoPrezenceRequest;
import com.example.moler.dto.Prezenca.PerditesoPrezenceRequest;
import com.example.moler.dto.Prezenca.PrezencaFormDataResponse;
import com.example.moler.dto.Prezenca.PrezencaPaVerifikuarResponse;
import com.example.moler.dto.Prezenca.PrezencaResponse;
import com.example.moler.exception.ResourceNotFoundException;
import com.example.moler.model.ProjektiPrezencaPuntoret;
import com.example.moler.model.prezencapuntoreve;
import com.example.moler.model.projekti;
import com.example.moler.model.puntori;
import com.example.moler.repository.PrezencaPuntoreveRepository;
import com.example.moler.repository.ProjektiPrezencaMenaxheretRepository;
import com.example.moler.repository.ProjektiPrezencaPuntoretRepository;
import com.example.moler.dto.Notification.Marresi;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.example.moler.repository.ProjektiRepository;
import com.example.moler.repository.PuntoriRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Optional;

@Service
public class PrezencaService {

    private final PrezencaPuntoreveRepository prezencaRepository;
    private final ProjektiRepository projektiRepository;
    private final PuntoriRepository puntoriRepository;
    private final ProjektiPrezencaPuntoretRepository projektiPrezencaRepository;
    private final ProjektiPrezencaMenaxheretRepository projektiMenaxheretRepository;
    private final NotificationService notificationService;

    private static final Logger log = LoggerFactory.getLogger(PrezencaService.class);

    public PrezencaService(
            PrezencaPuntoreveRepository prezencaRepository,
            ProjektiRepository projektiRepository,
            PuntoriRepository puntoriRepository,
            ProjektiPrezencaPuntoretRepository projektiPrezencaRepository,
            ProjektiPrezencaMenaxheretRepository projektiMenaxheretRepository,
            NotificationService notificationService
    ) {
        this.prezencaRepository = prezencaRepository;
        this.projektiRepository = projektiRepository;
        this.puntoriRepository = puntoriRepository;
        this.projektiPrezencaRepository = projektiPrezencaRepository;
        this.projektiMenaxheretRepository = projektiMenaxheretRepository;
        this.notificationService = notificationService;
    }

    @Transactional
    public prezencapuntoreve aplikoPrezence(
            AplikoPrezenceRequest request,
            Long idPuntorit
    ) {

        if (idPuntorit == null) {
            throw new RuntimeException("Puntori eshte i detyrueshem");
        }

        if (request.idProjektet() == null || request.idProjektet().isEmpty()) {
            throw new RuntimeException("Duhet te zgjidhni te pakten nje projekt");
        }

        if (!"h".equals(request.llojiOrarit()) &&
                !"f".equals(request.llojiOrarit())) {
            throw new RuntimeException("Lloji i orarit duhet te jete h ose f");
        }

        puntori puntori = puntoriRepository
                .findById(idPuntorit)
                .orElseThrow(() ->
                        new RuntimeException("Puntori nuk ekziston"));

        LocalDate dita = LocalDate.now(ZoneId.of("Europe/Skopje"));

        // Prevent applying more than once per day
        if (prezencaRepository.existsByPuntori_IdAndDita(idPuntorit, dita)) {
            throw new RuntimeException("Keni aplikuar tashme per sot");
        }

        // One attendance row for the day...
        prezencapuntoreve prezenca = new prezencapuntoreve();
        prezenca.setPuntori(puntori);
        prezenca.setDita(dita);
        prezenca.setLlojiOrarit(request.llojiOrarit());
        prezenca.setStatusi("nv");

        List<Long> idProjektet = request.idProjektet().stream().distinct().toList();

        // ...and one detail row per project (duplicates ignored)
        for (Long idProjekti : idProjektet) {

            if (idProjekti == null) {
                throw new RuntimeException("ID e projektit nuk eshte valide");
            }

            projekti projekti = projektiRepository
                    .findById(idProjekti)
                    .orElseThrow(() ->
                            new RuntimeException("Projekti nuk ekziston: " + idProjekti));

            prezenca.shtoProjekt(projekti);
        }

        // Details are saved together with the attendance (cascade)
        prezencapuntoreve ruajtur = prezencaRepository.save(prezenca);

        njoftoMenaxheret(ruajtur, puntori, idProjektet);

        return ruajtur;
    }

    /**
     * Notifies the managers of the selected projects about the new attendance.
     * Sent after commit; a notification problem never fails the application.
     */
    private void njoftoMenaxheret(
            prezencapuntoreve prezenca,
            puntori puntori,
            List<Long> idProjektet
    ) {
        try {
            List<Marresi> menaxheret = projektiMenaxheretRepository
                    .findMenaxheriIdsByProjektiIds(idProjektet)
                    .stream()
                    .map(Marresi::menaxher)
                    .toList();

            if (menaxheret.isEmpty()) {
                return;
            }

            String orari = "h".equals(prezenca.getLlojiOrarit()) ? "gjysme dite" : "dite e plote";

            notificationService.dergo(
                    menaxheret,
                    "Prezence e re per verifikim",
                    puntori.getEmri() + " " + puntori.getMbiemri()
                            + " aplikoi per prezence sot (" + orari + ").",
                    Map.of(
                            "tipi", "prezenca_e_re",
                            "idPrezences", prezenca.getId()
                    )
            );
        } catch (Exception e) {
            log.warn("Could not notify managers about attendance {}: {}",
                    prezenca.getId(), e.getMessage());
        }
    }

    @Transactional
    public void fshiPrezencenESotme(Long idPuntorit) {

        if (idPuntorit == null) {
            throw new IllegalArgumentException("Puntori eshte i detyrueshem");
        }

        LocalDate dita = LocalDate.now(ZoneId.of("Europe/Skopje"));

        prezencapuntoreve prezenca = prezencaRepository
                .findByPuntori_IdAndDita(idPuntorit, dita)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Nuk keni prezence per sot"));

        // Only allow deletion while still not verified
        if (!"nv".equals(prezenca.getStatusi())) {
            throw new IllegalArgumentException(
                    "Prezenca eshte verifikuar dhe nuk mund te fshihet");
        }

        // Details are removed together with the attendance (cascade)
        prezencaRepository.delete(prezenca);
    }

    @Transactional
    public void fshiPrezencat(List<Long> idPrezencave, Long idMenaxherit) {

        // Managers may delete attendance in any status, but only on their projects
        List<prezencapuntoreve> prezencat =
                gjejPrezencatPerMenaxherin(idPrezencave, idMenaxherit, false);

        // Notify before deleting, while worker and date are still loaded (sent after commit)
        prezencat.forEach(p -> njoftoPuntorin(
                p,
                "prezenca_fshire",
                "Prezenca u fshi",
                "Prezenca juaj per " + formatoDiten(p) + " u fshi nga menaxheri."
        ));

        // Details are removed together with the attendance (cascade)
        prezencaRepository.deleteAll(prezencat);
    }

    @Transactional
    public void perditesoPrezencen(
            Long idPrezences,
            PerditesoPrezenceRequest request,
            Long idMenaxherit
    ) {

        if (request == null) {
            throw new IllegalArgumentException("Te dhenat per perditesim mungojne");
        }

        // Any status can be edited, but only on the manager's projects
        prezencapuntoreve prezenca =
                gjejPrezencatPerMenaxherin(List.of(idPrezences), idMenaxherit, false).get(0);

        String statusiIVjeter = prezenca.getStatusi();

        String statusiIRi = request.statusi() == null
                ? statusiIVjeter
                : request.statusi().trim();

        if (!List.of("nv", "v", "c").contains(statusiIRi)) {
            throw new IllegalArgumentException("Statusi duhet te jete nv, v ose c");
        }

        if (request.llojiOrarit() != null) {
            String lloji = request.llojiOrarit().trim();

            if (!"h".equals(lloji) && !"f".equals(lloji)) {
                throw new IllegalArgumentException("Lloji i orarit duhet te jete h ose f");
            }

            prezenca.setLlojiOrarit(lloji);
        }

        if (request.idProjektet() != null) {
            perditesoProjektet(prezenca, request.idProjektet());
        }

        if ("c".equals(statusiIRi)) {
            // Keep the existing reason unless a new one is sent
            if (request.arsyejaAnulimit() != null) {
                String arsyeja = request.arsyejaAnulimit().trim();
                prezenca.setArsyejaAnulimit(arsyeja.isEmpty() ? null : arsyeja);
            }

            // Stamp the cancellation date only when it becomes canceled
            if (!"c".equals(statusiIVjeter) || prezenca.getDataAnulimit() == null) {
                prezenca.setDataAnulimit(LocalDateTime.now(ZoneId.of("Europe/Skopje")));
            }
        } else {
            if (request.arsyejaAnulimit() != null && !request.arsyejaAnulimit().isBlank()) {
                throw new IllegalArgumentException(
                        "Arsyeja e anulimit lejohet vetem kur statusi eshte c");
            }

            prezenca.setArsyejaAnulimit(null);
            prezenca.setDataAnulimit(null);
        }

        prezenca.setStatusi(statusiIRi);
        prezencaRepository.save(prezenca);
    }

    /**
     * Replaces the attendance projects, touching only the rows that change
     * so unchanged projects are not deleted and re-inserted.
     */
    private void perditesoProjektet(prezencapuntoreve prezenca, List<Long> idProjektet) {

        if (idProjektet.isEmpty()) {
            throw new IllegalArgumentException("Duhet te zgjidhni te pakten nje projekt");
        }

        if (idProjektet.stream().anyMatch(id -> id == null)) {
            throw new IllegalArgumentException("ID e projektit nuk eshte valide");
        }

        Set<Long> teRinjte = new LinkedHashSet<>(idProjektet);

        // Remove projects that are no longer selected (orphanRemoval deletes the rows)
        prezenca.getDetajet().removeIf(d ->
                !teRinjte.contains(d.getProjekti().getId()));

        Set<Long> ekzistuese = new HashSet<>();
        prezenca.getDetajet().forEach(d -> ekzistuese.add(d.getProjekti().getId()));

        // Add the newly selected projects
        for (Long idProjekti : teRinjte) {
            if (ekzistuese.contains(idProjekti)) {
                continue;
            }

            projekti projekti = projektiRepository
                    .findById(idProjekti)
                    .orElseThrow(() ->
                            new ResourceNotFoundException("Projekti nuk ekziston: " + idProjekti));

            prezenca.shtoProjekt(projekti);
        }
    }

    @Transactional
    public void verifikoPrezencat(List<Long> idPrezencave, Long idMenaxherit) {

        List<prezencapuntoreve> prezencat =
                gjejPrezencatPerMenaxherin(idPrezencave, idMenaxherit);

        prezencat.forEach(p -> p.setStatusi("v"));
        prezencaRepository.saveAll(prezencat);

        prezencat.forEach(p -> njoftoPuntorin(
                p,
                "prezenca_verifikuar",
                "Prezenca u verifikua",
                "Prezenca juaj per " + formatoDiten(p) + " u verifikua."
        ));
    }

    @Transactional
    public void anuloPrezencat(
            List<Long> idPrezencave,
            String arsyejaAnulimit,
            Long idMenaxherit
    ) {

        List<prezencapuntoreve> prezencat =
                gjejPrezencatPerMenaxherin(idPrezencave, idMenaxherit);

        String arsyeja = (arsyejaAnulimit == null || arsyejaAnulimit.isBlank())
                ? null
                : arsyejaAnulimit.trim();

        LocalDateTime tani = LocalDateTime.now(ZoneId.of("Europe/Skopje"));

        prezencat.forEach(p -> {
            p.setStatusi("c");
            p.setArsyejaAnulimit(arsyeja);
            p.setDataAnulimit(tani);
        });
        prezencaRepository.saveAll(prezencat);

        String pjesaArsyes = arsyeja == null ? "" : " Arsyeja: " + arsyeja;

        prezencat.forEach(p -> njoftoPuntorin(
                p,
                "prezenca_anuluar",
                "Prezenca u anulua",
                "Prezenca juaj per " + formatoDiten(p) + " u anulua." + pjesaArsyes
        ));
    }

    private static final DateTimeFormatter FORMATI_DITES = DateTimeFormatter.ofPattern("dd.MM.yyyy");

    private String formatoDiten(prezencapuntoreve p) {
        return p.getDita() == null ? "" : p.getDita().format(FORMATI_DITES);
    }

    /**
     * Tells the worker that a manager changed their attendance.
     * Sent after commit; a notification problem never fails the manager's action.
     */
    private void njoftoPuntorin(
            prezencapuntoreve prezenca,
            String tipi,
            String titulli,
            String mesazhi
    ) {
        try {
            Long idPuntorit = prezenca.getPuntori().getId();

            notificationService.dergo(
                    List.of(Marresi.puntor(idPuntorit)),
                    titulli,
                    // Notification body limit; a long cancel reason is shortened
                    mesazhi.length() > 500 ? mesazhi.substring(0, 497) + "..." : mesazhi,
                    Map.of(
                            "tipi", tipi,
                            "idPrezences", prezenca.getId(),
                            "dita", prezenca.getDita().toString()
                    )
            );
        } catch (Exception e) {
            log.warn("Could not notify worker about attendance {}: {}",
                    prezenca.getId(), e.getMessage());
        }
    }

    /**
     * Loads the given attendance for a manager and checks that every id exists,
     * includes at least one of the manager's projects, and is still "nv".
     */
    private List<prezencapuntoreve> gjejPrezencatPerMenaxherin(
            List<Long> idPrezencave,
            Long idMenaxherit
    ) {
        return gjejPrezencatPerMenaxherin(idPrezencave, idMenaxherit, true);
    }

    private List<prezencapuntoreve> gjejPrezencatPerMenaxherin(
            List<Long> idPrezencave,
            Long idMenaxherit,
            boolean vetemNv
    ) {

        if (idMenaxherit == null) {
            throw new IllegalArgumentException("Menaxheri eshte i detyrueshem");
        }

        if (idPrezencave == null || idPrezencave.isEmpty()) {
            throw new IllegalArgumentException("Duhet te zgjidhni te pakten nje prezence");
        }

        if (idPrezencave.stream().anyMatch(id -> id == null)) {
            throw new IllegalArgumentException("ID e prezences nuk eshte valide");
        }

        List<Long> ids = idPrezencave.stream().distinct().toList();

        List<prezencapuntoreve> prezencat =
                prezencaRepository.findByIdInAndMenaxheriId(ids, idMenaxherit);

        // Every id must exist and include at least one of this manager's projects
        if (prezencat.size() != ids.size()) {
            List<Long> gjetura = prezencat.stream()
                    .map(prezencapuntoreve::getId)
                    .toList();
            List<Long> mungojne = ids.stream()
                    .filter(id -> !gjetura.contains(id))
                    .toList();
            throw new ResourceNotFoundException(
                    "Prezencat nuk u gjeten ose nuk jane ne projektet tuaja: " + mungojne);
        }

        if (!vetemNv) {
            return prezencat;
        }

        // Only attendance that is still not verified can be verified or canceled
        List<Long> joNv = prezencat.stream()
                .filter(p -> !"nv".equals(p.getStatusi()))
                .map(prezencapuntoreve::getId)
                .toList();

        if (!joNv.isEmpty()) {
            throw new IllegalArgumentException(
                    "Prezencat jane verifikuar ose anuluar tashme: " + joNv);
        }

        return prezencat;
    }

    @Transactional(readOnly = true)
    public List<PrezencaPaVerifikuarResponse> getPrezencatPaVerifikuara(Long idMenaxherit) {

        if (idMenaxherit == null) {
            throw new IllegalArgumentException("Menaxheri eshte i detyrueshem");
        }

        return prezencaRepository.findPaVerifikuaraByMenaxheriId(idMenaxherit)
                .stream()
                .map(p -> {
                    puntori pu = p.getPuntori();

                    List<PrezencaPaVerifikuarResponse.ProjektiData> projektet =
                            p.getDetajet().stream()
                                    .map(d -> d.getProjekti())
                                    .map(pr -> new PrezencaPaVerifikuarResponse.ProjektiData(
                                            pr.getId(),
                                            pr.getEmri(),
                                            pr.getKerkesa(),
                                            pr.getAmbienti()
                                    ))
                                    .toList();

                    return new PrezencaPaVerifikuarResponse(
                            p.getId(),
                            pu.getId(),
                            pu.getEmri(),
                            pu.getMbiemri(),
                            pu.getPozita(),
                            p.getDita(),
                            p.getLlojiOrarit(),
                            projektet
                    );
                })
                .toList();
    }

    @Transactional(readOnly = true)
    public List<PrezencaResponse> getTeGjithaPrezencat(Long idMenaxherit) {

        if (idMenaxherit == null) {
            throw new IllegalArgumentException("Menaxheri eshte i detyrueshem");
        }

        return prezencaRepository.findAllByMenaxheriId(idMenaxherit)
                .stream()
                .map(this::toPrezencaResponse)
                .toList();
    }

    /**
     * A worker's verified and unverified attendance (canceled is excluded).
     * With a manager id, only attendance on that manager's projects is returned;
     * with null (the worker viewing their own), all of it is.
     */
    @Transactional(readOnly = true)
    public List<PrezencaResponse> getPrezencatEPuntorit(Long idPuntorit, Long idMenaxherit) {

        if (!puntoriRepository.existsById(idPuntorit)) {
            throw new ResourceNotFoundException("Puntori nuk ekziston: " + idPuntorit);
        }

        List<String> statuset = List.of("nv", "v");

        List<prezencapuntoreve> prezencat = idMenaxherit == null
                ? prezencaRepository.findByPuntoriIdAndStatusiIn(idPuntorit, statuset)
                : prezencaRepository.findByPuntoriIdAndMenaxheriIdAndStatusiIn(
                        idPuntorit, idMenaxherit, statuset);

        return prezencat.stream()
                .map(this::toPrezencaResponse)
                .toList();
    }

    private PrezencaResponse toPrezencaResponse(prezencapuntoreve p) {
        puntori pu = p.getPuntori();

        List<PrezencaResponse.ProjektiData> projektet =
                p.getDetajet().stream()
                        .map(d -> d.getProjekti())
                        .map(pr -> new PrezencaResponse.ProjektiData(
                                pr.getId(),
                                pr.getEmri(),
                                pr.getKerkesa(),
                                pr.getAmbienti()
                        ))
                        .toList();

        return new PrezencaResponse(
                p.getId(),
                pu.getId(),
                pu.getEmri(),
                pu.getMbiemri(),
                pu.getPozita(),
                p.getDita(),
                p.getLlojiOrarit(),
                p.getStatusi(),
                p.getArsyejaAnulimit(),
                p.getDataAnulimit(),
                p.getDataKrijimit(),
                projektet
        );
    }

    @Transactional(readOnly = true)
    public PrezencaFormDataResponse getFormData(Long idPuntorit) {

        puntoriRepository.findById(idPuntorit)
                .orElseThrow(() ->
                        new RuntimeException("Puntori nuk ekziston"));

        LocalDate sot = LocalDate.now(ZoneId.of("Europe/Skopje"));

        // Check whether the worker has already applied today
        Optional<prezencapuntoreve> prezencaESotme =
                prezencaRepository.findByPuntori_IdAndDita(idPuntorit, sot);

        boolean kaAplikuarSot = prezencaESotme.isPresent();

        // Today's status: "nv", "v", "c", or null if not applied
        String statusi = prezencaESotme
                .map(prezencapuntoreve::getStatusi)
                .orElse(null);

        // Cancellation details only when canceled
        boolean eshteAnuluar = "c".equals(statusi);

        String arsyejaAnulimit = eshteAnuluar
                ? prezencaESotme.get().getArsyejaAnulimit()
                : null;

        LocalDateTime dataAnulimit = eshteAnuluar
                ? prezencaESotme.get().getDataAnulimit()
                : null;

        // Find latest attendance date, excluding completed projects
        LocalDate ditaFundit =
                prezencaRepository.findLatestDitaByPuntoriId(idPuntorit);

        List<Long> projektetEFundit = new ArrayList<>();

        if (ditaFundit != null) {
            // Projects of that day, only if it was verified
            projektetEFundit = prezencaRepository
                    .findLatestProjektetByPuntoriId(idPuntorit, ditaFundit);
        }

        // Get all projects assigned to this worker, excluding completed projects
        List<ProjektiPrezencaPuntoret> projektetEPuntorit =
                projektiPrezencaRepository
                        .findAllByPuntori_IdAndProjekti_StatusiNot(
                                idPuntorit, "c"
                        );

        List<PrezencaFormDataResponse.ProjektiData> projektet =
                projektetEPuntorit.stream()
                        .map(p -> new PrezencaFormDataResponse.ProjektiData(
                                p.getProjekti().getId(),
                                p.getProjekti().getEmri(),
                                p.getProjekti().getKerkesa(),
                                p.getProjekti().getAmbienti()
                        ))
                        .toList();

        return new PrezencaFormDataResponse(
                projektetEFundit,
                projektet,
                kaAplikuarSot,
                statusi,
                arsyejaAnulimit,
                dataAnulimit
        );
    }
}
