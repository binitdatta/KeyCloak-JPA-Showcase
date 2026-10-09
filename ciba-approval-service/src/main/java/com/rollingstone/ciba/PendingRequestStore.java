package com.rollingstone.ciba;

import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicReference;

/**
 * Holds at most ONE pending CIBA request at a time — deliberate for a demo
 * approval device: a real Authentication Device would key requests by
 * auth_req_id / user and support many concurrent pending approvals, but a
 * single AtomicReference is enough to prove the protocol round-trip end to
 * end, and keeps the poll-and-approve page trivial ("is there a request
 * right now, yes or no").
 */
@Component
public class PendingRequestStore {

    private final AtomicReference<PendingCibaRequest> pending = new AtomicReference<>();

    public void set(PendingCibaRequest request) {
        pending.set(request);
    }

    public PendingCibaRequest get() {
        return pending.get();
    }

    public void clear() {
        pending.set(null);
    }
}
