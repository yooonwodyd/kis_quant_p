package com.kisquant.order.adapter.kis;

import com.kisquant.shared.config.KisQuantProperties;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.ObjectMapper;

@Component
public class KisTokenManager {

    private final RestClient restClient;
    private final KisQuantProperties properties;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private volatile String accessToken;
    private volatile Instant expiresAt = Instant.EPOCH;

    KisTokenManager(RestClient kisRestClient, KisQuantProperties properties) {
        this.restClient = kisRestClient;
        this.properties = properties;
    }

    public synchronized String accessToken() {
        if (accessToken != null && Instant.now().isBefore(expiresAt.minusSeconds(60))) {
            return accessToken;
        }
        return issueAccessToken();
    }

    public synchronized String refreshAccessToken() {
        return issueAccessToken();
    }

    public boolean isExpiredTokenResponse(Map<String, Object> response) {
        String message = text(response.get("msg1")).toLowerCase(Locale.ROOT);
        String code = text(response.get("msg_cd")).toUpperCase(Locale.ROOT);
        boolean tokenMentioned = message.contains("token") || message.contains("토큰");
        boolean expiryMentioned = message.contains("만료") || message.contains("expired");
        return "EGW00123".equals(code) || (tokenMentioned && expiryMentioned);
    }

    private String issueAccessToken() {
        Map<String, String> body = new LinkedHashMap<>();
        body.put("grant_type", "client_credentials");
        body.put("appkey", properties.kis().appKey());
        body.put("appsecret", properties.kis().appSecret());
        @SuppressWarnings("unchecked")
        Map<String, Object> response = restClient.post()
                .uri("/oauth2/tokenP")
                .contentType(MediaType.APPLICATION_JSON)
                .body(writeJsonBody(body))
                .retrieve()
                .body(Map.class);
        accessToken = text(response.get("access_token"));
        expiresAt = parseExpiresAt(response);
        return accessToken;
    }

    private Instant parseExpiresAt(Map<String, Object> response) {
        String explicitExpiry = text(response.get("access_token_token_expired"));
        if (!explicitExpiry.isBlank()) {
            return Instant.now().plusSeconds(Long.parseLong(text(response.getOrDefault("expires_in", "86400"))));
        }
        return Instant.now().plusSeconds(Long.parseLong(text(response.getOrDefault("expires_in", "86400"))));
    }

    private byte[] writeJsonBody(Map<?, ?> body) {
        try {
            return objectMapper.writeValueAsBytes(body);
        } catch (Exception exception) {
            throw new IllegalStateException("failed to serialize KIS token request", exception);
        }
    }

    private String text(Object value) {
        return value == null ? "" : String.valueOf(value);
    }
}
