# Investor Decision Log

| Field | Value |
|---|---|
| Status | Draft |
| Owner | [TODO: name] |
| Contributors | [TODO: names] |
| Deliverable | — |
| Sources | Assignment §5.7, §8.2 |
| Confluence parent | 6 · Investor Decisions |
| Labels | `investor`, `decision` |
| Jira | [TODO: SCRUM-xx] |
| Last updated | YYYY-MM-DD |

> **ℹ️ About this page.** The single list of product and technical decisions the team takes to the investors (the teaching staff): what was asked, what they said, and whether the decision was kept or revised. It is the evidence for investor engagement (Assignment §5.7); the team updates it at every Sprint Review and the investors read it to see what their feedback changed.

> **⚠️ Rule.** Assignment §5.7: *"At least one product or technical decision must be revised in response to investor feedback. The change and its rationale must be documented."* By the final submission, at least one row here has *Outcome* **revised**, with its rationale and what changed.

## Log

| ID | Date raised | Decision | Options | Investor feedback (date, who) | Outcome (kept / revised) | Rationale | What changed | Jira |
|---|---|---|---|---|---|---|---|---|
| D-01 | [TODO: YYYY-MM-DD] | Does the duty chief see ineligible candidates with the reason, or only eligible ones? (transparency vs exposure of colleagues' data) | A: ineligible candidates shown with the reason (RF16) · B: only eligible candidates shown | [TODO: YYYY-MM-DD, investor] | [TODO: kept / revised, or "— (open)"] | [TODO: why] | [TODO: pages, requirement IDs, ADRs] | [TODO: SCRUM-xx] |
| D-02 | [TODO: YYYY-MM-DD] | When nobody is eligible, is the assignment blocked, or is a recorded exceptional assignment allowed? (a vehicle stuck vs a documented exception) | A: block the assignment · B: allow an exceptional assignment recorded with a reason (RF19) | [TODO: YYYY-MM-DD, investor] | [TODO: kept / revised, or "— (open)"] | [TODO: why] | [TODO: pages, requirement IDs, ADRs] | [TODO: SCRUM-xx] |

Decision pages: [MACRO: Children display]

Jira: [JIRA: project = SCRUM AND issuetype in (Decision, Feedback) ORDER BY created DESC]

> **ℹ️ Guidance.** One row per decision taken to the investors, numbered D-01, D-02… and never reused; the detail lives on the decision's own page, a child of this page. D-01 and D-02 are illustrative proposals for the Sprint 1 review: keep, reword or replace them. Until the investors answer, *Outcome* is "— (open)"; *What changed* names every page, requirement ID, ADR or Jira issue that changed.

> **✏️ Example (illustrative — replace).** Row D-01 after the Sprint 1 review, matching the example on **Sprint 1 · Review**:
>
> | ID | Date raised | Decision | Options | Investor feedback (date, who) | Outcome (kept / revised) | Rationale | What changed | Jira |
> |---|---|---|---|---|---|---|---|---|
> | D-01 | YYYY-MM-DD | Does the duty chief see ineligible candidates with the reason, or only eligible ones? | A: with the reason · B: only eligible · C: with the rule code only | YYYY-MM-DD, Investor 1 (teaching staff): "Showing ineligible colleagues with the reason may expose their data." | revised: from A to C | C keeps the transparency the duty chief needs to fill the gap and exposes less of colleagues' data; B was rejected because, when nobody is eligible, the duty chief would not know why | RF16 on **Requirements and Quality Attributes**; access-control matrix on **09 · Security and Privacy Assessment** | SCRUM-xx (*Decision*), SCRUM-xx (*Feedback*) |

## How we use this page

- **Before every Sprint Review**, bring 1–2 decisions that are deliberately left open, each with its own page and a prepared *Question to the investors*. List them on the sprint's review page (e.g. **Sprint 1 · Review**, under *Decisions we asked the investors about*).
- **On the day of the review**, record the feedback the same day: fill *Feedback received* on the decision page, create one Jira *Feedback* issue per item (§8.2), and update the row above.
- **One page per decision**, created from **Decision — Template** as a child of this page and titled `D-NN · <decision as a short question>`. Each decision also has a Jira *Decision* issue (§8.2).
- **When a decision is revised**, change the affected pages first (requirements, security, backlog; an ADR if the decision is technical), then fill *Rationale* and *What changed* here.

> **ℹ️ Guidance.** A decision is worth bringing when research cannot settle it and the investors' view on value or risk can. Decisions that come up in meetings are recorded in the minutes (see **Meeting Minutes — Template**) and get a row here only when they are taken to the investors.

> **✏️ Example (illustrative — replace).** Sprint 1 review: D-01 and D-02 brought open. Feedback was recorded the same evening as SCRUM-xx and SCRUM-xx. D-01 was revised (row above); D-02 stayed open and goes to the Sprint 2 review with a narrower question. Page titles: `D-01 · Show ineligible candidates with the reason?`, `D-02 · Allow an exceptional assignment when nobody is eligible?`

## Done when
- [ ] Every decision taken to the investors has a row, a page from **Decision — Template** and a Jira *Decision* issue
- [ ] Every row with investor feedback has the date, who gave it, an outcome (kept / revised) and a linked Jira *Feedback* issue
- [ ] At least one decision is revised, with its rationale and what changed (Assignment §5.7)
- [ ] Reviewed by another team member
- [ ] Guidance and example blocks removed
- [ ] Status set to Done
