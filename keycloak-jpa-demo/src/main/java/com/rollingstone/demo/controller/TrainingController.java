package com.rollingstone.demo.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * One route per training article. Each maps to a static-content Thymeleaf
 * page under templates/training/ written as a step-by-step script for a
 * YouTube walkthrough.
 *
 * Episode lettering: A-D are the built-in Keycloak client-authentication
 * mechanisms and grant types; E is CIBA (its own page — needs an
 * Authentication Channel Provider Keycloak doesn't ship a working default
 * for, so it's documented separately from D rather than bundled in);
 * F1-F3 are the custom Keycloak SPI providers; G is JPA relationships.
 */
@Controller
public class TrainingController {

    @GetMapping("/training")
    public String trainingIndex() {
        return "training/index";
    }

    @GetMapping("/training/signed-jwt-generate-keys")
    public String signedJwtGenerateKeys() {
        return "training/signed-jwt-generate-keys";
    }

    @GetMapping("/training/signed-jwt")
    public String signedJwt() {
        return "training/signed-jwt";
    }

    @GetMapping("/training/x509-certificate")
    public String x509Certificate() {
        return "training/x509-certificate";
    }

    @GetMapping("/training/signed-jwt-client-secret")
    public String signedJwtClientSecret() {
        return "training/signed-jwt-client-secret";
    }

    @GetMapping("/training/capability-config")
    public String capabilityConfig() {
        return "training/capability-config";
    }

    @GetMapping("/training/ciba")
    public String ciba() {
        return "training/ciba";
    }

    @GetMapping("/training/custom-client-authenticator")
    public String customClientAuthenticator() {
        return "training/custom-client-authenticator";
    }

    @GetMapping("/training/first-broker-login")
    public String firstBrokerLogin() {
        return "training/first-broker-login";
    }

    @GetMapping("/training/post-login-flow")
    public String postLoginFlow() {
        return "training/post-login-flow";
    }

    @GetMapping("/training/keycloak-ext")
    public String keycloakExtensions() {
        return "training/keycloak-entra-custom-authenticators-guide-with-navbar";
    }

    @GetMapping("/training/keycloak-browser-flow")
    public String keycloakBrowserFlow() {
        return "training/keycloak-browser-vs-custom-entra-first-broker-login-with-navbar-and-flow-image";
    }



    @GetMapping("/training/jpa-relationships")
    public String jpaRelationships() {
        return "training/jpa-relationships";
    }
}