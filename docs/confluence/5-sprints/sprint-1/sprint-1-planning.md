# Sprint 1 · Planning


| Field | Value |
|---|---|
| Status | Draft |
| Owner | [TODO: name] |
| Contributors | [TODO: names] |
| Deliverable | — |
| Sources | Assignment §7.1, §8.2; Deck M5 (Definition of Ready), M6, M7, M8 |
| Confluence parent | Sprint 1 |
| Labels | `sprint-1`, `planning` |
| Jira | [TODO: ESC-xx] |
| Last updated | YYYY-MM-DD |

> **ℹ️ About this page.** The output of Sprint 1 Planning (Deck M8): dates, sprint goal, capacity, committed items and weekly gates. The team checks it at every weekly sync. It is also the evidence that the sprint was planned and its goal documented in Jira (Assignment §8.2).

## Dates

- Sprint start: [TODO: YYYY-MM-DD]
- Sprint end: [TODO: YYYY-MM-DD]
- Sprint review with the investors: [TODO: YYYY-MM-DD, time]

> **ℹ️ Guidance.** Use the dates of `"ESC Sprint 1"` in Jira and the review date agreed with the teaching staff. Put the same dates on **Escala — Home**.

## Sprint goal

[TODO: one sentence]

> **ℹ️ Guidance.** One sentence that states an outcome, not a list of tasks. Use the same text in the Jira sprint goal field and in the *Sprint goals* table on **05 · Product Backlog**.

> **✏️ Example (illustrative — replace).** "Prove that the problem is real at [corps], that the proposed MVP is the smallest credible experiment, and that the chain PWA → API → queue → worker → DB runs deployed."

## Capacity

| Member | Available hours | Notes |
|---|---|---|
| [TODO: name] | [TODO: hours] | [TODO: exams, absences] |
| [TODO: name] | [TODO: hours] | [TODO: exams, absences] |
| [TODO: name] | [TODO: hours] | [TODO: exams, absences] |
| [TODO: name] | [TODO: hours] | [TODO: exams, absences] |

> **ℹ️ Guidance.** Enter the hours each member can realistically give to the sprint once classes, exams and absences are taken out. Sprint tasks are estimated in hours (Deck M7), so the total here caps what the team commits to. Delete the fourth row if the team has three members.

> **✏️ Example (illustrative — replace).**
> | Member | Available hours | Notes |
> |---|---|---|
> | Member A | 30 | Exam in Week B; half availability that week |
> | Member B | 36 | — |

## Committed items

[JIRA: project = ESC AND sprint = "ESC Sprint 1" ORDER BY Rank]

> **ℹ️ Guidance.** Embed the Jira macro and never copy the items into this page. Commit only items that meet the **Definition of Ready (DoR)**, and make sure their task hours fit the capacity above.

## Checkpoints

| Week | Gate | If missed, then… |
|---|---|---|
| Week A ([TODO: dates]) | [TODO: gate] | [TODO: fallback] |
| Week B ([TODO: dates]) | [TODO: gate] | [TODO: fallback] |
| Week C ([TODO: dates]) | [TODO: gate] | [TODO: fallback] |

> **ℹ️ Guidance.** Set one gate per week: a few concrete outcomes that can be checked. Check the gate at the weekly sync and record the result in that meeting's minutes. Decide the fallback now, not after the gate is missed.

> **✏️ Example (illustrative — replace).**
> | Week | Gate | If missed, then… |
> |---|---|---|
> | Week A | Pipeline green + observation date booked + IPMA call done | Pair on the pipeline before starting any other work. If no observation date is booked, document the access limitation (§5.3) and plan interviews instead |
> | Week B | Skeleton goes end to end + interviews done + A4 answered | If A4 (fire risk changes the required *piquete* composition) is still open, keep the IPMA integration as an assumption and bring it to the review as an open decision |
> | Week C | Ten pages + deploy + timed rehearsal | Freeze new content, complete *Proof of done* on **Sprint 1 · Deliverables** and rehearse the demo's plan B |

## Sprint risks

- [TODO: risk] → [TODO: mitigation]. Owner: [TODO: name]

> **ℹ️ Guidance.** List only risks to delivering this sprint (access, dependencies, skills). Product risks belong on **01 · Problem and Opportunity**. Give each risk a mitigation and an owner, put risky items first (Deck M6), and label them `risk` in Jira.

> **✏️ Example (illustrative — replace).**
> - No access to a corps for observation before Week B → document the limitation (§5.3) and rely on interviews and public documents. Owner: Member A
> - The internal regulation (rest between duty periods; risk level → required composition) is not obtained → keep RN07 and RN10 configurable and marked "to obtain". Owner: Member B
> - The IPMA API is unavailable during development → develop against the IPMA simulator (`tools/ipma-sim`) from Week A. Owner: Member C

## Planning Poker record

| Date | Items estimated | Participants |
|---|---|---|
| [TODO: YYYY-MM-DD] | [TODO: ESC-xx (points)] | [TODO: builders only] |

> **ℹ️ Guidance.** Record every estimation session: the items, their final story points (Fibonacci 1–21) and who estimated. Only the people who build the increment estimate. The Product Owner and Scrum Master do not estimate and do not influence estimates (Deck M7). Any item above 8 points is split (DoR).

> **✏️ Example (illustrative — replace).**
> | Date | Items estimated | Participants |
> |---|---|---|
> | YYYY-MM-DD | ESC-23 create *piquete* with one slot (3); ESC-31 reject assigning someone whose TAS has expired (5) | Member B, Member C, Member D (Member A, the Product Owner, did not estimate) |

## Done when
- [ ] Dates, sprint goal and capacity filled, and identical to Jira
- [ ] Every committed item meets the DoR, and the committed hours fit the capacity
- [ ] Every checkpoint has dates and a fallback
- [ ] Planning Poker session recorded
- [ ] Reviewed by another team member
- [ ] Guidance and example blocks removed
- [ ] Status set to Done
