package com.example.moler.service;

import com.example.moler.dto.Notification.DergoNjoftimResponse;
import com.example.moler.dto.Notification.Marresi;
import com.example.moler.exception.ResourceNotFoundException;
import com.example.moler.model.menaxheri;
import com.example.moler.model.puntori;
import com.example.moler.repository.MenaxheriRepository;
import com.example.moler.repository.PuntoriRepository;
import com.example.moler.repository.PushTokenRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Sends a push notification to one or more workers / managers.
 * Usable from any service: when called inside a transaction, the push is sent
 * only after the transaction commits.
 */
@Service
public class NotificationService {

    private static final int MAX_MARRES = 1000;
    private static final int MAX_TITULLI = 100;
    private static final int MAX_MESAZHI = 500;

    private final PushTokenRepository pushTokenRepository;
    private final PuntoriRepository puntoriRepository;
    private final MenaxheriRepository menaxheriRepository;
    private final ExpoPushSender expoPushSender;

    public NotificationService(
            PushTokenRepository pushTokenRepository,
            PuntoriRepository puntoriRepository,
            MenaxheriRepository menaxheriRepository,
            ExpoPushSender expoPushSender
    ) {
        this.pushTokenRepository = pushTokenRepository;
        this.puntoriRepository = puntoriRepository;
        this.menaxheriRepository = menaxheriRepository;
        this.expoPushSender = expoPushSender;
    }

    /**
     * Used by the endpoint: also checks that every recipient exists.
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

        int pajisjet = dergo(m, titulli, mesazhi, null);

        return new DergoNjoftimResponse(m.puntoret().size() + m.menaxheret().size(), pajisjet);
    }

    /**
     * Sends to all devices of the given recipients. Recipients without devices are skipped.
     * Returns the number of devices the notification was queued for.
     */
    public int dergo(
            List<Marresi> marresit,
            String titulli,
            String mesazhi,
            Map<String, Object> data
    ) {
        return dergo(normalizo(marresit), titulli, mesazhi, data);
    }

    private int dergo(
            Marresit m,
            String titulli,
            String mesazhi,
            Map<String, Object> data
    ) {
        String t = validoTekstin(titulli, "Titulli", MAX_TITULLI);
        String b = validoTekstin(mesazhi, "Mesazhi", MAX_MESAZHI);

        // Load tokens now, while the request's tenant and transaction are available
        Set<String> tokenat = new LinkedHashSet<>();

        if (!m.puntoret().isEmpty()) {
            pushTokenRepository.findByPuntori_IdIn(m.puntoret())
                    .forEach(pt -> tokenat.add(pt.getExpoToken()));
        }

        if (!m.menaxheret().isEmpty()) {
            pushTokenRepository.findByMenaxheri_IdIn(m.menaxheret())
                    .forEach(pt -> tokenat.add(pt.getExpoToken()));
        }

        if (tokenat.isEmpty()) {
            return 0;
        }

        List<String> lista = new ArrayList<>(tokenat);
        ExpoPushSender.Tenant tenant = ExpoPushSender.Tenant.aktual();

        Runnable dergimi = () -> expoPushSender.dergo(lista, t, b, data, tenant);

        // Inside a transaction: send only once the data is committed
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    dergimi.run();
                }
            });
        } else {
            dergimi.run();
        }

        return lista.size();
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
