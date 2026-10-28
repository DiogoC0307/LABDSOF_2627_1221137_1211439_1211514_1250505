# 0006 · Mock authentication and enforce real roles

| Field | Value |
|---|---|
| Date | 2026-10-27 |
| Status | Accepted |
| Deciders | Diogo Pereira, Diogo Sousa, Pedro Costa, Diogo Costa |

## Context

§6.6 allows authentication to be mocked in the MVP, but authorisation must still be real. Only the supervisor builds and publishes the schedule (UJ1); members see the published schedule, declare unavailability, swap and accept covers; the admin registers members and their certifications (SCRUM-77). Contacts are shown only to the member and the admin (**09 · Security and Privacy Assessment**). §11 forbids turning security off for the demonstration.

## Decision

We will accept a mocked bearer token that carries `memberId` and `role` (`MEMBER`, `SUPERVISOR` or `ADMIN`). The sign-in screen lets the user pick one of the seeded users (SCRUM-63). Every business endpoint checks the role against the access-control matrix on **09 · Security and Privacy Assessment**, and takes the member from the token, never from the request body. `/health` and `/health/ready` stay unauthenticated for the container runtime and Prometheus.

## Alternatives considered

| Alternative | Why rejected |
|---|---|
| Full identity provider (e.g. Keycloak) in the MVP | Out of MVP scope; it would take time from the schedule, swap and cover journeys |
| No roles until real authentication exists | Would ship endpoints anyone can call, such as publishing the schedule, which §11 and the access-control matrix forbid |

## Consequences

- **Positive:** role rules can be tested now (TC-SCRUM-63-02: a member calling the publish endpoint gets 403); replacing the mock with a real identity provider later does not change the matrix.
- **Negative:** a token is not proof of identity, so only synthetic members may be used while the mock is in place (open risk on 09).
- **Trade-offs:** we postpone the login experience and keep authorisation honest.

## Assignment requirement served

§6.6 (authentication may be mocked; access control still applies); §11.

## Related

- Jira: SCRUM-63, SCRUM-40
- Requirements: RF40, RF36
- Supersedes: —
- Other ADRs or pages: **09 · Security and Privacy Assessment** (access-control matrix, T-01, T-12)
- Date evidence: the roles member, supervisor and admin were fixed when the Sprint 2 stories were written on 2026-10-27 (SCRUM-63); the mock itself was first proposed on 2026-10-02 in the 08 index.
- Drafted by Pedro Costa (branch `docs/SCRUM-39-technical`, 2026-10-26); revised for SCRUM-40 with the current roles.
