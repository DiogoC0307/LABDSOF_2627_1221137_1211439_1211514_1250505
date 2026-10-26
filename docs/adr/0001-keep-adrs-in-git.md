# 0001 · Keep architecture decision records in Git

| Field | Value |
|---|---|
| Date | 2026-09-23 |
| Status | Proposed |
| Deciders | Diogo Pereira, Diogo Sousa, Pedro Costa, Diogo Costa |

## Context

Assignment §7.1.2 #8 requires architecture decision records with alternatives, rationale and trade-offs. §9 prefers documentation kept close to the source code. Reviewers also need a short index in Confluence (**08 · Architecture Decision Records**).

## Decision

We will store one Markdown file per decision in `docs/adr/`, numbered and named as in the [ADR README](README.md). Confluence keeps only the index and links here. ADR 0001 is this decision.

## Alternatives considered

| Alternative | Why rejected |
|---|---|
| ADRs only in Confluence | They would drift from the code they justify and would not be reviewed in the same pull request |
| ADRs only in pull-request comments | Comments are hard to find later and do not form the Sprint 3 architecture-evolution narrative |

## Consequences

- **Positive:** every significant choice is versioned with the code and can be cited from the technical design.
- **Negative:** the Confluence index must be updated in the same change as a new ADR file.
- **Trade-offs:** reviewers click through to Git for the full text; that is acceptable because §12 evaluates the repository.

## Assignment requirement served

§7.1.2 #8; §9 (documentation close to source).

## Related

- Jira: SCRUM-39
- Requirements: —
- Supersedes: —
- Other ADRs or pages: [ADR README](README.md); **08 · Architecture Decision Records**
