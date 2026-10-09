package com.rollingstone.signedjwt;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

/**
 * The two outbound calls this demo makes: exchange the signed assertion for
 * a token, then call userinfo with that token to prove it's real.
 *
 * Uses Spring's RestClient (built into spring-boot-starter-web, no extra
 * dependency) rather than RestTemplate — RestTemplate still works fine here,
 * RestClient is just the current, non-deprecated choice for a new Spring
 * Boot 3.5.x app.
 */
@Component
public class SignedJwtTokenClient {

    private static final String CLIENT_ASSERTION_TYPE =
            "urn:ietf:params:oauth:client-assertion-type:jwt-bearer";

    private final RestClient restClient;
    private final String tokenEndpoint;
    private final String userinfoEndpoint;
    private final String clientId;

    public SignedJwtTokenClient(@Value("${keycloak.token-endpoint}") String tokenEndpoint,
                                 @Value("${keycloak.userinfo-endpoint}") String userinfoEndpoint,
                                 @Value("${keycloak.client-id}") String clientId) {
        this.tokenEndpoint = tokenEndpoint;
        this.userinfoEndpoint = userinfoEndpoint;
        this.clientId = clientId;

        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(5000);
        requestFactory.setReadTimeout(5000);
        this.restClient = RestClient.builder().requestFactory(requestFactory).build();
    }

    /**
     * Exchanges the signed client assertion for an access token via the
     * client_credentials grant. client_id is included in the form body in
     * addition to the iss/sub claims inside the assertion itself — not
     * strictly required by Keycloak (it can resolve the client from the
     * assertion's "iss" claim alone), but including it matches the plain
     * OAuth2 client_credentials request shape and avoids any ambiguity.
     *
     * scope=openid is requested explicitly: without it, Keycloak issues a
     * plain OAuth2 access token with no "openid" scope attached, and its
     * /userinfo endpoint (an OIDC-specific endpoint) rejects such a token
     * with a bare 403 Forbidden.
     */
    public TokenResponse fetchToken(String signedAssertion) {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "client_credentials");
        form.add("client_id", clientId);
        form.add("scope", "openid");
        form.add("client_assertion_type", CLIENT_ASSERTION_TYPE);
        form.add("client_assertion", signedAssertion);

        try {
            return restClient.post()
                    .uri(tokenEndpoint)
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(form)
                    .retrieve()
                    .body(TokenResponse.class);
        } catch (RestClientResponseException e) {
            throw new IllegalStateException("Token request failed: HTTP " + e.getStatusCode()
                    + " - " + e.getResponseBodyAsString(), e);
        }
    }

    /**
     * Calls Keycloak's userinfo endpoint with the access token — this is the
     * "do something with the token" step: userinfo only returns claims when
     * given a genuinely valid Bearer token, so a successful response here is
     * independent proof the Signed JWT flow actually produced a usable token,
     * not just that the token endpoint returned 200.
     */
    public String callUserinfo(String accessToken) {
        try {
            return restClient.get()
                    .uri(userinfoEndpoint)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                    .retrieve()
                    .body(String.class);
        } catch (RestClientResponseException e) {
            throw new IllegalStateException("Userinfo call failed: HTTP " + e.getStatusCode()
                    + " - " + e.getResponseBodyAsString(), e);
        }
    }
}
