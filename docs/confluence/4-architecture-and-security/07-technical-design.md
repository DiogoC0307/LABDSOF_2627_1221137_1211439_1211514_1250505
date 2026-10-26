# 07 · Technical Design

| Field | Value |
|---|---|
| Status | In review |
| Owner | Pedro Costa |
| Contributors | Diogo Pereira, Diogo Sousa, Diogo Costa |
| Deliverable | Sprint 1 · #7 Technical Design |
| Sources | Assignment §6, §7.1.2 #7; D-00; 04 · Product Vision |
| Jira | SCRUM-39 |
| Last updated | 2026-10-26 |

**Git first.** Full design: `docs/architecture/technical-design.md`. This page is the investor one-pager. Paste it into Confluence only with a page editor's permission (view-only otherwise). Export diagrams from Git (`c4-system-context.puml`, `c4-containers.puml`, `c4-deployment.puml`).

## Summary

Escala is a mobile roster for members of one volunteer fire corps: they see their shifts, say when they cannot serve, swap, confirm presence, and get cover requests only if they are free and qualified (**D-00**, UJ1 / UJ3 / UJ6). Angular PWA plus two Spring Boot services (**ADR 0002**). `escala-api` owns the published schedule and its changes; `elegibilidade-worker` runs deterministic function checks, text extraction and the IPMA poll. The client uses REST/HTTPS; the services use RabbitMQ (**ADR 0005**). Each service has its own PostgreSQL (**ADR 0003**). AI extracts unavailability only; it never decides swaps or cover (**ADR 0007**, RF34 Won't). IPMA is **shown**, not used to set composition (RN10 off, RF27/RF28 Won't). Docker Compose, GitHub Actions, Prometheus, Grafana. Readiness, never dispatch.

## System context

[DIAGRAM: export c4-system-context.puml]

## Containers

[DIAGRAM: export c4-containers.puml]

## Components and data ownership

No component reads another component's database (**ADR 0003**).

| Component | Responsibility | Owns data | Talks to | Sync / async |
|---|---|---|---|---|
| `escala-api` | Published schedule, members, unavailability, swaps, presence, cover, audit | `escala-db` | `web`; RabbitMQ | Sync REST; async events |
| `elegibilidade-worker` | Function checks (RF21), extraction (RF31), cached RCM (RF25, RF29) | `elegibilidade-db` | RabbitMQ; IPMA; LLM | Async events; sync HTTP |
| `web` | Member and supervisor screens; roster offline (RNF06) | None | `escala-api` | Sync REST |
| `ipma-sim` | IPMA paths and §6.5 failures | None | worker | Sync HTTP |

## Asynchronous workflows

| Workflow | Trigger | Result | Why async |
|---|---|---|---|
| Function check | `AvaliacaoPedida` (`SWAP` / `COVER` / `ASSIGNMENT`) | `AvaliacaoConcluida` | 202 without waiting (RNF02); schedule changes only after `ELIGIBLE` (RF24) |
| Extraction | `ExtracaoPedida` | `ExtracaoConcluida` | Model may be down; form stays up (RF35) |
| Risk update | Hourly poll | `RiscoAtualizado` | Display only; does not change slots |

## Reliability of messaging

| §6.3 | Mechanism |
|---|---|
| Retries | 5 s / 30 s / 2 min then DLQ (**ADR 0005**) |
| Duplicates | `eventId` in the same transaction (**ADR 0009**, RNF05) |
| Failures | Transactional outbox, then DLQ (**ADR 0009**) |
| Eventual consistency | pending / evaluated / failed; swap or cover not applied without a valid check (**ADR 0008**, RF24) |

## External integration — IPMA

Endpoints: RCM `rcm-d{0,1,2}.json`; warnings `warnings_www.json`. Poll hourly by the worker (**ADR 0010**). DICO **1312**. Staleness **3 h**: show age and an assumed-higher *warning*; **do not** add slots (**ADR 0013**, D-00).

| §6.5 | Detection | Behaviour |
|---|---|---|
| Unavailable | Timeout 5 s or HTTP error | Last cache + age; after 3 h, warning |
| Delayed | Old `dataPrev` / `fileDate` | Same |
| Incomplete | DICO missing | Keep last value for our municipality |
| Invalid | Bad JSON or RCM not 1–5 | Reject; keep last valid |

Simulator: `IPMA_SIM_MODE=ok\|unavailable\|delayed\|incomplete\|invalid`.

## Degraded modes

| Down | Behaviour | Message |
|---|---|---|
| IPMA | Last RCM + age; warning only | "Fire risk from IPMA last updated {age}. Level may be higher than shown. The schedule is unchanged." |
| AI | Form fallback (RF35) | "Automatic reading is unavailable. Please enter the dates in the form." |
| Worker or RabbitMQ | Requests 202; swap/cover not applied | "Check pending. Your shift will update when the check completes." |

If Escala itself is down, the corps uses the phone.

## Data model

- `escala-db`: Member, Certification, Licence, Piquete, Slot, Assignment, Unavailability, Swap request, Presence confirmation, Cover request, Application, Audit, Daily risk (display), outbox.
- `elegibilidade-db`: Evaluation (`purpose` SWAP/COVER/ASSIGNMENT), Extraction, Risk observation, outbox.

Full model: Git Technical Design § Data model. **D-01** / **D-02** stay open and must not be decided in this page.

## API outline

- REST: `contracts/openapi.yaml`
- Events: `contracts/events/`

`GET /me/roster` · `POST /piquetes` · `POST /piquetes/{id}/publish` · `POST /unavailabilities` (202) · `POST /swaps` · `POST /swaps/{id}/accept` · `POST /slots/{id}/presence` · `POST /cover-requests/{id}/accept` · `POST /assignments/{id}/confirm` (RF24) · `GET /risk/today` · `GET /health`, `GET /health/ready`

## Deployment view

[DIAGRAM: export c4-deployment.puml]

Environment: Docker Compose locally. PWA `http://localhost:4200`, API `http://localhost:8080`, Grafana `http://localhost:3000`. Runbook: `docs/operations/deployment-and-operations.md`.

## Observability and SLIs

- Logs: JSON stdout, `X-Trace-Id` / `traceId` (RF37)
- Health: `/health`, `/health/ready` (RF36)
- Metrics: checks, DLQ, IPMA age, extraction fallbacks, time-to-fill (RF38)
- Dashboard: Grafana `http://localhost:3000` (RF39)
- SLI 1: function-check p95 &lt; 5 s (RNF01)
- SLI 2: ≥ 95 % of checks without DLQ replay or exceptional assignment

## Done when

- [x] Git document matches D-00
- [ ] This text pasted to Confluence by someone with edit permission
- [ ] Diagrams exported from Git
- [ ] Reviewed by another team member
- [ ] Status Done
