# entra-idp-sync-api

Spring Boot 3.5.9 / Spring Security OAuth2 Resource Server REST API that performs
CRUD on the `idp_user`, `idp_user_role`, `idp_user_group`, and `idp_sync_audit`
tables in the `ecommerce_demo` MySQL 8 schema.

This API is a **direct, 1:1 replacement surface** for the JDBC calls the custom
Keycloak authenticators (`ExtendedEntraCreateUserAuthenticator`,
`EntraPostLoginRoleGroupSyncAuthenticator`, `RoleGroupSync`, `EntraUserSyncDao`)
currently make directly against MySQL. **Nothing in the authenticator project
was touched to build this API** — the authenticators still use JDBC today.
Swapping them to call this API over HTTP (OAuth2 `client_credentials`) instead
of JDBC is a deliberately separate, later step.

---

## 1. Schema prerequisite

Run the DDL you supplied against `ecommerce_demo` **before** starting this API
(it starts with `spring.jpa.hibernate.ddl-auto: validate`, so it expects the
tables to already exist exactly as given — it will not create them).

---

## 2. DAO-method -> REST-endpoint map

This is the mapping that matters for the future authenticator swap. Everything
the authenticators currently do via `EntraUserSyncDao` / `RoleGroupSync` has a
direct equivalent here:

| Old JDBC call (DAO / RoleGroupSync)     | New REST endpoint                                         |
|------------------------------------------|------------------------------------------------------------|
| `dao.upsertUser(...)` (create or update) | `PUT /api/v1/idp-users/by-entra-object-id/{entraObjectId}` |
| `dao.findIdByEntraObjectId(...)`         | `GET /api/v1/idp-users/by-entra-object-id/{entraObjectId}` |
| `dao.currentActiveRoles(...)`            | `GET /api/v1/idp-users/{id}/roles/active-names`             |
| `dao.currentActiveGroups(...)`           | `GET /api/v1/idp-users/{id}/groups/active-names`            |
| `RoleGroupSync.grantRoleInKeycloakAndDb` (DB half) | `POST /api/v1/idp-users/{id}/roles`               |
| `RoleGroupSync.revokeRoleInKeycloakAndDb` (DB half)| `DELETE /api/v1/idp-users/{id}/roles/{roleName}`  |
| `RoleGroupSync.joinGroupInKeycloakAndDb` (DB half) | `POST /api/v1/idp-users/{id}/groups`              |
| `RoleGroupSync.leaveGroupInKeycloakAndDb` (DB half)| `DELETE /api/v1/idp-users/{id}/groups/{groupName}`|
| `dao.touchLastLogin(...)`                | `POST /api/v1/idp-users/{id}/touch-login`                   |

Important: `RoleGroupSync` does **two** things per grant/revoke/join/leave —
the Keycloak-side role/group assignment, and the MySQL write. Only the MySQL
write moves to this API. The Keycloak-side call (`realm.getRole(...)`,
`user.grantRole(...)`, group membership, etc.) stays inside the authenticator,
unchanged, because that logic needs the live `KeycloakSession`/`RealmModel`
that only the authenticator has.

General-purpose CRUD/admin endpoints not currently called by the authenticators,
provided for completeness / future admin tooling:

- `GET /api/v1/idp-users/{id}` — fetch by internal id
- `GET /api/v1/idp-users?email=&status=` — paged list, optional filters
- `PATCH /api/v1/idp-users/{id}` — partial profile update
- `DELETE /api/v1/idp-users/{id}` — deletes user and cascades to roles/groups/audit (DB `ON DELETE CASCADE`)
- `GET /api/v1/idp-users/{id}/roles?activeOnly=` — full role history or active-only
- `GET /api/v1/idp-users/{id}/groups?activeOnly=` — full group history or active-only
- `GET /api/v1/idp-users/{id}/audit?eventType=` — paged audit trail (read-only; audit rows are only ever written as a side effect of the other operations)

---

## 3. Keycloak setup (OAuth2 client_credentials)

This API is a stateless OAuth2 Resource Server: every `/api/**` call must carry
a Keycloak-issued JWT access token with the realm role `idp-sync-service`. Set
this up once in the Keycloak admin console, on the same realm the authenticators
use (e.g. `jpa-demo-realm`):

1. **Realm roles** → **Create role** → name it exactly `idp-sync-service`.
2. **Clients** → **Create client**:
   - Client type: `OpenID Connect`
   - Client ID: `entra-idp-sync-client` (or any name you like)
   - Client authentication: **On** (this makes it confidential)
   - Enable **Service accounts roles** under "Authentication flow"
   - Save, then note the generated **Client secret** under the Credentials tab.
3. Still on that client, go to **Service account roles** → **Assign role** →
   pick `idp-sync-service` (filter by realm roles) → Assign.

That client can now obtain a token via `client_credentials` that carries
`realm_access.roles: ["idp-sync-service"]`, which `KeycloakRealmRoleConverter`
maps to the Spring Security authority `ROLE_IDP_SYNC_SERVICE` — exactly what
`SecurityConfig` requires on every endpoint.

### Getting a token (example)

```bash
curl -s -X POST \
  "http://localhost:8080/realms/jpa-demo-realm/protocol/openid-connect/token" \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "grant_type=client_credentials" \
  -d "client_id=entra-idp-sync-client" \
  -d "client_secret=<the-client-secret>" \
  | jq -r .access_token
```

Save that into a shell variable for the examples below:

```bash
TOKEN=$(curl -s -X POST \
  "http://localhost:8080/realms/jpa-demo-realm/protocol/openid-connect/token" \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "grant_type=client_credentials" \
  -d "client_id=entra-idp-sync-client" \
  -d "client_secret=<the-client-secret>" \
  | jq -r .access_token)
```

---

## 4. HTTP vs HTTPS issuer-uri (deliberate dev choice)

`application.yml` points
`security.oauth2.resourceserver.jwt.issuer-uri` at
`http://localhost:8080/realms/jpa-demo-realm` — **plain HTTP**, not HTTPS.

This is intentional for this training/dev setup: Keycloak's dev-mode TLS
certificate is self-signed, and this JVM's default truststore does not trust
it. Pointing the issuer-uri at HTTPS without first importing that cert into a
truststore (or disabling hostname/cert verification, which is worse) would
make every token validation call fail with an SSL handshake error. Since this
is a local training lab (not production), HTTP is the pragmatic choice. In a
real deployment, Keycloak would sit behind a properly-issued TLS certificate
and the issuer-uri would be `https://...`.

---

## 5. Running it

```bash
cd entra-idp-sync-api
mvn clean package
java -jar target/entra-idp-sync-api-0.0.1-SNAPSHOT.jar
```

Swagger UI: http://localhost:8082/swagger-ui.html
Health check (no auth required): http://localhost:8082/actuator/health

### Environment variables

| Variable               | Default                                              | Meaning                               |
|-------------------------|-------------------------------------------------------|----------------------------------------|
| `API_PORT`             | `8082`                                                | Port this API listens on              |
| `DB_HOST`              | `localhost`                                           | MySQL host                            |
| `DB_PORT`              | `3306`                                                | MySQL port                            |
| `DB_NAME`              | `ecommerce_demo`                                      | Schema name                           |
| `DB_USER`              | `root`                                                | MySQL user                            |
| `DB_PASSWORD`          | `localroot`                                           | MySQL password                        |
| `KEYCLOAK_ISSUER_URI`  | `http://localhost:8080/realms/jpa-demo-realm`         | Keycloak realm issuer for JWT validation |

---

## 6. Example usage (end-to-end)

Create/update a user (what the create-user authenticator would do on first login):

```bash
curl -s -X PUT "http://localhost:8082/api/v1/idp-users/by-entra-object-id/11111111-aaaa-bbbb-cccc-222222222222" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "keycloakUserId": "33333333-dddd-eeee-ffff-444444444444",
    "email": "susan.smith@rollingstonecorp.com",
    "firstName": "Susan",
    "lastName": "Smith"
  }'
```

Response (201 Created on first call, 200 OK on subsequent calls) returns the
`IdpUserResponse`, including the internal numeric `id` you use for the role/group
calls below.

Grant a role:

```bash
curl -s -X POST "http://localhost:8082/api/v1/idp-users/1/roles" \
  -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
  -d '{"roleName": "order-approver"}'
```

Get currently active role names (what the post-login sync diffs against):

```bash
curl -s "http://localhost:8082/api/v1/idp-users/1/roles/active-names" \
  -H "Authorization: Bearer $TOKEN"
```

Revoke a role:

```bash
curl -s -X DELETE "http://localhost:8082/api/v1/idp-users/1/roles/order-approver" \
  -H "Authorization: Bearer $TOKEN"
```

Join / leave a group follow the same shape under `/groups`.

View the audit trail for a user:

```bash
curl -s "http://localhost:8082/api/v1/idp-users/1/audit?sort=eventAt,asc" \
  -H "Authorization: Bearer $TOKEN"
```

---

## 7. `event_type` values written to `idp_sync_audit`

Matches exactly what the DDL comment specified, via the `SyncEventType` enum:

`USER_CREATED`, `USER_UPDATED`, `ROLE_GRANTED`, `ROLE_REVOKED`, `GROUP_JOINED`, `GROUP_LEFT`

---

## 8. What's deliberately NOT included

- No write endpoints for `idp_sync_audit` — it is read-only via this API, since
  every row is written only as a side effect of a user/role/group operation.
- No changes whatsoever to the existing authenticator project. This API is
  additive; the authenticators keep using JDBC until the explicit next step.

CLIENT_SECRET=vahXuU2dCm8MGRhYb1s6NiaAXMW7wN6h ./test-entra-idp-sync-api.sh