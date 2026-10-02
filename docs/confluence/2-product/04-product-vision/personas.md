# Personas

| Field | Value |
|---|---|
| Status | Draft |
| Owner | [TODO: name] |
| Contributors | [TODO: names] |
| Deliverable | Sprint 1 · #4 Product Vision (child page) |
| Sources | Assignment §5.4, §7.1.2 #4; Deck M3 (Pichler Persona Template) |
| Confluence parent | 04 · Product Vision |
| Labels | `sprint-1`, `deliverable-04`, `product`, `personas` |
| Jira | [TODO: ESC-xx] |
| Last updated | YYYY-MM-DD |

> **ℹ️ About this page.** The three personas Escala is designed for: a firefighter, the duty chief who composes the *piquete* (the primary user) and the commander. It covers the "Personas" item of Sprint 1 deliverable #4 Product Vision (§7.1.2) and the target users of §5.4. The team uses it when writing stories; investors read it at the review.

## How these personas were built

> **ℹ️ Guidance.** Derive each persona from the research notes on **03 · User Research** and cite the note IDs under it ("Based on: B-01, CS-01"). A persona is a design artefact, not evidence: it never replaces a finding on **Research Synthesis and Decisions**. Use placeholder names with "(persona)" and an illustration or initials, never a real person's name or photo. The table follows Roman Pichler's Persona Template (Picture & Name, Details, Goal; romanpichler.com, CC BY-SA 4.0) plus the deck's Frustration and Quote.

**Research notes used:** [TODO: note IDs, e.g. B-01, CS-01, CMD-01]

**Assumptions the personas still rely on:** [TODO: assumption IDs from **01 · Problem and Opportunity** not yet validated, e.g. A3]

Persona tables based on the Persona Template by Roman Pichler (romanpichler.com), licensed under CC BY-SA 4.0.

## Nuno (persona) — firefighter

> **ℹ️ Guidance.** In Details, keep only facts that change design decisions: rank, certifications and their validity, driving licence, job constraints, statutory-hours situation. The Quote is a line in the persona's voice that paraphrases what participants said in the cited notes; it is not a participant quote.

**Based on:** [TODO: note IDs]

| Picture & Name | Details | Goal | Frustration | Quote |
|---|---|---|---|---|
| [IMAGE: illustration or initials avatar — no photos of real people] [TODO: name (persona), age] | [TODO: rank, certifications, licence, job, statutory hours] | [TODO: what he wants to achieve] | [TODO: what gets in the way today] | [TODO: one line grounded in the notes] |

> **✏️ Example (illustrative — replace).**
>
> | Picture & Name | Details | Goal | Frustration | Quote |
> |---|---|---|---|---|
> | [IMAGE: initials avatar "N"] Nuno (persona), 29 | Firefighter 2nd class; TAS valid; heavy-vehicle licence; shift work in a factory; half of the statutory hours still owed | Fulfil the hours without harming job and family | Learns he is on duty from a group message at 11 pm | "I show up. I just need to know in advance." |

## Cátia (persona) — duty chief

> **ℹ️ Guidance.** This is the primary user: her goal should match the outcome metrics on **01 · Problem and Opportunity**. If research changes who composes the *piquete*, update this persona first.

**Based on:** [TODO: note IDs]

| Picture & Name | Details | Goal | Frustration | Quote |
|---|---|---|---|---|
| [IMAGE: illustration or initials avatar — no photos of real people] [TODO: name (persona), age] | [TODO: role, what she composes and when] | [TODO: what she wants to achieve] | [TODO: what gets in the way today] | [TODO: one line grounded in the notes] |

> **✏️ Example (illustrative — replace).**
>
> | Picture & Name | Details | Goal | Frustration | Quote |
> |---|---|---|---|---|
> | [IMAGE: initials avatar "C"] Cátia (persona), 44 | Duty chief; composes the *piquete*; primary user of Escala | A complete, qualified *piquete* every day | On high-risk days doesn't know whom to call first | [TODO: one line grounded in the CS- notes] |

## Rodrigues (persona) — commander

> **ℹ️ Guidance.** The commander is accountable for readiness and statutory hours but rarely composes the *piquete*; capture what he needs to see, and when.

**Based on:** [TODO: note IDs]

| Picture & Name | Details | Goal | Frustration | Quote |
|---|---|---|---|---|
| [IMAGE: illustration or initials avatar — no photos of real people] [TODO: name (persona), age] | [TODO: role, what he reviews and when] | [TODO: what he wants to achieve] | [TODO: what gets in the way today] | [TODO: one line grounded in the notes] |

> **✏️ Example (illustrative — replace).**
>
> | Picture & Name | Details | Goal | Frustration | Quote |
> |---|---|---|---|---|
> | [IMAGE: initials avatar "R"] Rodrigues (persona), 51 | Commander; follows the statutory hours of the active roster (*quadro ativo*) | No vehicle unavailable for lack of a qualified crew | Only discovers statutory-hours problems at year end | [TODO: one line grounded in the CMD- notes] |

## Where goals conflict

> **ℹ️ Guidance.** List each pair of personas whose goals pull in different directions and say how the product handles it; each answer should point to a rule, a feature or a requirement ID on **Requirements and Quality Attributes**. If the product does not resolve a conflict, say so and add it to the product risks on **04 · Product Vision**.

| Between | Conflict | How the product handles it |
|---|---|---|
| [TODO: persona ↔ persona] | [TODO: conflict] | [TODO: rule, feature or requirement ID] |

> **✏️ Example (illustrative — replace).**
>
> | Between | Conflict | How the product handles it |
> |---|---|---|
> | Cátia (persona) ↔ Rodrigues (persona) | Cátia wants the gap filled fast; Rodrigues wants no vehicle crewed by someone unqualified | Confirmation is blocked without a valid eligibility evaluation (RF24, RNF04); speed never overrides eligibility |
> | Cátia (persona) ↔ Nuno (persona) | Cátia must fill last-minute gaps; Nuno needs to know in advance | Cover requests reach eligible members, who apply rather than being ordered (RF15, RF17); the counter-metric tracks extra duty per person |
> | Rodrigues (persona) ↔ Nuno (persona) | Rodrigues follows everyone's statutory hours; Nuno does not want to be compared with colleagues | Hours progress is visible only to the person and the command, never ranked (see **MVP, Success Criteria and Ethical Engagement**) |

## Done when
- [ ] Each persona cites the research notes it is based on
- [ ] The duty chief (primary user) is a persona and matches **01 · Problem and Opportunity**
- [ ] No real names or photos; every Quote is grounded in the cited notes or left as a TODO
- [ ] At least one goal conflict is recorded with how the product handles it
- [ ] Reviewed by another team member
- [ ] Guidance and example blocks removed
- [ ] Status set to Done
