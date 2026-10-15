#!/bin/bash
set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
MOBILE_ROOT="$(dirname "$SCRIPT_DIR")"

# BUG CORRIGE (28/08) : interface codee en dur (wlp1s0) -- meme classe de
# probleme que run.sh (voir son commentaire), detection portable ici aussi.
IP=$(ip -o -4 addr show | awk '$2 !~ /^(lo|docker|virbr|br-|veth|tun|tap)/ {print $4}' | cut -d/ -f1 | head -1)

if [ -z "$IP" ]; then
  echo "⚠️  Aucune IP réseau détectée — utilisation de localhost (web/desktop uniquement)."
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
