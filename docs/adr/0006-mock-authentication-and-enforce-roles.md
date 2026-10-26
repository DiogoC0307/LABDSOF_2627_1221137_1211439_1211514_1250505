# 0006 · Mock authentication and enforce real roles

| Field | Value |
|---|---|
| Date | 2026-09-28 |
| Status | Proposed |
| Deciders | Diogo Pereira, Diogo Sousa, Pedro Costa, Diogo Costa |

## Context

§6.6 allows authentication to be mocked in the MVP, but authorisation must still be real: duty chiefs confirm assignments, firefighters must not confirm their own applications, and contacts are shown only to roles that need them. §11 forbids turning security off for the demo.

## Decision

We will accept a mocked bearer token that carries `memberId` and `role` (`FIREFIGHTER`, `DUTY_CHIEF`, `COMMANDER`). Every business endpoint checks the role against the access-control matrix on **09 · Security and Privacy Assessment**. `/health` and `/health/ready` stay unauthenticated for the container runtime and Prometheus.

## Alternatives considered

| Alternative | Why rejected |
|---|---|
| Full identity provider in Sprint 1 | Out of MVP scope; would delay the walking skeleton |
| No roles until later | Would ship endpoints that anyone can call, including confirm-own-application, which §11 and the matrix forbid |

## Consequences

- **Positive:** role rules can be tested now; swapping in a real IdP later does not change the matrix.
- **Negative:** tokens are not proof of identity; the evaluation environment must use synthetic members only.
- **Trade-offs:** we postpone login UX and keep authorisation honest.

## Assignment requirement served

§6.6 (authentication may be mocked; authorisation still applies); §11.

## Related

- Jira: SCRUM-39
- Requirements: RF36
- Supersedes: —
- Other ADRs or pages: **09 · Security and Privacy Assessment**
