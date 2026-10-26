# 0009 · Use a transactional outbox and idempotent consumers

| Field | Value |
|---|---|
| Date | 2026-10-02 |
| Status | Proposed |
| Deciders | Diogo Pereira, Diogo Sousa, Pedro Costa, Diogo Costa |

## Context

RabbitMQ delivers at least once (ADR 0005). A crash between "write assignment" and "publish event" would leave confirmation blocked forever. A duplicate `AvaliacaoPedida` must not create two evaluations (RNF05).

## Decision

Each service writes domain state and an `OUTBOX_EVENT` row in the same database transaction. A relay publishes unpublished rows to RabbitMQ and marks `publishedAt`. Consumers store `eventId` in `PROCESSED_EVENT` in the same transaction as the effect and ignore a second delivery. After three delayed retries the message goes to the DLQ.

## Alternatives considered

| Alternative | Why rejected |
|---|---|
| Publish to RabbitMQ in the HTTP handler, then commit | A broker outage fails the user request, or a commit-then-publish gap loses the event |
| At-most-once delivery | Silent loss of evaluations, which RF24 cannot tolerate |

## Consequences

- **Positive:** restarting a service republishes pending outbox rows; duplicates are no-ops.
- **Negative:** a relay process and two extra tables per database; operators must watch the DLQ.
- **Trade-offs:** we accept at-least-once plus idempotency instead of a distributed transaction.

## Assignment requirement served

§6.3 (retries, duplicates, failures).

## Related

- Jira: SCRUM-39
- Requirements: RNF05, RF24, RF37
- Supersedes: —
- Other ADRs or pages: 0005, 0008
