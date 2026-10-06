# Meeting Minutes — Template

| Field | Value |
|---|---|
| Status | Draft |
| Owner | [TODO: name] |
| Contributors | [TODO: names] |
| Deliverable | — |
| Sources | Assignment §5.7, §8.2, §10; Deck M8 |
| Confluence parent | 0 · Team and Process |
| Labels | `template`, `meeting-minutes` |
| Jira | [TODO: SCRUM-xx] |
| Last updated | YYYY-MM-DD |

> **ℹ️ About this page.** The template every team meeting's minutes are created from: who attended, what was decided and why, what investors or stakeholders said, and who does what next. The team uses the minutes to follow up actions; the teaching staff read them as evidence of decisions, feedback handling and individual participation.

> **ℹ️ Guidance — this page already exists.** In Confluence this page exists as *Ata de Reuniões – Template*. Merge these sections into it and rename it to **Meeting Minutes — Template**; do not create a second page. Page history is kept on rename.

> **ℹ️ Guidance.** Register this page as a **Confluence space template** so each meeting is created from the *Create* button. Title format: `YYYY-MM-DD · <Meeting type>`, e.g. `YYYY-MM-DD · Weekly sync`. Store the minutes as children of **Sprint N · Meeting Minutes** (e.g. **Sprint 1 · Meeting Minutes**); in each filled page keep the header table (Owner = note-taker), delete the About block, keep the `meeting-minutes` label and add `sprint-N`. For reviews and retrospectives, the minutes record attendance and decisions; the content itself lives on **Sprint 1 · Review** and **Sprint 1 · Retrospective** (or the matching sprint).

## Details

| Date | Time | Type (Planning / Weekly sync / Review / Retrospective / Stakeholder / Investor) | Facilitator | Note-taker | Attendees | Absent |
|---|---|---|---|---|---|---|
| YYYY-MM-DD | [TODO: HH:MM–HH:MM] | [TODO: one type] | [TODO: name] | [TODO: name] | [TODO: names] | [TODO: names, or "—"] |

> **ℹ️ Guidance.** Prefer different people as facilitator and note-taker, rotating across meetings. Attendance here is contribution evidence (§10 "Sprint-review participation"), so list everyone present, including investors.

> **✏️ Example (illustrative — replace).** All examples on this page together form one filled weekly sync.
>
> | Date | Time | Type (Planning / Weekly sync / Review / Retrospective / Stakeholder / Investor) | Facilitator | Note-taker | Attendees | Absent |
> |---|---|---|---|---|---|---|
> | YYYY-MM-DD | 18:00–18:30 | Weekly sync | Member A | Member B | Member A, Member B, Member C | Member D |

## Agenda

1. [TODO: agenda item]
2. [TODO: agenda item]
3. [TODO: agenda item]

> **ℹ️ Guidance.** Written before the meeting by the facilitator; always start with the open action items from the previous minutes.

> **✏️ Example (illustrative — replace).**
>
> 1. Open actions from the previous sync
> 2. User research: interviews scheduled, consent script ready
> 3. Walking skeleton: pipeline status
> 4. Open decisions to bring to the Sprint 1 review

## Discussion

**1. [TODO: agenda item]** — [TODO: one short paragraph: what was said, which options, what is still open.]

**2. [TODO: agenda item]** — [TODO: one short paragraph.]

> **ℹ️ Guidance.** One short paragraph per agenda item, facts and positions only. Anything decided goes in **Decisions**; anything someone must do goes in **Action items**.

> **✏️ Example (illustrative — replace).**
>
> **3. Walking skeleton** — The chain PWA → API → queue → worker → DB runs locally with Docker Compose but not yet in the deployed environment. The team discussed whether each component gets its own PostgreSQL instance now or later; the argument for now is that data ownership is easier to keep than to split afterwards.
>
> **4. Open decisions** — D-02 (block the assignment when nobody is eligible, or allow a recorded exceptional assignment) cannot be settled from research alone; it needs investor input.

## Decisions

| ID | Decision | Rationale | Jira *Decision* issue |
|---|---|---|---|
| 1 | [TODO: what was decided] | [TODO: why, and which alternative was rejected] | [TODO: SCRUM-xx] |

> **ℹ️ Guidance.** Number decisions within the meeting (1, 2…); the Jira *Decision* issue key is the lasting reference. A technical decision also gets an ADR; a decision taken to investors gets an entry in **Investor Decision Log** (D-01, D-02…).

> **✏️ Example (illustrative — replace).**
>
> | ID | Decision | Rationale | Jira *Decision* issue |
> |---|---|---|---|
> | 1 | One PostgreSQL database per component, from the walking skeleton on | Keeps data ownership clear between `escala-api` and `elegibilidade-worker`; recorded as ADR 0003 (Proposed) | SCRUM-xx |
> | 2 | Take D-02 to the Sprint 1 review as an open question | Research cannot settle it; the investors' view on risk is needed | SCRUM-xx |

## Stakeholder or investor feedback

| Feedback | From | Jira *Feedback* issue |
|---|---|---|
| [TODO: what was said, close to their words] | [TODO: name or role] | [TODO: SCRUM-xx] |

> **ℹ️ Guidance.** Every investor feedback item becomes a Jira *Feedback* issue (§5.7, §8.2), created the same day, and is answered on the sprint's review page. Write "None" for internal meetings.

> **✏️ Example (illustrative — replace).** None (internal weekly sync).

## Action items

- [ ] [TODO: @owner] — [TODO: action] — due YYYY-MM-DD — [TODO: SCRUM-xx]

> **ℹ️ Guidance.** One line per action, each with an owner (@mention), a due date and a Jira key; an action without a Jira key is not tracked. Tick it here only when the Jira item is done.

> **✏️ Example (illustrative — replace).**
>
> - [ ] @Member A — write ADR 0003 "One PostgreSQL database per component" — due YYYY-MM-DD — SCRUM-xx
> - [ ] @Member C — add the Docker Compose deploy step to the pipeline — due YYYY-MM-DD — SCRUM-xx
> - [ ] @Member B — prepare the D-02 page from **Decision — Template** for the review — due YYYY-MM-DD — SCRUM-xx

## Next meeting

[TODO: date, time, type, facilitator]

> **✏️ Example (illustrative — replace).** YYYY-MM-DD, 18:00 · Weekly sync · facilitator Member B.

## Done when
- [ ] Title follows `YYYY-MM-DD · <Meeting type>` and the page is a child of the right **Sprint N · Meeting Minutes**
- [ ] Every decision links a Jira *Decision* issue, and every stakeholder or investor feedback item links a Jira *Feedback* issue
- [ ] Every action item has an owner, a due date and a Jira key
- [ ] Reviewed by another team member
- [ ] Guidance and example blocks removed
- [ ] Status set to Done
