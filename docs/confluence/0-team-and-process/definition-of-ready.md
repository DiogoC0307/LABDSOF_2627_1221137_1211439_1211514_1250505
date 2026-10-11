# Definition of Ready (DoR)

| Field | Value |
|---|---|
| Status | Draft |
| Owner | [TODO: name] |
| Contributors | [TODO: names] |
| Deliverable | — |
| Sources | Deck M5 (Definition of Ready, INVEST); Deck M7 (Planning Poker) |
| Confluence parent | 0 · Team and Process |
| Labels | `team-process`, `scrum` |
| Jira | [TODO: SCRUM-xx] |
| Last updated | YYYY-MM-DD |

> **ℹ️ About this page.** The checklist every backlog item must pass before the team pulls it into a sprint, and how the team enforces it (deck M5). The whole team reads it before backlog refinement and Sprint Planning; the Product Owner applies it.

> **ℹ️ Guidance — this page already exists.** In Confluence this page exists as **Definition of Ready (DoR)**. Merge these sections into it instead of creating a new page; keep its history.

## Purpose

*"Can we start working on this?"* A checklist for the team, by the team — every backlog item in the sprint must adhere to it (deck M5).

The deck gives the reason: "A shared Definition of Ready prevents the team from pulling in stories that stall mid-sprint." It pairs with the **Definition of Done (DoD)**: the DoR guards the entry to *Ready*, the DoD guards the entry to *Done*.

> **ℹ️ Guidance.** Keep the deck's wording above and add one sentence saying which Jira issue types the DoR applies to (e.g. Story, Task, Spike and Bug; not Epic). Change the checklist only by team agreement, recorded in a retrospective or in meeting minutes.

## Checklist

- [ ] **1.** It is clear what is to be done (actor, goal, value in story format)
- [ ] **2.** All business rules are known and referenced by ID (`RNxx`, e.g. `RN02`, as listed on **Requirements and Quality Attributes**)
- [ ] **3.** Dependencies are cleared
- [ ] **4.** Acceptance criteria are written in Given/When/Then, including at least one boundary case
- [ ] **5.** A test plan and test cases are outlined (`TC-` IDs, e.g. `TC-SCRUM-999-01`)
- [ ] **6.** INVEST check passed
- [ ] **7.** Estimated through Planning Poker
- [ ] **8.** Linked to its requirement(s) (`RF`/`RNF`)
- [ ] **9.** Fits in one sprint (≤ 8 points; otherwise split)

> **ℹ️ Guidance.** Items 1–5 answer the deck's five DoR questions: "Is it clear what is to be done?" · "Do we know all the business rules?" · "Are dependencies cleared out?" · "Do we have all the acceptance criteria?" · "Do we have a test plan and test cases?" Items 6–9 are team additions: INVEST is explained on **05 · Product Backlog**, and only the people who build the increment estimate (deck M7). Record the IDs from items 2 and 8 in the Jira fields Business rule and Requirement (see **Git, Pull Request and Jira Rules**).

## How it is enforced

- Only the Product Owner moves an item to *Ready* in Jira (workflow `Backlog → Ready → In Progress → In Review → Done`, see **Scrum Methodology**).
- An item that fails any checklist item goes back to *Backlog* with a comment naming the failed items and what is missing.
- Sprint Planning pulls only items that are in *Ready*.
- Estimation (item 7) happens in refinement, before the move; the Product Owner and Scrum Master do not estimate (deck M7).

> **ℹ️ Guidance.** Name the Product Owner (see **Team and Roles**) and say whether the *Ready* transition is restricted in the Jira workflow or is a working agreement. Keep the returned-item comments short and specific: item numbers plus the missing piece.

> **✏️ Example (illustrative — replace).** Comment by the PO when returning SCRUM-xx to *Backlog*: "Fails DoR 2 and 4. The minimum rest between duty periods (RN07) is still 'to obtain' from the internal regulation, and there is no boundary case for a rest period ending exactly at the next start. Back to Backlog."

## Example

> **ℹ️ Guidance.** Keep one story that passes and one item that fails, each with the reason. Replace them with real SCRUM backlog items as soon as you have them; the passing story shows the level of detail the team expects before Sprint Planning.

### Passes — SCRUM-999

> **✏️ Example (illustrative — replace).**
>
> | Part | Content |
> |---|---|
> | Story (SCRUM-999) | "As a duty chief, I want the system to reject assigning someone whose TAS certification has expired, so that no ambulance leaves with an unqualified crew." |
> | Business rule | RN02 — an expired certification (e.g. TAS) doesn't count |
> | Requirements | RF08 (assign a member to a slot with an eligibility check); RF21 (evaluate eligibility against the business rules and return the violated rule codes) |
> | Dependencies | Certifications can be recorded with issue and expiry dates (RF02, SCRUM-xx) — Done |
> | Estimate | 5 points, Planning Poker |
>
> **Acceptance criteria**
> - **Scenario 1 — expired.** **Given** Member A's TAS expired before the slot date, **When** the duty chief assigns Member A to an ambulance slot, **Then** the assignment is rejected with rule code RN02 and the reason is shown. (`TC-SCRUM-999-01`)
> - **Scenario 2 — boundary: certification expires on the slot date.** **Given** Member A's TAS expires on the slot date, **When** the duty chief assigns Member A to that slot, **Then** the assignment is rejected with rule code RN02. (`TC-SCRUM-999-02`) *Illustrative reading of RN02: a certification does not count on its expiry date. Confirm the expiry-day rule before the real story is Ready.*
> - **Scenario 3 — valid.** **Given** Member A's TAS expires after the slot date, **When** the duty chief assigns Member A to that slot, **Then** RN02 does not block the assignment. (`TC-SCRUM-999-03`)
>
> **Why it passes.** 1 ✅ actor, goal and value · 2 ✅ RN02 referenced · 3 ✅ the certification story is Done · 4 ✅ three scenarios including the boundary · 5 ✅ `TC-SCRUM-999-01` to `-03` · 6 ✅ INVEST · 7 ✅ estimated · 8 ✅ RF08, RF21 · 9 ✅ 5 ≤ 8 points.

### Fails — "Improve notifications"

> **✏️ Example (illustrative — replace).**
> - **Item.** "Improve notifications"
> - **1 ❌ Not clear.** No actor, goal or value: who is notified, about what, and why?
> - **2 ❌ No business rules**, **4 ❌ no acceptance criteria**, so **5 ❌** no test cases can be written and **7 ❌** nobody can estimate it.
> - **What happens.** The PO returns it to *Backlog* with a comment and rewrites it as a vertical slice, e.g. "As a firefighter, I want to be notified when my application to a slot is confirmed, so that I know in advance that I am on duty." (RF20), then it goes through the checklist again.

## Done when
- [ ] Every team member has agreed the checklist (recorded in meeting minutes or as a page comment)
- [ ] The Jira *Ready* status exists in the SCRUM workflow and the Product Owner is named as the only person who moves items to it
- [ ] The `## Example` section shows one real passing story and one real failing item from the SCRUM backlog
- [ ] Merged into the existing Confluence page; no duplicate page created
- [ ] Reviewed by another team member
- [ ] Guidance and example blocks removed
- [ ] Status set to Done
