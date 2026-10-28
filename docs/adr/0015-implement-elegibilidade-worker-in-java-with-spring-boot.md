# 0015 · Implement elegibilidade-worker in Java with Spring Boot

| Field | Value |
|---|---|
| Date | 2026-10-11 |
| Status | Accepted |
| Deciders | Diogo Pereira, Diogo Sousa, Pedro Costa, Diogo Costa |

## Context

Escala has two independently deployable backend components, `escala-api` and `elegibilidade-worker`, which exchange events through RabbitMQ (assignment §6.2; ADR 0002). The documentation written at the start of Sprint 1 described `elegibilidade-worker` as a Python component and `escala-api` as a Spring Boot component. No code exists yet for either component, so the language of the worker can still change without a migration.

The team works mostly in Java and Spring Boot, and wants one stable framework across both services. The original plan chose Python for its date-parsing and AI libraries; the team judged that consistency across the two services matters more.

## Decision

We will implement `apps/elegibilidade-worker` in Java with Spring Boot, the same stack as `apps/escala-api`. The two remain separate services, each with its own database, and keep communicating through RabbitMQ. Java version: 21, the same for both components (`java.version` in both `pom.xml`).

## Alternatives considered

| Alternative | Why rejected |
|---|---|
| Python worker (original plan) | Two languages, toolchains and build pipelines for a team of four that mostly works in Java; Spring Boot is the framework the team already knows and considers stable, and we expect it to use fewer resources in long-running operation (the team's assessment, not measured) |

## Consequences

- **Positive:** one language, framework and build for both services, so any member can review and change either; shared conventions, libraries and CI steps.
- **Negative:**
  - The non-AI baseline planned for the AI evaluation, `dateparser` + rules, is a Python library and has to be replaced by a baseline in Java (library still to be chosen, SCRUM-54).
  - The worker must reach the language model from Java; resolved later by ADR 0016, which runs the Claude Code CLI as a process behind an `LlmClient` interface.
  - The Confluence pages that described the worker as Python or named `dateparser` were updated on 2026-10-11 and now carry the same pending-decision marker for the baseline: **Requirements and Quality Attributes**, **07 · Technical Design**, **06 · Responsible AI Opportunity Assessment**, **Sprint 2 · Deliverables** and **Sprint 3 · Deliverables**.
- **Trade-offs:** we give up Python's date-parsing and AI libraries and write the non-AI baseline in Java; acceptable because the baseline is small and the model is reached through a separate client.

## Assignment requirement served

§6.2 (at least two independently deployable backend components): the two components stay separate services after this change.

## Related

- Jira: SCRUM-54
- Requirements: RNF02 (the worker stays a separate service)
- Supersedes: — (no earlier ADR recorded the worker's language)
- Other ADRs or pages: 0002; 0016; **Git, Pull Request and Jira Rules** (repository layout, updated for this decision); **08 · Architecture Decision Records** (index, lists this ADR)
