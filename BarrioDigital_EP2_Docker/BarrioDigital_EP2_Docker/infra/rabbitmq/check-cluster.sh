#!/bin/sh
set -eu
for node in rabbit@rabbitmq-1 rabbit@rabbitmq-2; do
  status="$(rabbitmqctl --erlang-cookie "$RABBITMQ_ERLANG_COOKIE" -n "$node" cluster_status --formatter json)"
  printf '%s\n' "$status"
  running="$(printf '%s' "$status" | tr -d '\n ' | sed -n 's/.*"running_nodes":\[\([^]]*\)\].*/\1/p')"
  printf '%s' "$running" | grep -q 'rabbit@rabbitmq-1'
  printf '%s' "$running" | grep -q 'rabbit@rabbitmq-2'
done
echo 'Cluster verificado: ambos nodos están en running_nodes.'
