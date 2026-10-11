# 01 · Problem and Opportunity

| Field | Value |
|---|---|
| Status | Draft |
| Owner | [TODO: name] |
| Contributors | [TODO: names] |
| Deliverable | Sprint 1 · #1 Problem and Opportunity Report |
| Sources | Assignment §3, §5.1, §7.1.2 #1 |
| Confluence parent | 1 · Discovery |
| Labels | `sprint-1`, `deliverable-01`, `discovery` |
| Jira | [TODO: SCRUM-xx] |
| Last updated | YYYY-MM-DD |

> **ℹ️ About this page.** The Problem and Opportunity Report: which community we serve, what problem it has, who is affected, what evidence we hold and which assumptions we still have to test. It satisfies Sprint 1 deliverable #1 — §7.1.2 asks for Target community; Problem statement; Stakeholder map; Problem evidence; Assumptions and risks — plus the §3 scope choices and the §5.1 problem-discovery items; the team and the investors (teaching staff) read it.

> **ℹ️ Guidance.** Where each §7.1.2 item lives: Target community, Problem statement and Stakeholder map have their own sections; Problem evidence is the Evidence section; Assumptions and risks are the last two sections. Portuguese terms (*piquete*, *quadro ativo*, *Corpo de Bombeiros Voluntários*) are defined in **Glossary**.

## Summary — the four scope choices

| Choice | Our answer |
|---|---|
| Community | [TODO: one line] |
| Problem | [TODO: one line] |
| Primary users | [TODO: one line] |
| Measurable outcome | [TODO: one or two metrics] |

> **ℹ️ Guidance.** Assignment §3 asks for "A clearly identified community" · "A specific problem affecting that community" · "A primary group of users or beneficiaries" · "A measurable outcome that the product intends to improve". One line per choice, consistent with the sections below; fill this table last.

> **✏️ Example (illustrative — replace).**
> | Choice | Our answer |
> |---|---|
> | Community | The active roster (*quadro ativo*) of a *Corpo de Bombeiros Voluntários* ([TODO: which corps]) and the municipality that depends on its readiness |
> | Problem | The *piquete* (standby duty roster) is composed by phone and WhatsApp, from memory; nobody can check at that moment whether the person accepting holds a valid certification, a driving licence compatible with the vehicle and the availability they declared; on high fire-risk days the need to reinforce is noticed too late |
> | Primary users | The duty chief who composes the *piquete* |
> | Measurable outcome | Median time from detecting a gap to filling it with a verified-qualified person; % of days on which the *piquete* met the composition required for that day's risk level |

## Target community

- Who: [TODO: the named corps and the group inside it]
- How many: [TODO: number of active-roster members, with source]
- Where: [TODO: municipality]
- How we reach them: [TODO: contact role (e.g. CMD-01), agreed visits, channel]

> **ℹ️ Guidance.** Name it: a market segment is not a community. "Portuguese fire corps" is a segment; one named corps with its active roster and its municipality is a community. Record how you reach them, because access limits your research (document the limits on **03 · User Research**).

> **✏️ Example (illustrative — replace).** "The ~[TODO: number] members of the active roster of a *Corpo de Bombeiros Voluntários* ([TODO: which corps]), and, by extension, the municipality that depends on its readiness."

## Problem statement

[TODO: one sentence — subject, pain, frequency]

[TODO: one paragraph — when it happens, who is affected, what it costs today, what makes it worse]

> **ℹ️ Guidance.** First one sentence with subject, pain and frequency; then one paragraph of context. State a frequency only if the Evidence section supports it; otherwise point to the assumption (A1). Stay within readiness: Escala never dispatches vehicles or handles incidents.

> **✏️ Example (illustrative — replace).** "Duty chiefs at [TODO: corps] fill *piquete* gaps by phone and WhatsApp, from memory, without being able to check at that moment whether the person accepting is qualified for the vehicle — [TODO: frequency from evidence, or 'we assume at least weekly (A1)']."
>
> Nobody can confirm, when someone accepts, that their certification (e.g. TAS) is valid, that their driving licence matches the vehicle class and that they are available as declared. On high fire-risk days the need to reinforce the *piquete* is noticed too late. [TODO: what this costs, with evidence IDs.]

## Stakeholder map

| Stakeholder | Relation to product (user / buyer / affected / regulator / data provider) | Interest (H/M/L) | Influence (H/M/L) | What they need | Engagement (manage closely / keep satisfied / keep informed / monitor) |
|---|---|---|---|---|---|
| [TODO: stakeholder] | [TODO: relation] | [TODO: H/M/L] | [TODO: H/M/L] | [TODO: need] | [TODO: engagement] |

[DIAGRAM: interest × influence quadrant, draw.io]

> **ℹ️ Guidance.** List everyone the problem or the product touches, not only users; keep "affected, not users" explicit. Engagement follows the quadrant: high influence and high interest → manage closely; high influence, low interest → keep satisfied; low influence, high interest → keep informed; low and low → monitor. Revise the values after research.

> **✏️ Example (illustrative — replace).**
> | Stakeholder | Relation to product (user / buyer / affected / regulator / data provider) | Interest (H/M/L) | Influence (H/M/L) | What they need | Engagement (manage closely / keep satisfied / keep informed / monitor) |
> |---|---|---|---|---|---|
> | Volunteer firefighter | user | H | M | Know in advance when they are on duty; see their own statutory-hours progress | keep informed |
> | Duty chief | user (primary) | H | H | Fill gaps quickly with verified-qualified people | manage closely |
> | Commander | user | H | H | No vehicle unavailable for lack of a qualified crew; statutory hours followed during the year | manage closely |
> | Association board | buyer (to confirm) | M | H | [TODO: confirm who decides and pays] | keep satisfied |
> | Municipal civil protection | affected | M | M | [TODO: confirm] e.g. confidence in the corps' readiness on high-risk days | keep informed |
> | ANEPC | regulator (verify) | L | H | [TODO: which rules it sets that the product must respect] | keep satisfied |
> | IPMA | data provider | L | M | Source cited and usage notified to `webmaster@ipma.pt`, per its terms of use | monitor |
> | Residents of the municipality | affected, not users | M | L | Vehicles that leave with a qualified crew | monitor |

## Current practices and alternatives

- Today's practice: [TODO: how the *piquete* is composed and gaps are filled, step by step, as observed — whiteboard, WhatsApp, phone — with note IDs]
- Software in use: [TODO: which tool(s) the corps uses, if any, and what they don't solve]

> **ℹ️ Guidance.** Describe what people actually do today, from observation and interviews, not what you imagine. Summarise the software that exists in one or two lines; the full comparison lives on **02 · Market and Competitor Analysis**.

> **✏️ Example (illustrative — replace). Fictional example to show the format. Never copy into real notes.** The week's *piquete* is written on a whiteboard at the station (OBS-01). When a member drops out, the duty chief posts in the corps' WhatsApp group and then phones members from memory, most reliable first (CS-01). Certifications and licences are not checked at that moment (CS-01). Nobody mentioned software for this in the closing question (CS-01, B-01).

## Evidence

| Claim | Evidence | Source (note ID, document or URL) | Type (observed / interview / document / assumption) |
|---|---|---|---|
| [TODO: pain claim] | [TODO: what was seen, said or read — paraphrased] | [TODO: OBS-xx / B-xx / document / URL] | [TODO: type] |

> **ℹ️ Guidance.** Every pain claim made on this page has a source here or is listed as an assumption. §5.3: "Limitations in access to real users must be documented. Teams must not fabricate user-research evidence."

> **✏️ Example (illustrative — replace). Fictional example to show the format. Never copy into real notes.**
> | Claim | Evidence | Source (note ID, document or URL) | Type (observed / interview / document / assumption) |
> |---|---|---|---|
> | *Piquete* gaps are filled by phone | Observed during one composition | OBS-01 | observed |
> | Annual statutory service is ≥275 h, of which ≥140 h emergency/standby and ≥70 h training | bombeiros.pt FAQ (verify the article in DL 241/2007) | [TODO: URL, accessed YYYY-MM-DD] | document |
> | Last-minute gaps happen at least weekly | Not yet evidenced | A1 | assumption |

## Consequences of leaving it unresolved

[TODO: 3–5 bullets — what happens to the community if nothing changes, each with an evidence or assumption ID]

> **ℹ️ Guidance.** Think readiness (a vehicle that cannot leave), safety (an unqualified crew), people (the same members overloaded) and compliance (statutory hours). Tie each consequence to the Evidence section or to an assumption; do not claim numbers you cannot source.

> **✏️ Example (illustrative — replace).**
> - A vehicle stays in the station because nobody found out, in time, who was qualified and available to crew it (A1, A2).
> - A member whose TAS has expired crews an ambulance and nobody notices at the moment of assignment (A2).
> - On a high fire-risk day, reinforcement is decided too late (A4).
> - Statutory-hours shortfalls are discovered only at year end (A6).

## Assumptions

| ID | Assumption | How we test it | Status (untested / validated / refuted / inconclusive) | Evidence | If false, then… |
|---|---|---|---|---|---|
| A1 | Last-minute gaps happen at least weekly | Observation (**Observation Grid**); firefighter guide Q2, Q5 and duty chief guide Q1, Q2 (**Interview Guides and Consent**) | untested | — | Gap-filling speed is not the main pain; revisit the problem statement and the principal journey |
| A2 | Nobody can check certification validity at the moment of assignment | Duty chief guide Q3, Q5; observation | untested | — | The eligibility check is not a differentiator; revisit the value proposition on **02 · Market and Competitor Analysis** |
| A3 | Firefighters accept using a phone app for this | Firefighter guide Q1 (indirectly: channels already used); prototype testing with firefighters | untested | — | [TODO: e.g. only the duty chief uses Escala; firefighters' input reaches it another way] |
| A4 | Fire risk changes the required *piquete* composition | Duty chief guide Q4; commander guide Q3; the internal regulation | untested | — | **The IPMA integration must change** |
| A5 | A written internal regulation exists | Commander guide Q2, Q3; closing question "Could we see the internal regulation?" | untested | — | Minimum rest (RN07) and risk-driven composition (RN10) are agreed with the commander, recorded as assumptions and kept configurable |
| A6 | Firefighters don't know how many statutory hours they have done | Firefighter guide Q4; commander guide Q1 | untested | — | Statutory-hours tracking (RF04) loses priority |

> **ℹ️ Guidance.** Keep the IDs A1–A6 stable: **03 · User Research** and **Research Synthesis and Decisions** refer to them. Update Status and Evidence (note IDs) after every research session; a refuted assumption is a result, not a failure. Add A7, A8… as new assumptions appear.

> **✏️ Example (illustrative — replace). Fictional example to show the format. Never copy into real notes.**
> | ID | Assumption | How we test it | Status (untested / validated / refuted / inconclusive) | Evidence | If false, then… |
> |---|---|---|---|---|---|
> | A4 | Fire risk changes the required *piquete* composition | Duty chief guide Q4; commander guide Q3 | inconclusive | CS-01 described adding a crew on a maximum-risk day; CMD-01 said the rule is not written down | The IPMA integration must change |

## Risks

| Risk | Type (product / research / technical / ethical) | Likelihood | Impact | Mitigation |
|---|---|---|---|---|
| [TODO: risk] | [TODO: type] | [TODO: H/M/L] | [TODO: H/M/L] | [TODO: mitigation and owner] |

> **ℹ️ Guidance.** Cover the product and the project, not only technology; include at least one research and one ethical risk. Risky items go first in the backlog (deck M6: "Risky, uncertain items go first — a risk-driven approach surfaces early failure.").

> **✏️ Example (illustrative — replace).**
> | Risk | Type (product / research / technical / ethical) | Likelihood | Impact | Mitigation |
> |---|---|---|---|---|
> | No access to a corps for observation before the Sprint 1 review | research | M | H | Book the observation date in week A; if it fails, document the limitation on **03 · User Research** |
> | The internal regulation (minimum rest, risk-driven composition) cannot be obtained | product | M | H | Keep RN07 and RN10 configurable and marked as assumptions |
> | IPMA open data unavailable or changed | technical | L | M | Show the last value with its age; beyond a threshold assume the higher risk level; IPMA simulator for tests |
> | Research notes expose colleagues to the command | ethical | M | H | Role codes only, no recording, consent script says nothing is reported to the command |

## Done when
- [ ] Community named
- [ ] Each pain claim sourced or listed as an assumption
- [ ] Every assumption has a status
- [ ] Reviewed by another team member
- [ ] Guidance and example blocks removed
- [ ] Status set to Done
