#!/bin/sh
set -eu
cd "$(dirname "$0")/.."
[ -f .env ] || cp .env.example .env
case "${1:-entra}" in
  demo) docker compose -f compose.yml -f compose.demo.yml config --quiet
        docker compose -f compose.yml -f compose.demo.yml up -d --build --wait --wait-timeout 420 ;;
  entra) docker compose config --quiet
         docker compose up -d --build --wait --wait-timeout 420 ;;
  *) echo 'Usar: ./scripts/start.sh [entra|demo]'; exit 1 ;;
esac
echo 'BarrioDigital: http://localhost:4200'
