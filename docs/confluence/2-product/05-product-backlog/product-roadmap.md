# Product Roadmap (GO)

| Field | Value |
|---|---|
| Status | Draft |
| Owner | [TODO: name] |
| Contributors | [TODO: names] |
| Deliverable | Sprint 1 · #5 Product Backlog (child page) |
| Sources | Deck M4; Assignment §7.1.2 #5, §7.3.2 #13, §12 #11 |
| Confluence parent | 05 · Product Backlog |
| Labels | `sprint-1`, `deliverable-05`, `product`, `roadmap` |
| Jira | [TODO: SCRUM-xx] |
| Last updated | YYYY-MM-DD |

> **ℹ️ About this page.** Escala's outcome-based release plan in Roman Pichler's GO Product Roadmap format (deck M4): one column per release, from the walking skeleton to post-project. It is the *Initial release plan* of deliverable #5 (§7.1.2) and becomes the *Product roadmap* required in Sprint 3 (§7.3.2 #13) and in the final submission (§12 #11); read by the team and the investors.

## Roadmap

|  | R0 (end of Sprint 1) | R1 (end of Sprint 2) | R2 (end of Sprint 3) | v2 (post-project) |
|---|---|---|---|---|
| **Date** | [TODO: Sprint 1 review date] | [TODO: Sprint 2 review date] | [TODO: Sprint 3 review date] | [TODO: after the project] |
| **Name** | [TODO: release name] | [TODO: release name] | [TODO: release name] | [TODO: release name] |
| **Goal** | [TODO: problem solved or benefit] | [TODO: problem solved or benefit] | [TODO: problem solved or benefit] | [TODO: problem solved or benefit] |
| **Features** | [TODO: 3–5 features] | [TODO: 3–5 features] | [TODO: 3–5 features] | [TODO: 3–5 features] |
| **Metrics** | [TODO: how we know the goal is met] | [TODO: how we know the goal is met] | [TODO: how we know the goal is met] | [TODO: how we know the goal is met] |

> **ℹ️ Guidance.** Fill one column per release. **Goal** states the problem the release solves for the duty chief or the corps; **Features** are coarse, epic-level capabilities (the detail lives in Jira); **Metrics** say how you will know the goal was met, in the product or in the demo. Keep names and goals consistent with the sprint goals on **05 · Product Backlog**.

> **✏️ Example (illustrative — replace).**
> |  | R0 (end of Sprint 1) | R1 (end of Sprint 2) | R2 (end of Sprint 3) | v2 (post-project) |
> |---|---|---|---|---|
> | **Date** | YYYY-MM-DD (Sprint 1 review) | YYYY-MM-DD (Sprint 2 review) | YYYY-MM-DD (Sprint 3 review) | After the project |
> | **Name** | Walking skeleton | Gap to confirmed | Trustworthy readiness | Beyond one corps |
> | **Goal** | The chain PWA → API → queue → worker → DB runs deployed | Principal journey works: a declared unavailability becomes a confirmed, verified-qualified replacement | Hardened and evaluated: the duty chief can trust what Escala shows, even when a dependency fails | Less effort to build the *piquete*, for more than one corps (hypothesis) |
> | **Features** | Create a *piquete* with one slot (SCRUM-45); trivial evaluation by the worker; automated build and initial test; containerised deployment | Unavailability; eligibility rules; application and confirmation with hours counted (RF04); IPMA RCM; AI extraction with fallback | Explanations of ineligibility; weather warnings; operational dashboard; degraded modes; AI evaluation | Automatic roster generation; multi-corps; integration with existing tools (❓ feasibility unverified) |
> | **Metrics** | Pipeline green; one-command run | Median minutes to fill a gap in a simulated scenario | AI extractions accepted without correction (target ≥ 85%); SLIs: eligibility evaluation p95 latency, % of requests evaluated without manual intervention | [TODO: metric] |

## Guidance

- **Outcome-based.** State the problem to solve, not only the feature. Deck M4: "Outcome-based roadmaps state the problem to solve, not just the feature to ship."
- **3–5 features per release.** Deck M4: "top 3–5 features per version". If a release needs more, group them or move some to the next release.
- **Measurable.** Every release has at least one metric; R1 and R2 metrics trace back to the success criteria on **MVP, Success Criteria and Ethical Engagement**.
- **Living document.** Deck M4: "A living document capturing the goals of upcoming product versions". Update it at every Sprint Review and set *Last updated*; when investor feedback changes a release, record the decision on **Investor Decision Log**.
- **Horizon.** Deck M4: "Focus on the next 6–12 months rather than predicting 2–3 years out." Keep the v2 column coarse.
- **Deferred items.** Requirements marked N (v1.1) on **Requirements and Quality Attributes** (RF05 import members from a file, RF30 risk vs actual coverage history) go in the v2 column until a separate release is planned.

## Attribution

GO Product Roadmap by Roman Pichler (romanpichler.com), licensed under CC BY-SA 4.0.

> **ℹ️ Guidance.** Keep this section when the page is published; CC BY-SA 4.0 requires attribution. The template is also listed on **AI Usage and Third-Party Attribution**.

## Done when
- [ ] R0, R1 and R2 filled: each Goal states an outcome, each release has 3–5 features and at least one measurable metric
- [ ] Release names and goals match the sprint goals and Release plan on **05 · Product Backlog**
- [ ] Updated after the latest Sprint Review, with *Last updated* set
- [ ] Attribution present
- [ ] Reviewed by another team member
- [ ] Guidance and example blocks removed
- [ ] Status set to Done
