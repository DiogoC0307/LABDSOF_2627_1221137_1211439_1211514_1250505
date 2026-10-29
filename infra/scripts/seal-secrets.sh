#!/usr/bin/env bash
# Usage: infra/scripts/seal-secrets.sh dev|qa|prod|observability [--force]
# Generates random passwords and writes ENCRYPTED SealedSecrets (safe to commit) next to the app that uses them.
# Files that already exist are KEPT (so databases that are already running keep their passwords);
# use --force only to rotate (see the README before doing that).
# Needs: kubectl pointed at the cluster, kubeseal, and the sealed-secrets controller installed.
set -euo pipefail
ENV=${1:?usage: seal-secrets.sh dev|qa|prod|observability [--force]}
FORCE=${2:-}
BASE="$(cd "$(dirname "$0")/.." && pwd)/k8s/$ENV"
if [ "$ENV" = observability ]; then NS=observability; else NS=escala-$ENV; fi
rnd() { openssl rand -base64 30 | tr -d '/+=' | cut -c1-24; }
seal() { kubeseal --controller-name sealed-secrets --controller-namespace kube-system --format yaml; }

# one <relative path> <secret name> <literal>...
one() {
  local path=$1 name=$2; shift 2
  if [ -f "$BASE/$path" ] && [ "$FORCE" != "--force" ]; then echo "kept    $path"; return; fi
  kubectl create secret generic "$name" -n "$NS" "$@" --dry-run=client -o yaml | seal > "$BASE/$path"
  echo "sealed  $path"
}

if [ "$ENV" = observability ]; then
  one grafana/sealed-secret.yaml grafana-admin --from-literal=admin-user=admin --from-literal=admin-password="$(rnd)"
else
  one escala-api/sealed-secret.yaml                    db-escala-api        --from-literal=POSTGRES_DB=escala --from-literal=POSTGRES_USER=escala --from-literal=POSTGRES_PASSWORD="$(rnd)"
  one elegibilidade-worker/sealed-secret.yaml          db-elegibilidade     --from-literal=POSTGRES_DB=elegibilidade --from-literal=POSTGRES_USER=elegibilidade --from-literal=POSTGRES_PASSWORD="$(rnd)"
  one shared/sealed-secret.yaml                        rabbitmq-credentials --from-literal=username=escala --from-literal=password="$(rnd)"
  one escala-api/sealed-secret-rabbitmq.yaml           rabbitmq-escala-api  --from-literal=username=escala-api --from-literal=password="$(rnd)"
  one elegibilidade-worker/sealed-secret-rabbitmq.yaml rabbitmq-elegibilidade-worker --from-literal=username=elegibilidade-worker --from-literal=password="$(rnd)"
fi
echo "Done. Commit the new sealed-secret*.yaml files."
