package com.rollingstone.kc.fbl;

import org.keycloak.Config;
import org.keycloak.authentication.Authenticator;
import org.keycloak.authentication.AuthenticatorFactory;
import org.keycloak.models.AuthenticationExecutionModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.provider.ProviderConfigProperty;

import java.util.Collections;
import java.util.List;

/**
 * Registers ExtendedCreateUserIfUniqueAuthenticator under provider ID
 * "rollingstone-create-user-if-unique-ext" so it appears as a swappable execution
 * inside a duplicated "First broker login" flow (Keycloak's built-in flows
 * are read-only, hence the duplicate-then-swap step in the training page).
 */
public class ExtendedCreateUserIfUniqueAuthenticatorFactory implements AuthenticatorFactory {

    @Override
    public String getId() {
        return ExtendedCreateUserIfUniqueAuthenticator.PROVIDER_ID;
    }

    @Override
    public Authenticator create(KeycloakSession session) {
        return new ExtendedCreateUserIfUniqueAuthenticator();
    }

    @Override
    public void init(Config.Scope config) {
    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {
    }

    @Override
    public void close() {
    }

    @Override
    public String getDisplayType() {
        return "rollingstone Create User If Unique (Ext)";
    }

    @Override
    public String getReferenceCategory() {
        return "user-creation";
    }

    @Override
    public boolean isConfigurable() {
        return false;
    }

    @Override
    public AuthenticationExecutionModel.Requirement[] getRequirementChoices() {
        return new AuthenticationExecutionModel.Requirement[]{
                AuthenticationExecutionModel.Requirement.REQUIRED,
                AuthenticationExecutionModel.Requirement.ALTERNATIVE,
                AuthenticationExecutionModel.Requirement.DISABLED
        };
    }

    @Override
    public boolean isUserSetupAllowed() {
        return false;
    }

    @Override
    public String getHelpText() {
        return "Extends Create User If Unique to validate the broker identity against the "
                + "keycloak-jpa-demo /api/auth/validate REST endpoint before the local user is created, "
                + "and stashes returned roles/groups for the post-login provisioning step.";
    }

    @Override
    public List<ProviderConfigProperty> getConfigProperties() {
        return Collections.emptyList();
    }
}
