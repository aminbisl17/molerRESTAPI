package com.example.moler.service;

import com.example.moler.exception.ResourceNotFoundException;
import com.example.moler.model.PushToken;
import com.example.moler.model.menaxheri;
import com.example.moler.model.puntori;
import com.example.moler.repository.MenaxheriRepository;
import com.example.moler.repository.PuntoriRepository;
import com.example.moler.repository.PushTokenRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneOffset;

@Service
public class PushTokenService {

    private final PushTokenRepository pushTokenRepository;
    private final PuntoriRepository puntoriRepository;
    private final MenaxheriRepository menaxheriRepository;

    public PushTokenService(
            PushTokenRepository pushTokenRepository,
            PuntoriRepository puntoriRepository,
            MenaxheriRepository menaxheriRepository
    ) {
        this.pushTokenRepository = pushTokenRepository;
        this.puntoriRepository = puntoriRepository;
        this.menaxheriRepository = menaxheriRepository;
    }

    /**
     * Saves the device token for the logged-in user. If the token already exists
     * (same phone, other user logged in before) it is moved to this user.
     */
    @Transactional
    public void regjistroToken(String expoToken, String platform, Long idPerdoruesit, boolean eshteMenaxher) {

        String token = validoToken(expoToken);
        String platformaENormalizuar = normalizoPlatformen(platform);

        PushToken pushToken = pushTokenRepository.findByExpoToken(token)
                .orElseGet(() -> {
                    PushToken i_ri = new PushToken();
                    i_ri.setExpoToken(token);
                    return i_ri;
                });

        if (eshteMenaxher) {
            menaxheri menaxheri = menaxheriRepository.findById(idPerdoruesit)
                    .orElseThrow(() -> new ResourceNotFoundException("Menaxheri nuk ekziston"));
            pushToken.setPronarMenaxheri(menaxheri);
        } else {
            puntori puntori = puntoriRepository.findById(idPerdoruesit)
                    .orElseThrow(() -> new ResourceNotFoundException("Puntori nuk ekziston"));
            pushToken.setPronarPuntori(puntori);
        }

        // Keep the stored platform if the app did not send one
        if (platformaENormalizuar != null) {
            pushToken.setPlatform(platformaENormalizuar);
        }

        // Matches the table default SYSUTCDATETIME()
        pushToken.setDataPerditesimit(LocalDateTime.now(ZoneOffset.UTC));

        pushTokenRepository.save(pushToken);
    }

    /**
     * Removes the device token on logout. Only the current owner can remove it;
     * a missing or foreign token is ignored so logout always succeeds.
     */
    @Transactional
    public void fshiToken(String expoToken, Long idPerdoruesit, boolean eshteMenaxher) {

        String token = validoToken(expoToken);

        pushTokenRepository.findByExpoToken(token)
                .filter(t -> eshteMenaxher
                        ? t.getMenaxheri() != null && t.getMenaxheri().getId().equals(idPerdoruesit)
                        : t.getPuntori() != null && t.getPuntori().getId().equals(idPerdoruesit))
                .ifPresent(pushTokenRepository::delete);
    }

    private String validoToken(String expoToken) {

        if (expoToken == null || expoToken.isBlank()) {
            throw new IllegalArgumentException("Tokeni i njoftimeve eshte i detyrueshem");
        }

        String token = expoToken.trim();

        boolean formatValid =
                (token.startsWith("ExponentPushToken[") || token.startsWith("ExpoPushToken["))
                        && token.endsWith("]")
                        && token.length() <= 255;

        if (!formatValid) {
            throw new IllegalArgumentException("Tokeni i njoftimeve nuk eshte valid");
        }

        return token;
    }

    private String normalizoPlatformen(String platform) {

        if (platform == null || platform.isBlank()) {
            return null;
        }

        return switch (platform.trim().toLowerCase()) {
            case "ios", "i" -> "i";
            case "android", "a" -> "a";
            default -> throw new IllegalArgumentException("Platforma duhet te jete ios ose android");
        };
    }
}
