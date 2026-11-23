
package com.example.moler.service;

import com.example.moler.dto.Projekti.CreateProjektiRequest;
import com.example.moler.dto.Projekti.ProjektiResponse;
import com.example.moler.exception.ResourceNotFoundException;
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

        // 1. Find the manager creating the project
        menaxheri krijuesi = menaxheriRepository.findById(idMenaxherit)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Menaxheri nuk ekziston."
                        )
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

        // 3. Save project to generate its ID
        projekti = projektiRepository.save(projekti);

        // 4. Automatically assign the creating manager
        ProjektiPrezencaMenaxheret prezenca =
                new ProjektiPrezencaMenaxheret();

        prezenca.setProjekti(projekti);
        prezenca.setMenaxheri(krijuesi);

        prezencaMenaxheretRepository.save(prezenca);

        // 5. Add materials if provided
        if (request.materialet() != null
                && !request.materialet().isEmpty()) {

            List<ProjektiMateriali> materialet = new ArrayList<>();

            for (CreateProjektiRequest.MaterialRequest materialRequest
                    : request.materialet()) {

                // Find material
                materiali materiali = materialiRepository
                        .findById(materialRequest.idMaterialit())
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Materiali me ID "
                                                + materialRequest.idMaterialit()
                                                + " nuk ekziston."
                                )
                        );

                // Validate quantity
                if (materialRequest.sasia() == null
                        || materialRequest.sasia()
                                .compareTo(BigDecimal.ZERO) <= 0) {

                    throw new IllegalArgumentException(
                            "Sasia e materialit duhet te jete me e madhe se zero."
                    );
                }

                // Create project-material relationship
                ProjektiMateriali projektiMateriali =
                        new ProjektiMateriali();

                projektiMateriali.setProjekti(projekti);
                projektiMateriali.setMateriali(materiali);
                projektiMateriali.setSasia(materialRequest.sasia());

                // Save material price at assignment time
                projektiMateriali.setCmimiNeMomentinEPerdorimit(
                        materiali.getCmimi()
                );

                materialet.add(projektiMateriali);
            }

            projektiMaterialiRepository.saveAll(materialet);
        }

        // 6. Return created project
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
