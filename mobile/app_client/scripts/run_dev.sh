#!/bin/bash
set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
MOBILE_ROOT="$(dirname "$SCRIPT_DIR")"

IP=$(ip addr show wlp1s0 2>/dev/null | grep "inet " | awk '{print $2}' | cut -d/ -f1)

if [ -z "$IP" ]; then
  echo "⚠️  IP WiFi introuvable sur wlp1s0 — utilisation de localhost (web/desktop uniquement)."
  API_BASE="http://localhost:8080/api"
else
  API_BASE="http://$IP:8080/api"
fi

echo "✅ API_BASE=$API_BASE"
echo "   Gateway attendue sur le port 8080 (0.0.0.0)"

cd "$MOBILE_ROOT"
exec flutter run --dart-define=API_BASE="$API_BASE" "$@"
