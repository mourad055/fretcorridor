#!/usr/bin/env bash
# Arrete tous les microservices demarres par start-backend.sh
set -uo pipefail
cd "$(dirname "$0")"

if [ ! -d pids ] || [ -z "$(ls -A pids 2>/dev/null)" ]; then
  echo "Aucun PID trouve dans infra/pids/ (rien a arreter, ou deja arrete)."
  exit 0
fi

for pidfile in pids/*.pid; do
  svc="$(basename "$pidfile" .pid)"
  pid="$(cat "$pidfile")"
  if kill -0 "$pid" 2>/dev/null; then
    echo "Arret de $svc (pid $pid)..."
    kill "$pid" 2>/dev/null
  else
    echo "$svc (pid $pid) deja arrete."
  fi
  rm -f "$pidfile"
done

echo "Fait. (L'infra Docker (postgres/redis/kafka/minio) tourne toujours - 'docker compose down' dans infra/ pour l'arreter aussi.)"
