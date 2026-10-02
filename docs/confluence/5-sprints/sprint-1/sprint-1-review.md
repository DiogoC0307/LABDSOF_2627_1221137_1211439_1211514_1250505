# Sprint 1 · Review

| Field | Value |
|---|---|
| Status | Draft |
| Owner | [TODO: name] |
| Contributors | [TODO: names] |
| Deliverable | — |
| Sources | Assignment §5.7, §7.1.3; §10 (sprint-review participation) |
| Confluence parent | Sprint 1 |
| Labels | `sprint-1`, `review`, `investor` |
| Jira | [TODO: ESC-xx] |
| Last updated | YYYY-MM-DD |

> **ℹ️ About this page.** Plan and record of the Sprint 1 investor review (Assignment §7.1.3): agenda, demo script, the decisions put to the investors, the feedback received and what changed as a result. It is where investor engagement starts (§5.7). The team and the investors read it.

## Details

- Date and time: [TODO: YYYY-MM-DD, HH:MM]
- Attendees (team): [TODO: names]
- Attendees (investors = teaching staff): [TODO: names]
- Minutes: [TODO: child page `YYYY-MM-DD · Review` under **Sprint 1 · Meeting Minutes**]

> **ℹ️ Guidance.** The investors are the teaching staff. Record who attended; sprint-review participation is individual contribution evidence (§10).

## Review question

*"Is this a valuable problem, and is the proposed MVP the smallest credible product experiment?"* — Assignment §7.1.3

**Our answer:** [TODO: one or two sentences; each claim points to its evidence]

> **ℹ️ Guidance.** Write the answer the investors should leave with, then check that every agenda block below supports it. Use only evidence you really have.

> **✏️ Example (illustrative — replace). Fictional example to show the format. Never copy into real notes.** "Yes. In OBS-01, filling one gap took [TODO: minutes] minutes and [TODO: number of calls] calls, and nobody checked the certification (**03 · User Research**). The MVP is a single journey, from a declared unavailability to a confirmed verified-qualified replacement, and everything else is out of scope (**MVP, Success Criteria and Ethical Engagement**)."

## Agenda (15 min)

| Minutes | Topic | Presenter |
|---|---|---|
| [TODO: minutes] | Problem with one observed number | [TODO: name] |
| [TODO: minutes] | Who suffers and what they do today | [TODO: name] |
| [TODO: minutes] | What already exists and what it doesn't do | [TODO: name] |
| [TODO: minutes] | MVP journey and what was left out | [TODO: name] |
| [TODO: minutes] | Live skeleton and pipeline | [TODO: name] |
| [TODO: minutes] | Architecture in one view | [TODO: name] |
| [TODO: minutes] | Sprint 2 and the open decisions | [TODO: name] |

> **ℹ️ Guidance.** The topics are a suggested running order, so adapt them if needed, but the total must be 15 minutes. Timebox every block and rehearse with a timer. Every member presents at least one block.

> **✏️ Example (illustrative — replace).** 2 · 2 · 2 · 2 · 3 · 2 · 2 minutes, in the order above, for a total of 15. Member A opens with the problem, Member C drives the live skeleton, and Member D closes with the open decisions.

## Demo script

1. [TODO: step]. Who: [TODO: name]
2. [TODO: step]. Who: [TODO: name]
3. [TODO: step]. Who: [TODO: name]

**Plan B:** [TODO: what we do if the live demo fails]

> **ℹ️ Guidance.** Write down every step and who does it. The demo runs on the deployed environment with no hidden manual steps. Prepare and test plan B beforehand.

> **✏️ Example (illustrative — replace).**
> 1. Member C opens the deployed PWA ([TODO: URL]) and creates a *piquete* for today with one slot (`ESC-23`).
> 2. Member D shows the request travelling through the queue: the worker log carries the same trace ID, and the trivial evaluation comes back to the UI.
> 3. Member B opens the last green pipeline run and the PR linked to `ESC-23` in Jira.
>
> Plan B: if the deployed environment fails, play a recording of the same run and show the pipeline. If the network fails, run the skeleton from a clean clone on a laptop with `[TODO: one command]`.

## Decisions we asked the investors about

- [TODO: D-xx, the question asked, and its page under **Investor Decision Log**]

> **ℹ️ Guidance.** Bring one or two decisions that are deliberately left open, each with a decision page under **Investor Decision Log**. List the exact question you asked; record the answers under *Feedback received*.

> **✏️ Example (illustrative — replace).**
> - D-01: does the duty chief see ineligible candidates with the reason, or only eligible ones? (transparency vs exposure of colleagues' data). See **Investor Decision Log**.
> - D-02: when nobody is eligible, is the assignment blocked, or is a recorded exceptional assignment allowed? (a vehicle stuck vs a documented exception). See **Investor Decision Log**.

## Feedback received

| # | From | Feedback | Our response | Jira *Feedback* issue | Action |
|---|---|---|---|---|---|
| 1 | [TODO: investor] | [TODO: feedback] | [TODO: response] | [TODO: ESC-xx] | [TODO: action] |
| 2 | [TODO: investor] | [TODO: feedback] | [TODO: response] | [TODO: ESC-xx] | [TODO: action] |

> **ℹ️ Guidance.** Record every item on the same day, as close to the investor's words as you can. Each item becomes a Jira *Feedback* issue (§5.7, §8.2). *Action* is the concrete next step, such as a story, a decision or a page change, or "none, because …".

> **✏️ Example (illustrative — replace).**
> | # | From | Feedback | Our response | Jira *Feedback* issue | Action |
> |---|---|---|---|---|---|
> | 1 | Investor 1 (teaching staff) | "Showing ineligible colleagues with the reason may expose their data." | We will compare showing only the rule code with hiding ineligible candidates | ESC-xx | Revisit D-01; update **09 · Security and Privacy Assessment** |

## What changed as a result

[TODO: each change, the feedback that caused it, and where it was made]

> **ℹ️ Guidance.** §5.7: *"At least one product or technical decision must be revised in response to investor feedback. The change and its rationale must be documented."* For each change, record what changed, why (feedback #), and where (page, ADR or Jira issue). If nothing changed, say so and name the decision that is still open.

> **✏️ Example (illustrative — replace).** "D-01 revised after feedback #1: the duty chief sees ineligible candidates with the rule code only. Changed RF16 on **Requirements and Quality Attributes** and the access-control matrix on **09 · Security and Privacy Assessment** (ESC-xx)."

## Done when
- [ ] Review minutes recorded on the same day
- [ ] Every feedback item has a Jira *Feedback* issue and an action
- [ ] The decisions asked about are updated on **Investor Decision Log**
- [ ] Every change under "What changed as a result" links to its page, ADR or Jira issue
- [ ] Reviewed by another team member
- [ ] Guidance and example blocks removed
- [ ] Status set to Done
