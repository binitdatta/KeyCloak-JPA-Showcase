package com.rollingstone.ciba;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * Sends the actual decision back to Keycloak once a human has clicked
 * Approve or Deny on the poll-and-approve page. This is the ONLY place in
 * this app that talks to Keycloak.
 *
 * Uses the JDK's built-in java.net.http.HttpClient rather than adding
 * RestTemplate/RestClient — this module has exactly one outbound HTTP call
 * to make, so a Spring HTTP client abstraction would be more machinery
 * than the job needs.
 */
@Service
public class KeycloakCibaCallbackService {

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    private final String keycloakBaseUrl;
    private final String realm;

    public KeycloakCibaCallbackService(@Value("${keycloak.base-url}") String keycloakBaseUrl,
                                        @Value("${keycloak.realm}") String realm) {
        this.keycloakBaseUrl = keycloakBaseUrl;
        this.realm = realm;
    }

    /**
     * status must be one of "SUCCEED", "UNAUTHORIZED", or "CANCELLED"
     * (Keycloak's ciba-http-auth-channel callback contract).
     */
    public void sendCallback(String bearerToken, String status) throws IOException, InterruptedException {
        String callbackUrl = keycloakBaseUrl + "/realms/" + realm
                + "/protocol/openid-connect/ext/ciba/auth/callback";

        String jsonBody = "{\"status\":\"" + status + "\"}";

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(callbackUrl))
                .header("Authorization", "Bearer " + bearerToken)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() >= 300) {
            throw new IOException("Keycloak CIBA callback failed: HTTP " + response.statusCode()
                    + " - " + response.body());
        }
    }
}
