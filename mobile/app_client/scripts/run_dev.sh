#!/bin/bash
set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
MOBILE_ROOT="$(dirname "$SCRIPT_DIR")"

IP=$(ip addr show wlp1s0 2>/dev/null | grep "inet " | awk '{print $2}' | cut -d/ -f1)

if [ -z "$IP" ]; then
  echo "⚠️  IP WiFi introuvable sur wlp1s0 — utilisation de localhost (web/desktop uniquement)."
  HOST="localhost"
else
  HOST="$IP"
fi

# Pas de gateway unifiée pour l'app Client — un port par microservice réel
# (cf. dio_provider.dart). Ports hôte alignés sur les docker-compose.*.yml
# de chaque service. GEO/PAY ajoutés (audit du 20 août, soir) : présents
# dans dio_provider.dart mais absents ici jusqu'ici -- retombaient
# silencieusement sur localhost, injoignable depuis un téléphone physique.
API_BASE_IDA="http://$HOST:8081/api"
API_BASE_MKT="http://$HOST:8089/api"
API_BASE_NOT="http://$HOST:8094/api"
API_BASE_EXE="http://$HOST:8093/api"
API_BASE_FLT="http://$HOST:8092/api"
API_BASE_GEO="http://$HOST:8084"
API_BASE_PAY="http://$HOST:8088/api/v1/pay"

echo "✅ API_BASE_IDA=$API_BASE_IDA"
echo "✅ API_BASE_MKT=$API_BASE_MKT"
echo "✅ API_BASE_NOT=$API_BASE_NOT"
echo "✅ API_BASE_EXE=$API_BASE_EXE"
echo "✅ API_BASE_FLT=$API_BASE_FLT"
echo "✅ API_BASE_GEO=$API_BASE_GEO"
echo "✅ API_BASE_PAY=$API_BASE_PAY"

cd "$MOBILE_ROOT"
exec flutter run \
  --dart-define=API_BASE_IDA="$API_BASE_IDA" \
  --dart-define=API_BASE_MKT="$API_BASE_MKT" \
  --dart-define=API_BASE_NOT="$API_BASE_NOT" \
  --dart-define=API_BASE_EXE="$API_BASE_EXE" \
  --dart-define=API_BASE_FLT="$API_BASE_FLT" \
  --dart-define=API_BASE_GEO="$API_BASE_GEO" \
  --dart-define=API_BASE_PAY="$API_BASE_PAY" \
  "$@"
