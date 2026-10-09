package com.rollingstone.demo.service;

import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.rollingstone.demo.dto.SignedJwtRunResult;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Runs the full private_key_jwt flow server-side for the "Try it live" panel:
 * sign the assertion -> exchange it at Keycloak's token endpoint -> decode
 * both JWTs -> call userinfo with the access token. Mirrors DemoRunner from
 * the standalone signed-jwt-demo-client module, but returns everything as
 * one JSON object instead of printing to a console.
 */
@Service
public class SignedJwtDemoService {

    private static final String CLIENT_ASSERTION_TYPE =
            "urn:ietf:params:oauth:client-assertion-type:jwt-bearer";

    private final SignedJwtAssertionGenerator assertionGenerator;
    private final RestClient restClient;
    private final String clientId;
    private final String tokenEndpoint;
    private final String userinfoEndpoint;

    public SignedJwtDemoService(SignedJwtAssertionGenerator assertionGenerator,
                                @Value("${signed-jwt.client-id}") String clientId,
                                @Value("${signed-jwt.token-endpoint}") String tokenEndpoint,
                                @Value("${signed-jwt.userinfo-endpoint}") String userinfoEndpoint) {
        this.assertionGenerator = assertionGenerator;
        this.clientId = clientId;
        this.tokenEndpoint = tokenEndpoint;
        this.userinfoEndpoint = userinfoEndpoint;

        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(5000);
        requestFactory.setReadTimeout(5000);
        this.restClient = RestClient.builder().requestFactory(requestFactory).build();
    }

    public SignedJwtRunResult run() {
        SignedJwtRunResult result = new SignedJwtRunResult();

        String assertion;
        try {
            assertion = assertionGenerator.generateSignedAssertion();
            result.setSignedAssertion(assertion);
            result.setAssertionClaims(decodeClaims(assertion));
        } catch (Exception e) {
            result.setErrorStep("Build + sign the client assertion");
            result.setErrorMessage(e.getMessage());
            return result;
        }

        String accessToken;
        try {
            MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
            form.add("grant_type", "client_credentials");
            form.add("client_id", clientId);
            form.add("scope", "openid");
            form.add("client_assertion_type", CLIENT_ASSERTION_TYPE);
            form.add("client_assertion", assertion);

            @SuppressWarnings("unchecked")
            Map<String, Object> tokenResponse = restClient.post()
                    .uri(tokenEndpoint)
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(form)
                    .retrieve()
                    .body(Map.class);

            accessToken = (String) tokenResponse.get("access_token");
            result.setAccessToken(accessToken);
            result.setAccessTokenClaims(decodeClaims(accessToken));
        } catch (RestClientResponseException e) {
            result.setErrorStep("Exchange the assertion at Keycloak's token endpoint");
            result.setErrorMessage("HTTP " + e.getStatusCode() + " - " + e.getResponseBodyAsString());
            return result;
        } catch (Exception e) {
            result.setErrorStep("Exchange the assertion at Keycloak's token endpoint");
            result.setErrorMessage(e.getMessage());
            return result;
        }

        try {
            String userinfo = restClient.get()
                    .uri(userinfoEndpoint)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                    .retrieve()
                    .body(String.class);
            result.setUserinfo(userinfo);
        } catch (RestClientResponseException e) {
            result.setErrorStep("Call userinfo with the access token");
            result.setErrorMessage("HTTP " + e.getStatusCode() + " - " + e.getResponseBodyAsString());
            return result;
        } catch (Exception e) {
            result.setErrorStep("Call userinfo with the access token");
            result.setErrorMessage(e.getMessage());
            return result;
        }

        result.setSuccess(true);
        return result;
    }

    private Map<String, Object> decodeClaims(String compactJwt) throws java.text.ParseException {
        SignedJWT jwt = SignedJWT.parse(compactJwt);
        JWTClaimsSet claims = jwt.getJWTClaimsSet();
        return new LinkedHashMap<>(claims.getClaims());
    }
}