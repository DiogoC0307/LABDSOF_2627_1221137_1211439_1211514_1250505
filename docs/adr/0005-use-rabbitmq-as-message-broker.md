# 0005 · Use RabbitMQ as message broker

| Field | Value |
|---|---|
| Date | 2026-09-26 |
| Status | Proposed |
| Deciders | Diogo Pereira, Diogo Sousa, Pedro Costa, Diogo Costa |

## Context

`escala-api` and `elegibilidade-worker` are deployed independently (ADR 0002) and exchange `AvaliacaoPedida` → `AvaliacaoConcluida` and related events. §6.3 requires a meaningful asynchronous workflow with retries, duplicates, failures and eventual consistency. An evaluation must not be lost silently, because confirmation is blocked without a valid evaluation (RF24, RNF04).

## Decision

We will use RabbitMQ, fed by a transactional outbox (ADR 0009). Each event carries an idempotency key (`eventId`). Failed messages go through retry queues (5 s / 30 s / 2 min) and then to a dead-letter queue (DLQ).

## Alternatives considered

| Alternative | Why rejected |
|---|---|
| Apache Kafka | We need delayed retries and a visible DLQ, not an event log to replay; Kafka is heavier to operate for this team |
| Synchronous HTTP between the components | Ties the API's availability to the worker and buffers nothing when the worker is down; does not meet §6.3 or RNF02 |

## Consequences

- **Positive:** failed messages are visible in the DLQ; the API can accept work without waiting for the worker (RNF02).
- **Negative:** one more container to run; delivery is at-least-once, so every consumer must be idempotent (RNF05).
- **Trade-offs:** we give up event-history replay, which the MVP does not need, in exchange for simpler operation.

## Assignment requirement served

§6.3 (asynchronous workflow); supports §6.2 and §6.8.

## Related

- Jira: SCRUM-39
- Requirements: RF24, RNF02, RNF04, RNF05
- Supersedes: —
- Other ADRs or pages: 0002, 0008, 0009
