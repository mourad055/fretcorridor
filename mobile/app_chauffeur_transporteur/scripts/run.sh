#!/bin/bash
set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
MOBILE_ROOT="$(dirname "$SCRIPT_DIR")"

IP=$(ip addr show wlp3s0 2>/dev/null | grep "inet " | awk '{print $2}' | cut -d/ -f1)

if [ -z "$IP" ]; then
  echo "⚠️  IP WiFi introuvable sur wlp3s0 — utilisation de localhost (web/desktop uniquement)."
  HOST="localhost"
else
  HOST="$IP"
fi

# Passe par la gateway (port 8082, cf. dio_provider.dart) — contrairement à
# l'app Client, un seul point d'entrée pour ce rôle.
API_BASE="http://$HOST:8082/api/v1"

echo "✅ API_BASE=$API_BASE"

cd "$MOBILE_ROOT"
exec flutter run \
  --dart-define=API_BASE="$API_BASE" \
  "$@"
