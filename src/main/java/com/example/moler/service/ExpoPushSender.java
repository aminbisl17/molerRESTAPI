package com.example.moler.service;

import com.example.moler.configuration.TenantContext;
import com.example.moler.repository.PushTokenRepository;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Sends push messages through Expo's push API, in the background.
 * Tokens that Expo reports as DeviceNotRegistered (app uninstalled) are deleted.
 */
@Component
public class ExpoPushSender {

    private static final Logger log = LoggerFactory.getLogger(ExpoPushSender.class);

    private static final String EXPO_PUSH_URL = "https://exp.host/--/api/v2/push/send";

    // Expo accepts at most 100 messages per request
    private static final int MADHESIA_E_GRUPIT = 100;

    private final RestClient restClient;
    private final PushTokenRepository pushTokenRepository;

    public ExpoPushSender(
            PushTokenRepository pushTokenRepository,
            @Value("${EXPO_ACCESS_TOKEN:}") String expoAccessToken
    ) {
        this.pushTokenRepository = pushTokenRepository;

        RestClient.Builder builder = RestClient.builder()
                .baseUrl(EXPO_PUSH_URL)
                .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE);

        // Only needed when "Enhanced push security" is enabled in Expo
        if (expoAccessToken != null && !expoAccessToken.isBlank()) {
            builder.defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + expoAccessToken);
        }

        this.restClient = builder.build();
    }

    /** Tenant of the request, carried into the background thread for token cleanup. */
    public record Tenant(String schema, Long id, String key) {
        public static Tenant aktual() {
            return new Tenant(
                    TenantContext.getTenant(),
                    TenantContext.getTenantId(),
                    TenantContext.getTenantKey()
            );
        }
    }

    /** One push to one device. */
    public record Mesazh(
            String token,
            String titulli,
            String mesazhi,
            Map<String, Object> data
    ) {}

    @Async
    public void dergo(List<Mesazh> teGjitha, Tenant tenant) {
        List<String> teVdekur = new ArrayList<>();

        for (int i = 0; i < teGjitha.size(); i += MADHESIA_E_GRUPIT) {
            List<Mesazh> grupiM = teGjitha.subList(i, Math.min(i + MADHESIA_E_GRUPIT, teGjitha.size()));
            List<String> grupi = grupiM.stream().map(Mesazh::token).toList();

            List<ExpoMessage> mesazhet = grupiM.stream()
                    .map(m -> new ExpoMessage(
                            m.token(), m.titulli(), m.mesazhi(), m.data(), "default", "high", "default"))
                    .toList();

            try {
                ExpoResponse response = restClient.post()
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(mesazhet)
                        .retrieve()
                        .body(ExpoResponse.class);

                if (response == null || response.data() == null) {
                    log.warn("Expo push: empty response for {} messages", grupi.size());
                    continue;
                }

                // Tickets come back in the same order as the messages
                for (int j = 0; j < response.data().size() && j < grupi.size(); j++) {
                    ExpoTicket ticket = response.data().get(j);

                    if (!"error".equals(ticket.status())) {
                        continue;
                    }

                    String gabimi = ticket.details() == null ? null : ticket.details().error();

                    if ("DeviceNotRegistered".equals(gabimi)) {
                        teVdekur.add(grupi.get(j));
                    } else {
                        log.warn("Expo push error: {} ({})", ticket.message(), gabimi);
                    }
                }
            } catch (Exception e) {
                log.warn("Expo push request failed: {}", e.getMessage());
            }
        }

        if (!teVdekur.isEmpty()) {
            fshiTokenat(teVdekur, tenant);
        }
    }

    private void fshiTokenat(List<String> tokenat, Tenant tenant) {
        try {
            // Background thread has no tenant: restore it so the right schema is used
            TenantContext.setTenant(tenant.schema());
            TenantContext.setTenantId(tenant.id());
            TenantContext.setTenantKey(tenant.key());

            pushTokenRepository.deleteByExpoTokenIn(tokenat);
        } catch (Exception e) {
            log.warn("Could not delete unregistered push tokens: {}", e.getMessage());
        } finally {
            TenantContext.clear();
        }
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    record ExpoMessage(
            String to,
            String title,
            String body,
            Map<String, Object> data,
            String sound,
            String priority,
            String channelId
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    record ExpoResponse(List<ExpoTicket> data) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    record ExpoTicket(String status, String id, String message, ExpoTicketDetails details) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    record ExpoTicketDetails(String error) {}
}
