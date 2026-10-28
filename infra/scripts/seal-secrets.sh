#!/usr/bin/env bash
# Usage: infra/scripts/seal-secrets.sh dev|qa|prod
# Generates random passwords and writes ENCRYPTED SealedSecrets (safe to commit), one next to each app:
#   infra/k8s/<env>/escala-api/sealed-secret.yaml            (its database)
#   infra/k8s/<env>/elegibilidade-worker/sealed-secret.yaml  (its database)
#   infra/k8s/<env>/shared/sealed-secret.yaml                (RabbitMQ)
# Needs: kubectl pointed at the cluster, kubeseal, the sealed-secrets controller, and namespace escala-<env>.
set -euo pipefail
ENV=${1:?usage: seal-secrets.sh dev|qa|prod}
NS=escala-$ENV
BASE="$(cd "$(dirname "$0")/.." && pwd)/k8s/$ENV"
rnd() { openssl rand -base64 24 | tr -d '/+=' | cut -c1-24; }
seal() { kubeseal --controller-name sealed-secrets --controller-namespace kube-system --format yaml; }
kubectl create secret generic db-escala-api -n "$NS" \
  --from-literal=POSTGRES_DB=escala --from-literal=POSTGRES_USER=escala --from-literal=POSTGRES_PASSWORD="$(rnd)" \
  --dry-run=client -o yaml | seal > "$BASE/escala-api/sealed-secret.yaml"
kubectl create secret generic db-elegibilidade -n "$NS" \
  --from-literal=POSTGRES_DB=elegibilidade --from-literal=POSTGRES_USER=elegibilidade --from-literal=POSTGRES_PASSWORD="$(rnd)" \
  --dry-run=client -o yaml | seal > "$BASE/elegibilidade-worker/sealed-secret.yaml"
kubectl create secret generic rabbitmq-credentials -n "$NS" \
  --from-literal=username=escala --from-literal=password="$(rnd)" \
  --dry-run=client -o yaml | seal > "$BASE/shared/sealed-secret.yaml"
echo "Sealed secrets written under $BASE (commit them)."
