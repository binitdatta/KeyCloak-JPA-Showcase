package com.rollingstone.ciba;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;

/**
 * Backs the poll-and-approve page at /index.html (served as a static
 * resource, so it needs no controller of its own):
 *   GET  /ciba/pending  -> does a CIBA request exist right now?
 *   POST /ciba/approve  -> human clicked Approve; calls back to Keycloak with SUCCEED
 *   POST /ciba/deny     -> human clicked Deny; calls back to Keycloak with UNAUTHORIZED
 */
@RestController
public class CibaApprovalController {

    private static final Logger LOG = LoggerFactory.getLogger(CibaApprovalController.class);

    private final PendingRequestStore store;
    private final KeycloakCibaCallbackService callbackService;

    public CibaApprovalController(PendingRequestStore store, KeycloakCibaCallbackService callbackService) {
        this.store = store;
        this.callbackService = callbackService;
    }

    @GetMapping("/ciba/pending")
    public PendingStatusResponse pending() {
        PendingCibaRequest request = store.get();
        return request == null ? PendingStatusResponse.none() : PendingStatusResponse.from(request);
    }

    @PostMapping("/ciba/approve")
    public ResponseEntity<String> approve() {
        return respond("SUCCEED");
    }

    @PostMapping("/ciba/deny")
    public ResponseEntity<String> deny() {
        return respond("UNAUTHORIZED");
    }

    private ResponseEntity<String> respond(String status) {
        PendingCibaRequest request = store.get();
        if (request == null) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body("No pending CIBA request.");
        }
        try {
            callbackService.sendCallback(request.getBearerToken(), status);
            store.clear();
            return ResponseEntity.ok("Sent " + status + " to Keycloak.");
        } catch (IOException | InterruptedException e) {
            LOG.error("Failed to send CIBA callback to Keycloak", e);
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body("Callback to Keycloak failed: " + e.getMessage());
        }
    }
}
