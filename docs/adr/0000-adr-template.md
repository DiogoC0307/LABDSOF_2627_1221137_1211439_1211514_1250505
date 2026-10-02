# NNNN · <Decision>

| Field | Value |
|---|---|
| Date | YYYY-MM-DD |
| Status | Proposed |
| Deciders | [TODO: names] |

> **ℹ️ Guidance.** Copy this file to `docs/adr/NNNN-kebab-case-title.md` with the next free number (see the [ADR README](README.md)). Title the decision as a short active phrase, e.g. `0005 · Use RabbitMQ as message broker`. Fill it in **the day the decision is taken** and use that day as the Date. Status is one of Proposed, Accepted, Superseded by NNNN or Deprecated. Delete every guidance block and the example at the end before merging.

## Context

> **ℹ️ Guidance.** State the problem and the forces behind it: which requirement, quality attribute or constraint forces a choice now. Cite IDs (`RF`, `RNF`, `RN`) and facts, not opinions. Two to five sentences.

[TODO: context]

## Decision

> **ℹ️ Guidance.** One or two sentences in the active voice ("We will …"). Add only the detail needed to implement it, such as where it lives in the repository or what configuration it needs.

[TODO: decision]

## Alternatives considered

> **ℹ️ Guidance.** List at least one rejected alternative, each with the concrete reason it lost. "Not chosen" is not a reason.

| Alternative | Why rejected |
|---|---|
| [TODO: alternative] | [TODO: reason] |

## Consequences

> **ℹ️ Guidance.** Say what becomes easier and what becomes harder, including any new work the decision creates (tests, operations, follow-up ADRs).

- **Positive:** [TODO: benefits]
- **Negative:** [TODO: costs and new obligations]
- **Trade-offs:** [TODO: what we give up, and why that is acceptable]

## Assignment requirement served

> **ℹ️ Guidance.** Name the assignment section(s) this decision helps satisfy, e.g. §6.2 (at least two independently deployable backend components) or §6.3 (at least one meaningful asynchronous workflow).

[TODO: e.g. §6.2]

## Related

- Jira: [TODO: ESC-xx]
- Requirements: [TODO: RF / RNF / RN IDs]
- Supersedes: [TODO: NNNN, or —]
- Other ADRs or pages: [TODO: e.g. 0009, or **Investor Decision Log** entry D-xx]

---

> **✏️ Example (illustrative — replace).** A filled ADR that shows the expected level of detail. Its content is a proposal, not a decision the team has taken. Delete this example when you copy the template.
>
> ```markdown
> # 0005 · Use RabbitMQ as message broker
>
> | Field | Value |
> |---|---|
> | Date | YYYY-MM-DD |
> | Status | Proposed |
> | Deciders | Member A, Member B, Member C |
>
> ## Context
>
> `escala-api` and `elegibilidade-worker` are deployed independently (ADR 0002)
> and exchange events such as `AvaliacaoPedida` → `AvaliacaoConcluida`.
> The assignment requires at least one meaningful asynchronous workflow that
> handles retries, duplicates, failures and eventual consistency (§6.3).
> An evaluation must never be lost silently, because confirmation is blocked
> without a valid evaluation (RF24, RNF04).
>
> ## Decision
>
> We will use RabbitMQ as the message broker, fed by a transactional outbox
> (ADR 0009). Each event carries an idempotency key. Failed messages go through
> retry queues (5 s / 30 s / 2 min) and then to a dead-letter queue (DLQ).
>
> ## Alternatives considered
>
> | Alternative | Why rejected |
> |---|---|
> | Apache Kafka | We need native DLQ and retries, not replay of event history; RabbitMQ is simpler to operate for a team of 3–4 |
> | Synchronous HTTP between the components | Ties the API's availability to the worker's and buffers nothing when the worker is down; does not meet §6.3 |
>
> ## Consequences
>
> - **Positive:** failed messages end up visible in the DLQ instead of being
>   lost; the API accepts an unavailability without waiting for the
>   evaluation (RNF02); with the worker down, evaluations stay "pending".
> - **Negative:** one more container to run and monitor; delivery is
>   at-least-once, so every consumer must be idempotent (RNF05).
> - **Trade-offs:** we give up event-history replay, which the MVP does not
>   need, in exchange for simpler operation.
>
> ## Assignment requirement served
>
> §6.3 (at least one meaningful asynchronous workflow); supports §6.2
> (independently deployable backend components) and §6.8 (degraded mode).
>
> ## Related
>
> - Jira: ESC-xx
> - Requirements: RF24, RNF02, RNF04, RNF05
> - Supersedes: —
> - Other ADRs or pages: 0002, 0008, 0009
> ```
