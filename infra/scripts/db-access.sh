#!/usr/bin/env bash
# Prints the credentials of one service's database and opens a tunnel to it.
# Usage: infra/scripts/db-access.sh dev|qa|prod escala-api|elegibilidade-worker [local-port]
set -euo pipefail
ENV=${1:?usage: db-access.sh dev|qa|prod escala-api|elegibilidade-worker [local-port]}
SVC=${2:?usage: db-access.sh dev|qa|prod escala-api|elegibilidade-worker [local-port]}
NS=escala-$ENV
case "$SVC" in
  escala-api)            SECRET=db-escala-api;    PORT=${3:-5432} ;;
  elegibilidade-worker)  SECRET=db-elegibilidade; PORT=${3:-5433} ;;
  *) echo "unknown service $SVC"; exit 1 ;;
esac
get() { kubectl -n "$NS" get secret "$SECRET" -o "jsonpath={.data.$1}" | base64 -d; }
DB=$(get POSTGRES_DB); USER=$(get POSTGRES_USER); PASS=$(get POSTGRES_PASSWORD)
cat <<INFO

Environment : $ENV    Service: $SVC
Host        : localhost
Port        : $PORT
Database    : $DB
User        : $USER
Password    : $PASS
JDBC URL    : jdbc:postgresql://localhost:$PORT/$DB

Tunnel open. Leave this running, press Ctrl+C to close it.
INFO
kubectl -n "$NS" port-forward "pod/pg-$SVC-0" "$PORT:5432"
