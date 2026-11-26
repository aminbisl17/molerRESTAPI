package com.example.moler.service;

import com.example.moler.dto.Notification.DergoNjoftimResponse;
import com.example.moler.dto.Notification.Marresi;
import com.example.moler.exception.ResourceNotFoundException;
import com.example.moler.model.Njoftim;
import com.example.moler.model.PushToken;
import com.example.moler.model.menaxheri;
import com.example.moler.model.puntori;
import com.example.moler.repository.MenaxheriRepository;
import com.example.moler.repository.NjoftimRepository;
import com.example.moler.repository.PuntoriRepository;
import com.example.moler.repository.PushTokenRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;

import tools.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Sends a notification to one or more workers / managers:
 * saves one row per recipient in "njoftimet" (the app's notifications tab)
 * and pushes it to all of their devices.
 * When called inside a transaction, the push is sent only after the commit.
 */
@Service
public class NotificationService {

    private static final int MAX_MARRES = 1000;
    private static final int MAX_TITULLI = 100;
    private static final int MAX_MESAZHI = 500;
    private static final int MAX_DATA_JSON = 1000;

    private final PushTokenRepository pushTokenRepository;
    private final NjoftimRepository njoftimRepository;
    private final PuntoriRepository puntoriRepository;
    private final MenaxheriRepository menaxheriRepository;
    private final ExpoPushSender expoPushSender;
    private final ObjectMapper objectMapper;

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    // Joins an existing transaction or starts one
    private final TransactionTemplate transaksioni;

    // Always its own transaction (used after the caller's commit)
    private final TransactionTemplate transaksioniIRi;

    public NotificationService(
            PushTokenRepository pushTokenRepository,
            NjoftimRepository njoftimRepository,
            PuntoriRepository puntoriRepository,
            MenaxheriRepository menaxheriRepository,
            ExpoPushSender expoPushSender,
            ObjectMapper objectMapper,
            PlatformTransactionManager transactionManager
    ) {
        this.pushTokenRepository = pushTokenRepository;
        this.njoftimRepository = njoftimRepository;
        this.puntoriRepository = puntoriRepository;
        this.menaxheriRepository = menaxheriRepository;
        this.expoPushSender = expoPushSender;
        this.objectMapper = objectMapper;

        this.transaksioni = new TransactionTemplate(transactionManager);

        this.transaksioniIRi = new TransactionTemplate(transactionManager);
        this.transaksioniIRi.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    /**
     * Used by the endpoint: also checks that every recipient exists.
     * Not transactional on purpose, so saving and pushing happen right away.
     */
    public DergoNjoftimResponse dergoTeMarresTeVerifikuar(
            List<Marresi> marresit,
            String titulli,
            String mesazhi
    ) {
        Marresit m = normalizo(marresit);

        List<Long> puntoretQeMungojne = mungojne(
                m.puntoret(),
                puntoriRepository.findAllById(m.puntoret()).stream().map(puntori::getId).toList());

        List<Long> menaxheretQeMungojne = mungojne(
                m.menaxheret(),
                menaxheriRepository.findAllById(m.menaxheret()).stream().map(menaxheri::getId).toList());

        if (!puntoretQeMungojne.isEmpty() || !menaxheretQeMungojne.isEmpty()) {
            throw new ResourceNotFoundException(
                    "Marresit nuk ekzistojne - puntoret: " + puntoretQeMungojne
                            + ", menaxheret: " + menaxheretQeMungojne);
        }

        Njoftimi njoftimi = pergatit(m, null, titulli, mesazhi, null);

        int pajisjet = transaksioni.execute(status -> ruaj(njoftimi)).size();

        // Saved and committed: push now
        List<ExpoPushSender.Mesazh> mesazhet = njoftimi.mesazhet();
        if (!mesazhet.isEmpty()) {
            expoPushSender.dergo(mesazhet, ExpoPushSender.Tenant.aktual());
        }

        return new DergoNjoftimResponse(m.puntoret().size() + m.menaxheret().size(), pajisjet);
    }

    /**
     * Saves the notification for every recipient and pushes it to their devices.
     * {@code data} may contain "tipi"; it is stored in its own column.
     *
     * Inside a transaction (e.g. verifying attendance) nothing happens until it commits;
     * then the rows are saved in their own transaction, so a notification problem
     * can never roll back or fail the caller's action. Invalid input still throws here.
     */
    public void dergo(
            List<Marresi> marresit,
            String titulli,
            String mesazhi,
            Map<String, Object> data
    ) {
        Map<String, Object> teDhenat = data == null ? new LinkedHashMap<>() : new LinkedHashMap<>(data);
        Object tipi = teDhenat.remove("tipi");

        Njoftimi njoftimi = pergatit(
                normalizo(marresit),
                tipi == null ? null : tipi.toString(),
                titulli,
                mesazhi,
                teDhenat
        );

        ExpoPushSender.Tenant tenant = ExpoPushSender.Tenant.aktual();

        Runnable ruajDheDergo = () -> {
            try {
                transaksioniIRi.execute(status -> ruaj(njoftimi));

                if (!njoftimi.mesazhet().isEmpty()) {
                    expoPushSender.dergo(njoftimi.mesazhet(), tenant);
                }
            } catch (Exception e) {
                log.warn("Could not save/send notification '{}': {}", njoftimi.titulli(), e.getMessage());
            }
        };

        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    ruajDheDergo.run();
                }
            });
        } else {
            ruajDheDergo.run();
        }
    }

    /** Validated notification, ready to save; {@code mesazhet} is filled by {@link #ruaj}. */
    private record Njoftimi(
            Marresit marresit,
            String tipi,
            String titulli,
            String mesazhi,
            Map<String, Object> data,
            String dataJson,
            List<ExpoPushSender.Mesazh> mesazhet
    ) {}

    private Njoftimi pergatit(
            Marresit m,
            String tipi,
            String titulli,
            String mesazhi,
            Map<String, Object> data
    ) {
        String t = validoTekstin(titulli, "Titulli", MAX_TITULLI);
        String b = validoTekstin(mesazhi, "Mesazhi", MAX_MESAZHI);

        Map<String, Object> teDhenat = data == null ? Map.of() : data;
        String dataJson = teDhenat.isEmpty() ? null : objectMapper.writeValueAsString(teDhenat);

        if (dataJson != null && dataJson.length() > MAX_DATA_JSON) {
            throw new IllegalArgumentException("Te dhenat e njoftimit jane shume te gjata");
        }

        return new Njoftimi(m, tipi, t, b, teDhenat, dataJson, new ArrayList<>());
    }

    /**
     * Saves one row per recipient and builds one push per device.
     * Must run inside a transaction; returns the pushes (also kept in the record).
     */
    private List<ExpoPushSender.Mesazh> ruaj(Njoftimi njoftimi) {

        Marresit m = njoftimi.marresit();
        String tipi = njoftimi.tipi();
        String t = njoftimi.titulli();
        String b = njoftimi.mesazhi();
        String dataJson = njoftimi.dataJson();
        Map<String, Object> teDhenat = njoftimi.data();

        // 1. One row per recipient
        List<Njoftim> rreshtat = new ArrayList<>();

        for (Long id : m.puntoret()) {
            Njoftim n = krijoNjoftim(tipi, t, b, dataJson);
            n.setMarresPuntori(puntoriRepository.getReferenceById(id));
            rreshtat.add(n);
        }

        for (Long id : m.menaxheret()) {
            Njoftim n = krijoNjoftim(tipi, t, b, dataJson);
            n.setMarresMenaxheri(menaxheriRepository.getReferenceById(id));
            rreshtat.add(n);
        }

        List<Njoftim> ruajtura = njoftimRepository.saveAll(rreshtat);

        // 2. Devices of every recipient, loaded now while the tenant is known
        Map<String, List<String>> tokenatSipasMarresit = new HashMap<>();

        if (!m.puntoret().isEmpty()) {
            for (PushToken pt : pushTokenRepository.findByPuntori_IdIn(m.puntoret())) {
                tokenatSipasMarresit
                        .computeIfAbsent("p" + pt.getPuntori().getId(), k -> new ArrayList<>())
                        .add(pt.getExpoToken());
            }
        }

        if (!m.menaxheret().isEmpty()) {
            for (PushToken pt : pushTokenRepository.findByMenaxheri_IdIn(m.menaxheret())) {
                tokenatSipasMarresit
                        .computeIfAbsent("m" + pt.getMenaxheri().getId(), k -> new ArrayList<>())
                        .add(pt.getExpoToken());
            }
        }

        // 3. One push per device, carrying the recipient's own notification id
        List<ExpoPushSender.Mesazh> mesazhet = njoftimi.mesazhet();
        mesazhet.clear();

        for (Njoftim n : ruajtura) {
            String celesi = n.getPuntori() != null
                    ? "p" + n.getPuntori().getId()
                    : "m" + n.getMenaxheri().getId();

            List<String> tokenat = tokenatSipasMarresit.getOrDefault(celesi, List.of());

            if (tokenat.isEmpty()) {
                continue;
            }

            Map<String, Object> pushData = new LinkedHashMap<>(teDhenat);
            if (tipi != null) {
                pushData.put("tipi", tipi);
            }
            pushData.put("idNjoftimit", n.getId());

            for (String token : tokenat) {
                mesazhet.add(new ExpoPushSender.Mesazh(token, t, b, pushData));
            }
        }

        return mesazhet;
    }

    private Njoftim krijoNjoftim(String tipi, String titulli, String mesazhi, String dataJson) {
        Njoftim n = new Njoftim();
        n.setTipi(tipi);
        n.setTitulli(titulli);
        n.setMesazhi(mesazhi);
        n.setDataJson(dataJson);
        return n;
    }

    private record Marresit(List<Long> puntoret, List<Long> menaxheret) {}

    private Marresit normalizo(List<Marresi> marresit) {

        if (marresit == null || marresit.isEmpty()) {
            throw new IllegalArgumentException("Duhet te zgjidhni te pakten nje marres");
        }

        if (marresit.size() > MAX_MARRES) {
            throw new IllegalArgumentException("Maksimumi " + MAX_MARRES + " marres per njoftim");
        }

        Set<Long> puntoret = new LinkedHashSet<>();
        Set<Long> menaxheret = new LinkedHashSet<>();

        for (Marresi marresi : marresit) {

            if (marresi == null || marresi.id() == null || marresi.roli() == null) {
                throw new IllegalArgumentException("Cdo marres duhet te kete roli dhe id");
            }

            switch (marresi.roli().trim().toLowerCase()) {
                case "puntor", "puntori", "p" -> puntoret.add(marresi.id());
                case "menaxher", "menaxheri", "m" -> menaxheret.add(marresi.id());
                default -> throw new IllegalArgumentException(
                        "Roli duhet te jete puntor ose menaxher: " + marresi.roli());
            }
        }

        return new Marresit(new ArrayList<>(puntoret), new ArrayList<>(menaxheret));
    }

    private List<Long> mungojne(List<Long> kerkuara, List<Long> gjetura) {
        Set<Long> ekzistojne = new HashSet<>(gjetura);
        return kerkuara.stream().filter(id -> !ekzistojne.contains(id)).toList();
    }

    private String validoTekstin(String teksti, String emri, int max) {

        if (teksti == null || teksti.isBlank()) {
            throw new IllegalArgumentException(emri + " eshte i detyrueshem");
        }

        String t = teksti.trim();

        if (t.length() > max) {
            throw new IllegalArgumentException(emri + " mund te kete maksimumi " + max + " karaktere");
        }

        return t;
    }
}
