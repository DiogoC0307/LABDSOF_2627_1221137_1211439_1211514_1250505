# Sprint 2 · Deliverables

| Field | Value |
|---|---|
| Status | Draft |
| Owner | [TODO: name] |
| Contributors | [TODO: names] |
| Deliverable | — |
| Sources | Assignment §7.2 (§7.2.1, §7.2.2, §7.2.3) |
| Confluence parent | Sprint 2 |
| Labels | `sprint-2`, `deliverables` |
| Jira | [TODO: ESC-xx] |
| Last updated | YYYY-MM-DD |

> **ℹ️ About this page.** Tracks the fourteen deliverables that assignment §7.2.2 requires for Sprint 2: where the evidence for each one lives, who owns it and whether it is done. The team uses it to plan and close the sprint, and the investors use it at the Sprint 2 review.

## Objective

> "The objective of Sprint 2 is to deliver an operational end-to-end version of the product's most important user journey." — Assignment §7.2.1

**Sprint goal:** [TODO: one sentence]

> **ℹ️ Guidance.** Keep the assignment quote as it is. Below it, write the team's Sprint 2 goal in one sentence: name the journey and say how you will show that it works. The goal must match the Sprint goals table on **05 · Product Backlog**.

> **✏️ Example (illustrative — replace).** Sprint goal: "The principal journey, from a declared unavailability to a confirmed verified-qualified replacement, works deployed, observed, and with one degraded mode demonstrated."

## Deliverables

> **ℹ️ Guidance.** There is one row per §7.2.2 item. The *Deliverable* column uses the assignment's exact wording, so do not edit it. *Required content* says what counts as evidence for Escala; change it if the design changes. Fill *Page* (bold page title or repo path), *Jira* and *Owner* at Sprint Planning, and put the proof link in *Proof of done* when the row is finished. *Status*: Draft → In review → Done.

| # | Deliverable | Required content | Page | Jira | Owner | Proof of done | Status |
|---|---|---|---|---|---|---|---|
| 1 | Functional client application | Installable Angular PWA (`apps/web`) that runs the principal journey: declare an unavailability, see open slots, list eligible and ineligible candidates with reasons, apply, confirm (RF09, RF12, RF16–RF18) | [TODO: page] | [TODO: ESC-xx] | [TODO: name] | Deployed URL; the journey shown live on a phone | Draft |
| 2 | Authentication and authorization | Mocked authentication with real roles (Firefighter, Duty chief, Commander, Service account), enforced on every endpoint (RF40; §6.6 footnote). Nobody can confirm their own application | [TODO: page] | [TODO: ESC-xx] | [TODO: name] | Role tests green in CI; access-control matrix on **09 · Security and Privacy Assessment** | Draft |
| 3 | Persistent data storage | One PostgreSQL database per component (`escala-db`, `elegibilidade-db`); each service owns its data (ADR 0003) | [TODO: page] | [TODO: ESC-xx] | [TODO: name] | Data survives a restart of the stack; data model in **Technical Design** (`docs/architecture/technical-design.md`) | Draft |
| 4 | Documented API contracts | `contracts/openapi.yaml` for `escala-api`; event schemas in `contracts/events/` (`AvaliacaoPedida`, `AvaliacaoConcluida`, `ExtracaoPedida`, `ExtracaoConcluida`, `RiscoAtualizado`) | [TODO: page] | [TODO: ESC-xx] | [TODO: name] | Contract files versioned in the repo; contract tests green in CI | Draft |
| 5 | At least two backend components, or an approved modular alternative | `escala-api` (Spring Boot) and `elegibilidade-worker` (Python), each deployable on its own (ADR 0002) | [TODO: page] | [TODO: ESC-xx] | [TODO: name] | Both run as separate containers; one is redeployed without stopping the other | Draft |
| 6 | One meaningful asynchronous workflow | Eligibility evaluation over RabbitMQ (`AvaliacaoPedida` → `AvaliacaoConcluida`), with a transactional outbox, an idempotency key, retries after 5 s / 30 s / 2 min and then a DLQ. States are visible: pending / evaluated / failed | [TODO: page] | [TODO: ESC-xx] | [TODO: name] | Test: a duplicate event has no duplicate effect (RNF05); API accepts an unavailability with 202 in < 500 ms (RNF02) | Draft |
| 7 | Initial external-system integration | IPMA rural fire risk (RCM) for the corps' municipality (DICO code), polled hourly with a cache (RF25); IPMA simulator (`tools/ipma-sim`) for tests; source cited | [TODO: page] | [TODO: ESC-xx] | [TODO: name] | Today's RCM shown in the app with the age of the data (RF29); tests run against the simulator | Draft |
| 8 | First version of the AI-assisted capability | Structured unavailability extracted from free text, shown in editable fields and labelled "read automatically" until the user confirms it (RF13, RF31–RF33) | [TODO: page] | [TODO: ESC-xx] | [TODO: name] | Live demo; first run on the evaluation dataset, linked from **06 · Responsible AI Opportunity Assessment** | Draft |
| 9 | Non-AI baseline or fallback | Baseline: `dateparser` + rules. Fallback: form + template text behind a feature flag (RF12, RF35) | [TODO: page] | [TODO: ESC-xx] | [TODO: name] | With the flag turning AI off, the form flow still works (demo + test) | Draft |
| 10 | Automated unit, integration, and relevant end-to-end tests | Unit tests for every business rule (positive, negative, boundary, e.g. a certification that expires on the slot date); integration tests (e.g. Testcontainers); one end-to-end test of the principal journey | [TODO: page] | [TODO: ESC-xx] | [TODO: name] | CI test report; `TC-` IDs linked in Jira; **Test Strategy** (`docs/testing/test-strategy.md`) | Draft |
| 11 | CI/CD pipeline | GitHub Actions stages: build, tests, static analysis, dependency check, image, deploy | [TODO: page] | [TODO: ESC-xx] | [TODO: name] | Last green run on `main` and the deployment it produced | Draft |
| 12 | Structured logging, health checks, and initial metrics | Structured logs with an end-to-end trace ID (RF37); `/health` and `/health/ready` on both services (RF36); metrics exposed to Prometheus and shown in Grafana (RF38) | [TODO: page] | [TODO: ESC-xx] | [TODO: name] | One trace ID followed from the PWA request to the worker log; dashboard screenshot | Draft |
| 13 | Demonstration of at least one failure condition | At least one degraded mode (§6.8): IPMA down → the last value with its age shown; beyond a threshold, assume the higher risk level; worker down → evaluations "pending" and confirmation blocked; or AI down → form fallback | [TODO: page] | [TODO: ESC-xx] | [TODO: name] | Scripted demo from the runbook in **Deployment and Operations** (`docs/operations/deployment-and-operations.md`) | Draft |
| 14 | Updated backlog and architecture documentation | Jira backlog refined, with Sprint 3 planned. **07 · Technical Design**, the Git **Technical Design** (`docs/architecture/technical-design.md`) and the ADRs match what was built | [TODO: page] | [TODO: ESC-xx] | [TODO: name] | Page versions dated in Sprint 2; new ADRs listed on **08 · Architecture Decision Records** | Draft |

> **✏️ Example (illustrative — replace).** A finished row:
>
> | # | Deliverable | Required content | Page | Jira | Owner | Proof of done | Status |
> |---|---|---|---|---|---|---|---|
> | 11 | CI/CD pipeline | GitHub Actions stages: build, tests, static analysis, dependency check, image, deploy | **Deployment and Operations** (`docs/operations/deployment-and-operations.md`) | ESC-xx | Member A | Last green run on `main`: [TODO: link]; deployed image tag [TODO: image tag] | Done |

## Review question

> "Does the product already deliver observable user value, and can the team deploy, monitor, and operate it?" — Assignment §7.2.3

> **ℹ️ Guidance.** Answer both halves with evidence, not slides. For observable user value, show the principal journey live on the deployed product (rows 1, 8, 9). For deploy, monitor and operate, show the pipeline, the dashboard and a failure demo (rows 11–13). Bring 1–2 open decisions from **Investor Decision Log**, and record the feedback the same day (§5.7).

> **✏️ Example (illustrative — replace).** "Yes. On the deployed app, a firefighter declares an unavailability, the duty chief sees who is eligible and why, and confirms a qualified replacement. We deploy from `main` through GitHub Actions and watch evaluation latency on the dashboard. We will now stop the worker live to show that evaluations stay pending and confirmation is blocked."

## Done when
- [ ] Every row has a page or repo path, a Jira key and an owner
- [ ] Every row marked Done has a working proof link (CI run, demo recording or page version)
- [ ] Deliverables that were not achieved are marked and explained, not hidden
- [ ] The Sprint 2 goal is written and matches **05 · Product Backlog**
- [ ] Reviewed by another team member
- [ ] Guidance and example blocks removed
- [ ] Status set to Done
