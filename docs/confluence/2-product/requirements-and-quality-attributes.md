# Requirements and Quality Attributes

| Field | Value |
|---|---|
| Status | Draft |
| Owner | [TODO: name] |
| Contributors | [TODO: names] |
| Deliverable | — |
| Sources | Assignment §9 ("Requirements and quality attributes"), §5.1–§5.7, §6.1–§6.8; Deck M5 (DoR: business rules) |
| Confluence parent | 2 · Product |
| Labels | `product`, `requirements` |
| Jira | [TODO: SCRUM-xx] |
| Last updated | YYYY-MM-DD |

> **ℹ️ About this page.** The single register of Escala's business rules, functional requirements, quality attributes and use cases, plus a check that every assignment requirement (§5, §6) is covered and a light trace to stories, tests and design. It is the §9 documentation item "Requirements and quality attributes"; the team reads it when refining the backlog, reviewers read it to check coverage.

## How to read this page

> **ℹ️ Guidance.** Keep this legend short and change it only when the team changes a convention. IDs are stable: never renumber, never reuse a deleted ID.

- **IDs** — prefix + two digits:
  - `RN` — business rule: a domain constraint from statute, regulation, the corps' internal regulation or a product decision (e.g. RN02).
  - `RF` — functional requirement, read as "The system shall…" (e.g. RF24).
  - `RNF` — quality attribute (non-functional requirement), always with a measurable target (e.g. RNF04).
  - `UC` — use case: an actor's goal told as steps (e.g. UC05).
  - IDs from other pages: assumptions A1–A6 (**01 · Problem and Opportunity**), findings F1… (**Research Synthesis and Decisions**), investor decisions D-01… (**Investor Decision Log**), test cases `TC-<story>-<n>`.
- **Status values (business rules)** — `confirmed`: source read and cited · `to confirm`: source known, value or article still to verify · `to obtain`: the source document (e.g. the internal regulation) is not yet in hand, a variant of *to confirm* · `assumption`: no source yet, tested like the assumptions on **01 · Problem and Opportunity**.
- **Source types** — business rules: `statute` (law, e.g. DL 241/2007) · `regulation` (national rules outside the corps) · `internal` (the corps' internal regulation) · `product` (a team decision). Functional requirements: `finding` (F-id; until research validates it, the assumption A1–A6 it rests on) · `rule` (RN-id). Requirements imposed by the assignment cite its section (e.g. §6.7).
- **Other columns** — MVP: Y = inside the MVP boundary on **MVP, Success Criteria and Ethical Engagement**. Priority: MoSCoW (Must / Should / Could / Won't this release). Epic: E1 People & certifications · E2 Roster & slots · E3 Unavailability & coverage · E4 Eligibility · E5 Risk & reinforcement (IPMA) · E6 Responsible AI · E7 Platform & operations · E8 Discovery & product · E9 Architecture, security & privacy.

## Business rules

> **ℹ️ Guidance.** List every rule the roster or the eligibility engine must enforce, with its source and status. The **Definition of Ready (DoR)** asks "Do we know all the business rules?", so a story that depends on a rule still *to confirm* or *to obtain* is not Ready. Never write a law article or value you have not read — keep "verify" until you have; configurable rules become configuration, not code (ADR 0014 on **08 · Architecture Decision Records**).

| ID | Rule | Source (statute / regulation / internal / product) | Status (confirmed / to confirm / assumption) | Configurable? |
|---|---|---|---|---|
| RN01 | Only the active roster (*quadro ativo*) can be scheduled | statute | to confirm — verify article | No |
| RN02 | An expired certification (e.g. TAS) doesn't count | regulation | to confirm — confirm TAS/TAT validity periods | No |
| RN03 | The driving licence must match the vehicle class | regulation | to confirm | No |
| RN04 | Minimum crew composition per vehicle type | internal regulation | to confirm | Yes |
| RN05 | No overlapping slots for the same person | product | confirmed | No |
| RN06 | Nobody is assigned during a declared unavailability | product | confirmed | No |
| RN07 | Minimum rest between duty periods | internal regulation | to obtain | Yes |
| RN08 | Active-roster age limit (value to verify) | statute | to confirm — verify value and article | No |
| RN09 | Annual statutory service ≥275 h, of which ≥140 h emergency/standby and ≥70 h training | statute (source: bombeiros.pt FAQ) | to confirm — verify the article in DL 241/2007 | No |
| RN10 | The fire-risk level determines the required *piquete* composition | internal regulation | to obtain | Yes |

## Functional requirements

> **ℹ️ Guidance.** One testable capability per row, completing "The system shall…". The rows below are a starter set from the first analysis: the Product Owner confirms the MVP flags and MoSCoW priorities, and the source is replaced by a finding ID once research validates it. Every MVP row must reach at least one Jira story through the *Requirement* field.

> **✏️ Example (illustrative — replace).** Good: "RF24 — The system shall block confirmation when no valid evaluation exists." It is testable: stop the worker and try to confirm. Weak: "The system shall make rosters safer." There is no behaviour to test.

| ID | The system shall… | Epic | MVP (Y/N) | Priority (MoSCoW) | Source (finding / rule) |
|---|---|---|---|---|---|
| RF01 | register a member (roster status, rank, contact) | E1 | Y | Must | RN01 |
| RF02 | record certifications with issue and expiry dates | E1 | Y | Must | RN02 |
| RF03 | flag certifications expiring soon ([TODO: days]) | E1 | Y | Should | RN02 |
| RF04 | show my statutory-hours progress | E1 | Y | Must | RN09; A6; §5.5 |
| RF05 | import members from a file | E1 | N (v1.1) | Won't | [TODO: finding] |
| RF06 | create a *piquete* for a date and period with its slots | E2 | Y | Must | RN04 |
| RF07 | define a slot (role, vehicle, required certification) | E2 | Y | Must | RN02, RN03 |
| RF08 | assign a member to a slot with an eligibility check | E2 | Y | Must | A2; RN01–RN03, RN05–RN08 |
| RF09 | view today's *piquete* (filled and open slots) | E2 | Y | Must | A1 |
| RF10 | view my week's roster on my phone | E2 | Y | Should | A3 |
| RF11 | generate a *piquete* automatically | E2 | N (v2) | Won't | [TODO: finding] |
| RF12 | declare unavailability by form | E3 | Y | Must | RN06; §5.6.2 (fallback) |
| RF13 | declare unavailability in free text, with assisted extraction and human confirmation | E3 | Y | Must | RN06; [TODO: finding] |
| RF14 | detect open slots (gap detection) | E3 | Y | Must | A1 |
| RF15 | open a cover request for a slot | E3 | Y | Must | A1 |
| RF16 | list eligible and ineligible candidates with reasons | E4 | Y | Must | A2; D-01 (open) |
| RF17 | apply to an open slot | E3 | Y | Must | A3 |
| RF18 | confirm a candidate's assignment | E3 | Y | Must | RN05, RN06 |
| RF19 | record an exceptional assignment with a reason when nobody is eligible | E3 | Y (depends on D-02) | Should | D-02 (open) |
| RF20 | notify status changes | E3 | Y | Should | A3 |
| RF21 | evaluate eligibility against the business rules and return the violated rule codes | E4 | Y | Must | RN01–RN03, RN05–RN08 |
| RF22 | give a plain-language explanation of ineligibility | E4 | Y | Should | §5.6; D-01 (open) |
| RF23 | version the eligibility engine and store the version with each evaluation | E4 | Y | Should | §6.6 (audit records) |
| RF24 | block confirmation when no valid evaluation exists | E4 | Y | Must | A2; §6.8 |
| RF25 | fetch the municipality's RCM for today and the next two days | E5 | Y | Must | RN10; A4 |
| RF26 | fetch applicable weather warnings | E5 | Y | Could | A4 |
| RF27 | compute the required composition for the day's risk level | E5 | Y | Must | RN04, RN10 |
| RF28 | alert when the planned *piquete* falls below the required composition | E5 | Y | Should | RN10; A4 |
| RF29 | show the age of risk data served from cache | E5 | Y | Must | §6.5, §6.8 |
| RF30 | show risk vs actual coverage history | E5 | N (v1.1) | Won't | [TODO: finding] |
| RF31 | extract structured unavailability from free text | E6 | Y | Must | §5.6; [TODO: finding] |
| RF32 | show extracted values in editable fields before confirmation | E6 | Y | Must | §5.6.2, §5.6.3 |
| RF33 | mark AI-extracted vs confirmed content ("read automatically") | E6 | Y | Must | §5.6.2 |
| RF34 | generate an explanation from rule codes | E6 | Y | Should | §5.6 |
| RF35 | provide a feature flag that turns AI off and keeps the form flow | E6 | Y | Must | §5.6.2 |
| RF36 | expose `/health` and `/health/ready` on both services | E7 | Y | Must | §6.7 |
| RF37 | write structured logs with an end-to-end trace ID | E7 | Y | Must | §6.7 |
| RF38 | expose metrics | E7 | Y | Must | §6.7 |
| RF39 | provide an operational dashboard | E7 | Y | Must | §6.7 |
| RF40 | support mocked roles: firefighter, duty chief, commander | E9 | Y | Must | §6.6 |

## Quality attributes (non-functional requirements)

> **ℹ️ Guidance.** Name the ISO/IEC 25010 characteristic and state the edition you use (*safety* is a characteristic only in the 2023 edition — verify). Each row needs a number you can measure and the way you will verify it; "fast" or "secure" is not a target. Attributes the review demo must show (RNF03, RNF04) need both a test and a scripted demo step.

> **✏️ Example (illustrative — replace).** Weak: "The eligibility check must be fast." Good: "RNF01 — eligibility evaluation p95 < 5 s, read from the operational dashboard, where it is also an SLI."

| ID | Quality characteristic (ISO/IEC 25010) | Requirement | Measurable target | How verified |
|---|---|---|---|---|
| RNF01 | Performance efficiency | Eligibility evaluation returns in time for the duty chief to act | p95 < 5 s | Dashboard |
| RNF02 | Performance efficiency | The API accepts an unavailability without waiting for evaluation | 202 in < 500 ms | Test |
| RNF03 | Reliability | Works with IPMA unavailable; data age shown | Last RCM value shown with its age; beyond [TODO: threshold], the higher risk level is assumed | Demo + test |
| RNF04 | Safety | Never confirms without a valid evaluation | 0 confirmations without a valid evaluation | Test |
| RNF05 | Reliability | Duplicate events produce no duplicate effects | The same event delivered twice produces one effect | Test |
| RNF06 | Usability | PWA installable; roster readable offline | Installs from the browser; last-loaded roster readable with no network | Accessibility review |
| RNF07 | Security | No secrets in the repository | 0 secrets found by the CI scan | CI check |
| RNF08 | Maintainability | Rollback to the previous version with one documented command | 1 command, documented in the runbook | Runbook |
| RNF09 | Security/privacy | Free text deleted after confirmation | 0 free-text messages stored after confirmation | Test |
| RNF10 | Usability | Accessibility (contrast, focus, keyboard navigation) | [TODO: target, e.g. a WCAG 2.x level] | Sprint 3 review |

## Use cases

> **ℹ️ Guidance.** One row per user goal, named verb + object. Write the main success scenario as 3–6 numbered steps, alternating actor and system, and list extensions as "step + condition → behaviour", including the degraded modes. UC01 and UC05 are filled as examples; complete the others the same way.

| ID | Use case | Primary actor | Trigger | Main success scenario (3–6 steps) | Extensions |
|---|---|---|---|---|---|
| UC01 | Declare unavailability | Firefighter | The firefighter knows they cannot attend a period (e.g. writes "este fds não posso") | 1. Types the message or opens the form. 2. System extracts type, start and end and shows them in editable fields marked "read automatically". 3. Firefighter corrects them if needed and confirms. 4. System records the unavailability (RN06) and deletes the free text (RNF09). 5. Slots already assigned in that period become gaps (UC02). | 2a. AI unavailable → the form is shown instead (RF35). 2b. The text is not an unavailability → the system says so; nothing is stored. 3a. Firefighter cancels → nothing is stored. |
| UC02 | Request cover for a gap | Duty chief | A slot becomes open (RF14) | [TODO: 3–6 steps] | [TODO: e.g. nobody eligible → per D-02] |
| UC03 | Apply to a slot | Firefighter | A cover request reaches the firefighter (RF20) | [TODO: 3–6 steps] | [TODO: extensions] |
| UC04 | Evaluate eligibility | Duty chief (the system evaluates) | Candidates for a slot are requested (event `AvaliacaoPedida`, evaluation requested) | [TODO: 3–6 steps] | [TODO: e.g. worker down → evaluation "pending"] |
| UC05 | Confirm an assignment | Duty chief | A candidate has applied to an open slot (UC03) | 1. Opens the slot and sees the candidates with their evaluation result and reasons (RF16). 2. Selects an eligible candidate. 3. System checks that a valid, current evaluation exists (RF24). 4. System confirms, notifies the firefighter (RF20) and counts the hours (RN09). 5. System stores an audit record. | 2a. Nobody is eligible → behaviour per D-02 (RF19). 3a. Worker down → evaluation "pending"; confirmation blocked with a message. 3b. Evaluation outdated (e.g. a certification expired since) → re-evaluate first. |
| UC06 | Check my statutory hours | Firefighter | The firefighter wants to know how many hours they still owe this year | [TODO: 3–6 steps] | [TODO: extensions] |
| UC07 | Reinforce the *piquete* on a high-risk day | Duty chief | The day's RCM requires a composition the planned *piquete* does not meet (RF28) | [TODO: 3–6 steps] | [TODO: e.g. IPMA down → last value with its age; beyond the threshold, higher risk assumed] |

## Coverage of the assignment requirements

> **ℹ️ Guidance.** One row per assignment requirement, §5.1–§5.7 and §6.1–§6.8; reviewers use this table to check that nothing is missing. Say concretely how Escala satisfies each one and point to the page or Jira issue that holds the evidence. If a requirement is not yet satisfied, say so and name the sprint that will close it.

| Assignment § | Requirement | How Escala satisfies it | Where (page / Jira) |
|---|---|---|---|
| §5.1 | Problem Discovery: target community, problem, stakeholders, existing practices, consequences of leaving it unresolved, principal assumptions | The active roster of a *Corpo de Bombeiros Voluntários* ([TODO: which corps]); the *piquete* is composed by phone and WhatsApp, from memory; assumptions A1–A6 | **01 · Problem and Opportunity**; epic E8 |
| §5.2 | Market and Competitor Analysis: competing products and non-digital alternatives; a clear value proposition | Planned comparison: Ifprotec, GESCORP, SIGO, D4H, Aladtec and whiteboard + WhatsApp + phone [TODO: done / date]; Escala competes on roster quality, not roster management | **02 · Market and Competitor Analysis**; epic E8 |
| §5.3 | User and Stakeholder Research: validate important assumptions; document access limitations; never fabricate evidence | Planned: interviews and observation with firefighters, duty chiefs and the commander, identified by codes only; [TODO: sessions held and limitations documented] | **03 · User Research**; **Research Synthesis and Decisions** |
| §5.4 | Product Vision and Success Criteria: vision, target users, value proposition, journeys, community benefit, MVP boundary, risks, measurable success criteria | Primary user: the duty chief; outcome metrics: median time from gap detection to a verified-qualified fill, and % of days the *piquete* met the required composition | **04 · Product Vision**; **MVP, Success Criteria and Ethical Engagement** |
| §5.5 | Ethical Engagement Design: at least one mechanism for constructive participation, with the five questions answered | Individual statutory-hours progress, visible to the person and the command, never compared between peers; no leaderboard (RF04) | **MVP, Success Criteria and Ethical Engagement** |
| §5.6 (§5.6.1–§5.6.3) | Responsible AI: a meaningful AI-assisted workflow, with evaluation, baseline or fallback, and safety and trust | Extraction of structured unavailability from free text with human confirmation (RF31–RF33); `dateparser` + rules baseline; form fallback behind a feature flag (RF35) | **06 · Responsible AI Opportunity Assessment**; epic E6 |
| §5.7 | Investor Engagement: at least one decision revised after investor feedback, with change and rationale documented | Open decisions (D-01, D-02) taken to each review; feedback recorded as Jira *Feedback* issues | **Investor Decision Log**; [TODO: SCRUM-xx] |
| §6.1 | Client Application: supports the principal journeys; accessibility and usability | Angular PWA, installable, roster readable offline (RNF06, RNF10) | **07 · Technical Design** |
| §6.2 | Backend Architecture: at least two independently deployable backend components, justified | `escala-api` (Spring Boot) and `elegibilidade-worker` (Python), one PostgreSQL each | **07 · Technical Design**; **08 · Architecture Decision Records** (ADR 0002) |
| §6.3 | Asynchronous Workflow: retries, duplicate events, failures, eventual consistency | Eligibility evaluation over RabbitMQ (`AvaliacaoPedida` → `AvaliacaoConcluida`) with outbox, idempotency keys, retries and a dead-letter queue (RNF05) | **07 · Technical Design** |
| §6.4 | Data Management: persistent storage; ownership, consistency, lifecycle, retention and deletion, privacy | One PostgreSQL per component, each owning its data; free text deleted after confirmation (RNF09) | **07 · Technical Design**; **09 · Security and Privacy Assessment** |
| §6.5 | External Integration: external system or realistic simulator; unavailable, delayed, incomplete or invalid data handled | IPMA open data (RCM per municipality, weather warnings), hourly polling with cache; IPMA simulator container (RF25, RF26, RF29) | **07 · Technical Design**; epic E5 |
| §6.6 | Authentication and Authorization: identity and access controls (may be mocked for the MVP, §6.6 footnote) | Mocked authentication with real roles: firefighter, duty chief, commander (RF40) | **09 · Security and Privacy Assessment**; **08 · Architecture Decision Records** (ADR 0006) |
| §6.7 | Observability: structured logs, health checks, metrics, trace IDs, error reporting, dashboard, SLIs | `/health` and `/health/ready`, structured logs with a trace ID, Prometheus and Grafana (RF36–RF39); SLIs: evaluation p95 latency, % of requests evaluated without manual intervention | **07 · Technical Design**; epic E7 |
| §6.8 | Resilience and Degraded Operation: at least one degraded mode; fail safely and communicate limitations | IPMA down → last value with its age, then the higher risk level; AI down → form fallback; worker down → evaluations "pending", confirmation blocked (RNF03, RNF04) | **07 · Technical Design**; **Deployment and Operations** (`docs/operations/deployment-and-operations.md`) |

## Traceability

> **ℹ️ Guidance.** Keep it light: one row per MVP requirement with its stories, test cases and design section. Jira links are the source of truth — fill the *Requirement* field on each story and embed the macro below rather than maintaining a long table by hand.

[JIRA: project = SCRUM AND Requirement is not EMPTY ORDER BY Requirement ASC]

| Requirement | Stories (Jira) | Test cases | Design section |
|---|---|---|---|
| [TODO: RF/RNF ID] | [TODO: SCRUM-xx] | [TODO: TC-SCRUM-xx-01] | [TODO: page — section] |

> **✏️ Example (illustrative — replace).**
> | Requirement | Stories (Jira) | Test cases | Design section |
> |---|---|---|---|
> | RF08, RN02 | SCRUM-999 | TC-SCRUM-999-01, TC-SCRUM-999-02 | **07 · Technical Design** — Components and data ownership |
> | RF06 | SCRUM-45 | TC-SCRUM-45-01 | **10 · Walking Skeleton** |
> | RNF05 | SCRUM-xx | TC-SCRUM-xx-01 | **07 · Technical Design** — Reliability of messaging |

## Done when
- [ ] Every business rule has a source type and a status, and none is marked `confirmed` without its source read and cited
- [ ] Every RF has an epic, MVP flag, priority and source; every RNF has a measurable target and a verification method; UC01–UC07 have a main success scenario and extensions
- [ ] All 15 coverage rows (§5.1–§5.7, §6.1–§6.8) point to a page or Jira issue
- [ ] Every MVP requirement is linked to at least one Jira story
- [ ] Reviewed by another team member
- [ ] Guidance and example blocks removed
- [ ] Status set to Done
