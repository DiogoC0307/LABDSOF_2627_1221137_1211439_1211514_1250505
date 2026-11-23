# Grafana dashboards

Dashboards kept in Git as JSON (SCRUM-97). Grafana loads every file here, in Docker Compose (profile
`observability`, `infra/observability/grafana-dashboards.yaml`) and in Kubernetes (the `grafana-dashboards`
ConfigMap in `infra/k8s/observability/grafana/`, which holds the same files). They are read-only in Grafana: change
the JSON here and deploy.
