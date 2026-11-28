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

        klienti k = new klienti();

        k.setEmri(required(request.emri(), "Emri i klientit", 100));
        k.setMbiemri(required(request.mbiemri(), "Mbiemri i klientit", 100));
        k.setLokacioni(required(request.lokacioni(), "Lokacioni i klientit", 255));
        k.setNumriITelefonit(optional(request.numriITelefonit(), "Numri i telefonit", 20));

        return KlientiResponse.from(klientiRepository.save(k));
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

        if (request.emri() != null) {
            k.setEmri(required(request.emri(), "Emri i klientit", 100));
        }

        if (request.mbiemri() != null) {
            k.setMbiemri(required(request.mbiemri(), "Mbiemri i klientit", 100));
        }

        if (request.lokacioni() != null) {
            k.setLokacioni(required(request.lokacioni(), "Lokacioni i klientit", 255));
        }

        if (request.numriITelefonit() != null) {
            k.setNumriITelefonit(optional(request.numriITelefonit(), "Numri i telefonit", 20));
        }

        return KlientiResponse.from(klientiRepository.save(k));
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
