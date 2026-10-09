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

public class PostLoginRoleGroupAuthenticatorFactory implements AuthenticatorFactory {

    @Override
    public String getId() {
        return PostLoginRoleGroupAuthenticator.PROVIDER_ID;
    }

    @Override
    public Authenticator create(KeycloakSession session) {
        return new PostLoginRoleGroupAuthenticator();
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
        return "rollingstone Post-Login Role/Group Provisioning";
    }

    @Override
    public String getReferenceCategory() {
        return "post-login-provisioning";
    }

    @Override
    public boolean isConfigurable() {
        return false;
    }

    @Override
    public AuthenticationExecutionModel.Requirement[] getRequirementChoices() {
        return new AuthenticationExecutionModel.Requirement[]{
                AuthenticationExecutionModel.Requirement.REQUIRED,
                AuthenticationExecutionModel.Requirement.DISABLED
        };
    }

    @Override
    public boolean isUserSetupAllowed() {
        return false;
    }

    @Override
    public String getHelpText() {
        return "Grants realm roles and group membership captured during first broker login "
                + "(from rollingstone.externalRoles / rollingstone.externalGroups auth-session notes) once the user exists.";
    }

    @Override
    public List<ProviderConfigProperty> getConfigProperties() {
        return Collections.emptyList();
    }
}
