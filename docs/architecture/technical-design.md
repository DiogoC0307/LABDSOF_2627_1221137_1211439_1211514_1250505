# Technical Design

| Field | Value |
|---|---|
| Status | In review |
| Owner | Pedro Costa |
| Contributors | Diogo Pereira, Diogo Sousa, Diogo Costa |
| Deliverable | Sprint 1 · #7 Technical Design |
| Sources | Assignment §6, §7.1.2 #7; D-00; **04 · Product Vision** |
| Confluence page | **07 · Technical Design** (paste of this file; export diagrams from the PlantUML next to it) |
| Jira | SCRUM-39 |
| Last updated | 2026-10-30 |

This is the technical design of Escala. Decisions are in the [ADR index](../adr/README.md). REST and events: [`contracts/`](../../contracts/). Diagrams as code: [`c4-system-context.puml`](c4-system-context.puml), [`c4-containers.puml`](c4-containers.puml), [`c4-deployment.puml`](c4-deployment.puml), [`domain_model.puml`](domain_model.puml). Confluence does not render Mermaid; export those PlantUML files when pasting **07 · Technical Design**.

## Summary

Escala is a mobile roster for members of one volunteer fire corps: they see their shifts, say when they cannot serve, swap, confirm presence, and get cover requests only if they are free and qualified (**D-00**, UJ1 / UJ3 / UJ6). Angular PWA plus two Spring Boot Java 21 services ([ADR 0002](../adr/0002-split-backend-into-two-independently-deployable-components.md), [ADR 0015](../adr/0015-implement-elegibilidade-worker-in-java-with-spring-boot.md)). `escala-api` owns the published schedule and its changes; `elegibilidade-worker` runs deterministic function checks, text extraction and the IPMA poll. The client uses REST/HTTPS; the services use RabbitMQ ([ADR 0005](../adr/0005-use-rabbitmq-as-message-broker.md)). Each service has its own PostgreSQL ([ADR 0003](../adr/0003-one-postgresql-database-per-component.md)). AI extracts unavailability only, behind `AI_ENABLED` ([ADR 0011](../adr/0011-call-a-language-model-behind-a-feature-flag.md)); the planned extractor is Claude Haiku via the Claude Code CLI ([ADR 0016](../adr/0016-use-claude-haiku-through-the-claude-code-cli.md), *Proposed* until SCRUM-38). AI never decides swaps or cover ([ADR 0007](../adr/0007-deterministic-eligibility-engine-ai-never-decides.md), RF34 Won't). IPMA is **shown**, not used to set composition (RN10 off, RF27/RF28 Won't). Local and evaluation: Docker Compose (`docker compose up --build`). Cluster (team demo): Kubernetes + GitHub Actions + Argo CD. Readiness, never dispatch.

## Technology selection

Why these technologies, not others. Each row is an accepted ADR except 0016 (*Proposed* until SCRUM-38). Same table as **High Level System Architecture**.

| Choice | Why it fits Escala | Rejected | ADR |
|---|---|---|---|
| Angular PWA | Roster on the phone without an app store; last-loaded roster readable offline (RNF06) | Native apps (two stores) | — |
| Two Spring Boot Java 21 services | Roster writes stay up when a check or IPMA call is slow; same language on both backends | Monolith; Python worker | 0002, 0015 |
| PostgreSQL, one database per service | Each service owns its data; no FKs across the queue | Shared database | 0003 |
| RabbitMQ + outbox | At-least-once, retries 5 s / 30 s / 2 min, DLQ; 202 without waiting (RNF02) | Sync REST between services; Kafka (ops cost) | 0005, 0009 |
| IPMA open data, hourly cache | Fire risk as information only | Using RCM to size the *piquete* (RN10 off) | 0010, 0013 |
| Claude Haiku via CLI, `AI_ENABLED` | Extraction of unavailability only; evaluation turns it off | AI that decides swaps or cover | 0007, 0011, 0016 |
| Docker Compose for evaluation | One command on a staff laptop (§12) | Kubernetes as the evaluation contract | 07 · Technical Design |

**Where it runs.** Evaluation / walking skeleton: Docker Compose (`docker compose up --build`). That stack is the web PWA, two Java services, two PostgreSQL databases and RabbitMQ. Profile `observability` adds Tempo and Grafana. Profile `evaluation` adds `ipma-sim` and sets `AI_ENABLED=false`. Team demo host: Kubernetes overlays under `infra/k8s` (dev / qa / prod, Argo CD, duckdns). Compose is the evaluation contract.

## System context

Source: [`c4-system-context.puml`](c4-system-context.puml).

```mermaid
flowchart LR
  mem["Member"]
  sup["Supervisor / duty chief"]
  cmd["Commander"]
  escala["Escala: published roster, unavailability, swaps and cover"]
  ipma["IPMA open data: RCM and warnings, information only"]
  llm["Claude Haiku via CLI: extraction only, ADR 0016 Proposed"]
  subgraph out["Out of scope: readiness, never dispatch"]
    dispatch["Dispatch, call-out and incident systems: never connected"]
  end
  mem -->|"own roster, unavailability, swap, confirm presence, accept cover"| escala
  sup -->|"builds and publishes schedule; informed of changes (approval: D-01)"| escala
  cmd -->|"members and certifications"| escala
  escala -->|"polls hourly over HTTPS"| ipma
  escala -->|"extraction requests when AI_ENABLED"| llm
```

## Containers

Source: [`c4-containers.puml`](c4-containers.puml), [`high_level_system_architecture.puml`](high_level_system_architecture.puml). No arrow crosses into another component's database.

```mermaid
flowchart LR
  user["Member, supervisor, commander"]
  subgraph stack["Escala on Docker Compose"]
    web["web: Angular PWA"]
    api["escala-api: Spring Boot"]
    apidb[("escala-db: PostgreSQL")]
    mq{{"RabbitMQ"}}
    worker["elegibilidade-worker: Spring Boot (Java)"]
    wdb[("elegibilidade-db: PostgreSQL")]
    sim["ipma-sim: IPMA simulator"]
  end
  ipma["IPMA open data"]
  llm["Language model"]
  user -->|"HTTPS, sync"| web
  web -->|"REST over HTTPS, sync"| api
  api -->|"SQL, sync"| apidb
  api -->|"AMQP via outbox, async"| mq
  mq -->|"AvaliacaoPedida, ExtracaoPedida"| worker
  worker -->|"AMQP via outbox, async"| mq
  mq -->|"AvaliacaoConcluida, ExtracaoConcluida, RiscoAtualizado"| api
  worker -->|"SQL, sync"| wdb
  worker -->|"HTTPS, hourly poll, default profile"| ipma
  worker -.->|"HTTP, evaluation profile and tests"| sim
  worker -->|"HTTPS, extraction when AI_ENABLED"| llm
```

## Components and data ownership

No component reads another component's database ([ADR 0003](../adr/0003-one-postgresql-database-per-component.md)). The worker gets the facts it needs on the event ([ADR 0008](../adr/0008-events-carry-the-state-the-consumer-needs.md)).

| Component | Responsibility | Owns data | Talks to | Sync / async |
|---|---|---|---|---|
| `escala-api` | Published schedule, members, unavailability, swaps, presence, cover, audit; DailyRisk display read model | `escala-db` | `web`; RabbitMQ | Sync REST; async events |
| `elegibilidade-worker` | Function checks (RF21), extraction (RF31, ADR 0016), cached RCM (RF25, RF29) | `elegibilidade-db` | RabbitMQ; IPMA; Claude CLI | Async events; sync HTTP / local process |
| `web` | Member and supervisor screens; roster offline (RNF06) | None | `escala-api` | Sync REST |
| `ipma-sim` | IPMA paths and §6.5 failures (`evaluation` profile) | None | worker | Sync HTTP |

Split by question: published roster versus slow or external work. If the worker is down, `escala-api` still accepts unavailability, swap requests and cover accepts; applying a swap or filling a cover waits for a valid check (RF24).

## Asynchronous workflows

| Workflow | Trigger | Result | Why async |
|---|---|---|---|
| Function check | `AvaliacaoPedida` (`SWAP` / `COVER` / `ASSIGNMENT`) | `AvaliacaoConcluida` | 202 without waiting (RNF02); schedule changes only after `ELIGIBLE` (RF24) |
| Extraction | `ExtracaoPedida` | `ExtracaoConcluida` | Model may be down; form stays up (RF35) |
| Risk update | Hourly poll | `RiscoAtualizado` | Display only; does not change slots |

```mermaid
sequenceDiagram
  autonumber
  actor M as Member
  participant WEB as web
  participant API as escala-api
  participant MQ as RabbitMQ
  participant WK as elegibilidade-worker
  M->>WEB: Accept swap or cover
  WEB->>API: POST with X-Trace-Id
  API->>API: Transaction: pending + outbox AvaliacaoPedida
  API-->>WEB: 202 check pending
  API->>MQ: Outbox publishes AvaliacaoPedida
  MQ->>WK: At least once
  alt new eventId
    WK->>WK: Deterministic rules, no AI
    WK->>MQ: Outbox AvaliacaoConcluida
    MQ->>API: Store result or ignore duplicate / older
  else retry then DLQ
    Note over MQ: 5 s, 30 s, 2 min, then DLQ
  end
```

## Reliability of messaging

| §6.3 | Mechanism |
|---|---|
| Retries | 5 s / 30 s / 2 min then DLQ ([ADR 0005](../adr/0005-use-rabbitmq-as-message-broker.md)) |
| Duplicates | `eventId` in the same transaction ([ADR 0009](../adr/0009-transactional-outbox-and-idempotent-consumers.md), RNF05) |
| Failures | Transactional outbox, then DLQ ([ADR 0009](../adr/0009-transactional-outbox-and-idempotent-consumers.md)) |
| Eventual consistency | pending / evaluated / failed; swap or cover not applied without a valid check ([ADR 0008](../adr/0008-events-carry-the-state-the-consumer-needs.md), RF24) |

Exchange `escala.events`. Consumers ignore a result older than the stored one (`outdated`). `ExtracaoPedida` holds free text: purge the DLQ after 7 days (**09 · Security and Privacy Assessment**).

## External integration — IPMA

Endpoints: RCM `rcm-d{0,1,2}.json`; warnings `warnings_www.json`. Poll hourly by the worker ([ADR 0010](../adr/0010-poll-ipma-hourly-and-cache-the-last-valid-value.md)). DICO **1312**. Timeout **5 s**, two retries. Staleness **3 h**: show age and an assumed-higher *warning*; **do not** add slots ([ADR 0013](../adr/0013-assume-the-higher-risk-when-ipma-data-is-unusable.md), D-00). Cite IPMA; notify `webmaster@ipma.pt` (**AI Usage and Third-Party Attribution**).

| §6.5 | Detection | Behaviour |
|---|---|---|
| Unavailable | Timeout 5 s or HTTP error | Last cache + age; after 3 h, warning |
| Delayed | Old `dataPrev` / `fileDate` | Same |
| Incomplete | DICO missing | Keep last value for our municipality |
| Invalid | Bad JSON or RCM not 1–5 | Reject; keep last valid |

Simulator: `tools/ipma-sim`, `IPMA_SIM_MODE=ok\|unavailable\|delayed\|incomplete\|invalid` (tests and Compose profile `evaluation`, §12).

## Degraded modes

| Down | Behaviour | Message |
|---|---|---|
| IPMA | Last RCM + age; warning only | "Fire risk from IPMA last updated {age}. Level may be higher than shown. The schedule is unchanged." |
| AI | Form fallback (RF35, ADR 0011); `evaluation` profile sets `AI_ENABLED=false` | "Automatic reading is unavailable. Please enter the dates in the form." |
| Worker or RabbitMQ | Requests 202; swap/cover not applied | "Check pending. Your shift will update when the check completes." |

If Escala itself is down, the corps uses the phone. Runbook: [Deployment and Operations](../operations/deployment-and-operations.md).

## Data model

Source: [`domain_model.puml`](domain_model.puml). Cross-service IDs are references on events, never foreign keys. Instants in UTC; PWA shows `Europe/Lisbon` ([ADR 0012](../adr/0012-store-instants-in-utc.md)).

- `escala-db`: Member, Certification, Licence, Piquete, Slot, Assignment, Unavailability, Swap request, Presence confirmation, Cover request, Application, Audit, Daily risk (display), outbox.
- `elegibilidade-db`: Evaluation (`purpose` SWAP/COVER/ASSIGNMENT), Extraction, Risk observation, outbox.

```mermaid
erDiagram
  MEMBER ||--o{ UNAVAILABILITY : declares
  MEMBER ||--o{ SWAP_REQUEST : requests
  MEMBER ||--o{ ASSIGNMENT : receives
  PIQUETE ||--|{ SLOT : has
  SLOT ||--o{ ASSIGNMENT : filled_by
  SLOT ||--o{ COVER_REQUEST : opened_for
  SLOT ||--o{ PRESENCE_CONFIRMATION : presence
  ASSIGNMENT {
    string evaluation_state "pending evaluated failed outdated"
  }
  DAILY_RISK {
    boolean assumed_higher "display only never drives slots"
  }
```

Consistency is strong inside each database and eventual between services (visible states). Free text is deleted after confirmation (RNF09). No names, contacts or date of birth leave `escala-db`; free text leaves once in `ExtracaoPedida`.

**D-01** (supervisor approval of swaps) and **D-02** (nobody accepts cover / exceptional assignment) stay open on the **Investor Decision Log**.

## API outline

- REST: [`contracts/openapi.yaml`](../../contracts/openapi.yaml)
- Events: [`contracts/events/`](../../contracts/events/) — envelope `eventId`, `type`, `schemaVersion`, `occurredAt`, `traceId`
- Auth: mocked bearer token with `memberId` and `role` ([ADR 0006](../adr/0006-mock-authentication-and-enforce-roles.md)); health endpoints stay open
- Header: `X-Trace-Id` (RF37)

Walking skeleton (SCRUM-45): `POST /api/v1/piquetes` (201 + `Location`) · `GET /api/v1/piquetes`. Product API: `GET /me/roster` · `POST /piquetes/{id}/publish` · `POST /unavailabilities` (202) · `POST /swaps` · `POST /swaps/{id}/accept` · `POST /slots/{id}/presence` · `POST /cover-requests/{id}/accept` · `POST /assignments/{id}/confirm` (RF24) · `GET /risk/today`. Liveness/readiness: `/actuator/health` (RF36).

## Deployment view

Source: [`c4-deployment.puml`](c4-deployment.puml). Local and evaluation: `docker compose up --build` (PWA `http://localhost:8000` proxies `/api/` to the API; API `http://localhost:8080/actuator/health`; worker `:8081`). Observability: Tempo and Grafana (`--profile observability`, Grafana `http://localhost:3000`). Evaluation: `ipma-sim` (`--profile evaluation`). Java images include the OpenTelemetry agent (`JAVA_TOOL_OPTIONS`); Compose sets `OTEL_JAVAAGENT_ENABLED=false` unless tracing is on. On some Windows hosts port 5672 is excluded by Hyper-V; map RabbitMQ to `RABBITMQ_PORT` in `.env` (container AMQP stays 5672). Runbook: [Deployment and Operations](../operations/deployment-and-operations.md).

```mermaid
flowchart TB
  pr["PR merged to main"] --> ci["GitHub Actions: build, test, images"]
  ci --> reg["GHCR"]
  subgraph host["Docker Compose: laptop / evaluation"]
    web["web"]
    api["escala-api"]
    worker["elegibilidade-worker"]
    mq{{"RabbitMQ"}}
    sim["ipma-sim"]
    tempo["Tempo"]
    graf["Grafana"]
  end
  reg --> host
```

Kubernetes overlays under `infra/k8s` are the team demo host. Teaching-staff evaluation is Docker Compose.

## Observability and SLIs

- Logs: JSON stdout, `X-Trace-Id` / `traceId` (RF37); never contacts or free text
- Health: `/actuator/health` (RF36); readiness via the same actuator when datasource and broker are wired
- Traces: OpenTelemetry Java agent in both images; Tempo; Grafana. Kubernetes also runs Loki and Alloy.
- Metrics: checks, DLQ, IPMA age, extraction fallbacks, time-to-fill (RF38)
- Dashboard: Grafana `http://localhost:3000` (RF39)
- SLI 1: function-check p95 < 5 s (RNF01), from `AvaliacaoPedida` outbox to applied `AvaliacaoConcluida`
- SLI 2: ≥ 95 % of checks without DLQ replay or exceptional assignment

## Done when

- [x] §7.1.2 #7 covered: system context, containers, data model, API, async workflow, IPMA, deployment
- [x] Content matches D-00 (UJ1 / UJ3 / UJ6); D-01 and D-02 left open
- [x] This file pasted to Confluence **07 · Technical Design**
- [ ] PlantUML re-exported (deployment, domain model)
- [ ] Reviewed by another team member
- [ ] Status set to Done
