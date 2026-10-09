package com.rollingstone.kc.clientauth;

import org.keycloak.Config;
import org.keycloak.authentication.ClientAuthenticator;
import org.keycloak.authentication.ClientAuthenticatorFactory;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.provider.ProviderConfigProperty;

import java.util.Collections;
import java.util.List;

/**
 * ServiceLoader-discovered factory for CustomSecretHeaderClientAuthenticator.
 * Provider ID "rollingstone-secret-header" is what shows up in the Client
 * Authenticator dropdown on a client's Credentials tab, alongside the
 * built-in client-secret, client-jwt, client-x509 and client-secret-jwt
 * options.
 *
 * Registration file:
 *   src/main/resources/META-INF/services/org.keycloak.authentication.ClientAuthenticatorFactory
 * must list this class's fully-qualified name for Keycloak's provider
 * scanner to find it on boot.
 */
public class CustomSecretHeaderClientAuthenticatorFactory implements ClientAuthenticatorFactory {

    @Override
    public String getId() {
        return CustomSecretHeaderClientAuthenticator.PROVIDER_ID;
    }

    @Override
    public ClientAuthenticator create() {
        return new CustomSecretHeaderClientAuthenticator();
    }

    @Override
    public void init(Config.Scope config) {
        // no static config needed for this demo provider
    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {
        // no-op
    }

    @Override
    public void close() {
        // no-op
    }

    @Override
    public String getDisplayType() {
        return "rollingstone Secret Header";
    }

    @Override
    public String getHelpText() {
        return "Authenticates the client using a shared secret sent in the X-rollingstone-Client-Key header.";
    }

    @Override
    public String getReferenceCategory() {
        return "client-secret";
    }

    @Override
    public boolean isConfigurable() {
        return false;
    }

    @Override
    public List<ProviderConfigProperty> getConfigProperties() {
        return Collections.emptyList();
    }

    @Override
    public boolean isSupported() {
        return true;
    }
}
