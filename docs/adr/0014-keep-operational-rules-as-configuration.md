# 0014 · Keep operational rules as configuration

| Field | Value |
|---|---|
| Date | 2026-10-09 |
| Status | Proposed |
| Deciders | Diogo Pereira, Diogo Sousa, Pedro Costa, Diogo Costa |

## Context

Minimum rest (RN07), risk-based composition (RN10), the municipality DICO, IPMA timeouts and the staleness threshold (ADR 0013) come from the corps' regulation or from operations, not from code. Hard-coding them would force a release for every local rule.

## Decision

Operational thresholds live in configuration (`infra` / environment / a versioned composition-rules file owned by `escala-api`). Defaults for the synthetic corps: DICO `1312` (Porto), `RISK_STALENESS_THRESHOLD=3h`, IPMA poll timeout 5 s with two retries, retry delays `5s,30s,2m`. Composition rules are versioned so evaluations can cite the rule set.

## Alternatives considered

| Alternative | Why rejected |
|---|---|
| Constants in the eligibility engine | Every corps-specific change needs a rebuild; RF23 versioning would mix code and policy |
| Let duty chiefs edit rules in the UI in Sprint 1 | Out of MVP; too easy to weaken safety rules without audit |

## Consequences

- **Positive:** the evaluation environment can use the same code with simulator URLs; a later corps can change DICO without a fork.
- **Negative:** bad configuration can under- or over-constrain the roster; changes need audit.
- **Trade-offs:** we postpone a rules admin UI and keep files plus environment variables.

## Assignment requirement served

§6.4 (lifecycle of operational data); supports RN07, RN10, ADR 0013.

## Related

- Jira: SCRUM-39
- Requirements: RN07, RN10, RF23
- Supersedes: —
- Other ADRs or pages: 0010, 0013; [Deployment and Operations](../operations/deployment-and-operations.md)
