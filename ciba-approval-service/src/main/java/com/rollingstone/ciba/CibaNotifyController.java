package com.rollingstone.ciba;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;

/**
 * This is the URI you configure Keycloak's ciba-http-auth-channel provider
 * to call (see the --spi-ciba-auth-channel-... startup flag). Keycloak POSTs
 * here, server-to-server, immediately after accepting a
 * POST /ext/ciba/auth request — this is the "notify the Authentication
 * Device" step of the CIBA flow.
 *
 * CONFIRMED from a live 415 in the server log: Keycloak sends this
 * notification as application/json, not form-urlencoded. The Authorization
 * header still carries a bearer token that MUST be echoed back, unchanged,
 * on our later callback (see KeycloakCibaCallbackService).
 *
 * Also requires HTTP 201 Created on this response — not 200 — per
 * Keycloak's HttpAuthenticationChannelProvider implementation (the CIBA
 * spec itself describes 204, but Keycloak's own code checks for 201).
 */
@RestController
public class CibaNotifyController {

    private static final Logger LOG = LoggerFactory.getLogger(CibaNotifyController.class);

    private final PendingRequestStore store;

    public CibaNotifyController(PendingRequestStore store) {
        this.store = store;
    }

    @PostMapping(value = "/ciba/notify", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Void> notify(@RequestHeader(HttpHeaders.AUTHORIZATION) String authorizationHeader,
                                       @RequestBody CibaNotifyRequest body) {

        String bearerToken = authorizationHeader.replaceFirst("(?i)^Bearer\\s+", "");

        LOG.info("Received CIBA notification: login_hint={}, scope={}, binding_message={}",
                body.getLoginHint(), body.getScope(), body.getBindingMessage());

        store.set(new PendingCibaRequest(bearerToken, body.getLoginHint(), body.getScope(),
                body.getBindingMessage(), Instant.now()));

        return ResponseEntity.status(HttpStatus.CREATED).build();
    }
}