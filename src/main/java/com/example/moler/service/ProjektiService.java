package com.example.moler.service;

import com.example.moler.dto.Projekti.CreateProjektiRequest;
import com.example.moler.dto.Projekti.ProjektiResponse;

import com.example.moler.model.projekti;
import com.example.moler.model.menaxheri;
import com.example.moler.model.materiali;
import com.example.moler.model.ProjektiMateriali;
import com.example.moler.model.ProjektiPrezencaMenaxheret;

import com.example.moler.repository.ProjektiRepository;
import com.example.moler.repository.MenaxheriRepository;
import com.example.moler.repository.MaterialiRepository;
import com.example.moler.repository.ProjektiMaterialiRepository;
import com.example.moler.repository.ProjektiPrezencaMenaxheretRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Service
public class ProjektiService {

    private final ProjektiRepository projektiRepository;
    private final MenaxheriRepository menaxheriRepository;
    private final MaterialiRepository materialiRepository;
    private final ProjektiMaterialiRepository projektiMaterialiRepository;
    private final ProjektiPrezencaMenaxheretRepository prezencaMenaxheretRepository;

    public ProjektiService(
            ProjektiRepository projektiRepository,
            MenaxheriRepository menaxheriRepository,
            MaterialiRepository materialiRepository,
            ProjektiMaterialiRepository projektiMaterialiRepository,
            ProjektiPrezencaMenaxheretRepository prezencaMenaxheretRepository
    ) {
        this.projektiRepository = projektiRepository;
        this.menaxheriRepository = menaxheriRepository;
        this.materialiRepository = materialiRepository;
        this.projektiMaterialiRepository = projektiMaterialiRepository;
        this.prezencaMenaxheretRepository = prezencaMenaxheretRepository;
    }

    @Transactional
    public ProjektiResponse createProjekti(
            CreateProjektiRequest request,
            Long idMenaxherit
    ) {

        // 1. Find the manager who is creating the project
        menaxheri menaxheri = menaxheriRepository.findById(idMenaxherit)
                .orElseThrow(() ->
                        new RuntimeException("Menaxheri nuk u gjet.")
                );

        // 2. Create project
        projekti projekti = new projekti();

        projekti.setEmri(request.emri());
        projekti.setLokacioni(request.lokacioni());
        projekti.setKerkesa(request.kerkesa());

        projekti.setStatusi("e");
        projekti.setStatusiProjektit("a");

        projekti.setPrioriteti(
                request.prioriteti() != null
                        ? request.prioriteti()
                        : "s"
        );

        projekti.setAmbienti(
                request.ambienti() != null
                        ? request.ambienti()
                        : "m"
        );

        projekti.setDataFillimitTePlanifikuar(
                request.dataFillimitTePlanifikuar()
        );

        projekti.setDataPerfundimitTePlanifikuar(
                request.dataPerfundimitTePlanifikuar()
        );

        // Save project first to generate its ID
        projekti = projektiRepository.save(projekti);

        // 3. Automatically assign the creating manager to the project
        ProjektiPrezencaMenaxheret prezenca = new ProjektiPrezencaMenaxheret();

        prezenca.setProjekti(projekti);
        prezenca.setMenaxheri(menaxheri);

        prezencaMenaxheretRepository.save(prezenca);

        // 4. Add materials if provided
        if (request.materialet() != null && !request.materialet().isEmpty()) {

            List<ProjektiMateriali> materialet = new ArrayList<>();

            for (CreateProjektiRequest.MaterialRequest materialRequest
                    : request.materialet()) {

                materiali materiali = materialiRepository
                        .findById(materialRequest.idMaterialit())
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Materiali me ID "
                                                + materialRequest.idMaterialit()
                                                + " nuk u gjet."
                                )
                        );

                if (materialRequest.sasia() == null
                        || materialRequest.sasia().compareTo(BigDecimal.ZERO) <= 0) {
                    throw new IllegalArgumentException(
                            "Sasia e materialit duhet te jete me e madhe se zero."
                    );
                }

                ProjektiMateriali projektiMateriali =
                        new ProjektiMateriali();

                projektiMateriali.setProjekti(projekti);
                projektiMateriali.setMateriali(materiali);
                projektiMateriali.setSasia(materialRequest.sasia());

                // Preserve the material's price at the time of assignment
                projektiMateriali.setCmimiNeMomentinEPerdorimit(
                        materiali.getCmimi()
                );

                materialet.add(projektiMateriali);
            }

            projektiMaterialiRepository.saveAll(materialet);
        }

        // 5. Return created project
        return new ProjektiResponse(
                projekti.getId(),
                projekti.getEmri(),
                projekti.getLokacioni(),
                projekti.getStatusi(),
                projekti.getPrioriteti(),
                projekti.getAmbienti(),
                projekti.getDataFillimitTePlanifikuar(),
                projekti.getDataPerfundimitTePlanifikuar(),
                projekti.getDataKrijimit()
        );
    }
}