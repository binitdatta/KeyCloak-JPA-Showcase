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
 * Server-side half of the CIBA "Try it live" page. It performs exactly the two
 * requests the curl procedure does (initiate, then poll) and returns the raw
 * Keycloak JSON plus the HTTP status so the page can show what happened.
 */
@Service
public class CibaDemoService {

    private final RestClient rest = RestClient.create();

    private final String clientId;
    private final String clientSecret;
    private final String authEndpoint;
    private final String tokenEndpoint;

    public CibaDemoService(
            @Value("${authz-demo.client-id}") String clientId,
            @Value("${authz-demo.client-secret}") String clientSecret,
            @Value("${ciba-demo.base-url:http://localhost:8080/realms/jpa-demo-realm/protocol/openid-connect}") String base) {
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.authEndpoint = base + "/ext/ciba/auth";
        this.tokenEndpoint = base + "/token";
    }

    public String getClientId() { return clientId; }
    public String getAuthEndpoint() { return authEndpoint; }
    public String getTokenEndpoint() { return tokenEndpoint; }

    /** Step 1: backchannel authentication request. */
    public Map<String, Object> initiate(String loginHint, String scope, String bindingMessage) {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("client_id", clientId);
        form.add("client_secret", clientSecret);
        form.add("login_hint", loginHint);
        form.add("scope", scope);
        if (bindingMessage != null && !bindingMessage.isBlank()) {
            form.add("binding_message", bindingMessage);
        }
        return post(authEndpoint, form);
    }

    /** Step 2..n: poll the token endpoint with the CIBA grant. */
    public Map<String, Object> poll(String authReqId) {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "urn:openid:params:grant-type:ciba");
        form.add("auth_req_id", authReqId);
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
