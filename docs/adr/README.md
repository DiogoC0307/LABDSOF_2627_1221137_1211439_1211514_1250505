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

The planned first ADRs (0001–0014, all *Proposed*) are listed on **08 · Architecture Decision Records**. Use those numbers so that both indexes agree. ADR 0001 records the decision to keep ADRs in this folder.

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

One row per ADR, newest last. Link each file, e.g. `[0005](0005-use-rabbitmq-as-message-broker.md)`. The table starts empty: until the first ADR file is merged, the planned numbers and titles (0001–0014, all *Proposed*) are on **08 · Architecture Decision Records**. Add a row here when its file exists, with the same number and title.

| ADR | Title | Status | Date | Supersedes / superseded by |
|---|---|---|---|---|
