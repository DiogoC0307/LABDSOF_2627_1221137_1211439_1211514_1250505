#!/usr/bin/env bash
# Usage: infra/scripts/rabbitmq-ui.sh dev|qa|prod [local-port]   then open http://localhost:15672
set -euo pipefail
ENV=${1:?usage: rabbitmq-ui.sh dev|qa|prod [local-port]}
PORT=${2:-15672}
NS=escala-$ENV
get() { kubectl -n "$NS" get secret rabbitmq-credentials -o "jsonpath={.data.$1}" | base64 -d; }
echo
echo "Environment : $ENV"
echo "URL         : http://localhost:$PORT"
echo "User        : $(get username)"
echo "Password    : $(get password)"
echo
echo "Tunnel open. Leave this running, press Ctrl+C to close it."
kubectl -n "$NS" port-forward svc/rabbitmq "$PORT:15672"
