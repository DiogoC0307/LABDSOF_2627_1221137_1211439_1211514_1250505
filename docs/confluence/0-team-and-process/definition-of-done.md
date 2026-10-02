# Definition of Done (DoD)

| Field | Value |
|---|---|
| Status | Draft |
| Owner | [TODO: name] |
| Contributors | [TODO: names] |
| Deliverable | — |
| Sources | Deck (Definition of Done; Software engineering process); Assignment §8, §11 |
| Confluence parent | 0 · Team and Process |
| Labels | `team-process`, `scrum` |
| Jira | [TODO: ESC-xx] |
| Last updated | YYYY-MM-DD |

> **ℹ️ About this page.** The two checklists an item must meet before it moves to *Done* in Jira: one for code, one for documentation and research. Every member reads it; the PR reviewer and whoever closes an issue apply it, so each closed issue is evidence that the process in Assignment §8 and §11 was followed.

> **ℹ️ Guidance — this page already exists.** In Confluence this page exists as **Definition of Done (DoD)**. Merge these sections into it instead of creating a new page; keep its history.

## Purpose

*"Can we close this issue?"* Every closed item must comply — otherwise it was wrongly closed and must be reopened (deck).

- In Jira, *Done* requires the DoD, just as *Ready* requires the **Definition of Ready (DoR)** (see **Scrum Methodology**).
- The DoD is the commitment behind the Increment: only items that meet it count as delivered in the Sprint Review.
- Anyone who finds an item closed without meeting the DoD reopens it and comments which item was missing.

> **ℹ️ Guidance.** Choose the list by what the item produces: code (a pull request) or documentation and research (a Confluence page). An item that produces both, e.g. a story that also changes **07 · Technical Design**, meets both lists. Change the lists only by team agreement, recorded in a retrospective.

## DoD for code items

- [ ] Analysis and design updated
- [ ] Code committed with a proper message referencing the Jira key
- [ ] Unit tests developed and passing
- [ ] Functional tests performed and recorded
- [ ] Acceptance criteria verified
- [ ] PR reviewed and approved by another member
- [ ] CI green (build, tests, static analysis, dependency check)
- [ ] ADR or docs updated if a decision changed
- [ ] No secrets in code
- [ ] Demonstrable without hidden manual steps

> **ℹ️ Guidance.** The first four items are the deck's DoD; the others come from Assignment §8 (pull requests, code reviews, a CI pipeline with static analysis and dependency checks) and §11: "Security controls must not be disabled solely to simplify the demonstration." The PR template on **Git, Pull Request and Jira Rules** repeats this list word for word, so change both together. Record functional tests by their `TC-` IDs as described in **Test Strategy** (`docs/testing/test-strategy.md`).

> **✏️ Example (illustrative — replace).** Closing comment on ESC-31 ("reject assigning someone whose TAS has expired"):
> - Analysis and design: RN02, RF08 and RF21 linked in Jira; no design change needed, stated in the PR
> - Commit: `feat(eligibility): reject expired TAS certification (ESC-31)`
> - Unit tests: `TC-ESC-31-01` to `-03` passing in CI run [TODO: link]
> - Functional test: `TC-ESC-31-02` (TAS expiring on the slot date) run on the deployed environment; result recorded as a Jira comment
> - Acceptance criteria verified; PR [TODO: #] approved by Member B; CI green; no ADR change; no secrets; demo needs no manual steps

## DoD for documentation and research items

- [ ] Required sections filled
- [ ] Sources cited
- [ ] Reviewed by another member
- [ ] Guidance and example blocks removed
- [ ] AI-assisted content reviewed by a human (§11)
- [ ] Status set to Done
- [ ] Linked from the sprint deliverables page

> **ℹ️ Guidance.** "Required sections" means every H2 of the page's template plus its own "Done when" list; research pages also need participant codes, recorded consent and no invented answers (§11: "Research results must not be fabricated or misrepresented."). Record AI assistance and its human reviewer on **AI Usage and Third-Party Attribution** (§11: "AI-generated content must be reviewed before being treated as authoritative."). The sprint deliverables page is **Sprint 1 · Deliverables**, **Sprint 2 · Deliverables** or **Sprint 3 · Deliverables**.

> **✏️ Example (illustrative — replace).** Closing comment on ESC-xx (**01 · Problem and Opportunity**): all template sections and its "Done when" items met; every pain claim sourced or listed as an assumption; reviewed by Member C; AI-assisted wording recorded on **AI Usage and Third-Party Attribution**; guidance and examples removed; Status Done; linked from **Sprint 1 · Deliverables**.

## Why two lists

The deck: "Keep in mind that every 'thing' is part of a software project and must follow the software engineering process — even if it is not 'exactly' programming in 'Java'." It also asks the team to "Provide evidences that the process has been followed." A Confluence deliverable therefore follows the same steps as code, in its own form:

| Step (deck) | Code item | Documentation or research item |
|---|---|---|
| Analysis | Requirement and business rules linked | Sources and research questions identified |
| Design | Design or ADR updated | Page structure from the template |
| Test plan | `TC-` IDs outlined (DoR item 5) | "Done when" criteria known |
| Code | Commit and pull request | Page written |
| Test execution | Unit and functional tests recorded | Peer review by another member |

> **ℹ️ Guidance.** Keep this section short. If the team adds a third kind of item (e.g. a spike), say which list it follows here instead of creating a third list.

## Done when
- [ ] The whole team has agreed both lists (recorded in meeting minutes)
- [ ] The PR template in the repository (`.github/pull_request_template.md`) repeats "DoD for code items" exactly
- [ ] Merged into the existing Confluence page; no duplicate page created
- [ ] Reviewed by another team member
- [ ] Guidance and example blocks removed
- [ ] Status set to Done
