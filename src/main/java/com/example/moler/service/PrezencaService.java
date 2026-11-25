
package com.example.moler.service;

import com.example.moler.dto.Prezenca.AplikoPrezenceRequest;
import com.example.moler.dto.Prezenca.PrezencaFormDataResponse;
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
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

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
    public List<prezencapuntoreve> aplikoPrezence(
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

        List<prezencapuntoreve> prezencat = new ArrayList<>();

        for (Long idProjekti : request.idProjektet()) {

            if (idProjekti == null) {
                throw new RuntimeException("ID e projektit nuk eshte valide");
            }

            projekti projekti = projektiRepository
                    .findById(idProjekti)
                    .orElseThrow(() ->
                            new RuntimeException("Projekti nuk ekziston: " + idProjekti));

            prezencapuntoreve prezenca = new prezencapuntoreve();

            prezenca.setProjekti(projekti);
            prezenca.setPuntori(puntori);
            prezenca.setDita(dita);
            prezenca.setLlojiOrarit(request.llojiOrarit());
            prezenca.setStatusi("nv");

            prezencat.add(prezenca);
        }

        return prezencaRepository.saveAll(prezencat);
    }

    @Transactional(readOnly = true)
    public PrezencaFormDataResponse getFormData(Long idPuntorit) {

        puntoriRepository.findById(idPuntorit)
                .orElseThrow(() ->
                        new RuntimeException("Puntori nuk ekziston"));

        LocalDate sot = LocalDate.now(ZoneId.of("Europe/Skopje"));

        // Check whether the worker has already applied today
        boolean kaAplikuarSot =
                prezencaRepository.existsByPuntori_IdAndDita(
                        idPuntorit, sot
                );

        // Find latest VERIFIED attendance date, excluding completed projects
        LocalDate ditaFundit =
                prezencaRepository.findLatestDitaByPuntoriId(idPuntorit);

        List<Long> projektetEFundit = new ArrayList<>();

        if (ditaFundit != null) {
            projektetEFundit = prezencaRepository
                    .findLatestAttendanceByPuntoriId(idPuntorit, ditaFundit)
                    .stream()
                    .map(p -> p.getProjekti().getId())
                    .distinct()
                    .toList();
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
                kaAplikuarSot
        );
    }
}
