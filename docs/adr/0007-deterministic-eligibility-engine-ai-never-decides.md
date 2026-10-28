# 0007 · Keep eligibility deterministic; AI never decides

| Field | Value |
|---|---|
| Date | 2026-10-12 |
| Status | Accepted |
| Deciders | Diogo Pereira, Diogo Sousa, Pedro Costa, Diogo Costa |

## Context

Whether a swap or cover keeps the required functions on a *piquete* is a safety-related decision (RN01–RN08, RN11, RF21, RF23, RF24, RF41, RF43). A language model can help read an unavailability written in free text (RF31–RF33), but it must not grant a swap, choose who is asked to cover, or decide eligibility. At the Sprint 1 progress review the investors asked us to know and explain exactly where AI is used (FB-03, 2026-10-12, recorded as SCRUM-62).

## Decision

We will implement the function and eligibility checks as a versioned, deterministic rules engine inside `elegibilidade-worker`. The engine uses only the facts it receives with each evaluation request. AI is used in one place only: extraction proposals from free text, which the member confirms. Refusal reasons are template text per rule code (RF22), not AI output. The engine version is stored with every evaluation (RF23).

## Alternatives considered

| Alternative | Why rejected |
|---|---|
| Ask the model whether the member may swap or cover | Non-deterministic and impossible to audit; a crew decision cannot depend on a model's guess (RNF04) |
| Let the model write the explanation of a refusal | A second AI use the investors asked us to justify (FB-03); a template per rule code is exact and cannot leak another member's data |
| Run the rules inside `escala-api` | Mixes schedule writes with engine releases; a slow check would block the PWA |

## Consequences

- **Positive:** the same input always gives the same result; every refusal is tied to a rule code; the AI use is easy to explain.
- **Negative:** better extraction never improves eligibility; the engine must be changed when a business rule changes.
- **Trade-offs:** we keep AI off the critical path of applying swaps and covers.

## Assignment requirement served

§5.6 (responsible AI: human review, AI does not take consequential decisions); supports RF23, RF24 and RNF04.

## Related

- Jira: SCRUM-62, SCRUM-40
- Requirements: RF21–RF24, RF31–RF35, RF41, RF43, RNF04, RN01–RN08; RF34 Won't
- Supersedes: —
- Other ADRs or pages: 0002; later 0011 and 0016; **06 · Responsible AI Opportunity Assessment**
- Date evidence: investor feedback FB-03 ("Know and explain clearly where AI is used") was received at the 2026-10-12 review and recorded as SCRUM-62.
- Drafted by Pedro Costa (branch `docs/SCRUM-39-technical`, 2026-10-26); revised for SCRUM-40.
