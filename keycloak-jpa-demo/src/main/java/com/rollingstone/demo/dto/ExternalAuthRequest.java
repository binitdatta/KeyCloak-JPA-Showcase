package com.rollingstone.demo.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * Payload the Keycloak custom First-Broker-Login authenticator posts to
 * /api/auth/validate to check a legacy/external credential before Keycloak
 * creates (or links) the broker-side user record.
 */
public class ExternalAuthRequest {

    @NotBlank
    @Email
    private String email;

    @NotBlank
    private String password;

    public ExternalAuthRequest() {
    }

    public ExternalAuthRequest(String email, String password) {
        this.email = email;
        this.password = password;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
