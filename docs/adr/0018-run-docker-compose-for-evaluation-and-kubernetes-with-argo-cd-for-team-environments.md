# 0018 · Run Docker Compose for evaluation and Kubernetes with Argo CD for the team environments

| Field | Value |
|---|---|
| Date | 2026-10-31 |
| Status | Proposed |
| Deciders | Diogo Pereira, Diogo Sousa, Pedro Costa, Diogo Costa |

## Context

The teaching staff must be able to run the whole product on their own machine with documented instructions (assignment §12), and the walking skeleton needs a containerised deployment from Sprint 1 (§7.1.2 #10). The team also needs shared environments where every merge is deployed, a release candidate can be checked before the sprint review, and a release can be rolled back (§8.4, §8.5). These are two different needs: a reproducible one-command run on a laptop, and continuous deployment to hosted environments.

## Decision

- **Evaluation and local development:** Docker Compose (`compose.yaml`). One command, `docker compose up --build`, starts the web app, both services, both databases and RabbitMQ. Optional profiles add observability (`observability`) and, once built, the IPMA simulator with the AI off (`evaluation`, SCRUM-43). Compose is the evaluation contract.
- **Team environments (dev, QA, production):** Kubernetes with GitOps through Argo CD (`infra/k8s`, `infra/argocd`). GitHub Actions builds the images, tags them with the commit SHA, pushes them to GHCR and writes the new version to a deploy branch. Argo CD applies `deploy/dev` on every merge to `main`, `deploy/qa` for each `release/X.Y.Z` candidate and `deploy/prod` for each GitHub Release. Secrets are Sealed Secrets; observability runs Loki, Tempo, Grafana and Alloy.

## Alternatives considered

| Alternative | Why rejected |
|---|---|
| Kubernetes as the evaluation contract | The teaching staff would need a cluster or a local Kubernetes distribution; Compose needs only Docker |
| Docker Compose on a single VM for the team environments | No declarative rollback, no separate environments per branch and manual secret handling on the host |
| Deploy from GitHub Actions with `kubectl apply` (push model) | The cluster credentials would live in CI; with Argo CD the cluster pulls from Git and the desired state is always visible in the repository |
| A managed platform (for example Render or Fly.io) | Cost after the free tier and less control over RabbitMQ and the databases |

## Consequences

- **Positive:** one command for the teaching staff; every merge reaches dev without manual steps; QA and production follow releases; rolling back is a revert on the deploy branch.
- **Negative:** two sets of configuration (Compose and Kubernetes manifests) must be kept in step; the team depends on one member's cluster knowledge.
- **Trade-offs:** more infrastructure than the MVP strictly needs, accepted because CI/CD and operations are graded deliverables (Sprint 2 #11).

## Assignment requirement served

§7.1.2 #10 (containerised deployment), §8.4 and §8.5 (environments, CI/CD, rollback), §12 (the teaching staff run the product).

## Related

- Jira: SCRUM-44, SCRUM-49
- Requirements: RF36, RF37
- Supersedes: —
- Other ADRs or pages: 0002; 0003; 0005; **07 · Technical Design** (deployment view); **10 · Walking Skeleton**; `docs/operations/deployment-and-operations.md`
- Date evidence: written on 2026-10-31 and proposed in the pull request that adds it (SCRUM-40); `compose.yaml`, `infra/k8s`, `infra/argocd` and the deploy workflows are already in main.
