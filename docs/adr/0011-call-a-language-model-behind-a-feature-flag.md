# 0011 · Call a language model behind a feature flag

| Field | Value |
|---|---|
| Date | 2026-10-26 |
| Status | Accepted |
| Deciders | Diogo Pereira, Diogo Sousa, Pedro Costa, Diogo Costa |

## Context

Free-text unavailability needs extraction (RF31–RF33). The evaluation environment must run offline (§12), and the form must keep working when the model is slow, down or switched off (RF35). Eligibility must not depend on the model (ADR 0007), and AI is used for extraction only (FB-03, SCRUM-62).

## Decision

`elegibilidade-worker` calls a language model for extraction only, behind the `AI_ENABLED` flag. When the flag is off, the call times out or the model fails, the worker records the method `FALLBACK` and `escala-api` shows the unavailability form (RF35). The `evaluation` profile sets `AI_ENABLED=false`. The model and how it is reached are a separate decision, taken after this one. The non-AI extraction baseline is a Java implementation (library still to be chosen, SCRUM-54).

## Alternatives considered

| Alternative | Why rejected |
|---|---|
| Always-on model with no flag | The evaluation environment could not run offline (§12) and a model outage would break the unavailability screen (RF35) |
| Call the model from the PWA | Secrets and prompts would sit on the client, and the extraction would bypass the worker's records |

## Consequences

- **Positive:** the form never depends on the model; the evaluation environment stays offline.
- **Negative:** the quality of the chosen model is only known after it is measured on the dataset of 06; a Java baseline must replace the Python `dateparser` planned earlier.
- **Trade-offs:** we keep a working fallback now and choose the model separately.

## Assignment requirement served

§5.6.2 (non-AI baseline and fallback); §12 (offline evaluation); RF35.

## Related

- Jira: SCRUM-54, SCRUM-40
- Requirements: RF31–RF35
- Supersedes: —
- Other ADRs or pages: 0007; later 0016 chooses the model; **06 · Responsible AI Opportunity Assessment**
- Date evidence: written on 2026-10-26 with the technical design (SCRUM-39).
- Drafted by Pedro Costa (branch `docs/SCRUM-39-technical`, 2026-10-26); revised for SCRUM-40: AI explanations removed, since AI is used for extraction only (SCRUM-62).
