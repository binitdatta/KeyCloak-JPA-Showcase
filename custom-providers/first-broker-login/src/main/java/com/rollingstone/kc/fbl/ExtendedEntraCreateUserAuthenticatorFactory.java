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

public class ExtendedEntraCreateUserAuthenticatorFactory implements AuthenticatorFactory {

    @Override
    public String getId() {
        return ExtendedEntraCreateUserAuthenticator.PROVIDER_ID;
    }

    @Override
    public Authenticator create(KeycloakSession session) {
        return new ExtendedEntraCreateUserAuthenticator();
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
        return "Havi Entra Create User (Ext)";
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
        return "Extracts profile/role/group claims from the Entra SAML assertion, upserts the " +
                "local idp_user MySQL row, and grants the initial roles/groups. Runs once, on first login.";
    }

    @Override
    public List<ProviderConfigProperty> getConfigProperties() {
        return Collections.emptyList();
    }
}
