package com.rollingstone.demo.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;
import java.io.FileInputStream;
import java.security.KeyStore;
import java.security.cert.X509Certificate;

/**
 * Builds the SSLContext used for the one outbound call that actually needs
 * mutual TLS: the token request. The keystore holds x509-cert-client's
 * private key + certificate (presented to Keycloak during the TLS
 * handshake — this certificate IS the credential, there's no secret or
 * signed assertion involved). The truststore holds the lab CA, so this JVM
 * trusts Keycloak's self-signed HTTPS server certificate.
 */
@Component
public class X509MtlsContextFactory {

    private final String keystorePath;
    private final String keystorePassword;
    private final String truststorePath;
    private final String truststorePassword;
    private final String keyAlias;

    public X509MtlsContextFactory(@Value("${x509-demo.keystore-path}") String keystorePath,
                                  @Value("${x509-demo.keystore-password}") String keystorePassword,
                                  @Value("${x509-demo.truststore-path}") String truststorePath,
                                  @Value("${x509-demo.truststore-password}") String truststorePassword,
                                  @Value("${x509-demo.key-alias}") String keyAlias) {
        this.keystorePath = keystorePath;
        this.keystorePassword = keystorePassword;
        this.truststorePath = truststorePath;
        this.truststorePassword = truststorePassword;
        this.keyAlias = keyAlias;
    }

    public SSLContext buildSslContext() throws Exception {
        KeyStore keyStore = KeyStore.getInstance("PKCS12");
        try (FileInputStream fis = new FileInputStream(keystorePath)) {
            keyStore.load(fis, keystorePassword.toCharArray());
        }
        KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        kmf.init(keyStore, keystorePassword.toCharArray());

        KeyStore trustStore = KeyStore.getInstance("PKCS12");
        try (FileInputStream fis = new FileInputStream(truststorePath)) {
            trustStore.load(fis, truststorePassword.toCharArray());
        }
        TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        tmf.init(trustStore);

        SSLContext sslContext = SSLContext.getInstance("TLS");
        sslContext.init(kmf.getKeyManagers(), tmf.getTrustManagers(), null);
        return sslContext;
    }

    /** Used by the UI to show which certificate is about to be presented, before the call happens. */
    public X509Certificate loadClientCertificate() throws Exception {
        KeyStore keyStore = KeyStore.getInstance("PKCS12");
        try (FileInputStream fis = new FileInputStream(keystorePath)) {
            keyStore.load(fis, keystorePassword.toCharArray());
        }
        return (X509Certificate) keyStore.getCertificate(keyAlias);
    }
}