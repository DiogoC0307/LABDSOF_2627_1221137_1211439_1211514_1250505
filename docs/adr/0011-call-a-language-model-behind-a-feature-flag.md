# 0011 · Call a language model behind a feature flag

| Field | Value |
|---|---|
| Date | 2026-10-06 |
| Status | Proposed |
| Deciders | Diogo Pereira, Diogo Sousa, Pedro Costa, Diogo Costa |

## Context

Free-text unavailability needs extraction (RF31–RF33) and optional explanations from rule codes. The evaluation environment must run offline (§12). Eligibility must not depend on the model (ADR 0007). A spike will still choose the exact hosted or local model.

## Decision

`elegibilidade-worker` will call a language-model HTTP API for extraction and explanations, gated by `AI_ENABLED`. When the flag is off, the call times out, or the model errors, the worker records method `FALLBACK` and `escala-api` shows the unavailability form and template explanations (RF35). The `evaluation` profile sets `AI_ENABLED=false`. The non-AI extraction baseline is a Java implementation (decision of library pending, SCRUM-54).

## Alternatives considered

| Alternative | Why rejected |
|---|---|
| Always-on hosted model with no flag | The evaluation environment and IPMA-offline demo could not run (§12, RF35) |
| Ship AI inside the PWA | Secrets and prompts would sit on the client; eligibility-adjacent text would bypass the worker audit |

## Consequences

- **Positive:** form flow never depends on the model; evaluation stays offline.
- **Negative:** hosted-model quality is not proven until the spike; a Java baseline must replace Python `dateparser`.
- **Trade-offs:** we delay the final vendor choice and keep a working fallback now.

## Assignment requirement served

§5.6.2 (baseline/fallback); §12 (offline evaluation); RF35.

## Related

- Jira: SCRUM-54, SCRUM-39
- Requirements: RF31–RF35
- Supersedes: —
- Other ADRs or pages: 0007; **06 · Responsible AI Opportunity Assessment**
