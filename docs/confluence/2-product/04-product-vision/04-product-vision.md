# 04 · Product Vision

| Field | Value |
|---|---|
| Status | Draft |
| Owner | [TODO: name] |
| Contributors | [TODO: names] |
| Deliverable | Sprint 1 · #4 Product Vision |
| Sources | Assignment §5.4, §7.1.2 #4 |
| Confluence parent | 2 · Product |
| Labels | `sprint-1`, `deliverable-04`, `product` |
| Jira | [TODO: ESC-xx] |
| Last updated | YYYY-MM-DD |

> **ℹ️ About this page.** Entry page for Sprint 1 deliverable #4 (Assignment §5.4, §7.1.2 #4): a one-screen summary of where Escala is going, for whom, and how we will know it works. Investors (the teaching staff) read it at the Sprint 1 review; the team reads it before refining the backlog. The detail lives in the child pages.

> **ℹ️ Guidance.** Assignment §5.4 asks for eight elements: product vision, target users, value proposition, principal user journeys, expected community benefit, MVP boundary, product risks and measurable success criteria. Each has a section below; keep each to one to three sentences and point to the child page that holds the detail. §7.1.2 #4 also requires *Personas; Value proposition; Principal user journeys; Success criteria; MVP definition* — the child pages cover these.

## Vision

> **ℹ️ Guidance.** One sentence describing the positive change Escala should create in the community, not the product or its features. Use the same sentence on **Product Vision Board**.

[TODO: vision in one sentence]

> **✏️ Example (illustrative — replace).** "No fire-corps vehicle stays in the station because nobody could find out, in time, who was qualified and available to crew it."

## Value proposition

> **ℹ️ Guidance.** Copy the one-sentence value proposition from **02 · Market and Competitor Analysis**; do not rewrite it here. If it changes, change it there first, then here.

[TODO: value proposition, copied from **02 · Market and Competitor Analysis**]

> **✏️ Example (illustrative — replace).** "For duty chiefs of Portuguese volunteer fire corps who must fill *piquete* gaps quickly with qualified people, Escala is a readiness-support app that checks certification, driving licence and declared availability at the moment of assignment, against today's fire risk. Unlike roster tools such as Ifprotec, it doesn't just tell you who is on duty; it tells you whether they can actually crew that vehicle, given the risk outside."

## Target users

> **ℹ️ Guidance.** Name the primary user first, then the secondary users, one line each, and say who would pay or adopt if that is someone else. Details, goals and frustrations belong on **Personas**.

[TODO: primary user, secondary users, buyer — link to **Personas**]

> **✏️ Example (illustrative — replace).**
> - **Primary:** the duty chief who composes the *piquete* — Cátia (persona).
> - **Secondary:** firefighters of the active roster (*quadro ativo*), who declare unavailability and apply to open slots — Nuno (persona); the commander, who follows readiness and statutory hours — Rodrigues (persona).
> - **Buyer:** [TODO: the corps' association or the municipality — hypothesis, see **Questions and Effective Answers**].
>
> See **Personas**.

## Principal user journey

> **ℹ️ Guidance.** One line, from trigger to outcome. The as-is and to-be diagrams, steps and exceptions are on **User Journeys**.

[TODO: principal journey in one line — link to **User Journeys**]

> **✏️ Example (illustrative — replace).** J1: unavailability → gap detected → eligible people computed → request → application → confirmation → hours counted. See **User Journeys**.

## Expected community benefit

> **ℹ️ Guidance.** Say who benefits beyond the people who use the app, and how, in two or three sentences. Tie it to the outcome metrics; do not claim numbers you have not measured.

[TODO: expected community benefit]

> **✏️ Example (illustrative — replace).** The municipality that depends on the corps' readiness gets a *piquete* that can crew what the day's fire risk requires, because gaps are filled with verified-qualified people rather than whoever answers the phone first. Firefighters get earlier notice, and extra duty is spread fairly (tracked by the counter-metric on **MVP, Success Criteria and Ethical Engagement**).

## MVP boundary

> **ℹ️ Guidance.** Summarise in two or three lines what the MVP includes, what it deliberately excludes, and the product boundary. The full in-scope and out-of-scope tables are on **MVP, Success Criteria and Ethical Engagement**.

[TODO: MVP boundary summary — link to **MVP, Success Criteria and Ethical Engagement**]

> **✏️ Example (illustrative — replace).** **In:** journey J1 for one corps — eligibility checked against certification, driving licence and declared availability; today's IPMA fire risk (RCM); unavailability in free text with AI extraction and a form fallback; mocked authentication with real roles. **Out:** automatic roster generation, incident handling, multiple corps. **Boundary:** readiness, never dispatch — Escala never triggers vehicles or handles incidents; if it is down, the corps goes back to the phone.

## Success criteria

> **ℹ️ Guidance.** List the metrics with their targets, and name the counter-metric. Baselines, measurement method and timing are on **MVP, Success Criteria and Ethical Engagement**; keep the two pages identical.

[TODO: success criteria summary — link to **MVP, Success Criteria and Ethical Engagement**]

> **✏️ Example (illustrative — replace).**
> - Median minutes from gap detection to a verified-qualified confirmation — baseline [TODO: from observation], target [TODO: value].
> - % of days the *piquete* met the composition required for that day's risk level — baseline [TODO: value], target [TODO: value].
> - Confirmed assignments with an expired certification — target 0 (impossible by construction, proven by test).
> - % of AI extractions accepted without correction — target ≥ 85%.
> - Counter-metric: distribution of extra duty per person.

## Product risks

> **ℹ️ Guidance.** Product risks are about value, usability, adoption and viability; technical risks go in the Risks table on **01 · Problem and Opportunity** (type technical) and, once decided, in an ADR on **08 · Architecture Decision Records**. Reference the assumption (A1–A6 on **01 · Problem and Opportunity**) behind each risk, and give each risk a mitigation you can act on.

| Risk | Likelihood | Impact | Mitigation |
|---|---|---|---|
| [TODO: risk] | [TODO: H/M/L] | [TODO: H/M/L] | [TODO: mitigation] |

> **✏️ Example (illustrative — replace).**
>
> | Risk | Likelihood | Impact | Mitigation |
> |---|---|---|---|
> | Firefighters do not adopt a phone app for this (A3) | Medium | High | Test A3 in interviews and with a clickable prototype; accept free-text unavailability so declaring is as easy as sending a message |
> | Fire risk does not change the required *piquete* composition (A4) | [TODO: after CMD-01 interview] | High | Validate A4 with the commander before building E5; if false, the IPMA integration must change |
> | The internal regulation (rest, risk → composition) cannot be obtained (A5) | Medium | High | Ask the commander early; model RN07 and RN10 as configuration; mark them as assumptions |
> | Existing roster tools already cover eligibility checks | [TODO: after demo] | High | Verify the capability matrix on **02 · Market and Competitor Analysis** before the Sprint 1 review |

## Pages in this section

> **ℹ️ Guidance.** List the child pages with one line each. In Confluence you may replace or complement the list with `[MACRO: Children display]`.

- **Questions and Effective Answers** — the six questions an effective vision answers (deck M2).
- **Product Vision Board** — Roman Pichler's board: vision, target group, needs, product, business goals.
- **Personas** — the three personas and where their goals conflict.
- **User Journeys** — J1 and J2 as-is and to-be, with exceptions.
- **MVP, Success Criteria and Ethical Engagement** — scope, boundary, metrics and the ethical engagement mechanism.

## Done when
- [ ] Every §5.4 element is present: vision, target users, value proposition, principal user journey, expected community benefit, MVP boundary, product risks, measurable success criteria
- [ ] The vision matches **Product Vision Board** and the value proposition matches **02 · Market and Competitor Analysis** word for word
- [ ] Every product risk has a mitigation and, where relevant, an assumption ID
- [ ] Every child page is listed and linked from this page
- [ ] Reviewed by another team member
- [ ] Guidance and example blocks removed
- [ ] Status set to Done
