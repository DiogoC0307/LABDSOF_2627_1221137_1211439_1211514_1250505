# MVP, Success Criteria and Ethical Engagement

| Field | Value |
|---|---|
| Status | Draft |
| Owner | [TODO: name] |
| Contributors | [TODO: names] |
| Deliverable | Sprint 1 · #4 Product Vision (child page) |
| Sources | Assignment §5.4, §5.5, §7.1.2 #4, §7.1.3 |
| Confluence parent | 04 · Product Vision |
| Labels | `sprint-1`, `deliverable-04`, `product`, `mvp`, `ethics` |
| Jira | [TODO: ESC-xx] |
| Last updated | YYYY-MM-DD |

> **ℹ️ About this page.** What the MVP includes and excludes, how its success is measured, and the ethical engagement mechanism. It covers the "MVP definition" and "Success criteria" items of Sprint 1 deliverable #4 Product Vision (§7.1.2), the MVP boundary and measurable success criteria of §5.4, and the five explanations required by §5.5; investors read it to judge whether the MVP is the smallest credible experiment.

## MVP definition

> **ℹ️ Guidance.** In one paragraph, describe the smallest credible product experiment: the hypothesis it tests, the journey it delivers end to end, for whom, and which result would make you change course. It must answer the Sprint 1 review question: "Is this a valuable problem, and is the proposed MVP the smallest credible product experiment?"

[TODO: the MVP in one paragraph]

> **✏️ Example (illustrative — replace).** The MVP tests whether a duty chief fills *piquete* gaps faster, and only with verified-qualified people, when eligibility is checked at the moment of assignment against today's fire risk. It delivers J1 end to end and the risk alert of J2 (see **User Journeys**) for one corps, with mocked authentication. If the median time to fill does not improve in the simulated scenario, [TODO: what we change].

## In scope

> **ℹ️ Guidance.** Only what the MVP needs to test its hypothesis; every item traces to requirement IDs on **Requirements and Quality Attributes**. An item that cannot say why it is needed moves to Out of scope.

| Item | Why it is needed | Requirement IDs |
|---|---|---|
| [TODO: item] | [TODO: why the experiment needs it] | [TODO: RF/RNF IDs] |

> **✏️ Example (illustrative — replace).**
>
> | Item | Why it is needed | Requirement IDs |
> |---|---|---|
> | Declare unavailability by form or free text, with confirmation | Starts J1; the free text is the AI-assisted workflow (§5.6) | RF12, RF13, RF31, RF32 |
> | Eligibility check with reasons at assignment | The differentiator: roster quality, not roster listing | RF16, RF21, RF24 |
> | Fire risk from IPMA and the required composition | Reinforcement on high-risk days; the external integration (§6.5) | RF25, RF27, RF28 |
> | Statutory-hours progress | The ethical engagement mechanism (§5.5) | RF04 |

## Out of scope

> **ℹ️ Guidance.** Saying no is part of the MVP: list what investors might expect, why it is excluded, and whether it may come later (match the version to **Product Roadmap (GO)**). On authentication, the §6.6 footnote says: "For the MVP it is acceptable that the authentication mechanism is mocked as to allow the team more time to tackle higher-value user journeys".

| Item | Why excluded | Maybe later (version) |
|---|---|---|
| [TODO: item] | [TODO: reason] | [TODO: version, or "never"] |

> **✏️ Example (illustrative — replace).**
>
> | Item | Why excluded | Maybe later (version) |
> |---|---|---|
> | Automatic roster generation | An optimisation problem; not needed to test the hypothesis | v2 |
> | Incident handling | A different domain, and safety-critical; outside the product boundary | Never |
> | Multi-corps | One corps is enough for the experiment | v2 |
> | Real authentication | Mocked with real roles (§6.6 footnote) | [TODO: version] |

## Product boundary

**Readiness, never dispatch.** Escala never triggers vehicles or handles incidents; if it is down, the corps goes back to the phone.

> **ℹ️ Guidance.** Keep this statement and check every backlog item against it: a story that touches dispatch or incidents is out of scope. Say where users see the boundary in the product.

[TODO: where the boundary is shown to users, e.g. onboarding or the help page]

## Success criteria

> **ℹ️ Guidance.** Include the two outcome metrics from **01 · Problem and Opportunity**, a safety metric and an AI metric. A baseline comes from research (e.g. **Observation Grid** sessions) or is marked "to measure"; never guess it. Say whether the product measures it itself.

| Metric | Baseline | Target | How measured (in-product?) | When |
|---|---|---|---|---|
| [TODO: metric] | [TODO: value and source, or "to measure"] | [TODO: target] | [TODO: method; in-product Y/N] | [TODO: sprint or date] |

> **✏️ Example (illustrative — replace).**
>
> | Metric | Baseline | Target | How measured (in-product?) | When |
> |---|---|---|---|---|
> | Median minutes from gap detection to qualified confirmation | [TODO: from observation sessions] | [TODO: target] | Timestamps of gap detection and confirmation (in-product: Y) | [TODO: e.g. Sprint 3, simulated scenario] |
> | % of days the *piquete* met the required composition | [TODO: to measure] | [TODO: target] | Daily planned composition vs the composition required for the RCM level (in-product: Y) | [TODO: sprint] |
> | Confirmed assignments with an expired certification | [TODO: to measure] | 0 — impossible by construction | Proven by automated test, e.g. `TC-ESC-31-01` (in-product: N) | Every CI run |
> | % of AI extractions accepted without correction | [TODO: `dateparser` + rules result] | ≥ 85% | Extractions confirmed without edits / all extractions (in-product: Y) | [TODO: Sprint 3 AI evaluation] |

## Counter-metric

**Distribution of extra duty per person**, so that filling gaps faster does not overload the same people.

> **ℹ️ Guidance.** A counter-metric catches the harm that optimising the success criteria could cause. Say how it is measured, which threshold triggers action, and who acts.

[TODO: measurement, threshold, who acts]

> **✏️ Example (illustrative — replace).** Each month Escala counts the cover assignments of each member of the active roster (*quadro ativo*); if one member takes more than [TODO: threshold] of them, the commander sees a warning on the dashboard.

## Ethical engagement mechanism

> **ℹ️ Guidance.** §5.5 requires at least one mechanism that encourages constructive participation. Describe it in two or three sentences (what the user sees, who else sees it, which outcome it serves), then answer each of the five §5.5 questions below in 2–4 sentences.

[TODO: the mechanism]

> **✏️ Example (illustrative — replace).** Each member sees their individual progress on the statutory hours (≥275 h, of which ≥140 h emergency/standby and ≥70 h training — verify the article in DL 241/2007), with an optional reminder when they fall behind. The command sees the same progress per member; it is never compared between peers and never ranked.

### 1. Which behaviour the mechanism intends to encourage.

[TODO: answer]

> **✏️ Example (illustrative — replace).** Declaring availability and taking standby slots steadily through the year, instead of discovering a shortfall at year end.

### 2. Why that behaviour benefits the user or community.

[TODO: answer]

> **✏️ Example (illustrative — replace).** The member meets the statutory obligation without last-minute pressure on job and family; the corps keeps qualified people available across the year, so fewer vehicles lack a crew.

### 3. How misuse or manipulation is prevented.

[TODO: answer]

> **✏️ Example (illustrative — replace).** Hours come only from assignments confirmed in Escala, never from self-declaration, and every change is audited by `escala-api`; there is no ranking or reward, so claiming unneeded slots gains nothing. [TODO: how training hours are recorded]

### 4. Whether the mechanism could exclude, expose, or discourage users.

[TODO: answer]

> **✏️ Example (illustrative — replace).** It could expose members with less availability (shift work, family) to the command, or discourage those far behind. Mitigations: no peer comparison and neutral wording (e.g. "N h remaining"). [TODO: how members without a smartphone see their progress]

### 5. How users can control or opt out of the mechanism.

[TODO: answer]

> **✏️ Example (illustrative — replace).** Reminders are off by default and can be switched on or off at any time; the member can hide the progress card. [TODO: whether the command's view can be turned off or is required by the corps' regulation — verify]

## Rejected alternatives

> **ℹ️ Guidance.** List the mechanisms you considered and rejected, each with its reason. §5.5: "Points, badges, and leaderboards should not be introduced without a clear connection to the product's desired outcomes."

- [TODO: alternative — reason for rejecting it]

> **✏️ Example (illustrative — replace).**
>
> - **Public leaderboard of hours** — rejected: it turns an obligation into competition and pressures members with less availability; no clear connection to the outcome metrics (§5.5).
> - **Badges for extra slots** — rejected: they reward the overload that the counter-metric exists to prevent.

## Done when
- [ ] The MVP definition answers the Sprint 1 review question
- [ ] Every in-scope item has requirement IDs; every out-of-scope item has a reason
- [ ] Every success criterion has a baseline (or "to measure"), a target, a method and a date
- [ ] All five §5.5 questions are answered and at least one rejected alternative is recorded
- [ ] Reviewed by another team member
- [ ] Guidance and example blocks removed
- [ ] Status set to Done
