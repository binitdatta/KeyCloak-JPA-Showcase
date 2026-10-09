package com.rollingstone.demo.service;

import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.rollingstone.demo.dto.AuthzScopeResult;
import com.rollingstone.demo.dto.UmaAuthzRunResult;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class AuthzDemoService {

    private static final List<String> SCOPES_TO_TEST = List.of("order:view", "order:approve");

    private final String clientId;
    private final String clientSecret;
    private final String tokenEndpoint;
    private final String resourceName;
    private final RestClient restClient;

    public AuthzDemoService(@Value("${authz-demo.client-id}") String clientId,
                            @Value("${authz-demo.client-secret}") String clientSecret,
                            @Value("${authz-demo.token-endpoint}") String tokenEndpoint,
                            @Value("${authz-demo.resource-name}") String resourceName) {
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.tokenEndpoint = tokenEndpoint;
        this.resourceName = resourceName;
        this.restClient = RestClient.builder().build();
    }

    public UmaAuthzRunResult run() {
        UmaAuthzRunResult result = new UmaAuthzRunResult();
        List<AuthzScopeResult> scopeResults = new ArrayList<>();

        for (String scope : SCOPES_TO_TEST) {
            scopeResults.add(evaluateScope(scope));
        }

        result.setScopeResults(scopeResults);
        result.setSuccess(true);
        return result;
    }

    private AuthzScopeResult evaluateScope(String scope) {
        AuthzScopeResult scopeResult = new AuthzScopeResult();
        scopeResult.setScope(scope);

        String permission = resourceName + "#" + scope;
        scopeResult.setPermissionRequested(permission);

        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "urn:ietf:params:oauth:grant-type:uma-ticket");
        form.add("client_id", clientId);
        form.add("client_secret", clientSecret);
        form.add("audience", clientId);
        form.add("permission", permission);

        try {
            String rawBody = restClient.post()
                    .uri(tokenEndpoint)
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(form)
                    .retrieve()
                    .body(String.class);

            scopeResult.setHttpStatus(200);
            scopeResult.setGranted(true);
            scopeResult.setRawResponseBody(rawBody);

            String rpt = extractAccessToken(rawBody);
            scopeResult.setRptToken(rpt);
            scopeResult.setAuthorizationClaims(decodeAuthorizationClaims(rpt));

        } catch (RestClientResponseException e) {
            scopeResult.setHttpStatus(e.getStatusCode().value());
            scopeResult.setGranted(false);
            scopeResult.setRawResponseBody(e.getResponseBodyAsString());
            scopeResult.setErrorMessage("HTTP " + e.getStatusCode() + " - " + e.getResponseBodyAsString());
        } catch (Exception e) {
            scopeResult.setGranted(false);
            scopeResult.setErrorMessage(e.getMessage());
        }

        return scopeResult;
    }

    @SuppressWarnings("unchecked")
    private String extractAccessToken(String rawJsonBody) throws Exception {
        com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
        Map<String, Object> parsed = mapper.readValue(rawJsonBody, Map.class);
        return (String) parsed.get("access_token");
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> decodeAuthorizationClaims(String rpt) throws java.text.ParseException {
        SignedJWT jwt = SignedJWT.parse(rpt);
        JWTClaimsSet claims = jwt.getJWTClaimsSet();
        Map<String, Object> all = new LinkedHashMap<>(claims.getClaims());

        Object authorization = all.get("authorization");
        if (authorization instanceof Map) {
            return (Map<String, Object>) authorization;
        }
        return all;
    }
}