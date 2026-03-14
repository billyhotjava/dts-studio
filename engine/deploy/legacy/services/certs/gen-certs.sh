#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

BASE_DOMAIN="${BASE_DOMAIN:-copilot.local}"
P12_PASSWORD="${P12_PASSWORD:-changeit}"
TRUSTSTORE_PASSWORD="${TRUSTSTORE_PASSWORD:-changeit}"
SUBJECT_O="${SUBJECT_O:-DTS-Copilot}"
DAYS_CA=3650
DAYS_SERVER=825

echo "=== DTS Copilot Certificate Generator ==="
echo "Domain: *.${BASE_DOMAIN}"

# Check if valid certs already exist
if [[ -f server.crt && -f server.key ]]; then
    if openssl x509 -in server.crt -noout -checkend 86400 2>/dev/null; then
        EXISTING_SAN=$(openssl x509 -in server.crt -noout -ext subjectAltName 2>/dev/null || true)
        if echo "$EXISTING_SAN" | grep -q "${BASE_DOMAIN}"; then
            echo "Valid certificate found for *.${BASE_DOMAIN}, skipping generation."
            # Ensure truststore exists
            if [[ ! -f truststore.p12 ]]; then
                echo "Generating missing truststore..."
                # Generate truststore below
            else
                echo "All certificates present and valid."
                exit 0
            fi
        fi
    fi
fi

echo "Generating new certificates..."

# 1. Generate CA
echo "  [1/5] Generating CA key and certificate..."
openssl req -x509 -newkey rsa:2048 -nodes \
    -keyout ca.key -out ca.crt \
    -days $DAYS_CA \
    -subj "/O=${SUBJECT_O}/CN=${SUBJECT_O} CA" \
    -addext "basicConstraints=critical,CA:TRUE" \
    -addext "keyUsage=critical,keyCertSign,cRLSign" \
    2>/dev/null

# 2. Generate server key and CSR
echo "  [2/5] Generating server key and CSR..."
openssl req -newkey rsa:2048 -nodes \
    -keyout server.key -out server.csr \
    -subj "/O=${SUBJECT_O}/CN=*.${BASE_DOMAIN}" \
    2>/dev/null

# 3. Sign server certificate with CA
echo "  [3/5] Signing server certificate..."
cat > server_ext.cnf <<EOF
authorityKeyIdentifier=keyid,issuer
basicConstraints=CA:FALSE
keyUsage=digitalSignature,keyEncipherment
extendedKeyUsage=serverAuth
subjectAltName=DNS:*.${BASE_DOMAIN},DNS:${BASE_DOMAIN}
EOF

openssl x509 -req -in server.csr \
    -CA ca.crt -CAkey ca.key -CAcreateserial \
    -out server.only.crt \
    -days $DAYS_SERVER \
    -extfile server_ext.cnf \
    2>/dev/null

# Create full chain (server + CA)
cat server.only.crt ca.crt > server.crt

# 4. Generate PKCS#12
echo "  [4/5] Generating PKCS#12 keystore..."
openssl pkcs12 -export \
    -in server.crt -inkey server.key \
    -out server.p12 \
    -name "copilot" \
    -passout "pass:${P12_PASSWORD}" \
    2>/dev/null
cp server.p12 keystore.p12

# 5. Generate truststore
echo "  [5/5] Generating truststore..."
# Remove old truststore if exists
rm -f truststore.p12
# Use keytool if available, otherwise use openssl
if command -v keytool &>/dev/null; then
    keytool -importcert -noprompt \
        -keystore truststore.p12 -storetype PKCS12 \
        -storepass "${TRUSTSTORE_PASSWORD}" \
        -alias "copilot-ca" \
        -file ca.crt \
        2>/dev/null
else
    # Fallback: create PKCS12 truststore with openssl
    openssl pkcs12 -export \
        -nokeys -in ca.crt \
        -out truststore.p12 \
        -passout "pass:${TRUSTSTORE_PASSWORD}" \
        2>/dev/null
    echo "  (Used openssl fallback for truststore - keytool not found)"
fi

# Cleanup temporary files
rm -f server.csr server_ext.cnf ca.srl

# Validate
echo ""
echo "=== Validation ==="
echo "CA certificate:"
openssl x509 -in ca.crt -noout -subject -dates 2>/dev/null | sed 's/^/  /'
echo "Server certificate:"
openssl x509 -in server.only.crt -noout -subject -dates 2>/dev/null | sed 's/^/  /'
echo "SAN:"
openssl x509 -in server.only.crt -noout -ext subjectAltName 2>/dev/null | sed 's/^/  /'

# Verify chain
if openssl verify -CAfile ca.crt server.only.crt >/dev/null 2>&1; then
    echo "  Chain verification: OK"
else
    echo "  Chain verification: FAILED"
    exit 1
fi

echo ""
echo "=== Generated Files ==="
ls -la *.crt *.key *.p12 2>/dev/null | awk '{print "  " $NF " (" $5 " bytes)"}'
echo ""
echo "Done. To trust the CA on your system:"
echo "  macOS:  sudo security add-trusted-cert -d -r trustRoot -k /Library/Keychains/System.keychain ${SCRIPT_DIR}/ca.crt"
echo "  Ubuntu: sudo cp ${SCRIPT_DIR}/ca.crt /usr/local/share/ca-certificates/copilot-ca.crt && sudo update-ca-certificates"
