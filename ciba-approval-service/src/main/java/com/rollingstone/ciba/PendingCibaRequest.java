package com.rollingstone.ciba;

import java.time.Instant;

/**
 * A single in-flight CIBA authentication request as reported to us by
 * Keycloak's ciba-http-auth-channel provider, via its POST to /ciba/notify.
 *
 * bearerToken is the value Keycloak sent in the Authorization header of
 * that notification — it MUST be echoed back, unchanged, as the
 * Authorization header on our later callback to Keycloak's
 * /ext/ciba/auth/callback endpoint. It is what lets Keycloak correlate our
 * callback with this specific pending request; there is no other request ID
 * exchanged with us.
 */
public class PendingCibaRequest {

    private final String bearerToken;
    private final String loginHint;
    private final String scope;
    private final String bindingMessage;
    private final Instant receivedAt;

    public PendingCibaRequest(String bearerToken, String loginHint, String scope,
                               String bindingMessage, Instant receivedAt) {
        this.bearerToken = bearerToken;
        this.loginHint = loginHint;
        this.scope = scope;
        this.bindingMessage = bindingMessage;
        this.receivedAt = receivedAt;
    }

    public String getBearerToken() {
        return bearerToken;
    }

    public String getLoginHint() {
        return loginHint;
    }

    public String getScope() {
        return scope;
    }

    public String getBindingMessage() {
        return bindingMessage;
    }

    public Instant getReceivedAt() {
        return receivedAt;
    }
}
