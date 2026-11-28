
package com.example.moler.service;

import com.example.moler.dto.Klienti.KlientiResponse;
import com.example.moler.dto.Projekti.CreateProjektiRequest;
import com.example.moler.dto.Projekti.ProjektiDetailsResponse;
import com.example.moler.dto.Projekti.ProjektiFormDataResponse;
import com.example.moler.dto.Projekti.ProjektiResponse;
import com.example.moler.dto.Projekti.UpdateProjektiRequest;
import com.example.moler.exception.ResourceNotFoundException;

import com.example.moler.model.projekti;
import com.example.moler.model.klienti;
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
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class ProjektiService {

    private static final Set<String> STATUSET = Set.of("w", "e", "cm", "c");
    private final ProjektiRepository projektiRepository;
    private final MenaxheriRepository menaxheriRepository;
    private final PuntoriRepository puntoriRepository;
    private final MaterialiRepository materialiRepository;
    private final ProjektiMaterialiRepository projektiMaterialiRepository;
    private final ProjektiPrezencaMenaxheretRepository prezencaMenaxheretRepository;
    private final ProjektiPrezencaPuntoretRepository prezencaPuntoretRepository;
    private final ProjektiDetajetRepository projektidetajetRepository;
    private final KlientiService klientiService;

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
            ProjektiDetajetRepository projektidetajetRepository,
            KlientiService klientiService
    ) {
        this.projektiRepository = projektiRepository;
        this.menaxheriRepository = menaxheriRepository;
        this.puntoriRepository = puntoriRepository;
        this.materialiRepository = materialiRepository;
        this.projektiMaterialiRepository = projektiMaterialiRepository;
        this.prezencaMenaxheretRepository = prezencaMenaxheretRepository;
        this.prezencaPuntoretRepository = prezencaPuntoretRepository;
        this.projektidetajetRepository = projektidetajetRepository;
        this.klientiService = klientiService;
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

        // 3. Validate client (required on creation)
        if (request.idKlientit() == null) {
            throw new IllegalArgumentException(
                    "Klienti eshte i detyrueshem per krijimin e projektit."
            );
        }

        klienti klienti = klientiService.findKlienti(request.idKlientit());

        // 4. Create project
        projekti projekti = new projekti();

        projekti.setKlienti(klienti);

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
        return toProjektiResponse(projekti);
    }

    // Shared mapping for every endpoint that returns ProjektiResponse
    public static ProjektiResponse toProjektiResponse(projekti p) {
        return new ProjektiResponse(
                p.getId(),
                p.getEmri(),
                p.getLokacioni(),
                p.getKerkesa(),
                p.getStatusi(),
                p.getPrioriteti(),
                p.getAmbienti(),
                p.getDataFillimitTePlanifikuar(),
                p.getDataPerfundimitTePlanifikuar(),
                p.getDataKrijimit(),
                KlientiResponse.from(p.getKlienti())
        );
    }
@Transactional(readOnly = true)
public ProjektiDetailsResponse getProjektiById(Long id) {

    projekti projekti = projektiRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException(
                    "Projekti me ID " + id + " nuk ekziston."
            ));

    // Menaxheret: ID, emri, mbiemri
    List<ProjektiDetailsResponse.MenaxherResponse> menaxheret =
            prezencaMenaxheretRepository.findByProjekti_Id(id)
                    .stream()
                    .map(pm -> new ProjektiDetailsResponse.MenaxherResponse(
                            pm.getMenaxheri().getId(),
                            pm.getMenaxheri().getEmri(),
                            pm.getMenaxheri().getMbiemri()
                    ))
                    .toList();

    // Puntoret: ID, emri, mbiemri, pozita
    List<ProjektiDetailsResponse.PuntorResponse> puntoret =
            prezencaPuntoretRepository.findByProjekti_Id(id)
                    .stream()
                    .map(pp -> new ProjektiDetailsResponse.PuntorResponse(
                            pp.getPuntori().getId(),
                            pp.getPuntori().getEmri(),
                            pp.getPuntori().getMbiemri(),
                            pp.getPuntori().getPozita()
                    ))
                    .toList();

    // Materialet
    List<ProjektiDetailsResponse.MaterialResponse> materialet =
            projektiMaterialiRepository.findByProjekti_Id(id)
                    .stream()
                    .map(pm -> new ProjektiDetailsResponse.MaterialResponse(
                            pm.getMateriali().getId(),
                            pm.getMateriali().getEmri(),
                            pm.getMateriali().getKategoria(),
                            pm.getMateriali().getCmimi(),
                            pm.getSasia(),
                            pm.getCmimiNeMomentinEPerdorimit()
                    ))
                    .toList();

    // Detajet e projektit
    projektidetajet d = projektidetajetRepository.findByProjektiId(id)
            .orElseThrow(() -> new ResourceNotFoundException(
                    "Detajet e projektit me ID " + id + " nuk ekzistojne."
            ));

    ProjektiDetailsResponse.DetajetResponse detajet =
            new ProjektiDetailsResponse.DetajetResponse(
                    d.getM2(),
                    d.getPagesa(),
                    d.getPershkrimi(),
                    d.getDitaFillimit(),
                    d.getDitaAnulimit(),
                    d.getDitaPerfundimit(),
                    d.getPershkrimiAnulimit()
            );

    return new ProjektiDetailsResponse(
            menaxheret,
            puntoret,
            materialet,
            detajet,
            KlientiResponse.from(projekti.getKlienti())
    );
}

@Transactional(readOnly = true)
public List<ProjektiResponse> getAllProjektet() {

    return projektiRepository.findAll()
            .stream()
            .map(ProjektiService::toProjektiResponse)
            .toList();
}


@Transactional(readOnly = true)
public ProjektiFormDataResponse getProjektiFormData() {

    // All managers: ID, name, surname
    List<ProjektiFormDataResponse.MenaxherResponse> menaxheret =
            menaxheriRepository.findAll()
                    .stream()
                    .map(m -> new ProjektiFormDataResponse.MenaxherResponse(
                            m.getId(),
                            m.getEmri(),
                            m.getMbiemri()
                    ))
                    .toList();

    // All workers: ID, name, surname, position
    List<ProjektiFormDataResponse.PuntorResponse> puntoret =
            puntoriRepository.findAll()
                    .stream()
                    .map(p -> new ProjektiFormDataResponse.PuntorResponse(
                            p.getId(),
                            p.getEmri(),
                            p.getMbiemri(),
                            p.getPozita()
                    ))
                    .toList();

    // All materials: all material fields
    List<ProjektiFormDataResponse.MaterialResponse> materialet =
            materialiRepository.findAll()
                    .stream()
                    .map(m -> new ProjektiFormDataResponse.MaterialResponse(
                            m.getId(),
                            m.getEmri(),
                            m.getCmimi(),
                            m.getKategoria()
                    ))
                    .toList();

    // All clients, so one can be picked when creating a project
    List<KlientiResponse> klientet =
            klientiService.getAllKlientet();

    return new ProjektiFormDataResponse(
            menaxheret,
            puntoret,
            materialet,
            klientet
    );
}

@Transactional
public ProjektiResponse updateProjekti(Long id, UpdateProjektiRequest request) {

    projekti p = projektiRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException(
                    "Projekti me ID " + id + " nuk ekziston."
            ));

    projektidetajet detajet = projektidetajetRepository.findByProjektiId(id)
            .orElseThrow(() -> new ResourceNotFoundException(
                    "Detajet e projektit me ID " + id + " nuk ekzistojne."
            ));

    // 1. Basic fields
    if (request.emri() != null) p.setEmri(request.emri());
    if (request.lokacioni() != null) p.setLokacioni(request.lokacioni());
    if (request.kerkesa() != null) p.setKerkesa(request.kerkesa());
    if (request.prioriteti() != null) p.setPrioriteti(request.prioriteti());
    if (request.ambienti() != null) p.setAmbienti(request.ambienti());

    if (request.dataFillimitTePlanifikuar() != null) {
        p.setDataFillimitTePlanifikuar(request.dataFillimitTePlanifikuar());
    }
    if (request.dataPerfundimitTePlanifikuar() != null) {
        p.setDataPerfundimitTePlanifikuar(request.dataPerfundimitTePlanifikuar());
    }

    if (p.getDataFillimitTePlanifikuar() != null
            && p.getDataPerfundimitTePlanifikuar() != null
            && p.getDataPerfundimitTePlanifikuar()
                    .isBefore(p.getDataFillimitTePlanifikuar())) {
        throw new IllegalArgumentException(
                "Data e perfundimit nuk mund te jete para dates se fillimit."
        );
    }

    // Client: assign / change with idKlientit, remove with hiqKlientin = true
    if (Boolean.TRUE.equals(request.hiqKlientin())) {
        if (request.idKlientit() != null) {
            throw new IllegalArgumentException(
                    "Nuk mund te caktohet dhe te hiqet klienti njekohesisht."
            );
        }
        p.setKlienti(null);
    } else if (request.idKlientit() != null) {
        p.setKlienti(klientiService.findKlienti(request.idKlientit()));
    }

    // 2. Status + automatic dates
    if (request.statusi() != null) {
        String newStatus = request.statusi().trim().toLowerCase();

        if (!STATUSET.contains(newStatus)) {
            throw new IllegalArgumentException(
                    "Statusi i projektit nuk eshte i vlefshem."
            );
        }

        String oldStatus = p.getStatusi() == null ? "" : p.getStatusi().trim();

        if (!newStatus.equals(oldStatus)) {
            p.setStatusi(newStatus);
            applyStatusDates(detajet, newStatus);
        }
    }

    // Cancellation description is optional and only stored on cancelled projects
    String currentStatus = p.getStatusi() == null ? "" : p.getStatusi().trim();
    if ("c".equals(currentStatus) && request.pershkrimiAnulimit() != null) {
        String text = request.pershkrimiAnulimit().trim();
        detajet.setPershkrimiAnulimit(text.isEmpty() ? null : text);
    }

    // 3. Project details (m2, pagesa, pershkrimi)
    if (request.detajet() != null) {
        UpdateProjektiRequest.DetajetRequest d = request.detajet();

        if (d.m2() != null) {
            if (d.m2().compareTo(BigDecimal.ZERO) < 0) {
                throw new IllegalArgumentException(
                        "Siperfaqja m2 nuk mund te jete negative."
                );
            }
            detajet.setM2(d.m2());
        }

        if (d.pagesa() != null) {
            if (d.pagesa().compareTo(BigDecimal.ZERO) < 0) {
                throw new IllegalArgumentException(
                        "Pagesa nuk mund te jete negative."
                );
            }
            detajet.setPagesa(d.pagesa());
        }

        if (d.pershkrimi() != null) detajet.setPershkrimi(d.pershkrimi());
    }

    projektiRepository.save(p);
    projektidetajetRepository.save(detajet);

    // 4. Managers (replace the whole set when provided)
    if (request.menaxheret() != null) {
        prezencaMenaxheretRepository.deleteAll(
                prezencaMenaxheretRepository.findByProjekti_Id(id)
        );
        entityManager.flush();

        List<ProjektiPrezencaMenaxheret> assignments = new ArrayList<>();

        for (Long managerId : new LinkedHashSet<>(request.menaxheret())) {
            if (managerId == null) {
                throw new IllegalArgumentException(
                        "ID e menaxherit nuk mund te jete bosh."
                );
            }

            menaxheri manager = menaxheriRepository.findById(managerId)
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Menaxheri me ID " + managerId + " nuk ekziston."
                    ));

            ProjektiPrezencaMenaxheret a = new ProjektiPrezencaMenaxheret();
            a.setProjekti(p);
            a.setMenaxheri(manager);
            assignments.add(a);
        }

        prezencaMenaxheretRepository.saveAll(assignments);
    }

    // 5. Workers (replace the whole set when provided)
    if (request.puntoret() != null) {
        prezencaPuntoretRepository.deleteAll(
                prezencaPuntoretRepository.findByProjekti_Id(id)
        );
        entityManager.flush();

        List<ProjektiPrezencaPuntoret> assignments = new ArrayList<>();

        for (Long workerId : new LinkedHashSet<>(request.puntoret())) {
            if (workerId == null) {
                throw new IllegalArgumentException(
                        "ID e puntorit nuk mund te jete bosh."
                );
            }

            puntori worker = puntoriRepository.findById(workerId)
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Puntori me ID " + workerId + " nuk ekziston."
                    ));

            ProjektiPrezencaPuntoret a = new ProjektiPrezencaPuntoret();
            a.setProjekti(p);
            a.setPuntori(worker);
            assignments.add(a);
        }

        prezencaPuntoretRepository.saveAll(assignments);
    }

    // 6. Materials (replace the whole set when provided)
    if (request.materialet() != null) {
        List<ProjektiMateriali> existing =
                projektiMaterialiRepository.findByProjekti_Id(id);

        // Remember the price already locked in for materials that stay on the project
        Map<Long, BigDecimal> lockedPrices = new HashMap<>();
        for (ProjektiMateriali pm : existing) {
            lockedPrices.put(
                    pm.getMateriali().getId(),
                    pm.getCmimiNeMomentinEPerdorimit()
            );
        }

        projektiMaterialiRepository.deleteAll(existing);
        entityManager.flush();

        List<ProjektiMateriali> materialet = new ArrayList<>();

        for (UpdateProjektiRequest.MaterialRequest mr : request.materialet()) {
            if (mr.idMaterialit() == null) {
                throw new IllegalArgumentException(
                        "ID e materialit nuk mund te jete bosh."
                );
            }

            if (mr.sasia() == null || mr.sasia().compareTo(BigDecimal.ZERO) <= 0) {
                throw new IllegalArgumentException(
                        "Sasia e materialit duhet te jete me e madhe se zero."
                );
            }

            materiali m = materialiRepository.findById(mr.idMaterialit())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Materiali me ID " + mr.idMaterialit() + " nuk ekziston."
                    ));

            ProjektiMateriali pm = new ProjektiMateriali();
            pm.setProjekti(p);
            pm.setMateriali(m);
            pm.setSasia(mr.sasia());

            // Keep the old locked price if the material was already on the project,
            // otherwise lock the current price
            BigDecimal locked = lockedPrices.get(m.getId());
            pm.setCmimiNeMomentinEPerdorimit(locked != null ? locked : m.getCmimi());

            materialet.add(pm);
        }

        projektiMaterialiRepository.saveAll(materialet);
    }

    // 7. Return updated project
    return toProjektiResponse(p);
}

/**
 * Called only when the status changes. Clears the dates that belong to the
 * old state, then stamps the one for the new state.
 */
private void applyStatusDates(projektidetajet d, String status) {
    LocalDateTime now = LocalDateTime.now();

    d.setDitaPerfundimit(null);
    d.setDitaAnulimit(null);
    d.setPershkrimiAnulimit(null);

    switch (status) {
        case "e" -> d.setDitaFillimit(now);
        case "cm" -> d.setDitaPerfundimit(now);
        case "c" -> d.setDitaAnulimit(now);
        default -> { } // "w": no date
    }
}

}
