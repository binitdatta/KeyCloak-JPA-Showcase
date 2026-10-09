package com.rollingstone.demo.service;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

/**
 * Server-side half of the Device Authorization Grant "Live Demo" page.
 * Plays the role of the input-constrained device: asks Keycloak for a device
 * code + user code, then polls the token endpoint until a human approves.
 */
@Service
public class DeviceGrantDemoService {

    private final RestClient rest = RestClient.create();

    private final String clientId;
    private final String clientSecret;
    private final String deviceEndpoint;
    private final String tokenEndpoint;

    public DeviceGrantDemoService(
            @Value("${authz-demo.client-id}") String clientId,
            @Value("${authz-demo.client-secret}") String clientSecret,
            @Value("${device-demo.base-url:http://localhost:8080/realms/jpa-demo-realm/protocol/openid-connect}") String base) {
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.deviceEndpoint = base + "/auth/device";
        this.tokenEndpoint = base + "/token";
    }

    public String getClientId() { return clientId; }
    public String getDeviceEndpoint() { return deviceEndpoint; }
    public String getTokenEndpoint() { return tokenEndpoint; }

    /** Step 1: device authorization request. */
    public Map<String, Object> requestCodes(String scope) {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("client_id", clientId);
        form.add("client_secret", clientSecret);
        form.add("scope", scope);
        return post(deviceEndpoint, form);
    }

    /** Step 3..n: poll the token endpoint with the device_code grant. */
    public Map<String, Object> poll(String deviceCode) {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "urn:ietf:params:oauth:grant-type:device_code");
        form.add("device_code", deviceCode);
        form.add("client_id", clientId);
        form.add("client_secret", clientSecret);
        return post(tokenEndpoint, form);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> post(String url, MultiValueMap<String, String> form) {
        Map<String, Object> out = new LinkedHashMap<>();
        try {
            var resp = rest.post().uri(url)
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(form)
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, (req, res) -> { /* keep body, don't throw */ })
                    .toEntity(Map.class);
            out.put("status", resp.getStatusCode().value());
            out.put("body", resp.getBody() == null ? Map.of() : resp.getBody());
        } catch (Exception e) {
            out.put("status", 0);
            out.put("body", Map.of("error", "request_failed", "error_description", String.valueOf(e.getMessage())));
        }
        return out;
    }
}
