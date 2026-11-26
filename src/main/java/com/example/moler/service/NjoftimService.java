package com.example.moler.service;

import com.example.moler.dto.Notification.NjoftimResponse;
import com.example.moler.dto.Notification.NjoftimetFaqeResponse;
import com.example.moler.exception.ResourceNotFoundException;
import com.example.moler.model.Njoftim;
import com.example.moler.repository.NjoftimRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Map;

/**
 * The logged-in user's notifications tab: list, unread count, mark as read, delete.
 * Every method works only on the caller's own notifications.
 */
@Service
public class NjoftimService {

    private static final Logger log = LoggerFactory.getLogger(NjoftimService.class);

    private static final int MADHESIA_MAX = 100;

    private final NjoftimRepository njoftimRepository;
    private final ObjectMapper objectMapper;

    public NjoftimService(NjoftimRepository njoftimRepository, ObjectMapper objectMapper) {
        this.njoftimRepository = njoftimRepository;
        this.objectMapper = objectMapper;
    }

    /** Logged-in user taken from the JWT. */
    public record Perdoruesi(Long id, boolean eshteMenaxher) {}

    @Transactional(readOnly = true)
    public NjoftimetFaqeResponse listo(Perdoruesi p, int faqja, int madhesia) {

        if (faqja < 0) {
            throw new IllegalArgumentException("Faqja nuk mund te jete negative");
        }

        if (madhesia < 1 || madhesia > MADHESIA_MAX) {
            throw new IllegalArgumentException("Madhesia duhet te jete nga 1 deri ne " + MADHESIA_MAX);
        }

        // Newest first; id breaks ties for notifications created in the same instant
        PageRequest kerkesa = PageRequest.of(
                faqja, madhesia,
                Sort.by(Sort.Order.desc("dataKrijimit"), Sort.Order.desc("id")));

        Page<Njoftim> rezultati = p.eshteMenaxher()
                ? njoftimRepository.findByMenaxheri_Id(p.id(), kerkesa)
                : njoftimRepository.findByPuntori_Id(p.id(), kerkesa);

        return new NjoftimetFaqeResponse(
                rezultati.getContent().stream().map(this::toResponse).toList(),
                rezultati.getNumber(),
                rezultati.getSize(),
                rezultati.getTotalElements(),
                rezultati.hasNext()
        );
    }

    @Transactional(readOnly = true)
    public long numriTePalexuarave(Perdoruesi p) {
        return p.eshteMenaxher()
                ? njoftimRepository.countByMenaxheri_IdAndEshteLexuarFalse(p.id())
                : njoftimRepository.countByPuntori_IdAndEshteLexuarFalse(p.id());
    }

    @Transactional
    public NjoftimResponse shenoSiTeLexuar(Perdoruesi p, Long id) {

        Njoftim n = gjejTimin(p, id);

        // Keep the first read time if it was already read
        if (!n.isEshteLexuar()) {
            n.setEshteLexuar(true);
            n.setDataLeximit(LocalDateTime.now(ZoneOffset.UTC));
        }

        return toResponse(n);
    }

    @Transactional
    public int shenoTeGjithaSiTeLexuara(Perdoruesi p) {
        LocalDateTime tani = LocalDateTime.now(ZoneOffset.UTC);

        return p.eshteMenaxher()
                ? njoftimRepository.shenoTeGjithaSiTeLexuaraMenaxheri(p.id(), tani)
                : njoftimRepository.shenoTeGjithaSiTeLexuaraPuntori(p.id(), tani);
    }

    @Transactional
    public void fshi(Perdoruesi p, Long id) {
        njoftimRepository.delete(gjejTimin(p, id));
    }

    @Transactional
    public int fshiTeGjitha(Perdoruesi p) {
        return p.eshteMenaxher()
                ? njoftimRepository.fshiTeGjithaMenaxheri(p.id())
                : njoftimRepository.fshiTeGjithaPuntori(p.id());
    }

    /** Someone else's notification is reported as not found, never as forbidden. */
    private Njoftim gjejTimin(Perdoruesi p, Long id) {
        return (p.eshteMenaxher()
                ? njoftimRepository.findByIdAndMenaxheri_Id(id, p.id())
                : njoftimRepository.findByIdAndPuntori_Id(id, p.id()))
                .orElseThrow(() -> new ResourceNotFoundException("Njoftimi nuk u gjet"));
    }

    private NjoftimResponse toResponse(Njoftim n) {
        return new NjoftimResponse(
                n.getId(),
                n.getTipi(),
                n.getTitulli(),
                n.getMesazhi(),
                lexoData(n),
                n.isEshteLexuar(),
                n.getDataLeximit(),
                n.getDataKrijimit()
        );
    }

    private Map<String, Object> lexoData(Njoftim n) {

        if (n.getDataJson() == null || n.getDataJson().isBlank()) {
            return Map.of();
        }

        try {
            return objectMapper.readValue(n.getDataJson(), new TypeReference<Map<String, Object>>() {});
        } catch (Exception e) {
            // A broken row should not break the whole list
            log.warn("Invalid data_json in notification {}: {}", n.getId(), e.getMessage());
            return Map.of();
        }
    }
}
