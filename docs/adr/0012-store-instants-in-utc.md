# 0012 · Store instants in UTC and display Europe/Lisbon

| Field | Value |
|---|---|
| Date | 2026-10-07 |
| Status | Proposed |
| Deciders | Diogo Pereira, Diogo Sousa, Pedro Costa, Diogo Costa |

## Context

Duty periods cross midnight (RN05, RN09) and can span the daylight-saving change. Rest and hours must use real duration, not wall-clock subtraction. IPMA timestamps are UTC.

## Decision

All stored `start` / `end` / `occurredAt` values are UTC instants. Event `occurredAt` is an ISO-8601 UTC timestamp. The PWA displays times in `Europe/Lisbon`. Eligibility overlap and rest use instant arithmetic.

## Alternatives considered

| Alternative | Why rejected |
|---|---|
| Store local civil times without a zone | Overnight slots and DST changes would be ambiguous |
| Store Europe/Lisbon offsets in the database | Two services and IPMA would disagree on the offset around DST |

## Consequences

- **Positive:** tests can use a fixed clock; overlap is well-defined.
- **Negative:** the UI must convert every instant; logs stay in UTC.
- **Trade-offs:** we add a display conversion rather than store civil times.

## Assignment requirement served

Supports RN05, RN07, RN09 and event contracts.

## Related

- Jira: SCRUM-39
- Requirements: RN05, RN07, RN09
- Supersedes: —
- Other ADRs or pages: 0008
