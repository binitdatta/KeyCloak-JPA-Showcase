# X509 Certificate Client Authentication — HAVI JPA & Keycloak Lab

Episode B of the training series: demonstrates Keycloak's **X509 Certificate**
client authenticator — mutual TLS (mTLS), where the client proves its
identity by presenting a certificate *during the TLS handshake itself*,
rather than sending a shared secret or a signed JWT in the request body.
Built as a "Try it live" page inside the `keycloak-jpa-demo` training webapp.

## What this proves, and why it matters

Every other client-authentication episode in this lab (`Signed Jwt`,
`Client Id and Secret`) proves identity with something sent *inside* an
ordinary HTTPS request — a password-like secret, or a signed token. X509
Certificate authentication moves that proof down a layer: identity is
established as part of negotiating the TLS connection, before any
application-level request body even exists. Keycloak's HTTPS listener asks
the connecting client for a certificate; the client presents one; Keycloak
checks that certificate's Subject DN against what's configured on the
client record. Only then does the actual `client_credentials` token request
proceed — and that request carries no secret, no assertion, nothing beyond
`grant_type` and `client_id`. The certificate *is* the credential.

This requires an actual mTLS-capable HTTPS listener, which Keycloak's plain
`start-dev` on `:8080` does not provide — so this episode also stands up a
second, HTTPS listener on `:8443` configured to request a client
certificate, while leaving the existing `:8080` HTTP setup (browser login,
every other episode) completely untouched.

## Prerequisites

- JDK 21, Maven (same as the rest of this project)
- Keycloak 26.x running locally from an unzipped distribution (not Docker),
  since this episode needs Keycloak restarted with extra `bin/kc.sh` flags
- `openssl` and `keytool` (keytool ships with the JDK)
- Realm `jpa-demo-realm` already imported

---

## 1. Generate certificates

Three certificates are needed: a lab Certificate Authority (CA), Keycloak's
own HTTPS server certificate (signed by that CA), and the demo client's
mTLS certificate (also signed by that CA) — so both sides have a common
signer to trust.

From the `keycloak-jpa-demo` project root:

```bash
mkdir -p mtls
cd mtls

# Lab CA
openssl genrsa -out ca-key.pem 4096
openssl req -x509 -new -nodes -key ca-key.pem -sha256 -days 3650 -out ca-cert.pem -subj "/CN=jpa-demo-lab-ca"

# Keycloak's HTTPS server certificate (SAN required for hostname verification)
openssl genrsa -out kc-server-key.pem 2048
openssl req -new -key kc-server-key.pem -out kc-server.csr -subj "/CN=localhost"
openssl x509 -req -in kc-server.csr -CA ca-cert.pem -CAkey ca-key.pem -CAcreateserial \
  -out kc-server-cert.pem -days 825 -sha256 \
  -extfile <(printf "subjectAltName=DNS:localhost,IP:127.0.0.1")

# Client's mTLS certificate — CN matches the Keycloak client ID
openssl genrsa -out x509-cert-client-key.pem 2048
openssl req -new -key x509-cert-client-key.pem -out x509-cert-client.csr -subj "/CN=x509-cert-client"
openssl x509 -req -in x509-cert-client.csr -CA ca-cert.pem -CAkey ca-key.pem -CAcreateserial \
  -out x509-cert-client-cert.pem -days 825 -sha256

# Package client key+cert into a PKCS12 for the Java app to present during the TLS handshake
openssl pkcs12 -export -inkey x509-cert-client-key.pem -in x509-cert-client-cert.pem \
  -certfile ca-cert.pem -name x509-cert-client -out x509-cert-client-keystore.p12 -passout pass:changeit

# Truststore containing the CA — used both by our Java app (to trust Keycloak's server cert)
# and by Keycloak itself (to trust client certs presented to it)
keytool -importcert -alias jpa-demo-lab-ca -file ca-cert.pem \
  -keystore truststore.p12 -storetype PKCS12 -storepass changeit -noprompt
```

`mtls/` sits alongside this project's other generated-material folders
(e.g. `keys/` from the Signed JWT episode). None of these files should be
committed to source control — add `mtls/*.p12`, `mtls/*.pem`, and
`mtls/*.srl` to `.gitignore` if this project is version-controlled.

---

## 2. Create the Keycloak client

In the admin console, realm `jpa-demo-realm`:

**Clients → Create client**
- Client ID: `x509-cert-client`
- Client authentication: **On**
- Standard flow: **Off** (this client only ever authenticates itself — no browser login)
- Direct access grants: **Off**
- Service account roles: **On** (makes `client_credentials` work with no user in the loop)
- Save

**Credentials tab**
- Client Authenticator: **X509 Certificate**
- Allow regex pattern comparison: **On**
- Subject DN: `CN=x509-cert-client`
- Save

The Subject DN value must exactly match (as a regex, full-string match) the
Subject DN baked into the client certificate generated in Step 1 — if you
regenerate that certificate with a different CN, update this field to match.

---

## 3. Start Keycloak

Restart with two additions beyond whatever flags you already run: the HTTPS
server certificate, and `--https-client-auth=request` to ask connecting
clients for a certificate. `request` (not `required`) means the TLS
handshake still succeeds for connections that *don't* present a
certificate — so the admin console and everything else on `:8443` keeps
working; only this client's token requests actually need one.

A **trust store** is also required — separate from the server certificate —
so Keycloak knows which CA to accept client certificates from. Without it,
Keycloak will ask for a certificate but silently fail to validate whatever
gets presented, and client authentication fails with a bare `invalid_client`.

```bash
cd /Users/binitdatta/KeyCloak_Home/keycloak-26.2.5
bin/kc.sh start-dev \
  --spi-ciba-auth-channel-ciba-http-auth-channel-http-authentication-channel-uri=http://localhost:8092/ciba/notify \
  --https-certificate-file=/Users/binitdatta/Development/KeyCloak-JPA-Showcase/keycloak-jpa-demo/mtls/kc-server-cert.pem \
  --https-certificate-key-file=/Users/binitdatta/Development/KeyCloak-JPA-Showcase/keycloak-jpa-demo/mtls/kc-server-key.pem \
  --https-client-auth=request \
  --https-trust-store-file=/Users/binitdatta/Development/KeyCloak-JPA-Showcase/keycloak-jpa-demo/mtls/truststore.p12 \
  --https-trust-store-password=changeit
```

(The `--spi-ciba-auth-channel-...` flag is from an earlier episode and is
unrelated to this one — kept here only because it's part of this project's
existing startup command.)

Confirm the listener is up before moving on:

```bash
curl -kv https://localhost:8443/realms/jpa-demo-realm 2>&1 | grep -E "SSL connection|subject|HTTP/"
```

Expect `subject: CN=localhost` and `HTTP/2 200`.

---

## 4. `application.yml`

```yaml
x509-demo:
  client-id: x509-cert-client
  keystore-path: mtls/x509-cert-client-keystore.p12
  keystore-password: changeit
  key-alias: x509-cert-client
  truststore-path: mtls/truststore.p12
  truststore-password: changeit
  token-endpoint: https://localhost:8443/realms/jpa-demo-realm/protocol/openid-connect/token
  userinfo-endpoint: https://localhost:8443/realms/jpa-demo-realm/protocol/openid-connect/userinfo
```

Both endpoints use `:8443`, not `:8080`. This matters: whichever host:port
issues a token becomes that token's `iss` claim, and Keycloak's userinfo
endpoint rejects a token whose issuer doesn't match the endpoint being
called. Since the token is issued at `:8443`, userinfo must also be called
at `:8443` — even though that second call doesn't itself need a client
certificate (`request` mode allows a plain TLS connection through).

---

## 5. Java files

All under `keycloak-jpa-demo`'s existing package structure
(`com.rollingstone.demo`):

```
src/main/java/com/rollingstone/demo/
  service/X509MtlsContextFactory.java   <- builds the SSLContext from keystore + truststore;
                                            also loads the client cert for display
  service/X509DemoService.java          <- runs the flow: load cert -> mTLS token exchange
                                            -> decode claims -> userinfo (same :8443 client
                                            reused for both calls)
  dto/X509RunResult.java                <- result DTO: cert details, access token, claims,
                                            userinfo, success/error
  controller/X509UiController.java      <- GET/POST /training/x509-certificate-demo
src/main/resources/templates/
  training/x509-certificate-demo.html   <- "Try it live" page
  fragments/navbar.html                 <- nav link added under Training -> Keycloak Client
                                            Authentication -> B. X509 Certificate - Live Demo
```

No `SecurityConfig` change was needed — `/training/**` is already
`permitAll`. `pom.xml`'s Nimbus JOSE+JWT dependency (added for the Signed
JWT episode) is reused here to decode the access token; nothing new to add
there.

### What you should see

1. **Client certificate being presented** — Subject DN, Issuer DN, serial
   number, and validity window read directly from the `.p12` keystore,
   shown before any network call happens.
2. **Access token from Keycloak** — the raw token, obtained via the mTLS
   connection to `:8443`. This is the step that actually authenticates the
   client; if the certificate, Subject DN regex, or Keycloak's trust store
   are misconfigured, this is where it fails.
3. **Access token claims (decoded)** — `azp` / `client_id` both
   `x509-cert-client`, `preferred_username` the service-account pattern
   shared with every other client_credentials episode in this lab.
4. **Userinfo response** — proof the token is genuinely usable, not merely
   present in the response body.

---

## Troubleshooting

| Symptom | Likely cause |
|---|---|
| `invalid_client` / "Invalid client or Invalid client credentials" at the token step | Keycloak's `--https-trust-store-file` is missing or wrong — without it, Keycloak has nothing to validate the presented client certificate against, so the X509 authenticator sees no usable certificate at all. |
| `401 Unauthorized` (empty body) on the userinfo call, after a successful token | Issuer mismatch — the token was issued with `iss` set to whichever host:port was called for the token request; userinfo must be called on that same host:port. Check `token-endpoint` and `userinfo-endpoint` in `application.yml` both point at `:8443`. |
| `Invalid keystore format` / BouncyCastle ASN.1 parsing errors | Not applicable to this episode's own `.p12` files — those are only ever read by the JDK's own `KeyStore` API in our Spring Boot app, never uploaded through Keycloak's admin console. (This was a separate issue specific to importing externally-built PKCS12 files through Keycloak's Keys-tab UI in the Signed JWT episode.) |
| Subject DN doesn't match / certificate rejected | The Credentials tab's **Subject DN** field is a regex matched against the full Subject DN string of the presented certificate. If the certificate was generated with additional RDNs (e.g. `OU=`, `O=`, `C=`), the regex must account for them, or regenerate the certificate with a Subject DN matching exactly what's configured. |
| Browser login (`jpa-demo-web`) or other episodes stop working after this setup | Shouldn't happen — `:8080` HTTP is untouched by any of these flags. If it does, confirm `--https-client-auth` is `request`, not `required` (which would reject *any* TLS connection on `:8443` lacking a client certificate, though this still should not affect `:8080`). |

---

## Project layout addition

```
keycloak-jpa-demo/
  mtls/                                          <- certs/keystores from Step 1 (gitignored)
    ca-key.pem, ca-cert.pem
    kc-server-key.pem, kc-server-cert.pem
    x509-cert-client-key.pem, x509-cert-client-cert.pem
    x509-cert-client-keystore.p12
    truststore.p12
  src/main/java/com/rollingstone/demo/
    service/X509MtlsContextFactory.java
    service/X509DemoService.java
    dto/X509RunResult.java
    controller/X509UiController.java
  src/main/resources/templates/training/
    x509-certificate-demo.html
```
