#!/usr/bin/env bash
#
# test-entra-idp-sync-api.sh
#
# End-to-end smoke test for entra-idp-sync-api. Gets its own OAuth2 token via
# the client_credentials grant (service account), so there is no browser
# login step -- just run it.
#
# Prereqs:
#   - entra-idp-sync-api running (default http://localhost:8082)
#   - Keycloak realm role "idp-sync-service" created
#   - A confidential client with Service Accounts enabled, that role
#     assigned to its service account (see README.md section 3)
#   - jq and curl installed
#
# Usage:
#   CLIENT_SECRET=xxxxx ./test-entra-idp-sync-api.sh
#
# All other settings below have sane defaults matching the project's
# application.yml / README and can be overridden the same way, e.g.:
#   API_BASE=http://localhost:8082 KEYCLOAK_REALM=jpa-demo-realm \
#   CLIENT_ID=entra-idp-sync-client CLIENT_SECRET=xxxxx ./test-entra-idp-sync-api.sh

set -uo pipefail

# ---------------------------------------------------------------------------
# Config (override via env vars)
# ---------------------------------------------------------------------------
API_BASE="${API_BASE:-http://localhost:8082}"
KEYCLOAK_BASE="${KEYCLOAK_BASE:-http://localhost:8080}"
KEYCLOAK_REALM="${KEYCLOAK_REALM:-jpa-demo-realm}"
CLIENT_ID="${CLIENT_ID:-entra-idp-sync-client}"
CLIENT_SECRET="${CLIENT_SECRET:-}"

# Test data -- fake Entra identity, no real login/session needed.
ENTRA_OBJECT_ID="${ENTRA_OBJECT_ID:-test-entra-oid-00000000-1111-2222-3333-444444444444}"
KC_USER_ID="test-kc-uid-55555555-6666-7777-8888-999999999999"
TEST_EMAIL="qa.smoketest@rollingstonecorp.com"
TEST_FIRST="Quinn"
TEST_LAST="Smoketest"
TEST_ROLE="order-approver"
TEST_GROUP="Finance"

PASS=0
FAIL=0

# ---------------------------------------------------------------------------
# Helpers
# ---------------------------------------------------------------------------
color() { printf "\033[%sm%s\033[0m" "$1" "$2"; }
ok()    { PASS=$((PASS+1)); echo "$(color 32 PASS) $1"; }
bad()   { FAIL=$((FAIL+1)); echo "$(color 31 FAIL) $1"; }
info()  { echo "$(color 36 INFO) $1"; }

# curl wrapper: $1=method $2=path $3=body(optional) -> sets HTTP_STATUS and BODY
req() {
    local method="$1" path="$2" body="${3:-}"
    local resp
    if [[ -n "$body" ]]; then
        resp=$(curl -s -w '\n%{http_code}' -X "$method" "${API_BASE}${path}" \
            -H "Authorization: Bearer ${TOKEN}" \
            -H "Content-Type: application/json" \
            -d "$body")
    else
        resp=$(curl -s -w '\n%{http_code}' -X "$method" "${API_BASE}${path}" \
            -H "Authorization: Bearer ${TOKEN}")
    fi
    HTTP_STATUS=$(echo "$resp" | tail -n1)
    BODY=$(echo "$resp" | sed '$d')
}

expect_status() {
    local desc="$1" expected="$2"
    if [[ "$HTTP_STATUS" == "$expected" ]]; then
        ok "$desc (HTTP $HTTP_STATUS)"
    else
        bad "$desc (expected HTTP $expected, got $HTTP_STATUS) -- body: $BODY"
    fi
}

# ---------------------------------------------------------------------------
# 0. Get an OAuth2 token via client_credentials (no browser login)
# ---------------------------------------------------------------------------
if [[ -z "$CLIENT_SECRET" ]]; then
    echo "$(color 31 ERROR) Set CLIENT_SECRET, e.g.:"
    echo "  CLIENT_SECRET=xxxxx $0"
    exit 1
fi

info "Requesting client_credentials token from Keycloak..."
TOKEN_RESPONSE=$(curl -s -X POST \
    "${KEYCLOAK_BASE}/realms/${KEYCLOAK_REALM}/protocol/openid-connect/token" \
    -H "Content-Type: application/x-www-form-urlencoded" \
    -d "grant_type=client_credentials" \
    -d "client_id=${CLIENT_ID}" \
    -d "client_secret=${CLIENT_SECRET}")

TOKEN=$(echo "$TOKEN_RESPONSE" | jq -r '.access_token // empty')

if [[ -z "$TOKEN" ]]; then
    echo "$(color 31 ERROR) Could not obtain access token. Response was:"
    echo "$TOKEN_RESPONSE" | jq . 2>/dev/null || echo "$TOKEN_RESPONSE"
    exit 1
fi
ok "Obtained access token via client_credentials"
echo

# ---------------------------------------------------------------------------
# 1. Health check (no auth required)
# ---------------------------------------------------------------------------
info "1. Health check"
HEALTH=$(curl -s -w '\n%{http_code}' "${API_BASE}/actuator/health")
HEALTH_STATUS=$(echo "$HEALTH" | tail -n1)
[[ "$HEALTH_STATUS" == "200" ]] && ok "GET /actuator/health" || bad "GET /actuator/health (HTTP $HEALTH_STATUS)"
echo

# ---------------------------------------------------------------------------
# 2. Create (upsert) the test user
# ---------------------------------------------------------------------------
info "2. Create user by Entra object id (PUT upsert)"
req PUT "/api/v1/idp-users/by-entra-object-id/${ENTRA_OBJECT_ID}" "$(cat <<JSON
{
  "keycloakUserId": "${KC_USER_ID}",
  "email": "${TEST_EMAIL}",
  "firstName": "${TEST_FIRST}",
  "lastName": "${TEST_LAST}"
}
JSON
)"
expect_status "PUT upsert (create)" "201"
USER_ID=$(echo "$BODY" | jq -r '.id // empty')
if [[ -z "$USER_ID" ]]; then
    bad "Could not extract numeric user id from create response -- aborting remaining tests"
    echo "Response was: $BODY"
    exit 1
fi
info "   -> internal id = ${USER_ID}"
echo

# ---------------------------------------------------------------------------
# 3. Re-run the same upsert -- should now be an update (200, not 201)
# ---------------------------------------------------------------------------
info "3. Upsert again (idempotent update path)"
req PUT "/api/v1/idp-users/by-entra-object-id/${ENTRA_OBJECT_ID}" "$(cat <<JSON
{
  "keycloakUserId": "${KC_USER_ID}",
  "email": "${TEST_EMAIL}",
  "firstName": "${TEST_FIRST}",
  "lastName": "${TEST_LAST}"
}
JSON
)"
expect_status "PUT upsert (update)" "200"
echo

# ---------------------------------------------------------------------------
# 4. Fetch by id and by entra object id
# ---------------------------------------------------------------------------
info "4. Fetch user"
req GET "/api/v1/idp-users/${USER_ID}"
expect_status "GET /idp-users/{id}" "200"

req GET "/api/v1/idp-users/by-entra-object-id/${ENTRA_OBJECT_ID}"
expect_status "GET /idp-users/by-entra-object-id/{entraObjectId}" "200"
echo

# ---------------------------------------------------------------------------
# 5. List users, filtered by email
# ---------------------------------------------------------------------------
info "5. List users filtered by email"
req GET "/api/v1/idp-users?email=qa.smoketest"
expect_status "GET /idp-users?email=" "200"
echo

# ---------------------------------------------------------------------------
# 6. Partial profile update (PATCH)
# ---------------------------------------------------------------------------
info "6. PATCH profile (city/state)"
req PATCH "/api/v1/idp-users/${USER_ID}" '{"city": "Chicago", "state": "IL"}'
expect_status "PATCH /idp-users/{id}" "200"
echo

# ---------------------------------------------------------------------------
# 7. Touch last login
# ---------------------------------------------------------------------------
info "7. Touch last login"
req POST "/api/v1/idp-users/${USER_ID}/touch-login"
expect_status "POST /idp-users/{id}/touch-login" "204"
echo

# ---------------------------------------------------------------------------
# 8. Roles: grant, list active, list history, revoke
# ---------------------------------------------------------------------------
info "8. Roles lifecycle (${TEST_ROLE})"
req POST "/api/v1/idp-users/${USER_ID}/roles" "{\"roleName\": \"${TEST_ROLE}\"}"
expect_status "POST /idp-users/{id}/roles (grant)" "201"

req GET "/api/v1/idp-users/${USER_ID}/roles/active-names"
expect_status "GET /idp-users/{id}/roles/active-names" "200"
if echo "$BODY" | jq -e --arg r "$TEST_ROLE" 'index($r)' >/dev/null 2>&1; then
    ok "Granted role appears in active-names"
else
    bad "Granted role NOT in active-names -- body: $BODY"
fi

req POST "/api/v1/idp-users/${USER_ID}/roles" "{\"roleName\": \"${TEST_ROLE}\"}"
expect_status "POST /idp-users/{id}/roles (idempotent re-grant)" "201"

req GET "/api/v1/idp-users/${USER_ID}/roles?activeOnly=false"
expect_status "GET /idp-users/{id}/roles (full history)" "200"

req DELETE "/api/v1/idp-users/${USER_ID}/roles/${TEST_ROLE}"
expect_status "DELETE /idp-users/{id}/roles/{roleName} (revoke)" "204"

req GET "/api/v1/idp-users/${USER_ID}/roles/active-names"
if echo "$BODY" | jq -e --arg r "$TEST_ROLE" 'index($r)' >/dev/null 2>&1; then
    bad "Revoked role still showing as active -- body: $BODY"
else
    ok "Revoked role no longer active"
fi
echo

# ---------------------------------------------------------------------------
# 9. Groups: join, list active, leave
# ---------------------------------------------------------------------------
info "9. Groups lifecycle (${TEST_GROUP})"
req POST "/api/v1/idp-users/${USER_ID}/groups" "{\"groupName\": \"${TEST_GROUP}\"}"
expect_status "POST /idp-users/{id}/groups (join)" "201"

req GET "/api/v1/idp-users/${USER_ID}/groups/active-names"
expect_status "GET /idp-users/{id}/groups/active-names" "200"
if echo "$BODY" | jq -e --arg g "$TEST_GROUP" 'index($g)' >/dev/null 2>&1; then
    ok "Joined group appears in active-names"
else
    bad "Joined group NOT in active-names -- body: $BODY"
fi

req DELETE "/api/v1/idp-users/${USER_ID}/groups/${TEST_GROUP}"
expect_status "DELETE /idp-users/{id}/groups/{groupName} (leave)" "204"
echo

# ---------------------------------------------------------------------------
# 10. Audit trail
# ---------------------------------------------------------------------------
info "10. Audit trail"
req GET "/api/v1/idp-users/${USER_ID}/audit?sort=eventAt,asc"
expect_status "GET /idp-users/{id}/audit" "200"
AUDIT_COUNT=$(echo "$BODY" | jq -r '.content | length' 2>/dev/null || echo 0)
info "   -> ${AUDIT_COUNT} audit rows (expect: USER_CREATED, USER_UPDATED x2, ROLE_GRANTED, ROLE_REVOKED, GROUP_JOINED, GROUP_LEFT)"

req GET "/api/v1/idp-users/${USER_ID}/audit?eventType=ROLE_GRANTED"
expect_status "GET /idp-users/{id}/audit?eventType=ROLE_GRANTED" "200"
echo

# ---------------------------------------------------------------------------
# 11. Negative tests
# ---------------------------------------------------------------------------
info "11. Negative tests"
req GET "/api/v1/idp-users/999999999"
expect_status "GET nonexistent user -> 404" "404"

NO_TOKEN_STATUS=$(curl -s -o /dev/null -w '%{http_code}' "${API_BASE}/api/v1/idp-users/${USER_ID}")
if [[ "$NO_TOKEN_STATUS" == "401" ]]; then
    ok "GET without Authorization header -> 401 (HTTP $NO_TOKEN_STATUS)"
else
    bad "GET without Authorization header expected 401, got $NO_TOKEN_STATUS"
fi
echo

# ---------------------------------------------------------------------------
# 12. Cleanup -- delete the test user (cascades roles/groups/audit)
# ---------------------------------------------------------------------------
info "12. Cleanup: delete test user"
req DELETE "/api/v1/idp-users/${USER_ID}"
expect_status "DELETE /idp-users/{id}" "204"

req GET "/api/v1/idp-users/${USER_ID}"
expect_status "GET deleted user -> 404" "404"
echo

# ---------------------------------------------------------------------------
# Summary
# ---------------------------------------------------------------------------
echo "============================================"
echo "$(color 32 "PASS: $PASS")   $(color 31 "FAIL: $FAIL")"
echo "============================================"
[[ "$FAIL" -eq 0 ]] && exit 0 || exit 1
