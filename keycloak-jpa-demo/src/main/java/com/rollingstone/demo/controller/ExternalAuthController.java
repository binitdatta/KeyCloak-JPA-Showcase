package com.rollingstone.demo.controller;

import com.rollingstone.demo.dto.ExternalAuthRequest;
import com.rollingstone.demo.dto.ExternalAuthResponse;
import com.rollingstone.demo.service.ExternalAuthService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * Called by the custom Keycloak SPI authenticator
 * (ExtendedCreateUserIfUniqueAuthenticator, see custom-providers/first-broker-login)
 * during First Broker Login, BEFORE Keycloak creates the local broker user.
 * This is the "external authentication source" referenced in the training
 * page for First Broker Login.
 *
 * Left permitAll() in SecurityConfig; lock this down with mTLS or a shared
 * secret header before using it against anything but a local demo realm.
 */
@RestController
public class ExternalAuthController {

    private final ExternalAuthService externalAuthService;

    public ExternalAuthController(ExternalAuthService externalAuthService) {
        this.externalAuthService = externalAuthService;
    }

    @PostMapping("/api/auth/validate")
    public ResponseEntity<ExternalAuthResponse> validate(@Valid @RequestBody ExternalAuthRequest request) {
        ExternalAuthResponse response = externalAuthService.authenticate(request);
        if (!response.isAuthenticated()) {
            return ResponseEntity.status(401).body(response);
        }
        return ResponseEntity.ok(response);
    }
}
