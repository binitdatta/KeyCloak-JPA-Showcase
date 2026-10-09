package com.rollingstone.signedjwt;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.FileInputStream;
import java.io.IOException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.util.Date;
import java.util.UUID;

/**
 * Builds and RS256-signs the RFC 7523 client assertion JWT that Keycloak's
 * "Signed Jwt" (private_key_jwt) client authenticator expects.
 *
 * Required claims per RFC 7523 §3 / OIDC Core's ClientAuthentication section,
 * and how Keycloak's ClientJWTClientAuthenticator specifically validates them:
 *   iss (issuer) — MUST equal the client_id. Keycloak reads this to know
 *                   which client's public key to verify the signature with.
 *   sub (subject) — MUST also equal the client_id for client authentication
 *                    (as opposed to a JWT *authorization grant*, where sub
 *                    would identify an end user instead).
 *   aud (audience) — MUST identify Keycloak as the intended recipient.
 *                     Keycloak accepts either its issuer URL or its token
 *                     endpoint URL here; this app uses the token endpoint URL,
 *                     per OIDC Core's stated preference.
 *   exp (expiry) — short-lived. This assertion is meant to be used once,
 *                  immediately, not cached and reused like a client secret.
 *   jti (JWT ID) — a random, single-use identifier. Keycloak can be
 *                  configured to reject a reused jti as a replay-protection
 *                  measure.
 *   iat (issued at) — when this assertion was created.
 */
@Component
public class JwtAssertionGenerator {

    private final String keystorePath;
    private final String keystorePassword;
    private final String keyAlias;
    private final String keyPassword;
    private final String clientId;
    private final String audience;
    private final long assertionLifetimeSeconds;

    public JwtAssertionGenerator(
            @Value("${signed-jwt.keystore-path}") String keystorePath,
            @Value("${signed-jwt.keystore-password}") String keystorePassword,
            @Value("${signed-jwt.key-alias}") String keyAlias,
            @Value("${signed-jwt.key-password}") String keyPassword,
            @Value("${keycloak.client-id}") String clientId,
            @Value("${keycloak.token-endpoint}") String audience,
            @Value("${signed-jwt.assertion-lifetime-seconds}") long assertionLifetimeSeconds) {
        this.keystorePath = keystorePath;
        this.keystorePassword = keystorePassword;
        this.keyAlias = keyAlias;
        this.keyPassword = keyPassword;
        this.clientId = clientId;
        this.audience = audience;
        this.assertionLifetimeSeconds = assertionLifetimeSeconds;
    }

    /**
     * Produces the compact-serialized, signed JWT to send as the
     * client_assertion form parameter.
     */
    public String generateSignedAssertion() throws JOSEException, IOException, KeyStoreException,
            NoSuchAlgorithmException, UnrecoverableKeyException, CertificateException {

        PrivateKey privateKey = loadPrivateKey();

        Date now = new Date();
        Date expiry = new Date(now.getTime() + (assertionLifetimeSeconds * 1000));

        JWTClaimsSet claims = new JWTClaimsSet.Builder()
                .issuer(clientId)
                .subject(clientId)
                .audience(audience)
                .jwtID(UUID.randomUUID().toString())
                .issueTime(now)
                .expirationTime(expiry)
                .build();

        SignedJWT signedJWT = new SignedJWT(
                new JWSHeader.Builder(JWSAlgorithm.RS256).build(),
                claims);

        signedJWT.sign(new RSASSASigner(privateKey));

        return signedJWT.serialize();
    }

    private PrivateKey loadPrivateKey() throws IOException, KeyStoreException, NoSuchAlgorithmException,
            UnrecoverableKeyException, CertificateException {
        KeyStore keyStore = KeyStore.getInstance("PKCS12");
        try (FileInputStream in = new FileInputStream(keystorePath)) {
            keyStore.load(in, keystorePassword.toCharArray());
        }
        PrivateKey key = (PrivateKey) keyStore.getKey(keyAlias, keyPassword.toCharArray());
        if (key == null) {
            throw new IllegalStateException("No private key found under alias '" + keyAlias
                    + "' in keystore " + keystorePath
                    + " — check signed-jwt.key-alias matches the -alias used with keytool.");
        }
        return key;
    }
}
