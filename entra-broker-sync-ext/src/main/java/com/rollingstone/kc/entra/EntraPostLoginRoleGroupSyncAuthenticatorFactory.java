package com.rollingstone.kc.entra;

import org.keycloak.Config;
import org.keycloak.authentication.Authenticator;
import org.keycloak.authentication.AuthenticatorFactory;
import org.keycloak.models.AuthenticationExecutionModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.provider.ProviderConfigProperty;

import java.util.Collections;
import java.util.List;

public class EntraPostLoginRoleGroupSyncAuthenticatorFactory implements AuthenticatorFactory {

    @Override
    public String getId() {
        return EntraPostLoginRoleGroupSyncAuthenticator.PROVIDER_ID;
    }

    @Override
    public Authenticator create(KeycloakSession session) {
        return new EntraPostLoginRoleGroupSyncAuthenticator();
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
        return "rollingstone Entra Post-Login Role/Group Sync";
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
        return "Runs on every brokered login (bound to the IdP's Post Login Flow). Diffs the " +
                "Entra assertion's roles/groups against MySQL's last-known ACTIVE set and applies " +
                "grants and revocations to both Keycloak and MySQL.";
    }

    @Override
    public List<ProviderConfigProperty> getConfigProperties() {
        return Collections.emptyList();
    }
}