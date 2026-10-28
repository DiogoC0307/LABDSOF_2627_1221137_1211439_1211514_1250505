# 0010 · Poll IPMA hourly and cache the last valid value

| Field | Value |
|---|---|
| Date | 2026-10-11 |
| Status | Accepted |
| Deciders | Diogo Pereira, Diogo Sousa, Pedro Costa, Diogo Costa |

## Context

The MVP must fetch the municipality's rural fire risk and show how old the value is (RF25, RF29). After **D-00**, risk does **not** set required composition (RN10 off; RF27 and RF28 Won't). IPMA still belongs in the worker: `escala-api` must not block roster or swap screens on an external timeout.

## Decision

`elegibilidade-worker` polls `https://api.ipma.pt/open-data/forecast/meteorology/rcm/rcm-d{0,1,2}.json` and the warnings feed once per hour, validates the payload, stores a `RISK_OBSERVATION` in `elegibilidade-db`, and publishes `RiscoAtualizado`. `escala-api` keeps a `DailyRisk` read model **for display**. The poll never changes slot counts. The `evaluation` profile and tests point the same poller at `tools/ipma-sim`.

## Alternatives considered

| Alternative | Why rejected |
|---|---|
| `escala-api` calls IPMA on each roster view | Member and supervisor screens would wait on IPMA; unavailable data would fail the PWA |
| Webhooks from IPMA | IPMA open data does not offer them |

## Consequences

- **Positive:** risk is available from cache when IPMA is down; only the worker holds the integration.
- **Negative:** risk can be up to one hour old even when IPMA is healthy; we must show age (RF29).
- **Trade-offs:** we accept hourly freshness instead of live queries.

## Assignment requirement served

§6.5 (external integration); RF25, RF29.

## Related

- Jira: SCRUM-39
- Requirements: RF25, RF29, RNF03; RF27/RF28 Won't
- Supersedes: —
- Other ADRs or pages: 0013, 0014; **D-00**
- Date evidence: D-00 (SCRUM-59, 2026-10-11) made IPMA information only; this ADR records where and how it is polled under that decision.
- Drafted by Pedro Costa (branch `docs/SCRUM-39-technical`, 2026-10-26); revised for SCRUM-40.
