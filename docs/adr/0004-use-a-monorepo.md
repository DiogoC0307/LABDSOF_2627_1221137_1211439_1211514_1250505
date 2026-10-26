# 0004 · Keep the product in one monorepo

| Field | Value |
|---|---|
| Date | 2026-09-25 |
| Status | Proposed |
| Deciders | Diogo Pereira, Diogo Sousa, Pedro Costa, Diogo Costa |

## Context

The client, two services, contracts, simulator and Compose stack must stay consistent. A team of four cannot afford version skew between `contracts/events/` and two separate repositories.

## Decision

We will keep one GitHub repository with `apps/web`, `apps/escala-api`, `apps/elegibilidade-worker`, `contracts/`, `tools/ipma-sim`, `infra/` and `docs/`. GitHub Actions builds each image from this tree. Event and REST contracts change in the same pull request as their producers and consumers.

## Alternatives considered

| Alternative | Why rejected |
|---|---|
| One repository per component | Contract changes would need coordinated releases; too much process for four people |
| Backend monorepo and a separate frontend repo | The PWA contract would still drift from `contracts/openapi.yaml` |

## Consequences

- **Positive:** one pull request can change an event schema and both Java consumers; CI sees the whole product.
- **Negative:** a broken web build can block an unrelated worker fix unless workflows are path-filtered later.
- **Trade-offs:** we accept a larger repository in exchange for aligned contracts.

## Assignment requirement served

§8 (source repository, CI); supports §6.2 (independent deployables from one codebase).

## Related

- Jira: SCRUM-20, SCRUM-39
- Requirements: —
- Supersedes: —
- Other ADRs or pages: 0002
