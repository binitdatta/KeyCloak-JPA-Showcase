package com.rollingstone.ciba;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Keycloak's ciba-http-auth-channel provider POSTs this notification as
 * JSON (confirmed from a live 415 in the server log — not form-urlencoded
 * as some older community docs describe). @JsonIgnoreProperties tolerates
 * any additional fields Keycloak sends that we don't care about here
 * (e.g. is_consent_required, acr_values).
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class CibaNotifyRequest {

    @JsonProperty("login_hint")
    private String loginHint;

    @JsonProperty("scope")
    private String scope;

    @JsonProperty("binding_message")
    private String bindingMessage;

    public String getLoginHint() {
        return loginHint;
    }

    public void setLoginHint(String loginHint) {
        this.loginHint = loginHint;
    }

    public String getScope() {
        return scope;
    }

    public void setScope(String scope) {
        this.scope = scope;
    }

    public String getBindingMessage() {
        return bindingMessage;
    }

    public void setBindingMessage(String bindingMessage) {
        this.bindingMessage = bindingMessage;
    }
}