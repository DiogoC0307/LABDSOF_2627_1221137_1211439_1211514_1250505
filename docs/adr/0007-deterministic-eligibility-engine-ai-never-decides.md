# 0007 · Keep eligibility deterministic; AI never decides

| Field | Value |
|---|---|
| Date | 2026-09-29 |
| Status | Proposed |
| Deciders | Diogo Pereira, Diogo Sousa, Pedro Costa, Diogo Costa |

## Context

Whether a swap or cover keeps the required functions is a safety-related decision (RN01–RN08, RN11, RF21, RF23, RF24, RF41, RF43). A language model can help read free-text unavailability (RF31–RF33) but must not grant a swap, pick who is asked to cover, or write eligibility. **D-00** / RF34: explanations are template text per rule code (RF22), not AI.

## Decision

We will implement function and eligibility checks as a versioned, deterministic rules engine inside `elegibilidade-worker`. The engine reads only facts from `AvaliacaoPedida` (`purpose`: `SWAP`, `COVER` or `ASSIGNMENT`). AI is limited to extraction proposals that the member confirms. Engine version is stored on every evaluation (RF23). RN10 (composition from fire risk) is configuration default **off**.

## Alternatives considered

| Alternative | Why rejected |
|---|---|
| Ask the model whether the member may swap or cover | Non-deterministic, unauditable, and unsafe (RNF04, SCRUM-62) |
| Run the rules inside `escala-api` | Mixes roster writes with engine releases; a slow check would block the PWA |

## Consequences

- **Positive:** the same input always yields the same result; explanations stay tied to rule codes.
- **Negative:** extraction quality does not improve eligibility; the engine must be updated when a business rule changes.
- **Trade-offs:** we keep AI off the critical path of confirmation.

## Assignment requirement served

§5.6 (responsible AI); supports RF23, RF24, RNF04.

## Related

- Jira: SCRUM-39
- Requirements: RF21–RF24, RF31–RF35, RF41, RF43, RNF04, RN01–RN08; RF34 Won't
- Supersedes: —
- Other ADRs or pages: 0002, 0011; **06 · Responsible AI Opportunity Assessment**
