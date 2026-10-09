package com.rollingstone.demo.dto;

import java.util.List;

/**
 * Response returned to the Keycloak SPI authenticator. `roles` and `groups`
 * are consumed by the Post-Login-Flow authenticator
 * (PostLoginRoleGroupAuthenticator) to provision Keycloak realm roles and
 * group membership right after first broker login.
 */
public class ExternalAuthResponse {

    private boolean authenticated;
    private String customerId;
    private String email;
    private String firstName;
    private String lastName;
    private List<String> roles;
    private List<String> groups;

    public ExternalAuthResponse() {
    }

    public ExternalAuthResponse(boolean authenticated, String customerId, String email,
                                 String firstName, String lastName,
                                 List<String> roles, List<String> groups) {
        this.authenticated = authenticated;
        this.customerId = customerId;
        this.email = email;
        this.firstName = firstName;
        this.lastName = lastName;
        this.roles = roles;
        this.groups = groups;
    }

    public boolean isAuthenticated() {
        return authenticated;
    }

    public String getCustomerId() {
        return customerId;
    }

    public String getEmail() {
        return email;
    }

    public String getFirstName() {
        return firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public List<String> getRoles() {
        return roles;
    }

    public List<String> getGroups() {
        return groups;
    }
}
