# signed-jwt-demo-client

Standalone Spring Boot CLI app that demonstrates Keycloak's **Signed Jwt**
(`private_key_jwt`) client authenticator — Episode A of the training series,
now with real working code to go with the training page.

Unlike `ciba-approval-service`, this needs **no new running service**. It's a
one-shot program: build an RSA keypair, hand Keycloak the public half, run
this app, and it signs a client assertion, trades it for a token, and proves
the token works — then exits.

## What this proves, and why it matters

A normal confidential client (`jpa-demo-web`, `capability-demo-client`, etc.)
authenticates with a **shared secret** — a string both sides know, sent over
the wire on every token request. `private_key_jwt` removes that shared secret
entirely: the client proves its identity by **signing a JWT with a private
key only it holds**, and Keycloak verifies that signature against a **public**
key/certificate it was given in advance. The private key itself never
crosses the network, ever — not at setup time, not at request time.

## Prerequisites

- JDK 21, Maven (same as the other modules in this project)
- Keycloak 26.x running locally on `http://localhost:8080`, realm
  `jpa-demo-realm` already imported (same realm every other episode uses)
- `keytool`, which ships with every JDK — no separate install

---

## Step 1 — Generate the RSA keypair

From this module's root directory:

```bash
keytool -genkeypair -alias signed-jwt-client -keyalg RSA -keysize 2048 -keystore keys/signed-jwt-client-keystore.p12 -storetype PKCS12 -storepass changeit -keypass changeit -validity 3650 -dname "CN=signed-jwt-client"
```

This creates `keys/signed-jwt-client-keystore.p12`, holding both the private
key (which stays on disk, only this app ever reads it) and a self-signed
public certificate (which Keycloak needs next).

Export just the public certificate, to hand to Keycloak in the next step:

```bash
keytool -exportcert -alias signed-jwt-client -keystore keys/signed-jwt-client-keystore.p12 -storepass changeit -file keys/signed-jwt-client-cert.pem -rfc
```

`keys/signed-jwt-client-cert.pem` is safe to hand to Keycloak, paste into a
chat, or commit to a repo if you want — it's the public half.
`keys/signed-jwt-client-keystore.p12` is **not** — it contains the private
key. Keep it out of source control (the `.gitignore` below covers it, but
double-check if you fold this into a monorepo).

---

## Step 2 — Configure the client in Keycloak

The realm already has a placeholder client for this, `signed-jwt-client`
(created back when `realm-export.json` was first built, with a fake
`jwks.url`). Reconfigure it to actually work with the keypair from Step 1:

1. **Clients → signed-jwt-client → Settings** → confirm **Capability config**:
   Client authentication: **On**. Standard flow, Direct access grants:
   **Off** (this client only ever authenticates itself — no browser login).
   Service accounts roles: **On** (this is what makes `client_credentials`
   work with no user in the loop, same as `capability-demo-client` in
   Episode D).
2. **Credentials tab → Client Authenticator** → select **Signed Jwt** (the
   same dropdown you saw when we screenshotted `gateway-service` earlier —
   this client just needs it actually applied and saved).
3. **Keys tab** → **Use JWKS**: turn this **Off** (the placeholder config
   pointed at a JWKS URL we never stood up — a directly imported certificate
   is simpler for a local demo and needs no hosting). With "Use JWKS" off,
   click **Import Key**:
   - Archive Format: **PKCS12**
   - Import File: `keys/signed-jwt-client-keystore.p12` from Step 1
   - Store Password: `changeit`
   - Key Alias: `signed-jwt-client`
   - Save.

Keycloak now holds the public certificate and will verify any assertion
claiming to be from `signed-jwt-client` against it. It never sees the
private key.

---

## Step 3 — Run it

```bash
mvn clean spring-boot:run
```

No server stays up — `spring.main.web-application-type=none` in
`application.yml` means the app runs `DemoRunner`'s five steps and exits.

## What you should see

Five clearly labeled sections in the console:

1. **The signed assertion itself** — a compact JWS string, freshly built and
   RS256-signed with your private key.
2. **That assertion's own claims, decoded locally** — `iss`/`sub` both equal
   `signed-jwt-client`, `aud` is the token endpoint URL, `exp` a minute out.
   This is what you're about to send, shown before Keycloak ever sees it.
3. **A real access token from Keycloak** — this is the step that actually
   authenticates; if the keypair, alias, or Keycloak config from Step 2 are
   wrong, this is where it fails (see Troubleshooting).
4. **That access token's claims, decoded** — `azp: "signed-jwt-client"`,
   `preferred_username: "service-account-signed-jwt-client"` — same
   service-account pattern documented on the Episode D training page,
   different client authenticator having produced it.
5. **A userinfo response** — proof the token is genuinely usable, not just
   present in the response body: userinfo only answers for a truly valid
   Bearer token.

---

## Troubleshooting

| Symptom | Likely cause |
|---|---|
| `invalid_client` at Step 3 | Client Authenticator on the Keycloak client isn't actually set to **Signed Jwt** (still on Client Id and Secret), or the imported key's alias doesn't match `signed-jwt.key-alias` in `application.yml`. |
| `invalid_client_credentials` / signature verification failure | The certificate imported into Keycloak's Keys tab doesn't match the private key this app is signing with — re-run Step 1 and Step 2's import together, don't mix keystores from different runs. |
| `Audience` / `aud` rejected | Some Keycloak versions are stricter about accepting only the **issuer URL** rather than the token endpoint URL. If Step 3 fails specifically on audience validation, change `keycloak.token-endpoint`'s value used for signing — edit `JwtAssertionGenerator`'s `audience` source in `application.yml` to `http://localhost:8080/realms/jpa-demo-realm` (the issuer URL) instead of the token endpoint URL, and re-run. |
| `No private key found under alias` | The `-alias` used with `keytool` doesn't match `signed-jwt.key-alias` in `application.yml` — both must be exactly `signed-jwt-client` unless you change both together. |
| Everything hangs / connection refused | Keycloak isn't running on `localhost:8080`, or the realm name doesn't match — same checks as every other episode. |

If you hit something not listed here, paste the console output and we'll
root-cause it the same way we worked through the CIBA module.

---

## Project layout

```
signed-jwt-demo-client/
  pom.xml
  README.md
  keys/                                    <- keytool output goes here (gitignored)
  src/main/resources/application.yml
  src/main/java/com/rollingstone/signedjwt/
    SignedJwtDemoClientApplication.java    <- main() — web-application-type: none
    JwtAssertionGenerator.java             <- builds + RS256-signs the assertion
    SignedJwtTokenClient.java              <- token endpoint + userinfo HTTP calls
    TokenResponse.java                     <- token endpoint response DTO
    DemoRunner.java                        <- orchestrates steps 1-5, prints everything
```

## A note on this being untested-by-compiler

Every other fix in this project this session was verified against your
actually-running Keycloak and Spring Boot instances before being handed to
you. This module is new code that hasn't had that same live verification —
this sandbox's network policy blocks reaching Maven Central directly, so I
could not run `mvn compile` here first. I reviewed every file by hand against
the Nimbus JOSE+JWT and Spring `RestClient` APIs it uses, but "reviewed
carefully" is not the same guarantee as "compiled clean," and I want to say
that plainly rather than imply a green build that didn't happen. If
`mvn clean spring-boot:run` throws anything at Step 3's build/compile phase,
paste it back exactly the way we've done for every other issue this session
and we'll fix it root-cause-first, one command at a time.
