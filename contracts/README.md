# Contracts

Source of truth for the REST API and the RabbitMQ events.

| File | Used by |
|---|---|
| [`openapi.yaml`](openapi.yaml) | `apps/web`, `apps/escala-api`, contract tests |
| [`events/`](events/) | `escala-api` and `elegibilidade-worker` |

Change a contract in the same pull request as the producer and the consumer. Envelope fields are defined in [`events/envelope.schema.json`](events/envelope.schema.json).
