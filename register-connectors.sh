#!/bin/bash
# register-connectors.sh
# Registers all Debezium CDC connectors with Kafka Connect.
# Run this AFTER docker-compose up, once kafka-connect is healthy.

KAFKA_CONNECT_URL="http://localhost:8083"

echo "Waiting for Kafka Connect to be ready..."
until curl -s "$KAFKA_CONNECT_URL/connectors" > /dev/null 2>&1; do
  echo "  Kafka Connect not ready yet, retrying in 5s..."
  sleep 5
done
echo "Kafka Connect is ready."

register_connector() {
  local name=$1
  local file=$2
  echo ""
  echo "Registering connector: $name"
  response=$(curl -s -o /dev/null -w "%{http_code}" \
    -X POST "$KAFKA_CONNECT_URL/connectors" \
    -H "Content-Type: application/json" \
    -d @"$file")
  if [ "$response" = "201" ]; then
    echo "  ✅ $name registered successfully"
  elif [ "$response" = "409" ]; then
    echo "  ℹ️  $name already exists (skipped)"
  else
    echo "  ❌ $name failed with HTTP $response"
  fi
}

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

register_connector "enrollment-outbox-connector"  "$SCRIPT_DIR/debezium/enrollment-connector.json"
register_connector "payment-outbox-connector"     "$SCRIPT_DIR/debezium/payment-connector.json"
register_connector "exam-outbox-connector"        "$SCRIPT_DIR/debezium/exam-connector.json"
register_connector "grading-outbox-connector"     "$SCRIPT_DIR/debezium/grading-connector.json"
register_connector "certificate-outbox-connector" "$SCRIPT_DIR/debezium/certificate-connector.json"

echo ""
echo "All connectors registered. Current connector list:"
curl -s "$KAFKA_CONNECT_URL/connectors" | python3 -m json.tool 2>/dev/null || \
  curl -s "$KAFKA_CONNECT_URL/connectors"
echo ""
