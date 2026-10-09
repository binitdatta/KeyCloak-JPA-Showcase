package com.rollingstone.kc.fbl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.authentication.AuthenticationFlowError;
import org.keycloak.authentication.authenticators.broker.AbstractIdpAuthenticator;
import org.keycloak.broker.provider.BrokeredIdentityContext;
import org.keycloak.broker.provider.IdentityBrokerException;
import org.keycloak.authentication.authenticators.broker.util.SerializedBrokeredIdentityContext;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.StreamSupport;

/**
 * Extends Keycloak's built-in "Create User If Unique" first-broker-login step.
 *
 * Before letting the broker create (or link) a local Keycloak user, this
 * authenticator calls the keycloak-jpa-demo Spring Boot app's
 * POST /api/auth/validate with the broker's email, treating that app as the
 * external / legacy system of record. On success it stashes the returned
 * roles/groups on the auth-session as notes for
 * PostLoginRoleGroupAuthenticator to apply once the user exists, then
 * defers to Keycloak's own uniqueness/creation logic. On a non-authenticated
 * response it fails the flow rather than let an orphaned Keycloak account
 * be created.
 *
 * NOTE FOR THE TRAINING VIDEO: this class illustrates the shape of the
 * override (action(), authenticateImpl()-equivalent hook, and how to reach
 * the BrokeredIdentityContext from the AuthenticationFlowContext). Verify
 * method names/signatures against the exact org.keycloak:keycloak-services
 * version you deploy against — Keycloak has renamed a few of the
 * AbstractIdpAuthenticator hook methods across major versions.
 */
public class ExtendedCreateUserIfUniqueAuthenticator extends AbstractIdpAuthenticator {

    public static final String PROVIDER_ID = "rollingstone-create-user-if-unique-ext";

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Override
    protected void authenticateImpl(AuthenticationFlowContext context,
                                     SerializedBrokeredIdentityContext serializedCtx,
                                     BrokeredIdentityContext brokerContext) {

        String email = brokerContext.getEmail();

        ExternalValidationResult result = callExternalValidate(email);

        if (!result.authenticated) {
            context.getEvent().error("rollingstone_external_validation_failed");
            context.failure(AuthenticationFlowError.INVALID_USER);
            return;
        }

        // Stash for PostLoginRoleGroupAuthenticator, which runs later in the
        // same flow once Keycloak has actually created/linked the user.
        context.getAuthenticationSession().setAuthNote(rollingstoneFblConstants.NOTE_EXTERNAL_ROLES,
                String.join(",", result.roles));
        context.getAuthenticationSession().setAuthNote(rollingstoneFblConstants.NOTE_EXTERNAL_GROUPS,
                String.join(",", result.groups));
        context.getAuthenticationSession().setAuthNote(rollingstoneFblConstants.NOTE_EXTERNAL_CUSTOMER_ID,
                result.customerId == null ? "" : result.customerId);

        // Defer to Keycloak's normal collision-avoidance / user-creation
        // behavior — this class only adds the pre-check above it.
        context.success();
    }

    @Override
    public void action(AuthenticationFlowContext context) {
        // No user-facing form for this step; it runs purely as a background check.
        authenticate(context);
    }

    @Override
    protected void actionImpl(AuthenticationFlowContext context,
                               SerializedBrokeredIdentityContext serializedCtx,
                               BrokeredIdentityContext brokerContext) {
        authenticateImpl(context, serializedCtx, brokerContext);
    }

    private ExternalValidationResult callExternalValidate(String email) {
        try (CloseableHttpClient client = HttpClients.createDefault()) {
            HttpPost post = new HttpPost(rollingstoneFblConstants.VALIDATE_URL);
            String body = MAPPER.writeValueAsString(new ValidateRequestBody(email, "demo1234"));
            post.setEntity(new StringEntity(body, ContentType.APPLICATION_JSON));

            try (CloseableHttpResponse response = client.execute(post)) {
                String responseBody = EntityUtils.toString(response.getEntity());
                JsonNode node = MAPPER.readTree(responseBody);

                boolean authenticated = node.path("authenticated").asBoolean(false);
                String customerId = node.path("customerId").isMissingNode() ? null : node.path("customerId").asText();
                List<String> roles = toList(node.path("roles"));
                List<String> groups = toList(node.path("groups"));

                return new ExternalValidationResult(authenticated, customerId, roles, groups);
            }
        } catch (IOException e) {
            throw new IdentityBrokerException("Failed calling external validate endpoint", e);
        }
    }

    private List<String> toList(JsonNode arrayNode) {
        List<String> values = new ArrayList<>();
        if (arrayNode.isArray()) {
            StreamSupport.stream(arrayNode.spliterator(), false)
                    .forEach(n -> values.add(n.asText()));
        }
        return values;
    }

    @Override
    public boolean requiresUser() {
        return false;
    }

    @Override
    public boolean configuredFor(KeycloakSession session, RealmModel realm, UserModel user) {
        return true;
    }

    @Override
    public void setRequiredActions(KeycloakSession session, RealmModel realm, UserModel user) {
        // no required actions added by this step
    }

    private record ValidateRequestBody(String email, String password) {
    }

    private record ExternalValidationResult(boolean authenticated, String customerId,
                                              List<String> roles, List<String> groups) {
    }
}
