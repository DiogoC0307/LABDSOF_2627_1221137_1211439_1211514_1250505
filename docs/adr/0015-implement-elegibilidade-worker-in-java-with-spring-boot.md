# 0015 · Implement elegibilidade-worker in Java with Spring Boot

| Field | Value |
|---|---|
| Date | 2026-10-11 |
| Status | Proposed |
| Deciders | [TODO: names] |

## Context

Escala has two independently deployable backend components, `escala-api` and `elegibilidade-worker`, which exchange events through RabbitMQ (assignment §6.2; planned ADR 0002). The documentation written at the start of Sprint 1 described `elegibilidade-worker` as a Python component and `escala-api` as a Spring Boot component. No code exists yet for either component, so the language of the worker can still change without a migration.

[TODO: reasons agreed by the team]

## Decision

We will implement `apps/elegibilidade-worker` in Java with Spring Boot, the same stack as `apps/escala-api`. The two remain separate services, each with its own database, and keep communicating through RabbitMQ. Java version: [TODO: Java version], the same for both components.

## Alternatives considered

| Alternative | Why rejected |
|---|---|
| Python worker (original plan) | [TODO: reasons agreed by the team] |

## Consequences

- **Positive:** [TODO: benefits agreed by the team]
- **Negative:**
  - The non-AI baseline planned for the AI evaluation, `dateparser` + rules, is a Python library and has to be replaced by a baseline in Java [TODO: decision pending].
  - The client for the language model becomes a Java library [TODO: which].
  - The Confluence pages that described the worker as Python or named `dateparser` were updated on 2026-10-11 and now carry the same pending-decision marker for the baseline: **Requirements and Quality Attributes**, **07 · Technical Design**, **06 · Responsible AI Opportunity Assessment**, **Sprint 2 · Deliverables** and **Sprint 3 · Deliverables**.
- **Trade-offs:** [TODO: what we give up, and why that is acceptable]

## Assignment requirement served

§6.2 (at least two independently deployable backend components): the two components stay separate services after this change.

## Related

- Jira: SCRUM-54
- Requirements: [TODO: RF / RNF IDs]
- Supersedes: — (no earlier ADR recorded the worker's language)
- Other ADRs or pages: 0002 (planned, not yet written); **Git, Pull Request and Jira Rules** (repository layout, updated for this decision); **08 · Architecture Decision Records** (index, lists this ADR)
