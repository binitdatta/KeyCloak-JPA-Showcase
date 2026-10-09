#!/bin/bash

set -e

# ============================================================
# Keycloak 26.x - Signed JWT / private_key_jwt
# PKCS12 Key Generation Script
# ============================================================

CLIENT_ID="signed-jwt-client"
ALIAS="${CLIENT_ID}"
PASSWORD="changeit"
KEY_SIZE="2048"
VALIDITY_DAYS="3650"

OUTPUT_DIR="${HOME}/keycloak-signed-jwt"
KEYSTORE="${OUTPUT_DIR}/${CLIENT_ID}.p12"
CERTIFICATE="${OUTPUT_DIR}/${CLIENT_ID}.crt"
PUBLIC_KEY="${OUTPUT_DIR}/${CLIENT_ID}-public.pem"

echo
echo "============================================================"
echo " Keycloak Signed JWT - PKCS12 Key Generator"
echo "============================================================"
echo
echo "Client ID     : ${CLIENT_ID}"
echo "Alias         : ${ALIAS}"
echo "Output        : ${OUTPUT_DIR}"
echo "Keystore      : ${KEYSTORE}"
echo "Store Type    : PKCS12"
echo "Key Algorithm : RSA"
echo "Key Size      : ${KEY_SIZE}"
echo

# ------------------------------------------------------------
# 1. Create output directory
# ------------------------------------------------------------

mkdir -p "${OUTPUT_DIR}"

# ------------------------------------------------------------
# 2. Remove old files if they exist
# ------------------------------------------------------------

if [ -f "${KEYSTORE}" ]; then
    echo "Removing existing keystore: ${KEYSTORE}"
    rm -f "${KEYSTORE}"
fi

rm -f "${CERTIFICATE}" "${PUBLIC_KEY}"

# ------------------------------------------------------------
# 3. Generate RSA keypair directly into PKCS12
# ------------------------------------------------------------

echo
echo "Generating RSA keypair..."

keytool -genkeypair \
  -alias "${ALIAS}" \
  -keyalg RSA \
  -keysize "${KEY_SIZE}" \
  -sigalg SHA256withRSA \
  -validity "${VALIDITY_DAYS}" \
  -dname "CN=${CLIENT_ID}, OU=OAuth2, O=POC, C=US" \
  -keystore "${KEYSTORE}" \
  -storetype PKCS12 \
  -storepass "${PASSWORD}" \
  -keypass "${PASSWORD}"

# ------------------------------------------------------------
# 4. Verify PKCS12 keystore
# ------------------------------------------------------------

echo
echo "============================================================"
echo " Verifying PKCS12 Keystore"
echo "============================================================"

keytool -list -v \
  -keystore "${KEYSTORE}" \
  -storetype PKCS12 \
  -storepass "${PASSWORD}"

# ------------------------------------------------------------
# 5. Export X.509 public certificate
# ------------------------------------------------------------

echo
echo "============================================================"
echo " Exporting X.509 Certificate"
echo "============================================================"

keytool -exportcert \
  -alias "${ALIAS}" \
  -keystore "${KEYSTORE}" \
  -storetype PKCS12 \
  -storepass "${PASSWORD}" \
  -rfc \
  -file "${CERTIFICATE}"

# ------------------------------------------------------------
# 6. Extract public key from certificate
# ------------------------------------------------------------

echo
echo "============================================================"
echo " Extracting RSA Public Key"
echo "============================================================"

openssl x509 \
  -in "${CERTIFICATE}" \
  -pubkey \
  -noout \
  > "${PUBLIC_KEY}"

# ------------------------------------------------------------
# 7. Verify certificate
# ------------------------------------------------------------

echo
echo "============================================================"
echo " Certificate Information"
echo "============================================================"

openssl x509 \
  -in "${CERTIFICATE}" \
  -noout \
  -subject \
  -issuer \
  -serial \
  -dates

# ------------------------------------------------------------
# 8. Display generated files
# ------------------------------------------------------------

echo
echo "============================================================"
echo " Generated Files"
echo "============================================================"
echo

ls -lh "${OUTPUT_DIR}"

echo
echo "============================================================"
echo " SUCCESS"
echo "============================================================"
echo
echo "PKCS12 keystore:"
echo "  ${KEYSTORE}"
echo
echo "Public certificate:"
echo "  ${CERTIFICATE}"
echo
echo "Public key:"
echo "  ${PUBLIC_KEY}"
echo
echo "Keycloak import settings:"
echo "  Archive Format  : PKCS12"
echo "  Key Alias       : ${ALIAS}"
echo "  Store Password  : ${PASSWORD}"
echo "  Key Password    : ${PASSWORD}"
echo
echo "IMPORTANT:"
echo "  ${KEYSTORE} contains the PRIVATE KEY."
echo "  Do not commit it to Git or distribute it."
echo
