# 0002 · Split the backend into two independently deployable components

| Field | Value |
|---|---|
| Date | 2026-10-11 |
| Status | Accepted |
| Deciders | Diogo Pereira, Diogo Sousa, Pedro Costa, Diogo Costa |

## Context

Assignment §6.2 requires at least two independently deployable backend components, with justified boundaries, data ownership and failure handling. After **D-00**, Escala answers two different questions: the published roster and its changes (people, slots, unavailability, swaps, cover) versus slow or external work (function checks, text extraction, IPMA poll). Checks and extraction can be slow or fail without blocking a member's phone (RNF02, §6.8).

## Decision

We will ship two Spring Boot services: `escala-api` (the published roster and its changes) and `elegibilidade-worker` (function checks, extraction, observed risk). They are built from one monorepo (ADR 0004) as separate images and can be started, stopped and rolled back independently. The language of the worker is Java (ADR 0015).

## Alternatives considered

| Alternative | Why rejected |
|---|---|
| One Spring Boot monolith | A stopped eligibility engine would take down roster and unavailability APIs; does not meet §6.2 |
| Three or more backend services (e.g. a separate IPMA poller) | Extra deployables without a third data owner; too much operational load for a team of four |

## Consequences

- **Positive:** if the worker is down, `escala-api` still accepts unavailability, swap requests and cover accepts; applying a swap or filling a cover stays blocked until a valid check exists (RF24).
- **Negative:** two images, two health checks and two databases to operate.
- **Trade-offs:** we give up a single process for a failure boundary that the assignment and RNF02 require.

## Assignment requirement served

§6.2 (independently deployable backend components); supports §6.8 (degraded mode).

## Related

- Jira: SCRUM-39, SCRUM-54
- Requirements: RNF02, RF24, RF36
- Supersedes: —
- Other ADRs or pages: 0003, 0004, 0015; **D-00**
- Date evidence: SCRUM-54 "Setup both Java microservices" was created on 2026-10-11, and ADR 0015 (2026-10-11) already describes two independently deployable components.
- Drafted by Pedro Costa (branch `docs/SCRUM-39-technical`, 2026-10-26); revised for SCRUM-40.
