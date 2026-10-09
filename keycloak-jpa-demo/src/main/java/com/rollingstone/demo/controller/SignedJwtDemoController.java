package com.rollingstone.demo.controller;

import com.rollingstone.demo.dto.SignedJwtRunResult;
import com.rollingstone.demo.service.SignedJwtDemoService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Backs the "Try it live" panel on /training/signed-jwt. permitAll in
 * SecurityConfig — same reasoning as ExternalAuthController: this demo
 * authenticates itself with its own service-account credentials (the signed
 * JWT), not the visiting user's site session, so no login is required to
 * try it from the training page.
 */
@RestController
public class SignedJwtDemoController {

    private final SignedJwtDemoService signedJwtDemoService;

    public SignedJwtDemoController(SignedJwtDemoService signedJwtDemoService) {
        this.signedJwtDemoService = signedJwtDemoService;
    }

    @PostMapping("/api/signed-jwt/run")
    public ResponseEntity<SignedJwtRunResult> run() {
        return ResponseEntity.ok(signedJwtDemoService.run());
    }
}