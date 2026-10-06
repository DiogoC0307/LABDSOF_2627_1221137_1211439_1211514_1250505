# 10 · Walking Skeleton

| Field | Value |
|---|---|
| Status | Draft |
| Owner | [TODO: name] |
| Contributors | [TODO: names] |
| Deliverable | Sprint 1 · #10 Walking Skeleton |
| Sources | Assignment §7.1.2 #10; Deck (walking skeleton) |
| Confluence parent | 4 · Architecture and Security |
| Labels | `sprint-1`, `deliverable-10`, `architecture`, `walking-skeleton` |
| Jira | [TODO: SCRUM-xx] |
| Last updated | YYYY-MM-DD |

> **ℹ️ About this page.** Proof that Escala's main components are linked end to end, built, tested and deployed from the first sprint. The teaching staff read it at the Sprint 1 review; the team uses it to agree what the skeleton does and which vertical slices come next.

> **ℹ️ Guidance.** §7.1.2 #10 asks for "Minimal end-to-end implementation; Source repository; Automated build; Initial test; Containerized deployment"; each has a row under *Proof*. Start the skeleton on day 1, in parallel with discovery. Component names and the path must match **07 · Technical Design**; commands live in **Deployment and Operations** (`docs/operations/deployment-and-operations.md`).

## Definition

*"A Walking Skeleton is a tiny implementation of the system that performs a small end-to-end function. It need not use the final architecture, but it should link together the main architectural components. The architecture and the functionality can then evolve in parallel."* — Alistair Cockburn (quoted in the course deck)

> **ℹ️ Guidance.** Keep the quote and its attribution as they are; the credit is also listed on **AI Usage and Third-Party Attribution**. The point is to remove integration risk early: every component talks to the next before any business rule exists.

## What our skeleton does

It creates one *piquete* with one slot, and the worker returns a trivial evaluation. The path, PWA → API → DB → queue → worker → DB → back to the UI:

1. `web` (Angular PWA): one button sends the request to `escala-api`.
2. `escala-api` saves the *piquete* and its slot in `escala-db` and publishes `AvaliacaoPedida` (evaluation requested) to RabbitMQ [TODO: directly, or already through the transactional outbox (ADR 0009)].
3. RabbitMQ delivers the event to `elegibilidade-worker`.
4. `elegibilidade-worker` stores a trivial evaluation in `elegibilidade-db`, applying no rule, and publishes `AvaliacaoConcluida` (evaluation completed).
5. `escala-api` consumes the result and records it in `escala-db`; `web` shows the slot's evaluation state change from "pending" to "evaluated" [TODO: polling or push].

[DIAGRAM: the skeleton path — `web` → `escala-api` → `escala-db` → RabbitMQ → `elegibilidade-worker` → `elegibilidade-db`, and back through RabbitMQ and `escala-api` to `web`; one numbered arrow per step above; draw.io]

> **ℹ️ Guidance.** Describe what actually runs, not the target design. If a component or step is still missing, say which one and when it arrives. `web` never reads the worker's database: the result comes back through `escala-api`, as on **07 · Technical Design**.

> **✏️ Example (illustrative — replace).** The duty chief presses *Create piquete*. The slot appears as "pending"; a moment later it shows "evaluated". The worker's stored result is fixed: `{"result": "ELIGIBLE", "violatedRules": [], "engineVersion": "0.0.0"}`. The same trace ID appears in the `escala-api` and `elegibilidade-worker` logs (RF37). Story: SCRUM-45 "create *piquete* with one slot".

## What it deliberately does not do

- **No business rules.** The slot is saved without validation and the evaluation checks nothing; RN01–RN10 arrive in later slices.
- **No real authentication.** [TODO: no login at all, or one fixed mocked user]; even the MVP mocks authentication, with real roles enforced from Sprint 2 (ADR 0006, RF40).
- **No AI.** No extraction and no language model; the worker's evaluation is a fixed result.

> **ℹ️ Guidance.** Follow the deck's Scenario A: one button that sends a DTO with one attribute to the backend; "The backend persists the data without any business logic validation." Later slices add attributes, child objects and business rules one at a time. Listing what is left out stops the skeleton from growing into a half-built feature.

> **✏️ Example (illustrative — replace).** The *Create piquete* button sends `{"date": "YYYY-MM-DD"}` and nothing else. The *piquete* gets one slot with no role, vehicle or required certification. The next slice adds the slot's role, vehicle and required certification (RF07); the one after that rejects someone whose TAS has expired (SCRUM-999, RN02).

## Proof

| Required by §7.1.2 | Evidence | Link |
|---|---|---|
| Minimal end-to-end implementation | One *piquete* with one slot created in the deployed PWA, with the trivial evaluation shown back in the UI; the Jira story and its merged pull request | [TODO: Jira story, PR or recording] |
| Source repository | The GitHub repository, with `apps/web`, `apps/escala-api`, `apps/elegibilidade-worker`, `contracts/` and `infra/` | [TODO: GitHub link] |
| Automated build | Last green run of the GitHub Actions pipeline, building all three apps | [TODO: link to the last green run] |
| Initial test | At least one automated test that runs in the pipeline, named after its test case ID | [TODO: link to the test and its CI report] |
| Containerized deployment | Every component runs as a container (Docker Compose) in the deployed environment | [TODO: URL] |

> **ℹ️ Guidance.** Each *Link* must open something a reviewer can check without asking the team: a run, a URL, a file. Replace a link when a newer green run exists; a red pipeline on review day is not proof.

> **✏️ Example (illustrative — replace).** *Initial test:* `TC-SCRUM-45-01` — **Given** the stack is running, **When** a *piquete* with one slot is created through `escala-api`, **Then** the slot's evaluation state becomes "evaluated" within [TODO: s]. Link: the test file and the pipeline run that passed it.

## How to run

- Prerequisites: [TODO: e.g. Docker with Compose, versions]
- From a clean clone, one command: `[TODO: one command]`
- Full instructions: the repository README ([TODO: GitHub link to `README.md`]) and **Deployment and Operations** (`docs/operations/deployment-and-operations.md`)
- Last clean-clone run: [TODO: date, who, result]

> **ℹ️ Guidance.** One command, from a clean clone, with no manual steps and no secrets typed by hand. Ask a team member who did not build the skeleton to run it and record the result above. Do not copy the configuration here; link to the README.

> **✏️ Example (illustrative — replace).** `docker compose up --build`, then open `http://localhost:[TODO: port]` and press *Create piquete*. Last clean-clone run: YYYY-MM-DD, Member B, passed on the first try.

## Next slices

- [TODO: slice — what the user can do afterwards — Jira key]

> **ℹ️ Guidance.** List what Sprint 2 adds, as vertical slices: each one goes through the whole chain and ends in something a user can see, never "the database layer" or "the UI". Order them by the Sprint 2 goal and link each to its Jira story; the full plan is on **05 · Product Backlog** and **Product Roadmap (GO)**.

> **✏️ Example (illustrative — replace).** Sprint 2 goal: "The principal journey, from a declared unavailability to a confirmed verified-qualified replacement, works deployed, observed, and with one degraded mode demonstrated."
> 1. Declare unavailability by form (RF12), which opens a gap on the *piquete* (RF14).
> 2. Real eligibility rules returning the violated rule codes (RF21), starting with TAS expiry (SCRUM-999, RN02).
> 3. List eligible and ineligible candidates with reasons (RF16), apply (RF17) and confirm (RF18); confirmation blocked without a valid evaluation (RF24).
> 4. Reliable messaging: outbox, idempotency key, retries 5 s / 30 s / 2 min, then the DLQ (ADR 0005, ADR 0009).
> 5. Today's fire risk from IPMA through the simulator (RF25), with the age of cached data (RF29).
> 6. Free-text unavailability with AI extraction and confirmation in editable fields (RF13, RF31, RF32), with the form as fallback (RF35).

## Done when
- [ ] The path on this page matches what runs in the deployed environment and the component names on **07 · Technical Design**
- [ ] Every row under *Proof* has a working link: repository, last green run, initial test, deployed URL
- [ ] A team member who did not build it ran the skeleton from a clean clone with the one command, and the run is recorded
- [ ] Sprint 2's next slices are listed and linked to Jira stories
- [ ] Reviewed by another team member
- [ ] Guidance and example blocks removed
- [ ] Status set to Done
