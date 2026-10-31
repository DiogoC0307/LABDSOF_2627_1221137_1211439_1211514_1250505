# Architecture Decision Records (README)

This folder holds Escala's architecture decision records (ADRs), one file per decision. It satisfies Sprint 1 deliverable #8, "Architecture Decision Records" (assignment §7.1.2), which asks for *"Principal architectural and technological choices; Alternatives considered; Decision rationale; Consequences and trade-offs"*. The Confluence page **08 · Architecture Decision Records** is the reviewers' index and points here. How this folder relates to the rest of the documentation is explained in the [Documentation README](../README.md).

## What an ADR is

An ADR is a short record of one significant architecture or technology decision. It covers the context that forced the decision, the decision itself, the alternatives rejected and why, and the consequences. Read in order, the ADRs tell how the architecture evolved, which is the basis of the Sprint 3 *architecture evolution narrative* (§7.3.2 #11).

## When to write one

Write an ADR when a decision:

- chooses or changes a component, technology, data store, integration style or security mechanism (e.g. the message broker, one database per component, mocked authentication)
- is hard to reverse, or affects more than one component (`escala-api`, `elegibilidade-worker`, `web`, `infra`)
- changes a quality attribute (`RNF`) or how an assignment requirement from §6 is met
- revises an earlier ADR, including a technical decision revised after investor feedback (§5.7). In that case, also link the entry on **Investor Decision Log**

Do not write one for routine code choices (naming, a helper library, refactorings) that stay inside one component.

The index below and **08 · Architecture Decision Records** list the same ADRs. ADR 0001 records the decision to keep ADRs in this folder.

## Naming

- File: `NNNN-kebab-case-title.md`, with four digits, numbered in sequence and never reused. Example: `0005-use-rabbitmq-as-message-broker.md`.
- H1 inside the file: `# NNNN · <Decision>`, e.g. `# 0005 · Use RabbitMQ as message broker`.
- Title the decision, not the topic ("Use RabbitMQ as message broker", not "Messaging").
- `0000` is reserved for the [template](0000-adr-template.md).

## Statuses

| Status | Meaning |
|---|---|
| Proposed | Written and open for review; not yet agreed by the team |
| Accepted | Agreed; the code follows it |
| Superseded by NNNN | Replaced by a later ADR; kept for history |
| Deprecated | No longer applies and nothing replaces it (e.g. the component was removed) |

## Rules

- **At least one rejected alternative, with its reason.** An ADR with no alternatives is a statement, not a decision.
- **Written the day the decision is taken.** The Date field is that day, not the day the ADR was tidied up. Each ADR has its own date.
- **Never edit an accepted ADR's decision; supersede it.** Write a new ADR that says "Supersedes NNNN" under *Related*. In the old ADR, change only the status line to "Superseded by NNNN". Superseding this way keeps the history.
- **One decision per file.** Split a decision that covers two separate questions.
- **Reviewed like code.** Each ADR goes through a pull request whose branch or title carries the Jira key (`SCRUM-xx`), and another member approves it. The *Deciders* field and the PR author are individual contribution evidence ("Architecture decisions", §10).
- **Keep both indexes in sync.** When you add or change an ADR, update the table below and **08 · Architecture Decision Records** at the same time.

## How to add an ADR

1. Copy [0000-adr-template.md](0000-adr-template.md) to `NNNN-kebab-case-title.md`, using the next free number.
2. Fill every section and delete the guidance blocks and the example.
3. Open a pull request referencing the Jira issue. Set the status to *Accepted* once the team agrees.
4. Add a row to the index below and to **08 · Architecture Decision Records**.

## Index

One row per ADR, in number order. The Date is the day the decision was taken; each ADR's *Related* section says what that date is based on.

| ADR | Title | Status | Date | Supersedes / superseded by |
|---|---|---|---|---|
| [0001](0001-keep-adrs-in-git.md) | Keep architecture decision records in Git | Accepted | 2026-10-02 | — |
| [0002](0002-split-backend-into-two-independently-deployable-components.md) | Split the backend into two independently deployable components | Accepted | 2026-10-11 | — |
| [0003](0003-one-postgresql-database-per-component.md) | Give each component its own PostgreSQL database | Accepted | 2026-10-11 | — |
| [0004](0004-use-a-monorepo.md) | Keep the product in one monorepo | Accepted | 2026-10-04 | — |
| [0005](0005-use-rabbitmq-as-message-broker.md) | Use RabbitMQ as message broker | Accepted | 2026-10-11 | — |
| [0006](0006-mock-authentication-and-enforce-roles.md) | Mock authentication and enforce real roles | Accepted | 2026-10-27 | — |
| [0007](0007-deterministic-eligibility-engine-ai-never-decides.md) | Keep eligibility deterministic; AI never decides | Accepted | 2026-10-12 | — |
| [0008](0008-events-carry-the-state-the-consumer-needs.md) | Carry the consumer's required state on the event | Accepted | 2026-10-26 | — |
| [0009](0009-transactional-outbox-and-idempotent-consumers.md) | Use a transactional outbox and idempotent consumers | Accepted | 2026-10-26 | — |
| [0010](0010-poll-ipma-hourly-and-cache-the-last-valid-value.md) | Poll IPMA hourly and cache the last valid value | Accepted | 2026-10-11 | — |
| [0011](0011-call-a-language-model-behind-a-feature-flag.md) | Call a language model behind a feature flag | Accepted | 2026-10-26 | — |
| [0012](0012-store-instants-in-utc.md) | Store instants in UTC and display Europe/Lisbon | Accepted | 2026-10-26 | — |
| [0013](0013-assume-the-higher-risk-when-ipma-data-is-unusable.md) | Assume the higher risk when IPMA data is unusable | Accepted | 2026-10-11 | — |
| [0014](0014-keep-operational-rules-as-configuration.md) | Keep operational rules as configuration | Accepted | 2026-10-26 | — |
| [0015](0015-implement-elegibilidade-worker-in-java-with-spring-boot.md) | Implement elegibilidade-worker in Java with Spring Boot | Accepted | 2026-10-11 | — |
| [0016](0016-use-claude-haiku-through-the-claude-code-cli.md) | Use Claude Haiku through the Claude Code CLI for free-text extraction | Proposed | 2026-10-27 | — |
| [0017](0017-build-the-client-as-an-angular-pwa.md) | Build the client as an Angular PWA | Proposed | 2026-10-31 | — |
| [0018](0018-run-docker-compose-for-evaluation-and-kubernetes-with-argo-cd-for-team-environments.md) | Run Docker Compose for evaluation and Kubernetes with Argo CD for the team environments | Proposed | 2026-10-31 | — |
