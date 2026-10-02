# Scrum Methodology

| Field | Value |
|---|---|
| Status | Draft |
| Owner | [TODO: name] |
| Contributors | [TODO: names] |
| Deliverable | — |
| Sources | Assignment §7, §8.2; Deck M5–M9 |
| Confluence parent | 0 · Team and Process |
| Labels | `team-process`, `scrum` |
| Jira | [TODO: ESC-xx] |
| Last updated | YYYY-MM-DD |

> **ℹ️ About this page.** How the team runs Scrum on Escala: sprints, events, artifacts, the Jira board, estimation, prioritisation, progress tracking and the evidence that the software engineering process was followed. Read it before the first Sprint Planning; reviewers use it to check that Jira is used as §8.2 requires.

> **ℹ️ Guidance — this page already exists.** In Confluence this page exists as **Scrum Methodology**. Merge these sections into it instead of creating a new page; keep its history.

## Sprints and their goals

> **ℹ️ Guidance.** The objectives and review questions are fixed by the assignment (§7); quote them, do not reword them. Add the dates. The team's own Sprint Goal for each sprint lives in the Jira sprint and on **05 · Product Backlog**.

| Sprint | Objective | Review question |
|---|---|---|
| 1 — Discover, Validate, Design ([TODO: dates]) | "The objective of Sprint 1 is to establish that the team is addressing a meaningful problem and has proposed a credible product and technical direction." | "Is this a valuable problem, and is the proposed MVP the smallest credible product experiment?" |
| 2 — Build and Operate the Vertical Slice ([TODO: dates]) | "The objective of Sprint 2 is to deliver an operational end-to-end version of the product's most important user journey." | "Does the product already deliver observable user value, and can the team deploy, monitor, and operate it?" |
| 3 — Evaluate, Harden, and Present ([TODO: dates]) | "The objective of Sprint 3 is to refine the MVP, evaluate its most important qualities, and demonstrate that it is a credible and maintainable product." | "Is this a credible product and a maintainable software system worth further investment?" |

## Roles

Product Owner, Scrum Master and Developers (deck M8; Scrum Guide 2020). Who holds each role, what it means in this team, and the rule that the PO and SM do not estimate are on **Team and Roles**.

## Events

> **ℹ️ Guidance.** Adapt the inputs and outputs to how the team really works, but keep one Confluence page per event per sprint so a reviewer can follow the trail. Open every retrospective with Norm Kerth's prime directive: "Everyone did the best job they could, given what they knew at the time."

| Event | Purpose | Inputs | Outputs | Confluence page |
|---|---|---|---|---|
| Sprint Planning | Agree the Sprint Goal and pull the items that achieve it | Ordered Product Backlog (items in *Ready*); previous velocity; the sprint's assignment deliverables | Sprint Goal; Sprint Backlog in Jira; tasks estimated in hours | **Sprint 1 · Planning** (one per sprint) |
| Daily Scrum | Inspect progress towards the Sprint Goal and surface blockers early | Board; burndown | Three lines per member in the team channel (async); blockers raised the same day | — (team channel; see **Team and Roles**) |
| Sprint Review | Show the increment to the investors and collect their feedback | Increment that meets the DoD; the sprint's deliverables | Feedback as Jira *Feedback* issues; updated backlog; decisions in **Investor Decision Log** | **Sprint 1 · Review** |
| Sprint Retrospective | Answer "How can we, as a team, work even better next sprint?" | Burndown and velocity; review outcome; previous actions | Went Well / To Improve / Ideas / Actions (or Start / Stop / Continue); actions as Jira issues | **Sprint 1 · Retrospective** |

## Artifacts and commitments

> **ℹ️ Guidance.** Each artifact has one commitment that gives it focus (Scrum Guide 2020). Write the Product Goal once and change it only through the PO; write the Sprint Goal at every Sprint Planning.

- **Product Backlog → Product Goal.** Ordered by the PO in Jira; summarised on **05 · Product Backlog**. Product Goal: [TODO: one sentence]
- **Sprint Backlog → Sprint Goal.** The items and hour-estimated tasks pulled at Sprint Planning; the Sprint Goal is written in the Jira sprint and on the sprint's planning page. Sprint Goal: [TODO: one sentence per sprint]
- **Increment → Definition of Done.** Only items that meet the **Definition of Done (DoD)** are part of the increment shown at the Sprint Review.

> **✏️ Example (illustrative — replace).** Product Goal: "Every day, the duty chief knows whether the *piquete* can crew what today's fire risk requires, and fills any gap with a verified-qualified person."

## Board

> **ℹ️ Guidance.** Configure the Jira workflow with exactly these five statuses. Ready requires the **Definition of Ready (DoR)**; Done requires the **Definition of Done (DoD)**. An item closed without meeting the DoD must be reopened.

Jira workflow: `Backlog → Ready → In Progress → In Review → Done`, mapped to the deck's Kanban board (Backlog / To Do / In Progress / Testing / Done):

| Jira status | Deck Kanban column | Entry rule |
|---|---|---|
| Backlog | Backlog | Any item, refined or not |
| Ready | To Do | Meets the DoR; only the PO moves items here |
| In Progress | In Progress | A developer has pulled it and assigned themselves |
| In Review | Testing | PR open; code review, CI and functional tests under way |
| Done | Done | Meets the DoD |

Current sprint: [JIRA: project = ESC AND sprint in openSprints() ORDER BY Rank ASC]

## Estimation

> **ℹ️ Guidance.** "Story points are coarse-grained, relative measures of effort and size — not hours" (deck M7). Only the people who build estimate; the PO and SM do not estimate and do not influence estimates. Record each session in the estimation table on **05 · Product Backlog**.

- **Backlog items:** story points on the Fibonacci scale 1, 2, 3, 5, 8, 13, 21.
- **Sprint tasks:** hours.
- **Planning Poker:** builders reveal their cards at the same time; the highest and lowest explain their reasoning; vote again until the estimates converge.
- **Size limit:** an item above 8 points is split before it can be *Ready* (see **Definition of Ready (DoR)**).

> **✏️ Example (illustrative — replace).** For ESC-31 (reject assigning someone whose TAS has expired), the developers reveal 3 and 13 at the same time while the PO only answers questions. The member who voted 13 explains that certifications have no expiry date in the data model yet; the one who voted 3 had assumed they did. Once the story is agreed to include the expiry date, a second round converges on 5 and the value is recorded in Jira.

## Prioritisation

> **ℹ️ Guidance.** The PO orders the backlog with these four factors (deck M6). The current list of risk items that go first is on **05 · Product Backlog**.

1. **Value** — "Is the item necessary for bringing the product to life? Could the product still succeed without it?"
2. **Knowledge, uncertainty and risk** — "Risky, uncertain items go first — a risk-driven approach surfaces early failure."
3. **Releasability** — "Each release should provide useful functionality and generate feedback."
4. **Dependencies** — "Resolve them where possible; combine or re-split dependent stories."

> **✏️ Example (illustrative — replace).** The IPMA risk-fetching spike [TODO: ESC-xx] goes before the statutory-hours screen: it depends on an external service, it is the most uncertain item, and the reinforcement journey depends on it.

## Tracking progress

> **ℹ️ Guidance.** Burndown and velocity come from Jira reports. Paste a burndown screenshot and the velocity at the end of each sprint, before the review, and write one line on what they told you. "Metrics evidence progress and surface risk to the sprint early — they are not just reporting overhead" (deck M9).

- **Sprint 1** — [IMAGE: Sprint 1 burndown screenshot from Jira] · Velocity: [TODO: points completed / points committed] · What it told us: [TODO: one line]
- **Sprint 2** — [IMAGE: Sprint 2 burndown screenshot from Jira] · Velocity: [TODO: points completed / points committed] · What it told us: [TODO: one line]
- **Sprint 3** — [IMAGE: Sprint 3 burndown screenshot from Jira] · Velocity: [TODO: points completed / points committed] · What it told us: [TODO: one line]

> **✏️ Example (illustrative — replace).** Sprint 1: 26 of 34 points completed. The line stayed flat for a week while the IPMA story waited for the answer to A4; it was raised in the daily and the story was split.

## Software engineering process

> **ℹ️ Guidance.** "Keep in mind that every 'thing' is part of a software project and must follow the software engineering process — even if it is not 'exactly' programming in 'Java'." "Provide evidences that the process has been followed." (deck). The steps apply per item, not as a waterfall: the walking skeleton starts on day 1.

*Analysis → Design → Test plan → Code → Test execution*. How the evidence is kept:

1. **Analysis** — each Jira item links the requirement (`RF`/`RNF`) and business rule (`RN`) it came from, on **Requirements and Quality Attributes**.
2. **Design** — each Jira item links the design section it came from: **07 · Technical Design**, the Git **Technical Design** (`docs/architecture/technical-design.md`) or an ADR.
3. **Test plan** — test cases (`TC-<story>-<n>`) are outlined before coding and linked in Jira.
4. **Code** — the PR references the Jira key and its template has an "Analysis and design updated" checkbox (see **Git, Pull Request and Jira Rules**).
5. **Test execution** — CI results and the recorded functional tests are linked from the Jira item before it moves to Done.

> **✏️ Example (illustrative — replace).** ESC-31 → RF08, RN02 → **07 · Technical Design**, section Components and data ownership → TC-ESC-31-01, TC-ESC-31-02 → PR [TODO: link] with the checkbox ticked → CI run [TODO: link].

## Done when
- [ ] Sprint dates filled in
- [ ] Jira board configured with the five-status workflow and the Ready and Done rules
- [ ] A burndown screenshot, velocity and one-line reading added at the end of each sprint
- [ ] Reviewed by another team member
- [ ] Guidance and example blocks removed
- [ ] Status set to Done
