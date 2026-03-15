#!/bin/bash
# generate-jwt-keys.sh
# Generates RSA key pair for JWT RS256 signing.
# Run once before starting the application.
# Private key stays in auth-service only.
# Public key is shared to all other services for token verification.

set -e

KEY_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)/keys"
mkdir -p "$KEY_DIR"

echo "Generating RSA 2048-bit key pair for JWT RS256..."

# Generate private key (PKCS8 format required by JJWT)
openssl genpkey -algorithm RSA -pkeyopt rsa_keygen_bits:2048 \
  -out "$KEY_DIR/private_key_raw.pem" 2>/dev/null

openssl pkcs8 -topk8 -inform PEM -outform PEM -nocrypt \
  -in "$KEY_DIR/private_key_raw.pem" \
  -out "$KEY_DIR/private_key.pem"

rm "$KEY_DIR/private_key_raw.pem"

# Extract public key
openssl rsa -pubout \
  -in "$KEY_DIR/private_key.pem" \
  -out "$KEY_DIR/public_key.pem" 2>/dev/null

echo "Keys generated at: $KEY_DIR"
echo "  private_key.pem  → auth-service only"
echo "  public_key.pem   → shared to all services"
echo ""
echo "Copy public_key.pem to each service's /keys/ directory before starting."
