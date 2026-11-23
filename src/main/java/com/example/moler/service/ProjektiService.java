
package com.example.moler.service;

import com.example.moler.dto.Projekti.CreateProjektiRequest;
import com.example.moler.dto.Projekti.ProjektiResponse;
import com.example.moler.exception.ResourceNotFoundException;

import com.example.moler.model.projekti;
import com.example.moler.model.menaxheri;
import com.example.moler.model.materiali;
import com.example.moler.model.puntori;
import com.example.moler.model.ProjektiMateriali;
import com.example.moler.model.ProjektiPrezencaMenaxheret;
import com.example.moler.model.ProjektiPrezencaPuntoret;

import com.example.moler.repository.ProjektiRepository;
import com.example.moler.repository.MenaxheriRepository;
import com.example.moler.repository.MaterialiRepository;
import com.example.moler.repository.PuntoriRepository;
import com.example.moler.repository.ProjektiMaterialiRepository;
import com.example.moler.repository.ProjektiPrezencaMenaxheretRepository;
import com.example.moler.repository.ProjektiPrezencaPuntoretRepository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Service
public class ProjektiService {

    private final ProjektiRepository projektiRepository;
    private final MenaxheriRepository menaxheriRepository;
    private final PuntoriRepository puntoriRepository;
    private final MaterialiRepository materialiRepository;
    private final ProjektiMaterialiRepository projektiMaterialiRepository;
    private final ProjektiPrezencaMenaxheretRepository prezencaMenaxheretRepository;
    private final ProjektiPrezencaPuntoretRepository prezencaPuntoretRepository;

    @PersistenceContext
    private EntityManager entityManager;

    public ProjektiService(
            ProjektiRepository projektiRepository,
            MenaxheriRepository menaxheriRepository,
            PuntoriRepository puntoriRepository,
            MaterialiRepository materialiRepository,
            ProjektiMaterialiRepository projektiMaterialiRepository,
            ProjektiPrezencaMenaxheretRepository prezencaMenaxheretRepository,
            ProjektiPrezencaPuntoretRepository prezencaPuntoretRepository
    ) {
        this.projektiRepository = projektiRepository;
        this.menaxheriRepository = menaxheriRepository;
        this.puntoriRepository = puntoriRepository;
        this.materialiRepository = materialiRepository;
        this.projektiMaterialiRepository = projektiMaterialiRepository;
        this.prezencaMenaxheretRepository = prezencaMenaxheretRepository;
        this.prezencaPuntoretRepository = prezencaPuntoretRepository;
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
                                "Menaxheri qe po krijon projektin nuk ekziston."
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

        // 3. Save project and retrieve database-generated values
        projekti = projektiRepository.saveAndFlush(projekti);

        entityManager.refresh(projekti);

        // 4. Assign managers
        // The manager creating the project is always assigned automatically.
        // Additional managers from the request are also assigned.

        Set<Long> managerIds = new LinkedHashSet<>();

        if (request.menaxheret() != null) {
            managerIds.addAll(request.menaxheret());
        }

        managerIds.add(idMenaxherit);

        List<ProjektiPrezencaMenaxheret> managerAssignments =
                new ArrayList<>();

        for (Long managerId : managerIds) {

            if (managerId == null) {
                throw new IllegalArgumentException(
                        "ID e menaxherit nuk mund te jete bosh."
                );
            }

            menaxheri manager = menaxheriRepository.findById(managerId)
                    .orElseThrow(() ->
                            new ResourceNotFoundException(
                                    "Menaxheri me ID "
                                            + managerId
                                            + " nuk ekziston."
                            )
                    );

            ProjektiPrezencaMenaxheret assignment =
                    new ProjektiPrezencaMenaxheret();

            assignment.setProjekti(projekti);
            assignment.setMenaxheri(manager);

            managerAssignments.add(assignment);
        }

        prezencaMenaxheretRepository.saveAll(managerAssignments);

        // 5. Assign workers
        if (request.puntoret() != null
                && !request.puntoret().isEmpty()) {

            Set<Long> workerIds = new LinkedHashSet<>(
                    request.puntoret()
            );

            List<ProjektiPrezencaPuntoret> workerAssignments =
                    new ArrayList<>();

            for (Long workerId : workerIds) {

                if (workerId == null) {
                    throw new IllegalArgumentException(
                            "ID e puntorit nuk mund te jete bosh."
                    );
                }

                puntori worker = puntoriRepository.findById(workerId)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Puntori me ID "
                                                + workerId
                                                + " nuk ekziston."
                                )
                        );

                ProjektiPrezencaPuntoret assignment =
                        new ProjektiPrezencaPuntoret();

                assignment.setProjekti(projekti);
                assignment.setPuntori(worker);

                workerAssignments.add(assignment);
            }

            prezencaPuntoretRepository.saveAll(workerAssignments);
        }

        // 6. Add materials
        if (request.materialet() != null
                && !request.materialet().isEmpty()) {

            List<ProjektiMateriali> materialet = new ArrayList<>();

            for (CreateProjektiRequest.MaterialRequest materialRequest
                    : request.materialet()) {

                if (materialRequest.idMaterialit() == null) {
                    throw new IllegalArgumentException(
                            "ID e materialit nuk mund te jete bosh."
                    );
                }

                materiali materiali = materialiRepository
                        .findById(materialRequest.idMaterialit())
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Materiali me ID "
                                                + materialRequest.idMaterialit()
                                                + " nuk ekziston."
                                )
                        );

                if (materialRequest.sasia() == null
                        || materialRequest.sasia()
                                .compareTo(BigDecimal.ZERO) <= 0) {

                    throw new IllegalArgumentException(
                            "Sasia e materialit duhet te jete me e madhe se zero."
                    );
                }

                ProjektiMateriali projektiMateriali =
                        new ProjektiMateriali();

                projektiMateriali.setProjekti(projekti);
                projektiMateriali.setMateriali(materiali);
                projektiMateriali.setSasia(materialRequest.sasia());

                // Preserve material price at assignment time
                projektiMateriali.setCmimiNeMomentinEPerdorimit(
                        materiali.getCmimi()
                );

                materialet.add(projektiMateriali);
            }

            projektiMaterialiRepository.saveAll(materialet);
        }

        // 7. Return created project
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
