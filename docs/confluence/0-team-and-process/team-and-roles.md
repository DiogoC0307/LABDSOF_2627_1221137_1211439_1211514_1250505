# Team and Roles

| Field | Value |
|---|---|
| Status | Draft |
| Owner | [TODO: name] |
| Contributors | [TODO: names] |
| Deliverable | — |
| Sources | Assignment §10; Deck M1, M8 |
| Confluence parent | 0 · Team and Process |
| Labels | `team-process` |
| Jira | [TODO: SCRUM-xx] |
| Last updated | YYYY-MM-DD |

> **ℹ️ About this page.** Who is on the team, who is accountable for what, when we meet, the working agreements we all signed, and where each member's contribution evidence is recorded. Teaching staff read it to know whom to ask; evaluation is individual (§10), so every member relies on it.

## Members

> **ℹ️ Guidance.** One row per member. The Scrum role comes from the section below and the responsibility area from the table after it. Contact is an institutional channel only (ISEP email or team-chat handle), never a personal phone number.

| Name | Student no. | Scrum role | Responsibility area | Contact |
|---|---|---|---|---|
| [TODO: name] | [TODO: student no.] | [TODO: Product Owner / Scrum Master / Developer] | [TODO: area] | [TODO: institutional contact] |
| [TODO: name] | [TODO: student no.] | [TODO: Scrum role] | [TODO: area] | [TODO: institutional contact] |
| [TODO: name] | [TODO: student no.] | [TODO: Scrum role] | [TODO: area] | [TODO: institutional contact] |
| [TODO: name] | [TODO: student no.] | [TODO: Scrum role] | [TODO: area] | [TODO: institutional contact] |

## Scrum roles

- **Product Owner.** [TODO: name]. One person, not a committee (deck M1). Orders the backlog and communicates the Product Goal; stakeholders who want changes, including the teaching staff acting as investors, convince the PO instead of bypassing them.
- **Scrum Master.** [TODO: name]. Facilitates the Scrum events, removes impediments, and makes sure items enter *Ready* only when they meet the **Definition of Ready (DoR)** and are closed only when they meet the **Definition of Done (DoD)**.
- **Developers.** Everyone builds. [TODO: names]

> **ℹ️ Guidance.** The PO and SM do not estimate: "only the people who build the increment estimate. The Product Owner and Scrum Master do not estimate, and do not influence estimates" (deck M7). In a team of 3–4 the PO and SM also build, but in Planning Poker they only answer questions. The event rules are on **Scrum Methodology**.

## Responsibility areas

> **ℹ️ Guidance.** Accountable means ensuring it happens, not owning it exclusively: everyone still writes code, tests and docs in every area. Everyone must be able to explain every area (§10: vision, journeys, architecture, components, decisions, security and privacy, AI evaluation, deployment), which is why the cadence includes a knowledge-sharing session.

| Area | Accountable | What "accountable" means |
|---|---|---|
| Product | [TODO: name] | Backlog ordered and refined; discovery and vision pages current; investor feedback recorded as Jira *Feedback* issues |
| Architecture | [TODO: name] | ADRs written on the day the decision is taken; **07 · Technical Design** and the Git technical design current; component boundaries respected |
| Platform/DevOps | [TODO: name] | CI/CD pipeline green; one-command run and rollback work; health checks, metrics and dashboard up |
| Quality & AI | [TODO: name] | Test strategy followed and DoD checked before closing; AI evaluation dataset, results and fallback current |

## Cadence

> **ℹ️ Guidance.** Agree times and durations in the first week and keep them fixed for the sprint. Every event with an output page is recorded there; minutes are created from **Meeting Minutes — Template**.

| Event | When | Duration | Output page |
|---|---|---|---|
| Sprint Planning | [TODO: first day of each sprint, time] | [TODO: duration] | **Sprint 1 · Planning** (one per sprint) |
| Daily (async, three lines in the team channel) | [TODO: every working day, by time] | [TODO: e.g. 5 min to write] | — (team channel: done since last post / next / blockers) |
| Weekly sync | [TODO: weekday, time] | [TODO: duration] | Minutes under **Sprint 1 · Meeting Minutes** |
| Sprint Review | [TODO: last day of each sprint, with the teaching staff] | [TODO: duration] | **Sprint 1 · Review** |
| Retrospective | [TODO: after each Sprint Review] | [TODO: duration] | **Sprint 1 · Retrospective** |
| Knowledge-sharing (each member explains their work to the others, §10) | [TODO: e.g. once per sprint] | [TODO: duration] | Minutes under **Sprint 1 · Meeting Minutes** |

> **✏️ Example (illustrative — replace).** Weekly sync: Wednesdays 18:00, 45 min, minutes created from the template under **Sprint 1 · Meeting Minutes**. Daily post: "Done: outbox table in `escala-api` (SCRUM-xx). Next: publish `AvaliacaoPedida`. Blocker: none."

## Working agreements

> **ℹ️ Guidance.** These agreements change only at a retrospective; record the change and its date here and ask everyone to sign off again.

- Nobody approves their own PR.
- No PR stays open more than 48 h.
- A decision not solved in 10 minutes becomes a Jira *Decision* issue, resolved in writing within 48 h.
- Everyone reviews everyone.
- **Each member edits their own Confluence pages with their own account**, because page history is contribution evidence.

## Individual contribution evidence

> **ℹ️ Guidance.** "Raw commit counts will not be treated as reliable evidence of contribution" (§8.1), and "Assessment will focus on the quality and impact of contributions, not merely their quantity" (§10). Make each piece of work traceable to you: your own account, a Jira key in every branch, commit and PR, and your name in author fields.

| Evidence type (§10 list) | Where it is recorded |
|---|---|
| Authored or co-authored pull requests | GitHub (PR author and co-authors) |
| Code reviews | GitHub (PR reviews and comments) |
| Tests | Repository and CI (test files, CI runs) |
| Architecture decisions | ADR author field (`docs/adr/`), indexed on **08 · Architecture Decision Records** |
| Product-research activities | Research notes (interviewer and note-taker fields) under **03 · User Research** |
| Backlog refinement | Jira issue history |
| Deployment work | CI/CD history (GitHub Actions) |
| Operational improvements | GitHub PRs on `infra/` and dashboard changes |
| Documentation | Confluence page history |
| Sprint-review participation | Minutes (attendees) and **Sprint 1 · Review** |
| Individual reflections | Retrospective pages (**Sprint 1 · Retrospective**) |

## Sign-off

> **ℹ️ Guidance.** Each member adds their own row, with their own account, after reading this page. Sign again whenever the working agreements change.

| Name | Agreed on (date) |
|---|---|
| [TODO: name] | YYYY-MM-DD |
| [TODO: name] | YYYY-MM-DD |
| [TODO: name] | YYYY-MM-DD |
| [TODO: name] | YYYY-MM-DD |

## Done when
- [ ] Every member is listed with Scrum role, responsibility area and contact
- [ ] Every responsibility area has one accountable member
- [ ] Cadence times and durations agreed
- [ ] Every member has signed off with their own account
- [ ] Reviewed by another team member
- [ ] Guidance and example blocks removed
- [ ] Status set to Done
