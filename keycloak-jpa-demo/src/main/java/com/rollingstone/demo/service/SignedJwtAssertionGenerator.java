package com.rollingstone.demo.service;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.FileInputStream;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.util.Date;
import java.util.UUID;

/**
 * Builds and RS256-signs the client_assertion JWT for the private_key_jwt
 * (Signed Jwt) flow — same mechanics as the standalone signed-jwt-demo-client
 * CLI module, wired for the "Try it live" panel on /training/signed-jwt.
 *
 * The "aud" claim is configured separately from the URL we POST to. This realm's
 * Frontend URL is pinned to https://localhost:8443 (needed for the Entra SAML
 * setup), so Keycloak only accepts audiences built from that base, even when the
 * token request itself is sent to the plain-HTTP :8080 listener.
 */
@Component
public class SignedJwtAssertionGenerator {

    private final String clientId;
    private final String audience;
    private final String keystorePath;
    private final String keystorePassword;
    private final String keyAlias;

    public SignedJwtAssertionGenerator(@Value("${signed-jwt.client-id}") String clientId,
                                       @Value("${signed-jwt.audience:${signed-jwt.token-endpoint}}") String audience,
                                       @Value("${signed-jwt.keystore-path}") String keystorePath,
                                       @Value("${signed-jwt.keystore-password}") String keystorePassword,
                                       @Value("${signed-jwt.key-alias}") String keyAlias) {
        this.clientId = clientId;
        this.audience = audience;
        this.keystorePath = keystorePath;
        this.keystorePassword = keystorePassword;
        this.keyAlias = keyAlias;
    }

    public String generateSignedAssertion() throws Exception {
        KeyStore keyStore = KeyStore.getInstance("PKCS12");
        try (FileInputStream fis = new FileInputStream(keystorePath)) {
            keyStore.load(fis, keystorePassword.toCharArray());
        }
        PrivateKey privateKey = (PrivateKey) keyStore.getKey(keyAlias, keystorePassword.toCharArray());
        if (privateKey == null) {
            throw new IllegalStateException(
                    "No private key found under alias '" + keyAlias + "' in " + keystorePath);
        }

        Date now = new Date();
        Date expiry = new Date(now.getTime() + 60_000L);

        JWTClaimsSet claims = new JWTClaimsSet.Builder()
                .issuer(clientId)
                .subject(clientId)
                .audience(audience)
                .expirationTime(expiry)
                .issueTime(now)
                .jwtID(UUID.randomUUID().toString())
                .build();

        SignedJWT signedJWT = new SignedJWT(new JWSHeader(JWSAlgorithm.RS256), claims);
        signedJWT.sign(new RSASSASigner(privateKey));
        return signedJWT.serialize();
    }
}