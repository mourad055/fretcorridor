#!/usr/bin/env bash
# Demarre tous les microservices backend necessaires aux tests live
# (mobile Client + Chauffeur). service-flt est deja demarre via Docker
# (infra/docker-compose.yml), pas relance ici.
#
# Usage :
#   cd infra && docker compose up -d   # infra (postgres/redis/kafka/minio) d'abord
#   ./start-backend.sh                 # puis les microservices
#
# Logs de chaque service dans infra/logs/<service>.log
# Pour tout arreter : ./stop-backend.sh

set -euo pipefail
cd "$(dirname "$0")/.."

JAVA_HOME="${JAVA_HOME:-/usr/lib/jvm/java-21-openjdk-amd64}"
export JAVA_HOME

mkdir -p infra/logs infra/pids

declare -A SERVICES=(
  [service-ida]=""
  [gateway]=""
  [service-cap]=""
  [service-opt]=""
  [service-mkt]="SERVER_PORT=8089"
  [service-exe]=""
  [service-not]=""
  [service-mat]=""
  [service-trk]=""
  [service-geo]=""
  [service-adm]=""
  [service-pay]=""
)

for svc in "${!SERVICES[@]}"; do
  env_prefix="${SERVICES[$svc]}"
  echo "Demarrage de $svc..."
  (
    cd "backend/$svc"
    if [ -n "$env_prefix" ]; then
      env $env_prefix nohup mvn -o spring-boot:run > "../../infra/logs/$svc.log" 2>&1 &
    else
      nohup mvn -o spring-boot:run > "../../infra/logs/$svc.log" 2>&1 &
    fi
    echo $! > "../../infra/pids/$svc.pid"
  )
done

echo ""
echo "Les 12 services demarrent en arriere-plan (comptez 30-60s chacun le temps"
echo "que Spring Boot soit pret). Logs dans infra/logs/, PIDs dans infra/pids/."
echo ""
echo "Pour verifier qu'un service est pret, cherchez la ligne \"Started ... in\" :"
echo "  tail -f infra/logs/service-ida.log"
echo ""
echo "Pour tout arreter : ./infra/stop-backend.sh"
