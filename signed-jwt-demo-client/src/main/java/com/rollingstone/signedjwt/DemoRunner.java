package com.rollingstone.signedjwt;

import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

/**
 * Runs the full Signed JWT demo end to end, once, on application startup:
 *   1. Build + sign the client assertion JWT (proves possession of the
 *      private key matching the public certificate Keycloak has on file).
 *   2. Decode and print that assertion's own claims, unverified — just to
 *      show what we're about to send, before Keycloak ever sees it.
 *   3. Exchange it at the token endpoint for a real Keycloak-issued access
 *      token (this is the step that actually authenticates the client).
 *   4. Decode and print the access token's claims.
 *   5. Call userinfo with the access token — proof the token Keycloak
 *      issued is genuinely usable, not just present in the response body.
 *
 * spring.main.web-application-type=none means the app exits right after
 * this runner returns — no server stays up.
 */
@Component
public class DemoRunner implements CommandLineRunner {

    private final JwtAssertionGenerator assertionGenerator;
    private final SignedJwtTokenClient tokenClient;

    public DemoRunner(JwtAssertionGenerator assertionGenerator, SignedJwtTokenClient tokenClient) {
        this.assertionGenerator = assertionGenerator;
        this.tokenClient = tokenClient;
    }

    @Override
    public void run(String... args) throws Exception {
        divider("STEP 1: Build and RS256-sign the client assertion JWT");
        String assertion = assertionGenerator.generateSignedAssertion();
        System.out.println("Signed assertion (compact JWS):");
        System.out.println(assertion);

        divider("STEP 2: Assertion's own claims (decoded locally, unverified — just showing what we built)");
        printClaims(assertion);

        divider("STEP 3: Exchange the assertion at Keycloak's token endpoint");
        TokenResponse tokenResponse = tokenClient.fetchToken(assertion);
        System.out.println("Received access_token (expires_in=" + tokenResponse.getExpiresIn()
                + "s, token_type=" + tokenResponse.getTokenType()
                + ", scope=" + tokenResponse.getScope() + ")");
        System.out.println(tokenResponse.getAccessToken());

        divider("STEP 4: Access token claims (decoded — this came from Keycloak, not us)");
        printClaims(tokenResponse.getAccessToken());

        divider("STEP 5: Call Keycloak's userinfo endpoint with the access token");
        String userinfo = tokenClient.callUserinfo(tokenResponse.getAccessToken());
        System.out.println(userinfo);

        divider("DONE — Signed JWT (private_key_jwt) flow completed successfully");
    }

    private void printClaims(String compactJwt) throws java.text.ParseException {
        SignedJWT jwt = SignedJWT.parse(compactJwt);
        JWTClaimsSet claims = jwt.getJWTClaimsSet();
        claims.getClaims().forEach((key, value) -> System.out.println("  " + key + " = " + value));
    }

    private void divider(String label) {
        System.out.println();
        System.out.println("==================================================================");
        System.out.println(label);
        System.out.println("==================================================================");
    }
}
