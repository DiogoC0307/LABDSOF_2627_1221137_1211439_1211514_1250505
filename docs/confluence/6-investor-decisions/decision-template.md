# Decision — Template

| Field | Value |
|---|---|
| Status | Draft |
| Owner | [TODO: name] |
| Contributors | [TODO: names] |
| Deliverable | — |
| Sources | Assignment §5.7, §8.2; Deck M1 |
| Confluence parent | 6 · Investor Decisions |
| Labels | `template`, `investor`, `decision` |
| Jira | [TODO: SCRUM-xx] |
| Last updated | YYYY-MM-DD |

> **ℹ️ About this page.** The template for one decision taken to the investors: its context and options, the question asked, the feedback received, and what was decided and why. Each filled page is the detailed record behind one row of **Investor Decision Log** and the evidence for Assignment §5.7; the team and the investors read it.

> **ℹ️ Guidance.** Register this page as a Confluence space template so it appears under *Create*, and create each decision as a child of **Investor Decision Log**. Title: `D-NN · <decision as a short question>`, e.g. `D-01 · Show ineligible candidates with the reason?`. In each filled page keep the header table (Owner = decision owner), delete the About block, keep the `investor` and `decision` labels, drop `template` and add `sprint-N` for the review where it was asked. All examples on this page together form one filled decision, D-01.

## Status

[TODO: Open / Decided / Revised]

> **ℹ️ Guidance.** *Open*: options on the table, to be asked at the next review. *Decided*: settled, and investor feedback did not change it (*kept* on the log). *Revised*: changed in response to investor feedback (§5.7). This is the decision's status; the header *Status* (Draft → In review → Done) is the page's.

> **✏️ Example (illustrative — replace).** **Revised** on YYYY-MM-DD, after the Sprint 1 review (Open before it).

## Decision owner

[TODO: name, role]

> **ℹ️ Guidance.** One person, not the whole team. For a product decision this is the Product Owner (Deck M1: "One person, not a committee."). The owner prepares the options, asks the question at the review and updates this page and **Investor Decision Log** the same day.

> **✏️ Example (illustrative — replace).** Member A (Product Owner).

## Context

[TODO: 3–5 sentences: what forces the decision, which journey and requirement IDs it affects, what research shows and what it cannot settle]

> **ℹ️ Guidance.** Cite evidence by ID (findings F1…, research codes such as CS-01, RF/RN IDs from **Requirements and Quality Attributes**); never invent it. Say why research alone cannot settle the decision and why the investors' view matters.

> **✏️ Example (illustrative — replace).** On the candidate list (RF16), Escala can show each ineligible colleague with the rule that excludes them, e.g. an expired TAS (RN02). The duty chief then sees at once who cannot crew and why, but whoever composes the *piquete* also sees colleagues' certification status. [TODO: finding ID showing whether the duty chief needs the reason, or "not yet researched"]; research cannot tell us how much exposure is acceptable.

## Options

| Option | Pros | Cons | Effort |
|---|---|---|---|
| A — [TODO: option] | [TODO: pros] | [TODO: cons] | [TODO: S / M / L] |
| B — [TODO: option] | [TODO: pros] | [TODO: cons] | [TODO: S / M / L] |

> **ℹ️ Guidance.** Two to four real options, each one someone would defend; "do nothing" counts only if it is real. *Effort* is a rough relative size (S / M / L), not hours; the stories that follow are estimated in the backlog as usual.

> **✏️ Example (illustrative — replace).**
>
> | Option | Pros | Cons | Effort |
> |---|---|---|---|
> | A — show ineligible candidates with the reason | The duty chief sees at once why someone cannot crew and who could be made eligible | Exposes colleagues' certification data to whoever composes the *piquete* | S |
> | B — show only eligible candidates | No exposure of colleagues' data | When nobody is eligible, the duty chief does not know why and goes back to the phone | S |
> | C — show ineligible candidates with the rule code only | Keeps the reason; exposes less | The code needs a readable explanation (RF22) | M |

## Question to the investors

[TODO: one closed question, the trade-off, and our current preference]

> **ℹ️ Guidance.** One question the investors can answer during the review, with the trade-off stated and the team's current preference. List the same question on the sprint's review page under *Decisions we asked the investors about*.

> **✏️ Example (illustrative — replace).** "Does the duty chief see ineligible candidates with the reason, or only eligible ones? We prefer A for transparency, at the cost of exposing colleagues' data. Would you accept that exposure?"

## Feedback received

- [TODO: who] — [TODO: YYYY-MM-DD] — [TODO: what was said, close to their words] — Jira *Feedback* issue [TODO: SCRUM-xx]

> **ℹ️ Guidance.** Who, when and what, one line per item, recorded the same day and as close to the investor's words as you can. Each item is a Jira *Feedback* issue (§8.2) and also appears under *Feedback received* on the sprint's review page. If there was no feedback, say so and keep the status Open.

> **✏️ Example (illustrative — replace).**
>
> - Investor 1 (teaching staff) — YYYY-MM-DD (Sprint 1 review) — "Showing ineligible colleagues with the reason may expose their data." — SCRUM-xx

## Decision

[TODO: what was decided, and when]

> **ℹ️ Guidance.** One or two sentences. If the decision was revised, state the previous decision and the new one.

> **✏️ Example (illustrative — replace).** Revised on YYYY-MM-DD: from A (ineligible candidates with the reason) to C (ineligible candidates with the rule code only).

## Rationale

[TODO: why this option, which feedback or evidence tipped it, why the others were rejected]

> **ℹ️ Guidance.** §5.7 requires the rationale of a revised decision to be documented. Name the feedback items and evidence that tipped it, and give one reason for rejecting each other option.

> **✏️ Example (illustrative — replace).** The feedback from Investor 1 (SCRUM-xx) showed that A exposes more of colleagues' data than the duty chief needs to fill a gap. C keeps the reason and follows data minimisation. B was rejected because, when nobody is eligible, the duty chief would not know why.

## Consequences

[TODO: what changes (requirements, pages, stories, ADRs, tests) and any new risk or obligation]

> **ℹ️ Guidance.** List every change, so that the *What changed* cell on **Investor Decision Log** can be filled from here. Include what it costs: new stories, tests or risks.

> **✏️ Example (illustrative — replace).** RF16 changes on **Requirements and Quality Attributes**, and the duty chief's row in the access-control matrix on **09 · Security and Privacy Assessment**. New story to update the candidate list (SCRUM-xx). New obligation: the explanation of a rule code (RF22) reveals no more than the code itself.

## Links

- Jira *Decision* issue: [TODO: SCRUM-xx]
- Jira *Feedback* issues: [TODO: SCRUM-xx, SCRUM-xx]
- Affected pages: [TODO: bold page titles]
- ADR (if technical): [TODO: NNNN · <decision>, or —]
- Row on **Investor Decision Log**: [TODO: D-NN]

> **ℹ️ Guidance.** A technical decision also gets an ADR in `docs/adr/`, listed on **08 · Architecture Decision Records** and written as described in **Architecture Decision Records (README)** (`docs/adr/README.md`); the ADR links back to this page. A product decision needs no ADR.

> **✏️ Example (illustrative — replace).** Jira: SCRUM-xx (*Decision*), SCRUM-xx (*Feedback*). Affected pages: **Requirements and Quality Attributes**, **09 · Security and Privacy Assessment**, **Sprint 1 · Review**. ADR: — (product decision). Row: D-01.

## Done when
- [ ] Status, decision owner, context, at least two options and the question are filled before the review
- [ ] Feedback recorded the same day, each item linked to a Jira *Feedback* issue
- [ ] Decision, rationale and consequences filled, and the row on **Investor Decision Log** updated to match
- [ ] Links cover the Jira *Decision* issue, every affected page and, for a technical decision, the ADR
- [ ] Reviewed by another team member
- [ ] Guidance and example blocks removed
- [ ] Status set to Done
