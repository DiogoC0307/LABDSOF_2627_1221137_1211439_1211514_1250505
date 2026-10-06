# Sprint 1 · Deliverables

| Field | Value |
|---|---|
| Status | Draft |
| Owner | [TODO: name] |
| Contributors | [TODO: names] |
| Deliverable | — |
| Sources | Assignment §7.1 (§7.1.1–§7.1.3); §5.5, §5.7 (cross-cutting) |
| Confluence parent | Sprint 1 |
| Labels | `sprint-1`, `deliverables` |
| Jira | [TODO: SCRUM-xx] |
| Last updated | YYYY-MM-DD |

> **ℹ️ About this page.** The audit page for Sprint 1 (Assignment §7.1). It has one row for each of the ten §7.1.2 deliverables, showing the page that holds it, its owner and the proof that it is done. The team uses it to track the sprint, and the investors use it to check the sprint at the review.

## Objective

*"The objective of Sprint 1 is to establish that the team is addressing a meaningful problem and has proposed a credible product and technical direction."* — Assignment §7.1.1

> **ℹ️ Guidance.** Keep the quote as it is. The team's own sprint goal goes on **Sprint 1 · Planning** and in Jira, not here.

## Deliverables

| # | Deliverable | Required content | Page | Jira | Owner | Proof of done | Status |
|---|---|---|---|---|---|---|---|
| 1 | Problem and Opportunity Report | Target community; Problem statement; Stakeholder map; Problem evidence; Assumptions and risks | **01 · Problem and Opportunity** | [TODO: SCRUM-xx] | [TODO: name] | Community named; every pain claim sourced or listed as an assumption; every assumption has a status | Draft |
| 2 | Market and Competitor Analysis | Relevant alternatives; Strengths and weaknesses; Opportunity for differentiation | **02 · Market and Competitor Analysis** | [TODO: SCRUM-xx] | [TODO: name] | ≥ 6 alternatives, including the informal one; every cell verified or marked ❓/dated; a one-sentence value proposition | Draft |
| 3 | User Research | Research method; Findings; Limitations; Resulting product decisions | **03 · User Research** | [TODO: SCRUM-xx] | [TODO: name] | Consent recorded for every session; limitations documented; ≥ 1 product decision changed by research on **Research Synthesis and Decisions** | Draft |
| 4 | Product Vision | Personas; Value proposition; Principal user journeys; Success criteria; MVP definition | **04 · Product Vision** | [TODO: SCRUM-xx] | [TODO: name] | **Personas**, **User Journeys** and **MVP, Success Criteria and Ethical Engagement** at Done | Draft |
| 5 | Product Backlog | Prioritized user stories or backlog items; Acceptance criteria; Sprint objectives; Initial release plan | **05 · Product Backlog** | [TODO: SCRUM-xx] | [TODO: name] | Jira backlog ordered by rank; Sprint 2 items meet the DoR; sprint goals written; **Product Roadmap (GO)** filled for R0–R2 | Draft |
| 6 | Responsible AI Opportunity Assessment | Proposed AI-assisted workflow; Expected value; Risks; Evaluation approach; Baseline or fallback strategy | **06 · Responsible AI Opportunity Assessment** | [TODO: SCRUM-xx] | [TODO: name] | Numeric acceptance criteria written before measuring; baseline and fallback defined; all eight §5.6.3 risks addressed | Draft |
| 7 | Technical Design | System context; Container or component view; Data model; API outline; Asynchronous workflow; External integration; Deployment view | **07 · Technical Design** | [TODO: SCRUM-xx] | [TODO: name] | All seven views on the page; full version in Git: **Technical Design** (`docs/architecture/technical-design.md`) | Draft |
| 8 | Architecture Decision Records | Principal architectural and technological choices; Alternatives considered; Decision rationale; Consequences and trade-offs | **08 · Architecture Decision Records** | [TODO: SCRUM-xx] | [TODO: name] | One ADR per decision in `docs/adr/`, each with ≥ 1 rejected alternative; the index matches the repository | Draft |
| 9 | Security and Privacy Assessment | Initial threat model; Sensitive data; Access-control requirements; Data-minimization measures; Retention considerations | **09 · Security and Privacy Assessment** | [TODO: SCRUM-xx] | [TODO: name] | STRIDE table, sensitive data inventory, access-control matrix and retention rules filled | Draft |
| 10 | Walking Skeleton | Minimal end-to-end implementation; Source repository; Automated build; Initial test; Containerized deployment | **10 · Walking Skeleton** | [TODO: SCRUM-xx] | [TODO: name] | Last green pipeline run linked; deployed URL; runs from a clean clone with one command | Draft |

> **ℹ️ Guidance.** Do not reword the *Required content* cells. They quote Assignment §7.1.2, and reviewers check them against it. For each row, fill in the Jira issue that tracks the deliverable (label `deliverable-s1`) and one accountable owner. The *Proof of done* cells hold suggested criteria: when a criterion is met, replace it with a link to the evidence (a page at Done, the last green run or the deployed URL). *Status* follows the linked page: `Draft` → `In review` → `Done`.

> **✏️ Example (illustrative — replace).** Row 10 when it is complete: *Jira* `SCRUM-45` · *Owner* Member C · *Proof of done* "Last green run: [TODO: link to the last green run] · Deployed: [TODO: deployed URL] · clean-clone run recorded on **10 · Walking Skeleton**" · *Status* `Done`.

## Cross-cutting items

- **Ethical engagement (§5.5)** is documented on **MVP, Success Criteria and Ethical Engagement**, which answers the five §5.5 questions. Status: [TODO: Draft / In review / Done]
- **Investor engagement (§5.7)** starts at this review. The open decisions are on **Investor Decision Log**. Status: [TODO: Draft / In review / Done]

> **ℹ️ Guidance.** These two items are not numbered deliverables, but they are assessed from Sprint 1 onwards. §5.7 requires: *"At least one product or technical decision must be revised in response to investor feedback. The change and its rationale must be documented."*

## Review question

*"Is this a valuable problem, and is the proposed MVP the smallest credible product experiment?"* — Assignment §7.1.3

> **ℹ️ Guidance.** Every row above should help answer this question. The review itself is planned and recorded on **Sprint 1 · Review**.

## Bring to the review

- [ ] Repository: [TODO: GitHub URL]
- [ ] Green pipeline: [TODO: link to the last green run]
- [ ] Skeleton running from a clean clone
- [ ] Jira with Sprint 2 planned
- [ ] One physical piece of field evidence (anonymised)
- [ ] Two open decisions, on **Investor Decision Log**

> **ℹ️ Guidance.** Work through this list the day before the review and again one hour before it. Field evidence must come from real research and be anonymised. If you have none, say so; do not produce a substitute.

> **✏️ Example (illustrative — replace). Fictional example to show the format. Never copy into real notes.** Field evidence: a roster photo from OBS-01, taken only with permission, with names removed before it left the premises. Open decisions: D-01 (does the duty chief see ineligible candidates with the reason, or only eligible ones?) and D-02 (when nobody is eligible, is the assignment blocked, or is a recorded exceptional assignment allowed?).

## Done when
- [ ] Every row has a page, a Jira issue and an owner
- [ ] Every linked page is at Done, and its *Proof of done* cell links to the evidence
- [ ] The "Bring to the review" checklist is complete before the review
- [ ] Reviewed by another team member
- [ ] Guidance and example blocks removed
- [ ] Status set to Done
