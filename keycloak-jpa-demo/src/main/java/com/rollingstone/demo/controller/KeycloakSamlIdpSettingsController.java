// CHANGE THIS PACKAGE to a package scanned by your @SpringBootApplication.
package com.rollingstone.demo.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class KeycloakSamlIdpSettingsController {
    @GetMapping("/training/keycloak-saml-idp-settings")
    public String showSettings() {
        return "training/keycloak-saml-idp-settings";
    }
}
