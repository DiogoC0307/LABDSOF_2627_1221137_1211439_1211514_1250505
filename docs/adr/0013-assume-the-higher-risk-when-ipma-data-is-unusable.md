# 0013 · Assume the higher risk when IPMA data is unusable

| Field | Value |
|---|---|
| Date | 2026-10-08 |
| Status | Proposed |
| Deciders | Diogo Pereira, Diogo Sousa, Pedro Costa, Diogo Costa |

## Context

§6.5 requires defined behaviour for unavailable, delayed, incomplete or invalid external data. After **D-00**, RCM is information (RF25, RF29). Turning a stale feed into extra required slots would reintroduce RN10, which is off.

## Decision

While a cached RCM is younger than `RISK_STALENESS_THRESHOLD` (3 hours), serve it and show its age (RF29). When the cache is missing or older than the threshold, or the latest poll is unusable, keep the last valid value and set `assumedHigher` on the read model so the PWA shows a warning that the real level may be higher. Never invent a lower level. **Do not** add or remove slots, and do not block swaps or covers because of stale IPMA data.

## Alternatives considered

| Alternative | Why rejected |
|---|---|
| Hide risk entirely when the poll fails | Members and the supervisor would have no badge; RF29 would not be met |
| Raise required composition when data is stale | RF27/RF28 are Won't; F7 found no fixed composition per risk in either corps |

## Consequences

- **Positive:** §6.5 cases are testable with `ipma-sim`; the published roster keeps working.
- **Negative:** a long outage can show an optimistic number next to a warning; humans still decide whether to add a slot (Should, not engine).
- **Trade-offs:** we warn instead of auto-crewing.

## Assignment requirement served

§6.5; §6.8; RF29, RNF03.

## Related

- Jira: SCRUM-39
- Requirements: RF25, RF29, RNF03; RF27/RF28 Won't
- Supersedes: —
- Other ADRs or pages: 0010, 0014; **D-00**
