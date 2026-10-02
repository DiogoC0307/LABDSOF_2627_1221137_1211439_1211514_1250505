# 07 · Technical Design

| Field | Value |
|---|---|
| Status | Draft |
| Owner | [TODO: name] |
| Contributors | [TODO: names] |
| Deliverable | Sprint 1 · #7 Technical Design |
| Sources | Assignment §6, §7.1.2 #7 |
| Confluence parent | 4 · Architecture and Security |
| Labels | `sprint-1`, `deliverable-07`, `architecture` |
| Jira | [TODO: ESC-xx] |
| Last updated | YYYY-MM-DD |

> **ℹ️ About this page.** A one-page summary of how Escala is built to check who can crew today's *piquete*; the full design lives in Git as **Technical Design** (`docs/architecture/technical-design.md`). Investors read it at the Sprint 1 review, and every team member must be able to explain it (§10).

> **ℹ️ Guidance.** §7.1.2 #7 asks for: System context; Container or component view; Data model; API outline; Asynchronous workflow; External integration; Deployment view. Update the Git document first, then this summary, and export diagrams from Git rather than drawing them twice. §6 says "All significant technical decisions must be justified.", so link each choice to its ADR on **08 · Architecture Decision Records**.

## Summary

> **ℹ️ Guidance.** One paragraph: the client, the backend components, how they communicate, where data lives, the external integration, and how the system is deployed and observed. Someone who reads only this paragraph should be able to sketch the container diagram.

> **✏️ Example (illustrative — replace).** Escala is an Angular PWA backed by two independently deployable components. `escala-api` (Spring Boot) owns what the piquete needs: people, rosters, slots, unavailability, requests and audit. `elegibilidade-worker` (Python) works out who can fill it. It runs the deterministic eligibility rules, reads IPMA fire risk and runs the AI extraction, but the AI never decides eligibility. The two components exchange events through RabbitMQ using a transactional outbox, and each owns its own PostgreSQL database. Everything runs in Docker Compose, is built and deployed by GitHub Actions, and is observed with Prometheus and Grafana. Escala is about readiness, never dispatch.

## System context

[DIAGRAM: C4 level 1, exported from the Git technical design]

> **ℹ️ Guidance.** Draw Escala as one box, with the people who use it and the external systems it depends on. Make the boundary visible: no dispatch or incident system is connected, because Escala is about readiness, never dispatch.

> **✏️ Example (illustrative — replace).** Actors: Firefighter, Duty chief, Commander. External systems: IPMA open data (fire risk and weather warnings) and the language model ([TODO: local or hosted — ADR 0011]). Left out on purpose: any dispatch or incident-handling system.

## Containers

[DIAGRAM: C4 level 2 — `web`, `escala-api`, `elegibilidade-worker`, `escala-db`, `elegibilidade-db`, RabbitMQ, `ipma-sim`, with each arrow labelled sync (REST/HTTP) or async (event); exported from the Git technical design]

> **ℹ️ Guidance.** Draw one box per deployable unit and one per data store, and label every arrow with its protocol. §6.2 requires "at least two independently deployable backend components". Name them here and point to ADR 0002.

## Components and data ownership

> **ℹ️ Guidance.** Write one row per deployable component. §6.2 asks you to justify component boundaries, responsibilities and data ownership, synchronous and asynchronous communication, coupling and cohesion, deployment implications and failure handling. Keep one line per component here and put the reasoning in ADR 0002 and ADR 0003. No component reads another component's database.

| Component | Responsibility | Owns data | Talks to | Sync / async |
|---|---|---|---|---|
| [TODO: component] | [TODO: responsibility] | [TODO: database] | [TODO: components and systems] | [TODO: sync / async, protocol] |

> **✏️ Example (illustrative — replace).**
>
> | Component | Responsibility | Owns data | Talks to | Sync / async |
> |---|---|---|---|---|
> | `escala-api` (Spring Boot) | What the piquete needs: people, rosters, slots, unavailability, requests, audit | `escala-db` | `web`; RabbitMQ | Sync REST with `web`; async events with the worker |
> | `elegibilidade-worker` (Python) | Who can fill it: evaluations, extractions, observed risk | `elegibilidade-db` | RabbitMQ; IPMA; language model | Async events; sync HTTP to IPMA and the model |
> | `web` (Angular PWA) | Screens for firefighters, duty chiefs and commanders; roster readable offline | None (browser cache only) | `escala-api` | Sync REST |
> | `ipma-sim` | Imitates the IPMA endpoints and their failures for tests and the `evaluation` profile | None | `elegibilidade-worker` | Sync HTTP |

## Asynchronous workflows

> **ℹ️ Guidance.** §6.3 requires "at least one meaningful asynchronous workflow". List every workflow that goes through RabbitMQ and say why it cannot be a synchronous call. Event schemas live in `contracts/events/`.

| Workflow | Trigger event | Result event | Why async |
|---|---|---|---|
| Eligibility | `AvaliacaoPedida` (evaluation requested) | `AvaliacaoConcluida` (evaluation completed) | [TODO: why] |
| Extraction | `ExtracaoPedida` (extraction requested) | `ExtracaoConcluida` (extraction completed) | [TODO: why] |
| Risk update | Hourly IPMA poll in the worker (timer, no incoming event) | `RiscoAtualizado` (risk updated) | [TODO: why] |

> **✏️ Example (illustrative — replace).** *Eligibility:* the API accepts the request without waiting for the evaluation (RNF02), so a slow or stopped worker never freezes the duty chief's screen. *Extraction:* the language model can be slow or down, and the form flow must keep working regardless. *Risk update:* IPMA is external and polled on a schedule; `escala-api` reacts to the new risk level instead of calling IPMA itself.

## Reliability of messaging

> **ℹ️ Guidance.** §6.3: the workflow "should demonstrate appropriate handling of retries, duplicate events, failures, and eventual consistency." Name one mechanism per concern, record it in an ADR, and prove it with a test in **Test Strategy** (`docs/testing/test-strategy.md`).

| §6.3 concern | Mechanism |
|---|---|
| Retries | [TODO: mechanism] |
| Duplicate events | [TODO: mechanism] |
| Failures | [TODO: mechanism] |
| Eventual consistency | [TODO: mechanism] |

> **✏️ Example (illustrative — replace).**
>
> | §6.3 concern | Mechanism |
> |---|---|
> | Retries | Retry queues after 5 s, 30 s and 2 min, then the dead-letter queue (DLQ) |
> | Duplicate events | Idempotency key per event; the consumer records the keys it has processed and ignores a second delivery (RNF05) |
> | Failures | Transactional outbox: the event is written in the same transaction as the state change, then relayed to RabbitMQ (ADR 0009). After the last retry the message goes to the DLQ and shows on the dashboard |
> | Eventual consistency | Visible states: pending / evaluated / failed. Confirmation stays blocked while no valid evaluation exists (RF24) |

## External integration — IPMA

> **ℹ️ Guidance.** Name the endpoints, the polling frequency and the cache. §6.5: "The application must handle unavailable, delayed, incomplete, or invalid external data appropriately." Write one row per case. The IPMA terms of use require citing the source and notifying `webmaster@ipma.pt` (see **AI Usage and Third-Party Attribution**).

- Rural fire risk (RCM 1–5 per municipality, by DICO code) for today and the next two days: `https://api.ipma.pt/open-data/forecast/meteorology/rcm/rcm-d{0,1,2}.json`
- Weather warnings: `https://api.ipma.pt/open-data/forecast/warnings/warnings_www.json`
- Polling: hourly, by `elegibilidade-worker`, with the last valid response cached (ADR 0010)
- Our municipality's DICO code: [TODO: DICO]
- Staleness threshold: [TODO: hours]

| §6.5 case | Detection | Behaviour |
|---|---|---|
| Unavailable | [TODO: detection] | [TODO: behaviour] |
| Delayed | [TODO: detection] | [TODO: behaviour] |
| Incomplete | [TODO: detection] | [TODO: behaviour] |
| Invalid | [TODO: detection] | [TODO: behaviour] |

> **✏️ Example (illustrative — replace).**
>
> | §6.5 case | Detection | Behaviour |
> |---|---|---|
> | Unavailable | Timeout ([TODO: s]) or HTTP error | Serve the last cached value with its age; beyond the staleness threshold, assume the higher risk level (ADR 0013) |
> | Delayed | The data's reference date is older than expected (verify the field) | Same as unavailable: show the age; beyond the threshold, assume the higher risk level |
> | Incomplete | Our DICO code is missing from the response | Keep the last value for our municipality; log a warning with the trace ID |
> | Invalid | Schema validation fails, or an RCM value is outside 1–5 | Reject the whole response, keep the last valid value and increment an error metric |

**IPMA simulator.** `tools/ipma-sim` serves the same endpoints and can reproduce each case above on demand. Tests and the `evaluation` profile use it, so the product runs offline in the evaluation environment (§12). Simulator modes and how to switch them: [TODO: modes].

## Degraded modes

> **ℹ️ Guidance.** §6.8: "The product should fail safely and communicate limitations clearly to users." Write one row per dependency and put the exact text the user sees in the last column. If Escala itself is down, the corps goes back to the phone. The runbook is in **Deployment and Operations** (`docs/operations/deployment-and-operations.md`).

| Dependency down | Behaviour | Message to the user |
|---|---|---|
| IPMA | [TODO: behaviour] | [TODO: message] |
| AI service | [TODO: behaviour] | [TODO: message] |
| `elegibilidade-worker` | [TODO: behaviour] | [TODO: message] |

> **✏️ Example (illustrative — replace).**
>
> | Dependency down | Behaviour | Message to the user |
> |---|---|---|
> | IPMA | Last value shown with its age; beyond the threshold, the higher risk level is assumed | "Fire risk from IPMA last updated 3 h ago. Showing the higher level until IPMA responds." |
> | AI service | Free-text box replaced by the unavailability form (RF35) | "Automatic reading is unavailable. Please enter the dates in the form." |
> | `elegibilidade-worker` | Evaluations stay "pending"; confirmation is blocked | "Eligibility check pending. You can confirm once the check is complete." |

## Data model

> **ℹ️ Guidance.** Summarise the main entities of each component and how they relate. The ER diagram and field lists live in the Git **Technical Design** (`docs/architecture/technical-design.md`). §6.4 also asks about consistency, lifecycle, retention and deletion, backup or recovery, and privacy constraints. Give each one line, or point to **09 · Security and Privacy Assessment**.

- `escala-db`: [TODO: main entities]
- `elegibilidade-db`: [TODO: main entities]
- Full model: [TODO: GitHub link to the data model section]

> **✏️ Example (illustrative — replace).** `escala-db`: Member, Certification (type, expiry date), Driving licence, Piquete, Slot, Assignment, Unavailability, Cover request, Application, Audit record. `elegibilidade-db`: Evaluation (result, violated rule codes, engine version), Extraction (proposed values, confirmed or not), Risk observation (DICO, RCM, date, fetched at). Both run on PostgreSQL: one technology, one database per component (ADR 0003). The free text of an unavailability is deleted after confirmation (RNF09).

## API outline

> **ℹ️ Guidance.** `contracts/openapi.yaml` is the source of truth. List only the main endpoints here and link to the file, plus the rendered docs if you publish them. Event schemas live in `contracts/events/`.

- REST contract: `contracts/openapi.yaml` — [TODO: GitHub link]
- Rendered API docs: [TODO: URL, if published]
- Event schemas: `contracts/events/` — [TODO: GitHub link]

> **✏️ Example (illustrative — replace).**
> - `POST /piquetes` — create a piquete with its slots (ESC-23)
> - `POST /unavailabilities` — declare unavailability; answers 202 without waiting for evaluation (RNF02)
> - `GET /slots/{id}/candidates` — eligible and ineligible candidates with reasons (RF16)
> - `POST /assignments/{id}/confirm` — refused while no valid evaluation exists (RF24)
> - `GET /health`, `GET /health/ready` — on both services (RF36)

## Deployment view

[DIAGRAM: deployment view — the containers on the Docker Compose host (`web`, `escala-api`, `elegibilidade-worker`, `escala-db`, `elegibilidade-db`, RabbitMQ, `ipma-sim`, Prometheus, Grafana) and the GitHub Actions pipeline that builds and deploys them; exported from the Git technical design]

> **ℹ️ Guidance.** Show where each container runs, how it gets there (the pipeline) and which profile is used: `default`, or `evaluation` (IPMA simulator and AI fallback). Commands, configuration and rollback live in **Deployment and Operations** (`docs/operations/deployment-and-operations.md`); link to it instead of copying them here.

- Environment: [TODO: where it is deployed]
- URL: [TODO: URL]

## Observability and SLIs

> **ℹ️ Guidance.** §6.7 sets a minimum: structured application logs, health checks, relevant metrics, correlation or trace identifiers, error reporting and a small operational dashboard. Say where each one is. Give each SLI a target and the place where it is shown.

- Logs: [TODO: format and where to read them], with one trace ID from `web` through `escala-api` and the event to `elegibilidade-worker` (RF37)
- Health checks: `/health` and `/health/ready` on both services (RF36)
- Metrics: [TODO: main metrics], exposed to Prometheus (RF38)
- Error reporting: [TODO: how errors surface to the team]
- Dashboard: Grafana — [TODO: link] (RF39)
- SLI 1 — eligibility evaluation p95 latency; target < 5 s (RNF01)
- SLI 2 — % of requests evaluated without manual intervention; target [TODO: %]

> **✏️ Example (illustrative — replace).** Metrics: evaluations requested / completed / failed, DLQ size, age of the IPMA data served, and how many extractions fell back to the form.

## Done when
- [ ] Every §7.1.2 #7 item is covered: System context; Container or component view; Data model; API outline; Asynchronous workflow; External integration; Deployment view
- [ ] The diagrams are exported from the Git **Technical Design** (`docs/architecture/technical-design.md`), and that document agrees with this page
- [ ] All four §6.5 cases and every degraded mode have a defined behaviour and a user message
- [ ] Every significant choice links to its ADR on **08 · Architecture Decision Records**
- [ ] Reviewed by another team member
- [ ] Guidance and example blocks removed
- [ ] Status set to Done
