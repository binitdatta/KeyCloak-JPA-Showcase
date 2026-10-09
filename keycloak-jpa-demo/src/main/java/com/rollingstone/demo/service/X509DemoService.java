package com.rollingstone.demo.service;

import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.rollingstone.demo.dto.X509RunResult;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.net.http.HttpClient;
import java.security.cert.X509Certificate;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Runs the X509 client-authentication flow: load the client certificate
 * (just to show what's about to be presented), open an mTLS connection to
 * Keycloak's token endpoint presenting that certificate during the TLS
 * handshake itself (no client_secret, no client_assertion — the certificate
 * IS the proof of identity), decode the resulting access token, then call
 * userinfo with it.
 *
 * Both calls go to the :8443 mTLS listener, not :8080 — the access token's
 * "iss" claim is set to whichever host:port issued it, and Keycloak's
 * userinfo endpoint rejects a token whose issuer doesn't match the endpoint
 * being called. userinfo itself doesn't require a client certificate
 * (https-client-auth=request only asks, never requires), so reusing the
 * same mTLS-capable RestClient for both calls is simplest and correct.
 */
@Service
public class X509DemoService {

    private final X509MtlsContextFactory mtlsContextFactory;
    private final String clientId;
    private final String tokenEndpoint;
    private final String userinfoEndpoint;

    public X509DemoService(X509MtlsContextFactory mtlsContextFactory,
                           @Value("${x509-demo.client-id}") String clientId,
                           @Value("${x509-demo.token-endpoint}") String tokenEndpoint,
                           @Value("${x509-demo.userinfo-endpoint}") String userinfoEndpoint) {
        this.mtlsContextFactory = mtlsContextFactory;
        this.clientId = clientId;
        this.tokenEndpoint = tokenEndpoint;
        this.userinfoEndpoint = userinfoEndpoint;
    }

    public X509RunResult run() {
        X509RunResult result = new X509RunResult();

        try {
            X509Certificate cert = mtlsContextFactory.loadClientCertificate();
            result.setCertSubjectDn(cert.getSubjectX500Principal().getName());
            result.setCertIssuerDn(cert.getIssuerX500Principal().getName());
            result.setCertSerialNumber(cert.getSerialNumber().toString(16));
            result.setCertNotBefore(cert.getNotBefore().toString());
            result.setCertNotAfter(cert.getNotAfter().toString());
        } catch (Exception e) {
            result.setErrorStep("Load the client certificate");
            result.setErrorMessage(e.getMessage());
            return result;
        }

        RestClient mtlsRestClient;
        try {
            HttpClient httpClient = HttpClient.newBuilder()
                    .sslContext(mtlsContextFactory.buildSslContext())
                    .connectTimeout(Duration.ofSeconds(5))
                    .build();
            mtlsRestClient = RestClient.builder()
                    .requestFactory(new JdkClientHttpRequestFactory(httpClient))
                    .build();
        } catch (Exception e) {
            result.setErrorStep("Build the mTLS-capable HTTP client");
            result.setErrorMessage(e.getMessage());
            return result;
        }

        String accessToken;
        try {
            MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
            form.add("grant_type", "client_credentials");
            form.add("client_id", clientId);
            form.add("scope", "openid");

            @SuppressWarnings("unchecked")
            Map<String, Object> tokenResponse = mtlsRestClient.post()
                    .uri(tokenEndpoint)
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(form)
                    .retrieve()
                    .body(Map.class);

            accessToken = (String) tokenResponse.get("access_token");
            result.setAccessToken(accessToken);
            result.setAccessTokenClaims(decodeClaims(accessToken));
        } catch (RestClientResponseException e) {
            result.setErrorStep("Exchange via mTLS at Keycloak's token endpoint");
            result.setErrorMessage("HTTP " + e.getStatusCode() + " - " + e.getResponseBodyAsString());
            return result;
        } catch (Exception e) {
            result.setErrorStep("Exchange via mTLS at Keycloak's token endpoint");
            result.setErrorMessage(e.getMessage());
            return result;
        }

        try {
            String userinfo = mtlsRestClient.get()
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