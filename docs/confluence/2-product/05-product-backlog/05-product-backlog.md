# 05 · Product Backlog

| Field | Value |
|---|---|
| Status | Draft |
| Owner | [TODO: name] |
| Contributors | [TODO: names] |
| Deliverable | Sprint 1 · #5 Product Backlog |
| Sources | Assignment §7.1.2 #5, §8.2; Deck M5–M7 |
| Confluence parent | 2 · Product |
| Labels | `sprint-1`, `deliverable-05`, `product`, `backlog` |
| Jira | [TODO: ESC-xx] |
| Last updated | YYYY-MM-DD |

> **ℹ️ About this page.** Deliverable #5 (§7.1.2): the Product Goal, the rules for writing, slicing, ordering and estimating backlog items, and the sprint goals; the items themselves live in Jira (§8.2). It covers the four required items — *Prioritized user stories or backlog items* (Live backlog), *Acceptance criteria* (Story format, Example story), *Sprint objectives* (Sprint goals) and *Initial release plan* (Release plan) — and is read by the team, the Product Owner and the investors.

> **ℹ️ Guidance — this page already exists.** In Confluence this page exists as *Product Backlog – Template*. Merge these sections into it and rename it to **05 · Product Backlog**; do not create a second page. Page history is kept on rename.

## Product Goal

[TODO: Product Goal in one sentence]

> **ℹ️ Guidance.** One sentence stating the outcome the whole backlog serves; every item should move the product towards it. The Product Owner owns it, and it must agree with **04 · Product Vision**. It changes only when the product direction changes, not every sprint.

> **✏️ Example (illustrative — replace).** "Every day, the duty chief knows whether the *piquete* can crew what today's fire risk requires, and fills any gap with a verified-qualified person."

## Live backlog

[JIRA: project = ESC AND issuetype in (Story, Task, Spike, Bug) AND statusCategory != Done ORDER BY Rank ASC]

> **ℹ️ Guidance.** Never copy stories into this page; embed the Jira macro so the order shown is always the order in Jira. The Product Owner orders the backlog by Rank, and only items that pass **Definition of Ready (DoR)** move to *Ready*.

## Epics

| Epic | Scope | Requirement IDs |
|---|---|---|
| E1 People & certifications | Members of the active roster (*quadro ativo*), certifications with expiry dates, statutory-hours progress | RF01–RF05 |
| E2 Roster & slots | *Piquete* per date and period; slots with role, vehicle and required certification; my roster on my phone | RF06–RF11, RNF06, RNF10 |
| E3 Unavailability & coverage | Declaring unavailability, gap detection, cover requests, applications, confirmation, notifications | RF12–RF15, RF17–RF20, RNF02 |
| E4 Eligibility | Deterministic rule engine; eligible and ineligible candidates with reasons; confirmation blocked without a valid evaluation | RF16, RF21–RF24, RNF04 |
| E5 Risk & reinforcement (IPMA) | Fire risk (RCM) and weather warnings, required composition per risk level, reinforcement alerts, data age | RF25–RF30, RNF03 |
| E6 Responsible AI | Free-text extraction with human confirmation, explanations from rule codes, AI feature flag and fallback, AI evaluation | RF31–RF35 |
| E7 Platform & operations | Health checks, logs, metrics, dashboard, CI/CD, deployment and rollback, messaging reliability | RF36–RF39, RNF01, RNF05, RNF08 |
| E8 Discovery & product | Research, market analysis, vision, backlog and roadmap (deliverables 01–05) | — (documentation items) |
| E9 Architecture, security & privacy | Technical design, ADRs, threat model, mocked roles, data minimisation (deliverables 07–09) | RF40, RNF07, RNF09 |

> **ℹ️ Guidance.** The rows are the nine Jira epics. The scope and IDs above are a proposed starting mapping from **Requirements and Quality Attributes**: confirm them, make sure every RF and RNF belongs to exactly one epic, and keep this table in sync with Jira.

## Story format

```
ESC-xx · <short title: verb + object>
As a <persona>, I want <capability>, so that <benefit>.
Business rules: RNxx          Requirements: RFxx / RNFxx
Acceptance criteria:
  AC1  Given <context>, when <action>, then <observable result>.
  AC2  (boundary) Given <edge context>, when <action>, then <result>.
Test cases: TC-ESC-xx-01, TC-ESC-xx-02
```

> **ℹ️ Guidance.** Write every story as *"As a <persona>, I want <capability>, so that <benefit>."*, using a persona or role from **Personas**. Acceptance criteria use Given/When/Then and include at least one boundary case; each criterion maps to a test case ID `TC-<story>-<n>`. In Jira, put RN IDs in the *Business rule* field and RF/RNF IDs in the *Requirement* field.

## INVEST checklist

- [ ] **Independent** — "Stories don't depend on one another"
- [ ] **Negotiable** — "Details are open to discussion"
- [ ] **Valuable** — "Customer value + team learning value"
- [ ] **Estimable** — "The team can size it"
- [ ] **Small** — "Feel progress during the sprint"
- [ ] **Testable** — "Clear acceptance criteria"

> **ℹ️ Guidance.** Deck M5: "Give special attention to 'Small' so the team can feel progress during the sprint." Run this check during refinement; a story that fails it cannot pass **Definition of Ready (DoR)** and must be rewritten or split.

## Slicing

> **ℹ️ Guidance.** Slice vertically: each story goes through UI, logic and persistence and can be demonstrated on its own. Never slice by horizontal layer (database, then API, then screen). Useful splits: one business rule per story; happy path first, exceptions next; form first, free text later (RF12 before RF13).

> **✏️ Example (illustrative — replace).** Horizontal (don't): "Create the slots table" → "Create the slots API" → "Create the slots screen"; nothing works for a user until all three are done. Vertical (do): ESC-23 "Create a *piquete* with one slot" (PWA → API → DB, no rules) → "Add the required certification to a slot" (RF07) → ESC-31 "Reject assigning someone whose TAS has expired" (RN02). Each slice runs end to end.

## Prioritisation

The Product Owner orders the backlog using the four factors of deck M6:

1. **Value** — "Is the item necessary for bringing the product to life? Could the product still succeed without it?"
2. **Knowledge, uncertainty and risk** — "Risky, uncertain items go first — a risk-driven approach surfaces early failure."
3. **Releasability** — "Each release should provide useful functionality and generate feedback."
4. **Dependencies** — "Resolve them where possible; combine or re-split dependent stories."

### Current risk items that go first

[JIRA: project = ESC AND labels = risk AND statusCategory != Done ORDER BY Rank ASC]

> **ℹ️ Guidance.** List the items that carry the most uncertainty and give the reason each goes first. Tag them in Jira with the `risk` label so the macro above stays current.

> **✏️ Example (illustrative — replace).**
> - ESC-23 walking skeleton: the chain PWA → API → queue → worker → DB runs deployed (technical risk).
> - Spike: where the AI model runs, local model vs hosted API (ADR 0011, pending the spike).
> - IPMA RCM integration against the simulator, including unavailable, delayed, incomplete and invalid data (tests assumption A4).
> - Obtain the corps' internal regulation for minimum rest (RN07) and risk → composition (RN10) (tests assumption A5).

## Estimation

| Date | Items estimated | Participants |
|---|---|---|
| [TODO: YYYY-MM-DD] | [TODO: ESC keys and points] | [TODO: builders who voted] |

> **ℹ️ Guidance.** Backlog items get story points on the Fibonacci scale 1, 2, 3, 5, 8, 13, 21 through Planning Poker; sprint tasks are estimated in hours. Deck M7: "only the people who build the increment estimate. The Product Owner and Scrum Master do not estimate, and do not influence estimates." Anything above 8 points does not fit one sprint (see **Definition of Ready (DoR)**) and must be split.

> **✏️ Example (illustrative — replace).**
> | Date | Items estimated | Participants |
> |---|---|---|
> | YYYY-MM-DD | ESC-23 (3), ESC-31 (5) | Member B, Member C, Member D (Member A, the Product Owner, did not estimate) |

## Sprint goals

| Sprint | Goal |
|---|---|
| Sprint 1 | [TODO: sprint goal] |
| Sprint 2 | [TODO: sprint goal] |
| Sprint 3 | [TODO: sprint goal] |

> **ℹ️ Guidance.** One outcome sentence per sprint, also written in the Jira sprint's goal field (§8.2 "Document sprint goals"). The Sprint 1 goal must answer that sprint's review question; the Sprint 2 and 3 goals are provisional until their planning.

> **✏️ Example (illustrative — replace).**
> - **Sprint 1:** "Prove that the problem is real at [corps], that the proposed MVP is the smallest credible experiment, and that the chain PWA → API → queue → worker → DB runs deployed."
> - **Sprint 2:** "The principal journey, from a declared unavailability to a confirmed verified-qualified replacement, works deployed, observed, and with one degraded mode demonstrated."
> - **Sprint 3:** "Harden and evaluate: AI evaluation results, degraded modes, operational dashboard, security review and a credible roadmap."

## Release plan

The initial release plan is **Product Roadmap (GO)**.

> **ℹ️ Guidance.** Keep one line per release here and the detail on **Product Roadmap (GO)**. Optionally create Jira fix versions with the same release names so each backlog item shows the release it belongs to.

> **✏️ Example (illustrative — replace).** R0 "Walking skeleton" (end of Sprint 1) · R1 "Gap to confirmed" (end of Sprint 2) · R2 "Trustworthy readiness" (end of Sprint 3) · v2 (post-project).

## Example story

> **ℹ️ Guidance.** Use this as the quality bar for every story that reaches *Ready*: story sentence, business rules, requirements, Given/When/Then with a boundary case, test case IDs, estimate and cleared dependencies.

> **✏️ Example (illustrative — replace).**
> **ESC-31 · Reject assigning someone whose TAS has expired** — Epic E4 Eligibility · 5 points · Business rules RN01, RN02 · Requirements RF08, RF21, RNF04
>
> *"As a duty chief, I want the system to reject assigning someone whose TAS certification has expired, so that no ambulance leaves with an unqualified crew."*
>
> **Acceptance criteria**
> - **AC1 — expired.** Given Member B's TAS expired before the slot date, when the duty chief tries to assign Member B, then the assignment is rejected and the reason shows rule code RN02. → `TC-ESC-31-01`
> - **AC2 — boundary.** Given Member C's TAS expires on the slot date, when the duty chief assigns Member C to that slot, then the assignment is rejected with RN02 (illustrative reading: a certification does not count on its expiry date). [TODO: confirm with the commander how the expiry date is interpreted] → `TC-ESC-31-02`
> - **AC3 — valid.** Given Member A is on the *quadro ativo* (RN01) and holds a TAS valid beyond the slot, when the duty chief assigns Member A to an ambulance slot that requires TAS, then the assignment is accepted and goes to confirmation. → `TC-ESC-31-03`
>
> **Dependencies:** RF02 (certifications with expiry dates, E1) and RF07 (slot with a required certification, E2) are done. **DoR:** passed on YYYY-MM-DD.

## Done when
- [ ] Product Goal written and agreed by the Product Owner
- [ ] Live backlog ordered by Rank in Jira; the items planned for the next sprint pass **Definition of Ready (DoR)**, with acceptance criteria and test case IDs
- [ ] Epics table and sprint goals match Jira
- [ ] Release plan points to a filled **Product Roadmap (GO)**
- [ ] Reviewed by another team member
- [ ] Guidance and example blocks removed
- [ ] Status set to Done
