package com.rollingstone.ciba;

/**
 * JSON shape returned by GET /ciba/pending for the browser page to poll.
 */
public class PendingStatusResponse {

    private final boolean pending;
    private final String loginHint;
    private final String scope;
    private final String bindingMessage;

    public PendingStatusResponse(boolean pending, String loginHint, String scope, String bindingMessage) {
        this.pending = pending;
        this.loginHint = loginHint;
        this.scope = scope;
        this.bindingMessage = bindingMessage;
    }

    public static PendingStatusResponse none() {
        return new PendingStatusResponse(false, null, null, null);
    }

    public static PendingStatusResponse from(PendingCibaRequest request) {
        return new PendingStatusResponse(true, request.getLoginHint(), request.getScope(), request.getBindingMessage());
    }

    public boolean isPending() {
        return pending;
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
}
