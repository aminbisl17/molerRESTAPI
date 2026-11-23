
package com.example.moler.service;

import com.example.moler.dto.Projekti.CreateProjektiRequest;
import com.example.moler.dto.Projekti.ProjektiResponse;
import com.example.moler.exception.ResourceNotFoundException;

import com.example.moler.model.projekti;
import com.example.moler.model.menaxheri;
import com.example.moler.model.puntori;
import com.example.moler.model.materiali;
import com.example.moler.model.projektidetajet;
import com.example.moler.model.ProjektiMateriali;
import com.example.moler.model.ProjektiPrezencaMenaxheret;
import com.example.moler.model.ProjektiPrezencaPuntoret;

import com.example.moler.repository.ProjektiRepository;
import com.example.moler.repository.MenaxheriRepository;
import com.example.moler.repository.PuntoriRepository;
import com.example.moler.repository.MaterialiRepository;
import com.example.moler.repository.ProjektiMaterialiRepository;
import com.example.moler.repository.ProjektiPrezencaMenaxheretRepository;
import com.example.moler.repository.ProjektiPrezencaPuntoretRepository;
import com.example.moler.repository.ProjektiDetajetRepository;

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
    private final ProjektiDetajetRepository projektidetajetRepository;

    @PersistenceContext
    private EntityManager entityManager;

    public ProjektiService(
            ProjektiRepository projektiRepository,
            MenaxheriRepository menaxheriRepository,
            PuntoriRepository puntoriRepository,
            MaterialiRepository materialiRepository,
            ProjektiMaterialiRepository projektiMaterialiRepository,
            ProjektiPrezencaMenaxheretRepository prezencaMenaxheretRepository,
            ProjektiPrezencaPuntoretRepository prezencaPuntoretRepository,
            ProjektiDetajetRepository projektidetajetRepository
    ) {
        this.projektiRepository = projektiRepository;
        this.menaxheriRepository = menaxheriRepository;
        this.puntoriRepository = puntoriRepository;
        this.materialiRepository = materialiRepository;
        this.projektiMaterialiRepository = projektiMaterialiRepository;
        this.prezencaMenaxheretRepository = prezencaMenaxheretRepository;
        this.prezencaPuntoretRepository = prezencaPuntoretRepository;
        this.projektidetajetRepository = projektidetajetRepository;
    }

    @Transactional
    public ProjektiResponse createProjekti(
            CreateProjektiRequest request,
            Long idMenaxherit
    ) {

        // 1. Validate the manager creating the project
        menaxheri krijuesi = menaxheriRepository.findById(idMenaxherit)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Menaxheri qe po krijon projektin nuk ekziston."
                        )
                );

        // 2. Validate project details
        if (request.detajet() == null) {
            throw new IllegalArgumentException(
                    "Detajet e projektit jane te detyrueshme."
            );
        }

        if (request.detajet().m2() == null) {
            throw new IllegalArgumentException(
                    "Siperfaqja m2 eshte e detyrueshme."
            );
        }

        if (request.detajet().pagesa() == null) {
            throw new IllegalArgumentException(
                    "Pagesa eshte e detyrueshme."
            );
        }

        if (request.detajet().m2().compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException(
                    "Siperfaqja m2 nuk mund te jete negative."
            );
        }

        if (request.detajet().pagesa().compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException(
                    "Pagesa nuk mund te jete negative."
            );
        }

        // 3. Create project
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

        // 4. Save project and retrieve database-generated values
        projekti = projektiRepository.saveAndFlush(projekti);

        entityManager.refresh(projekti);

        // 5. Create project details
        projektidetajet detajet = new projektidetajet();

        detajet.setProjekti(projekti);
        detajet.setM2(request.detajet().m2());
        detajet.setPagesa(request.detajet().pagesa());
        detajet.setPershkrimi(request.detajet().pershkrimi());

        projektidetajetRepository.save(detajet);

        // 6. Assign managers
        // The manager creating the project is always assigned.
        // Additional managers are taken from the request.

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

        // 7. Assign workers
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

        // 8. Add materials
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

                // Preserve the material price at assignment time
                projektiMateriali.setCmimiNeMomentinEPerdorimit(
                        materiali.getCmimi()
                );

                materialet.add(projektiMateriali);
            }

            projektiMaterialiRepository.saveAll(materialet);
        }

        // 9. Return created project
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
