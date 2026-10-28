# 0008 · Carry the consumer's required state on the event

| Field | Value |
|---|---|
| Date | 2026-10-26 |
| Status | Accepted |
| Deciders | Diogo Pereira, Diogo Sousa, Pedro Costa, Diogo Costa |

## Context

The worker must not read `escala-db` (ADR 0003). If it called `escala-api` back for member facts, the two services would be synchronously coupled and eligibility would fail when the API is busy.

## Decision

Events are self-contained. `AvaliacaoPedida` carries roster flags, age-limit flag (not date of birth), certifications, licence classes, unavailabilities and overlapping assignments. `ExtracaoPedida` is the only event that carries free text. Consumers never call the producer to complete an event. Eventual consistency is visible as assignment states `pending` / `evaluated` / `failed` (and `outdated` when a newer result replaces an older one).

## Alternatives considered

| Alternative | Why rejected |
|---|---|
| Notification events with IDs only, worker fetches the rest | Reintroduces a sync dependency and a privacy leak of names and contacts into the worker |
| Shared database read models | Violates ADR 0003 |

## Consequences

- **Positive:** the worker can evaluate while `escala-api` is restarting; payloads stay minimised (no names, no contacts, no date of birth).
- **Negative:** a large candidate payload; schema changes need a `schemaVersion` and dual-read during rollout.
- **Trade-offs:** we copy facts onto the event rather than query them live.

## Assignment requirement served

§6.2 (coupling); §6.3 (eventual consistency); §6.4 (privacy constraints).

## Related

- Jira: SCRUM-39
- Requirements: RF24, RNF05, RN08
- Supersedes: —
- Other ADRs or pages: 0003, 0005, 0009
- Date evidence: the event schemas in `contracts/events/` and this ADR were written on 2026-10-26 with the technical design (SCRUM-39).
- Drafted by Pedro Costa (branch `docs/SCRUM-39-technical`, 2026-10-26); revised for SCRUM-40.
