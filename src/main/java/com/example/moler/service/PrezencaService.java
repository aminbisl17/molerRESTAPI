package com.example.moler.service;

import com.example.moler.dto.Prezenca.AplikoPrezenceRequest;
import com.example.moler.dto.Prezenca.PrezencaFormDataResponse;
import com.example.moler.dto.Prezenca.PrezencaPaVerifikuarResponse;
import com.example.moler.exception.ResourceNotFoundException;
import com.example.moler.model.ProjektiPrezencaPuntoret;
import com.example.moler.model.prezencapuntoreve;
import com.example.moler.model.projekti;
import com.example.moler.model.puntori;
import com.example.moler.repository.PrezencaPuntoreveRepository;
import com.example.moler.repository.ProjektiPrezencaPuntoretRepository;
import com.example.moler.repository.ProjektiRepository;
import com.example.moler.repository.PuntoriRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class PrezencaService {

    private final PrezencaPuntoreveRepository prezencaRepository;
    private final ProjektiRepository projektiRepository;
    private final PuntoriRepository puntoriRepository;
    private final ProjektiPrezencaPuntoretRepository projektiPrezencaRepository;

    public PrezencaService(
            PrezencaPuntoreveRepository prezencaRepository,
            ProjektiRepository projektiRepository,
            PuntoriRepository puntoriRepository,
            ProjektiPrezencaPuntoretRepository projektiPrezencaRepository
    ) {
        this.prezencaRepository = prezencaRepository;
        this.projektiRepository = projektiRepository;
        this.puntoriRepository = puntoriRepository;
        this.projektiPrezencaRepository = projektiPrezencaRepository;
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

        // ...and one detail row per project (duplicates ignored)
        for (Long idProjekti : request.idProjektet().stream().distinct().toList()) {

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
        return prezencaRepository.save(prezenca);
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
    public void verifikoPrezencat(List<Long> idPrezencave, Long idMenaxherit) {

        List<prezencapuntoreve> prezencat =
                gjejPrezencatPerMenaxherin(idPrezencave, idMenaxherit);

        prezencat.forEach(p -> p.setStatusi("v"));
        prezencaRepository.saveAll(prezencat);
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
    }

    /**
     * Loads the given attendance for a manager and checks that every id exists,
     * includes at least one of the manager's projects, and is still "nv".
     */
    private List<prezencapuntoreve> gjejPrezencatPerMenaxherin(
            List<Long> idPrezencave,
            Long idMenaxherit
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
                            projektet
                    );
                })
                .toList();
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
