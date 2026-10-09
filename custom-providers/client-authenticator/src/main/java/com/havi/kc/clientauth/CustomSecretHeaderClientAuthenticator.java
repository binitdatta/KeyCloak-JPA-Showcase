package com.havi.kc.clientauth;

import jakarta.ws.rs.core.MultivaluedMap;
import org.keycloak.authentication.AuthenticationFlowError;
import org.keycloak.authentication.ClientAuthenticationFlowContext;
import org.keycloak.authentication.ClientAuthenticator;
import org.keycloak.models.ClientModel;
import org.keycloak.models.RealmModel;
import org.keycloak.services.clientpolicy.ClientPolicyException;

import java.util.List;

/**
 * Authenticates a confidential client using a shared secret sent as a custom
 * request header (X-Havi-Client-Key) instead of the standard client_secret
 * form parameter that Keycloak's built-in "Client Id and Secret" option uses.
 *
 * Registered under provider ID "havi-secret-header" — see
 * CustomSecretHeaderClientAuthenticatorFactory and the
 * META-INF/services registration file.
 *
 * This is intentionally a minimal, readable example for the training video:
 * it reuses the client's normal "secret" credential (ClientModel.getSecret())
 * rather than inventing a second credential type, so the only thing that
 * changes versus the built-in client-secret authenticator is WHERE the
 * secret is transmitted on the wire.
 */
public class CustomSecretHeaderClientAuthenticator implements ClientAuthenticator {

    public static final String PROVIDER_ID = "havi-secret-header";
    public static final String SECRET_HEADER_NAME = "X-Havi-Client-Key";

    @Override
    public void authenticateClient(ClientAuthenticationFlowContext context) {
        MultivaluedMap<String, String> headers = context.getHttpRequest().getHttpHeaders().getRequestHeaders();
        List<String> values = headers.get(SECRET_HEADER_NAME);

        if (values == null || values.isEmpty() || values.get(0).isBlank()) {
            failNoCredentials(context);
            return;
        }

        String presentedSecret = values.get(0);

        // The client ID still has to be resolved the normal way (path param,
        // query param, or an earlier flow step) before this authenticator runs;
        // context.getClient() reflects whatever client the flow has already
        // identified as the caller.
        ClientModel client = context.getClient();
        if (client == null) {
            failNoClient(context);
            return;
        }

        RealmModel realm = context.getRealm();
        String storedSecret = client.getSecret();

        if (storedSecret == null || !storedSecret.equals(presentedSecret)) {
            context.failure(AuthenticationFlowError.INVALID_CLIENT_CREDENTIALS, null);
            return;
        }

        context.success();
    }

    private void failNoCredentials(ClientAuthenticationFlowContext context) {
        context.failure(AuthenticationFlowError.CLIENT_CREDENTIALS_SETUP_REQUIRED, null);
    }

    private void failNoClient(ClientAuthenticationFlowContext context) {
        context.failure(AuthenticationFlowError.CLIENT_NOT_FOUND, null);
    }

    @Override
    public String getDisplayType() {
        return "Havi Secret Header";
    }

    @Override
    public String getHelpText() {
        return "Validates the client using a shared secret sent as the " + SECRET_HEADER_NAME
                + " HTTP header instead of the client_secret form parameter.";
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
    public boolean isSupported() {
        return true;
    }

    @Override
    public void close() {
        // no-op: stateless
    }
}
