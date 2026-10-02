# Sprint 3 · Deliverables

| Field | Value |
|---|---|
| Status | Draft |
| Owner | [TODO: name] |
| Contributors | [TODO: names] |
| Deliverable | — |
| Sources | Assignment §7.3 (§7.3.1, §7.3.2, §7.3.3), §12, §13 |
| Confluence parent | Sprint 3 |
| Labels | `sprint-3`, `deliverables` |
| Jira | [TODO: ESC-xx] |
| Last updated | YYYY-MM-DD |

> **ℹ️ About this page.** Tracks the sixteen deliverables that assignment §7.3.2 requires for Sprint 3, the final submission (§12) and the closing challenge (§13). The team uses it to close the project, and the investors use it at the final review.

## Objective

> "The objective of Sprint 3 is to refine the MVP, evaluate its most important qualities, and demonstrate that it is a credible and maintainable product." — Assignment §7.3.1

**Sprint goal:** [TODO: one sentence]

> **ℹ️ Guidance.** Keep the assignment quote as it is. Below it, write the team's Sprint 3 goal in one sentence, consistent with the Sprint goals table on **05 · Product Backlog** and with R2 on **Product Roadmap (GO)**.

> **✏️ Example (illustrative — replace).** Sprint goal: "Harden and evaluate: AI evaluation results, degraded modes, operational dashboard, security review and a credible roadmap."

## Deliverables

> **ℹ️ Guidance.** There is one row per §7.3.2 item. The *Deliverable* column uses the assignment's exact wording, so do not edit it. *Required content* says what counts as evidence for Escala; fill *Page*, *Jira* and *Owner* at Sprint Planning, and put the proof link in *Proof of done* when the row is finished. For row 16, remember §8.1: "Raw commit counts will not be treated as reliable evidence of contribution."

| # | Deliverable | Required content | Page | Jira | Owner | Proof of done | Status |
|---|---|---|---|---|---|---|---|
| 1 | Refined MVP incorporating stakeholder feedback | Changes made after the Sprint 2 review, each traced to a Jira *Feedback* issue; at least one decision revised on **Investor Decision Log** (§5.7) | [TODO: page] | [TODO: ESC-xx] | [TODO: name] | Feedback issues closed with the change linked; the revised decision documented | Draft |
| 2 | Completed principal user journeys | J1 (unavailability → confirmed replacement) complete; J2 (fire-risk-driven reinforcement) and J3 (checking my statutory hours) as scoped on **User Journeys** | [TODO: page] | [TODO: ESC-xx] | [TODO: name] | End-to-end tests green; journeys shown live on the release | Draft |
| 3 | AI evaluation results | Extraction measured on the 60-case dataset (35 normal / 15 difficult / 10 adverse) against the acceptance criteria written before measuring, and compared with the `dateparser` + rules baseline; latency and cost per message recorded | [TODO: page] | [TODO: ESC-xx] | [TODO: name] | AI evaluation report (§12 item 8), linked from **06 · Responsible AI Opportunity Assessment** | Draft |
| 4 | Usability evaluation or testing plan | A usability test of the principal journey with real users (role codes only, consent recorded), or a documented plan that states the access limitation; results are never fabricated (§11) | [TODO: page] | [TODO: ESC-xx] | [TODO: name] | Results or plan page; findings turned into Jira items | Draft |
| 5 | Accessibility review | PWA contrast, focus and keyboard navigation reviewed (RNF10) | [TODO: page] | [TODO: ESC-xx] | [TODO: name] | Review checklist with each issue fixed or ticketed | Draft |
| 6 | Security review and updated threat model | STRIDE table on **09 · Security and Privacy Assessment** updated for what was built; access control tested per role; no secrets in the repository (RNF07); dependency check green | [TODO: page] | [TODO: ESC-xx] | [TODO: name] | Updated page version; open risks listed | Draft |
| 7 | Performance and scalability evidence | Eligibility evaluation p95 < 5 s (RNF01); API accepts an unavailability in < 500 ms (RNF02); [TODO: load scenario and scaling approach] | [TODO: page] | [TODO: ESC-xx] | [TODO: name] | Load-test report or dated dashboard screenshot | Draft |
| 8 | Resilience and degraded-mode demonstration | All three degraded modes. IPMA down: the last value with its age, then the higher risk level beyond the threshold. AI down: form fallback. Worker down: evaluations "pending", confirmation blocked. Duplicate events have no duplicate effects (RNF05) | [TODO: page] | [TODO: ESC-xx] | [TODO: name] | Scripted demo from the runbook in **Deployment and Operations** (`docs/operations/deployment-and-operations.md`); resilience tests in CI | Draft |
| 9 | Operational dashboard | Grafana dashboard with the two SLIs, eligibility evaluation p95 latency and % of requests evaluated without manual intervention (RF39) | [TODO: page] | [TODO: ESC-xx] | [TODO: name] | Dashboard URL and a dated screenshot | Draft |
| 10 | Updated architecture views and decision records | The C4 views on **07 · Technical Design** and in the Git **Technical Design** (`docs/architecture/technical-design.md`) match the deployed system; ADRs added or superseded | [TODO: page] | [TODO: ESC-xx] | [TODO: name] | Page versions; ADR index on **08 · Architecture Decision Records** | Draft |
| 11 | Architecture evolution narrative | How the architecture moved from the Sprint 1 walking skeleton to the final system, told through the ADR history, superseded decisions included | [TODO: page] | [TODO: ESC-xx] | [TODO: name] | Narrative linked from **08 · Architecture Decision Records** | Draft |
| 12 | Known limitations and technical-debt register | Product, AI and operational limitations (including what cannot run in the evaluation environment) and technical debt, each with its impact and a Jira key | [TODO: page] | [TODO: ESC-xx] | [TODO: name] | Register page; every item has a Jira key | Draft |
| 13 | Product roadmap | **Product Roadmap (GO)** updated: R2 delivered vs planned; v2 (automatic roster generation, multi-corps, integration with existing tools) | [TODO: page] | [TODO: ESC-xx] | [TODO: name] | Roadmap page version dated in Sprint 3 | Draft |
| 14 | Final investor pitch | Problem and evidence, MVP and outcome metrics, differentiation, business-model hypothesis, roadmap; answers the review question | [TODO: page] | [TODO: ESC-xx] | [TODO: name] | Slides linked; timed rehearsal done | Draft |
| 15 | Final technical demonstration | The principal journey on the release version, one degraded mode, the dashboard and the pipeline; a recorded plan B | [TODO: page] | [TODO: ESC-xx] | [TODO: name] | Demo script; release tag | Draft |
| 16 | Individual contribution evidence | For each member, the §10 evidence types recorded as described on **Team and Roles** (PRs, reviews, tests, architecture decisions, research, backlog refinement, deployment, operational improvements, documentation, review participation, reflections) | [TODO: page] | [TODO: ESC-xx] | [TODO: name] | Individual contribution statements (§12 item 13) | Draft |

> **✏️ Example (illustrative — replace).** A finished row:
>
> | # | Deliverable | Required content | Page | Jira | Owner | Proof of done | Status |
> |---|---|---|---|---|---|---|---|
> | 9 | Operational dashboard | Grafana dashboard with the two SLIs | [TODO: dashboard link]; summary on **07 · Technical Design** | ESC-xx | Member B | Screenshot dated YYYY-MM-DD attached to the page | Done |

## Review question

> "Is this a credible product and a maintainable software system worth further investment?" — Assignment §7.3.3

> **ℹ️ Guidance.** Answer both halves with evidence. For a credible product, use rows 1–5, 13 and 14 and the outcome metrics. For a maintainable software system, use rows 6–12 and 15. "Worth further investment" is answered by the roadmap and by the decisions revised with the investors on **Investor Decision Log**.

## Final submission checklist (§12)

> **ℹ️ Guidance.** There is one item per §12 entry, in the assignment's exact wording. Tick an item only when its link opens for the teaching staff, not just for the team.

- [ ] 1. Source-code repository — [TODO: GitHub URL]
- [ ] 2. Release version of the product — [TODO: release tag and deployed URL]
- [ ] 3. Deployment instructions — **Deployment and Operations** (`docs/operations/deployment-and-operations.md`)
- [ ] 4. Architecture documentation — **07 · Technical Design**, **Technical Design** (`docs/architecture/technical-design.md`), **08 · Architecture Decision Records**
- [ ] 5. Product and requirements documentation — **04 · Product Vision**, **Requirements and Quality Attributes**, **05 · Product Backlog**
- [ ] 6. API documentation — `contracts/openapi.yaml`, `contracts/events/`
- [ ] 7. Test and quality evidence — **Test Strategy** (`docs/testing/test-strategy.md`); CI reports [TODO: link]
- [ ] 8. AI evaluation report — [TODO: link], summarised on **06 · Responsible AI Opportunity Assessment**
- [ ] 9. Security and privacy assessment — **09 · Security and Privacy Assessment**
- [ ] 10. Operational dashboard — [TODO: Grafana URL]
- [ ] 11. Product roadmap — **Product Roadmap (GO)**
- [ ] 12. Final presentation — [TODO: link to slides]
- [ ] 13. Individual contribution statements — [TODO: link]; evidence recorded as on **Team and Roles**

> "Any functionality that cannot be executed in the evaluation environment must be clearly identified, explained, and demonstrated through suitable evidence." — Assignment §12

### Not executable in the evaluation environment

> **ℹ️ Guidance.** List every such functionality with the reason and the evidence that replaces it. The `evaluation` profile (IPMA simulator + AI fallback, runs offline) should keep this list short. Keep it in sync with the known limitations in **Deployment and Operations** (`docs/operations/deployment-and-operations.md`).

| Functionality | Why it cannot run there | Evidence provided |
|---|---|---|
| [TODO: functionality] | [TODO: reason] | [TODO: recording, logs, report] |

> **✏️ Example (illustrative — replace).**
>
> | Functionality | Why it cannot run there | Evidence provided |
> |---|---|---|
> | Live IPMA data | The `evaluation` profile runs offline with `tools/ipma-sim` | Recording of the deployed environment reading live RCM; worker logs with the IPMA source cited |
> | AI extraction with a hosted model (only if ADR 0011 chooses a hosted API) | Needs network access and an API key | The `evaluation` profile uses the form fallback; recorded demo and the AI evaluation report |
> | Automated deploy stage | Runs only in GitHub Actions, against the deployed environment | Link to a green pipeline run and its deploy log |

## Closing challenge (§13)

> "At the end of the project, each team should be able to make a convincing case that: The team has identified a real community problem, built the smallest credible product that addresses it, evaluated its most important assumptions, and delivered a secure, responsible, observable, and maintainable software system." — Assignment §13

> **ℹ️ Guidance.** Before the final review, back each part of the claim with one line of evidence. If a line is weak, say so in the pitch instead of overstating it.

| Claim | Our evidence | Where |
|---|---|---|
| Identified a real community problem | [TODO: one line] | **01 · Problem and Opportunity**, **03 · User Research** |
| Built the smallest credible product that addresses it | [TODO: one line] | **MVP, Success Criteria and Ethical Engagement**; release [TODO: tag] |
| Evaluated its most important assumptions | [TODO: one line] | **Research Synthesis and Decisions**; AI evaluation report |
| Delivered a secure, responsible, observable, and maintainable software system | [TODO: one line] | **09 · Security and Privacy Assessment**, **06 · Responsible AI Opportunity Assessment**, operational dashboard, **08 · Architecture Decision Records** |

## Done when
- [ ] Every row has a page or repo path, a Jira key, an owner and a working proof link
- [ ] Every §12 item is ticked, with a link the teaching staff can open
- [ ] Every functionality that cannot run in the evaluation environment is listed with its evidence
- [ ] Each part of the §13 claim has one line of evidence
- [ ] Reviewed by another team member
- [ ] Guidance and example blocks removed
- [ ] Status set to Done
