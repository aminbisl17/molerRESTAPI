package com.example.moler.service;

import com.example.moler.dto.Klienti.CreateKlientiRequest;
import com.example.moler.dto.Klienti.KlientiResponse;
import com.example.moler.dto.Klienti.UpdateKlientiRequest;
import com.example.moler.dto.Projekti.ProjektiResponse;
import com.example.moler.exception.ResourceNotFoundException;
import com.example.moler.model.klienti;
import com.example.moler.repository.KlientiRepository;
import com.example.moler.repository.ProjektiRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class KlientiService {

    private final KlientiRepository klientiRepository;
    private final ProjektiRepository projektiRepository;

    public KlientiService(
            KlientiRepository klientiRepository,
            ProjektiRepository projektiRepository
    ) {
        this.klientiRepository = klientiRepository;
        this.projektiRepository = projektiRepository;
    }

    @Transactional
    public KlientiResponse createKlienti(CreateKlientiRequest request) {

        String emri = required(request.emri(), "Emri i klientit", 100);
        String mbiemri = required(request.mbiemri(), "Mbiemri i klientit", 100);
        String lokacioni = required(request.lokacioni(), "Lokacioni i klientit", 255);
        String numriITelefonit =
                optional(request.numriITelefonit(), "Numri i telefonit", 20);

        // Duplicate checks (null id = new client)
        checkDuplicates(emri, mbiemri, lokacioni, numriITelefonit, null);

        klienti k = new klienti();

        k.setEmri(emri);
        k.setMbiemri(mbiemri);
        k.setLokacioni(lokacioni);
        k.setNumriITelefonit(numriITelefonit);

        // Flush here so a constraint violation (e.g. concurrent insert)
        // is raised inside this call and handled by GlobalExceptionHandler
        return KlientiResponse.from(klientiRepository.saveAndFlush(k));
    }

    @Transactional(readOnly = true)
    public List<KlientiResponse> getAllKlientet() {

        return klientiRepository.findAll()
                .stream()
                .map(KlientiResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public KlientiResponse getKlientiById(Long id) {
        return KlientiResponse.from(findKlienti(id));
    }

    @Transactional(readOnly = true)
    public List<ProjektiResponse> getProjektetEKlientit(Long id) {

        findKlienti(id);

        return projektiRepository.findByKlienti_Id(id)
                .stream()
                .map(ProjektiService::toProjektiResponse)
                .toList();
    }

    @Transactional
    public KlientiResponse updateKlienti(Long id, UpdateKlientiRequest request) {

        klienti k = findKlienti(id);

        // Resolve the final values first; fields left null keep their current value
        String emri = request.emri() != null
                ? required(request.emri(), "Emri i klientit", 100)
                : k.getEmri();

        String mbiemri = request.mbiemri() != null
                ? required(request.mbiemri(), "Mbiemri i klientit", 100)
                : k.getMbiemri();

        String lokacioni = request.lokacioni() != null
                ? required(request.lokacioni(), "Lokacioni i klientit", 255)
                : k.getLokacioni();

        String numriITelefonit = request.numriITelefonit() != null
                ? optional(request.numriITelefonit(), "Numri i telefonit", 20)
                : k.getNumriITelefonit();

        // Check before modifying the managed entity, otherwise Hibernate's
        // auto-flush before the check query would hit the DB constraint first.
        // The current client is excluded so it can keep its own phone number.
        checkDuplicates(emri, mbiemri, lokacioni, numriITelefonit, id);

        k.setEmri(emri);
        k.setMbiemri(mbiemri);
        k.setLokacioni(lokacioni);
        k.setNumriITelefonit(numriITelefonit);

        return KlientiResponse.from(klientiRepository.saveAndFlush(k));
    }

    // excludeId = null when creating, the client's own ID when updating
    private void checkDuplicates(
            String emri,
            String mbiemri,
            String lokacioni,
            String numriITelefonit,
            Long excludeId
    ) {

        if (numriITelefonit != null) {

            boolean phoneExists = excludeId == null
                    ? klientiRepository.existsByNumriITelefonit(numriITelefonit)
                    : klientiRepository.existsByNumriITelefonitAndIdNot(
                            numriITelefonit, excludeId
                    );

            if (phoneExists) {
                throw new IllegalArgumentException(
                        "Ky numër telefoni ekziston tashmë."
                );
            }

            // A unique phone number also makes the full detail set unique
            return;
        }

        // No phone number: UQ_klienti_unique_details still applies
        boolean detailsExist = excludeId == null
                ? klientiRepository
                        .existsByEmriAndMbiemriAndLokacioniAndNumriITelefonitIsNull(
                                emri, mbiemri, lokacioni
                        )
                : klientiRepository
                        .existsByEmriAndMbiemriAndLokacioniAndNumriITelefonitIsNullAndIdNot(
                                emri, mbiemri, lokacioni, excludeId
                        );

        if (detailsExist) {
            throw new IllegalArgumentException(
                    "Ky klient me keto te dhena ekziston tashmë."
            );
        }
    }

    @Transactional
    public void deleteKlienti(Long id) {

        klienti k = findKlienti(id);

        // Projects reference the client through a foreign key
        if (projektiRepository.existsByKlienti_Id(id)) {
            throw new IllegalArgumentException(
                    "Klienti ka projekte te lidhura dhe nuk mund te fshihet."
            );
        }

        klientiRepository.delete(k);

        // Flush so a FK violation is raised here and handled by GlobalExceptionHandler
        klientiRepository.flush();
    }

    // Used by ProjektiService when assigning a client to a project
    @Transactional(readOnly = true)
    public klienti findKlienti(Long id) {

        if (id == null) {
            throw new IllegalArgumentException(
                    "ID e klientit nuk mund te jete bosh."
            );
        }

        return klientiRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Klienti me ID " + id + " nuk ekziston."
                ));
    }

    private String required(String value, String field, int maxLength) {

        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " eshte i detyrueshem.");
        }

        return checkLength(value.trim(), field, maxLength);
    }

    private String optional(String value, String field, int maxLength) {

        if (value == null || value.isBlank()) {
            return null;
        }

        return checkLength(value.trim(), field, maxLength);
    }

    private String checkLength(String value, String field, int maxLength) {

        if (value.length() > maxLength) {
            throw new IllegalArgumentException(
                    field + " nuk mund te kaloje " + maxLength + " karaktere."
            );
        }

        return value;
    }
}
