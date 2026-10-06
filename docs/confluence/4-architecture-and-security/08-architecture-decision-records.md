# 08 · Architecture Decision Records

| Field | Value |
|---|---|
| Status | Draft |
| Owner | [TODO: name] |
| Contributors | [TODO: names] |
| Deliverable | Sprint 1 · #8 Architecture Decision Records |
| Sources | Assignment §7.1.2 #8; §7.3.2 #10, #11 |
| Confluence parent | 4 · Architecture and Security |
| Labels | `sprint-1`, `deliverable-08`, `architecture`, `adr` |
| Jira | [TODO: SCRUM-xx] |
| Last updated | YYYY-MM-DD |

> **ℹ️ About this page.** The index of Escala's architecture decision records (ADRs): one row per decision, with its status, date and a link to the file. The ADRs themselves live in Git, next to the code (**Architecture Decision Records (README)**, `docs/adr/README.md`); reviewers and investors start here to see which choices were made and why.

> **ℹ️ Guidance.** §7.1.2 #8 asks for "Principal architectural and technological choices; Alternatives considered; Decision rationale; Consequences and trade-offs". This index lists the choices; each ADR file covers the alternatives, the rationale and the consequences. Every significant choice on **07 · Technical Design** must point to a row here.

## Where ADRs live

- Folder: `docs/adr/` in the repository — [TODO: GitHub link to the folder]
- One file per decision, named `NNNN-kebab-case-title.md` and copied from **ADR Template** (`docs/adr/0000-adr-template.md`)
- Each ADR is **written the day the decision is taken** and reviewed in a pull request that carries its Jira key, like code
- Statuses: *Proposed* → *Accepted*; later *Superseded by NNNN* or *Deprecated*

> **ℹ️ Guidance.** Keep ADRs in Git because they change with the code they affect; §9 says "Teams should prefer documentation generated from or maintained close to the source code where appropriate." Do not copy an ADR's text into Confluence: link to the file. When you add or change an ADR, update this table and the index in `docs/adr/README.md` in the same pull request.

> **✏️ Example (illustrative — replace).** File `docs/adr/0005-use-rabbitmq-as-message-broker.md`, whose first line is `# 0005 · Use RabbitMQ as message broker`. Its row below links to that file on GitHub.

## Index

> **ℹ️ Guidance.** The rows below are illustrative: the decisions the current design implies, all *Proposed*, with the numbers already used on **07 · Technical Design** and **09 · Security and Privacy Assessment**. Confirm, reword or drop each one as its ADR is written; never reuse a number. Change *Status* and *Date* only to match the merged ADR file.

| ADR | Title | Status | Date | Supersedes / superseded by | Link |
|---|---|---|---|---|---|
| 0001 | Record architecture decisions in the repository | Proposed | YYYY-MM-DD | — | [TODO: link] |
| 0002 | Two independently deployable backend components | Proposed | YYYY-MM-DD | — | [TODO: link] |
| 0003 | One PostgreSQL database per component | Proposed | YYYY-MM-DD | — | [TODO: link] |
| 0004 | Monorepo | Proposed | YYYY-MM-DD | — | [TODO: link] |
| 0005 | Use RabbitMQ as message broker | Proposed | YYYY-MM-DD | — | [TODO: link] |
| 0006 | Mocked authentication with real roles | Proposed | YYYY-MM-DD | — | [TODO: link] |
| 0007 | Deterministic eligibility engine; AI never decides | Proposed | YYYY-MM-DD | — | [TODO: link] |
| 0008 | Events carry the required state | Proposed | YYYY-MM-DD | — | [TODO: link] |
| 0009 | Transactional outbox | Proposed | YYYY-MM-DD | — | [TODO: link] |
| 0010 | IPMA by polling with cache | Proposed | YYYY-MM-DD | — | [TODO: link] |
| 0011 | Where the AI model runs (pending the spike) | Proposed | YYYY-MM-DD | — | [TODO: link] |
| 0012 | UTC instants with explicit time zone | Proposed | YYYY-MM-DD | — | [TODO: link] |
| 0013 | When in doubt, assume the higher risk | Proposed | YYYY-MM-DD | — | [TODO: link] |
| 0014 | Operational rules as configuration | Proposed | YYYY-MM-DD | — | [TODO: link] |

## Rules

- **At least one rejected alternative, with its reason.** An ADR without alternatives is a statement, not a decision.
- **Dates are distinct.** Each ADR carries the day its decision was taken, not the day the files were tidied up; fourteen ADRs with one date suggest they were written after the fact.
- **Superseding keeps the history.** Never edit an accepted decision: write a new ADR that supersedes it, and change the old one's status to *Superseded by NNNN*.
- **The history is the story.** Read in order, the ADRs feed the Sprint 3 *architecture evolution narrative* (§7.3.2 #11) and the "Updated architecture views and decision records" (§7.3.2 #10). Narrative: [TODO: link, Sprint 3].

> **ℹ️ Guidance.** A decision revised after investor feedback (§5.7: "At least one product or technical decision must be revised in response to investor feedback.") is recorded as a superseding ADR and linked to its entry on **Investor Decision Log**. Superseded rows stay in the index.

> **✏️ Example (illustrative — replace).** If the investors' feedback changes where the AI model runs, the team writes a new ADR (the next free number, say 0015) whose *Related* section says "Supersedes 0011". In this index, row 0011 becomes *Superseded by 0015* and row 0015 shows *Supersedes 0011*, and the ADR links to the decision's page under **Investor Decision Log**.

## Done when
- [ ] Every row links to an existing file in `docs/adr/`, and its status and date match that file
- [ ] Every ADR has at least one rejected alternative with its reason, plus consequences and trade-offs (§7.1.2 #8)
- [ ] This index and the index in `docs/adr/README.md` list the same ADRs
- [ ] Every significant choice on **07 · Technical Design** points to an ADR listed here
- [ ] Reviewed by another team member
- [ ] Guidance and example blocks removed
- [ ] Status set to Done
