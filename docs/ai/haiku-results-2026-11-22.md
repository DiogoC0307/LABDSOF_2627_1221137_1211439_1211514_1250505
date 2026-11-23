# Claude Haiku through the Claude Code CLI on the evaluation dataset (SCRUM-38)

- **Date of the run:** 2026-11-22. **Who:** the PO, from their own shell; the login token existed only in that shell for the run and was revoked afterwards.
- **Extractor:** `ClaudeCliExtractor`, **prompt v0** (zero-shot, frozen before the run, not tuned afterwards: there is no v1). Model alias `haiku`; the model the CLI reported for every call: **`claude-haiku-5-5`**.
- **Where:** the `ai-eval` service of `compose.yaml`, in the worker image with Claude Code 2.1.323 (#75, no Node in the final image), on Docker Desktop for Windows over a home network. Code: commit `f079c67` (#76).
- **Dataset:** commit `70e2005a743b408dea80c542a603cb4395b0427d`; SHA-256 `658cc60c8b2fc3dba019be25bec4ec375682f2ba832abfb8eb390bfa4d808ee3` (the same file as the baseline).
- **Runs:** 3, one case at a time, timeout 60 s per call (the product uses 10 s). **Run 1 is the headline**, as decided before the run; runs 2 and 3 show how much the model varies.
- **Latency:** time of one extract call: it starts the CLI as a new process and waits for the hosted model over the network (request to proposal).
- **Memory of the container:** peak **191 MiB** of a 640 MiB limit (cgroup v2, whole run: the runner's JVM plus one CLI process at a time).

## The model against the targets of 06 and the baseline

| Measure | Target (06) | Baseline v0 | Haiku run 1 (headline) | Run 2 | Run 3 |
|---|---|---|---|---|---|
| Periods exactly right (normal) | ≥ 85% | 77.1% (27/35) | **82.9% (29/35)** | 91.4% (32/35) | 91.4% (32/35) |
| Dates right (normal) | ≥ 95% | 82.9% (29/35) | **82.9% (29/35)** | 91.4% (32/35) | 91.4% (32/35) |
| Right number of periods (all) | ≥ 95% | 91.7% (55/60) | **96.7% (58/60)** | 100% (60/60) | 100% (60/60) |
| Adverse handled | 100% | 90.0% (9/10) | **100% (10/10)** | 100% (10/10) | 100% (10/10) |
| Periods exactly right (difficult) | — | 73.3% (11/15) | 80.0% (12/15) | 86.7% (13/15) | 86.7% (13/15) |
| p50 latency | — | under 1 ms | 1.70 s | 1.78 s | 1.76 s |
| p95 latency | < 4 s | 0.69 ms (same process) | **3.12 s** | 3.10 s | 2.75 s |
| Slower than 4 s / 10 s | — | — | 3.3% / 0% | 0% / 0% | 0% / 0% |
| No valid structured output, timeouts, errors | — | — | 0, 0, 0 | 0, 0, 0 | 0, 0, 0 |
| Cost per call (CLI estimate) | measured | none | 0.00025 USD | 0.00022 USD | 0.00022 USD |

**Met by the headline run:** right number of periods, adverse handled, p95 latency. **Not met:** periods exactly right on normal cases (82.9% against 85%) and dates right on normal cases (82.9% against 95%). Dates right stays below 95% in all three runs (best 91.4%).

## How to read these results

- **Against the baseline:** on the headline run the model is better on exact periods (+5.8 points on normal cases, +6.7 on difficult ones), on the number of periods (+5.0) and on adverse cases (100% against 90%: it handles `A-04`, the message about someone else, which the baseline missed). It ties the baseline on dates right for normal cases (82.9%). Runs 2 and 3 are better than the baseline on every accuracy measure.
- **The main failure is one convention, not the dates.** A night on one day ("hoje à noite", "quinta à noite", "sábado à noite", "amanhã à noite", "dia 31 de dezembro à noite") is answered with `endDate` on the next day: `N-04`, `N-05`, `N-23`, `N-31`, `D-15`. That is 5 of the 9 failures of run 1 and 4 of the 5 failures of runs 2 and 3. The convention (dataset README; 06's own example "quinta à noite não posso") keeps `startDate` and `endDate` on that day with the window 20:00 to 08:00. The prompt says that the night crosses midnight but not that the end date stays on the same day, which is the likely cause. 06's own example (`N-05`) fails in runs 1 and 3. A prompt v1 that states it would be **tuned after seeing these results**, so it would be reported as v1 next to v0, and should be checked on new cases, not only on these 60.
- **Other failures:** `D-06` "a partir das 18h" gives 18:00 to 00:00 where the dataset expects 18:00 to 23:59 (the same evening; the dataset is frozen, so it counts as wrong). Only in run 1: `N-28` (the weekend of the 21st and 22nd as two single days), `N-17` ("em novembro" started on the reference date), `D-05` (the nights of this week as seven periods instead of one range).
- **Variance:** the same prompt and model gave 9, 5 and 5 failures. `N-17`, `N-28` and `D-05` fail only in run 1; `N-05` fails in runs 1 and 3, `N-31` in runs 1 and 2; `N-04`, `N-23`, `D-06` and `D-15` fail in all three.
- **Latency:** the risk ADR 0016 named (each call starts the full CLI, so p95 < 4 s was at risk) did not happen here: p95 was 2.75 s to 3.12 s, two calls in 180 (both in run 1) took longer than 4 s, none longer than 10 s, and there was no timeout or invalid output. This is one machine and one network; the cluster may differ.
- **Cost:** 58 of the 60 cases reach the model in each run. The only code path that answers OK without a cost is the one that keeps empty input and input over 500 characters from the model (`ProposalRules`), so those are the other two. The CLI's estimate is about 0.0002 USD per call; on a subscription it is not billed per call.
- **Memory:** 191 MiB is the peak of the whole evaluation container. It includes a small JVM and the CLI, so the CLI alone used less than that. The worker's own JVM (Spring, listeners, JPA) is not in this figure.
- **No token** appears in any report or in the run's output.

---

The three runs in detail follow, as `AiEvaluationMain` wrote them (`haiku-results-run1.md` to `run3.md`).

## Run 1 of 3 (headline)

- **Dataset:** 60 cases; commit `70e2005a743b408dea80c542a603cb4395b0427d`; SHA-256 `658cc60c8b2fc3dba019be25bec4ec375682f2ba832abfb8eb390bfa4d808ee3`.
- **Extractor:** Claude Haiku via the Claude Code CLI, prompt v0, run 1 of 3; model reported: claude-haiku-5-5.
- **Latency:** Time of one extract call, in ms: it starts the Claude Code CLI as a new process and waits for the hosted model over the network, as the product would (request to proposal).
- **Cost per message:** 0.00025 USD on average over 58 calls (0.0143 USD in total), as estimated by the CLI.

### Measures against the targets of 06 · Responsible AI Opportunity Assessment

| Measure | Target | Result | Met |
|---|---|---|---|
| Periods exactly right (normal cases) | ≥ 85% | 82.9% (29/35) | no |
| Dates right (normal cases) | ≥ 95% | 82.9% (29/35) | no |
| Right number of periods (all cases) | ≥ 95% | 96.7% (58/60) | yes |
| Adverse handled (adverse cases) | 100% | 100.0% (10/10) | yes |
| p95 latency | < 4 s | 3117.901 ms | yes |

### Per category

| Category | Cases | Exact | Dates right | Right number |
|---|---|---|---|---|
| normal | 35 | 82.9% (29/35) | 82.9% (29/35) | 97.1% (34/35) |
| difficult | 15 | 80.0% (12/15) | 84.6% (11/13) | 93.3% (14/15) |
| adverse | 10 | 100.0% (10/10) | n/a (no expected periods) | 100.0% (10/10) |
| **all** | 60 | 85.0% (51/60) | 83.3% (40/48) | 96.7% (58/60) |

Dates right is counted over the cases that expect at least one period.

### Calls

| Measure | Value |
|---|---|
| p50 latency | 1703.850 ms |
| p95 latency | 3117.901 ms |
| Slower than 4 s (target of 06) | 3.3% |
| Slower than 10 s (the product's timeout, ADR 0016) | 0.0% |
| Calls with no valid structured output | 0 |
| Timeouts | 0 |
| Errors (process, no token) | 0 |
| Reasons | - |

### Cases not exactly right (9)

| Case | Category | Input | Expected | Actual |
|---|---|---|---|---|
| `N-04` | normal | hoje à noite não posso (2026-10-26) | 2026-10-26 20:00-08:00 | 2026-10-26..2026-10-27 20:00-08:00 |
| `N-05` | normal | quinta à noite não posso (2026-10-26) | 2026-10-29 20:00-08:00 | 2026-10-29..2026-10-30 20:00-08:00 |
| `N-17` | normal | em novembro às quartas não consigo (2026-10-26) | 2026-11-01..2026-11-30 [WED] | 2026-10-26..2026-11-30 [WED] |
| `N-23` | normal | sábado à noite não posso (2026-11-12) | 2026-11-14 20:00-08:00 | 2026-11-14..2026-11-15 20:00-08:00 |
| `N-28` | normal | fim de semana de 21 e 22 não posso (2026-11-12) | 2026-11-21..2026-11-22 | 2026-11-21 ; 2026-11-22 |
| `N-31` | normal | amanhã à noite não posso (2026-12-28) | 2026-12-29 20:00-08:00 | 2026-12-29..2026-12-30 20:00-08:00 |
| `D-05` | difficult | qq dia desta semana à noite n dá (2026-10-26) | 2026-10-26..2026-11-01 20:00-08:00 | 2026-10-26 20:00-08:00 ; 2026-10-27 20:00-08:00 ; 2026-10-28 20:00-08:00 ; 2026-10-29 20:00-08:00 ; 2026-10-30 20:00-08:00 ; 2026-10-31 20:00-08:00 ; 2026-11-01 20:00-08:00 |
| `D-06` | difficult | não posso sexta a partir das 18h (2026-10-26) | 2026-10-30 18:00-23:59 | 2026-10-30 18:00-00:00 |
| `D-15` | difficult | nao posso dia 31 de dezembro a noite (2026-12-28) | 2026-12-31 20:00-08:00 | 2026-12-31..2027-01-01 20:00-08:00 |

## Run 2 of 3

- **Dataset:** 60 cases; commit `70e2005a743b408dea80c542a603cb4395b0427d`; SHA-256 `658cc60c8b2fc3dba019be25bec4ec375682f2ba832abfb8eb390bfa4d808ee3`.
- **Extractor:** Claude Haiku via the Claude Code CLI, prompt v0, run 2 of 3; model reported: claude-haiku-5-5.
- **Latency:** Time of one extract call, in ms: it starts the Claude Code CLI as a new process and waits for the hosted model over the network, as the product would (request to proposal).
- **Cost per message:** 0.00022 USD on average over 58 calls (0.0126 USD in total), as estimated by the CLI.

### Measures against the targets of 06 · Responsible AI Opportunity Assessment

| Measure | Target | Result | Met |
|---|---|---|---|
| Periods exactly right (normal cases) | ≥ 85% | 91.4% (32/35) | yes |
| Dates right (normal cases) | ≥ 95% | 91.4% (32/35) | no |
| Right number of periods (all cases) | ≥ 95% | 100.0% (60/60) | yes |
| Adverse handled (adverse cases) | 100% | 100.0% (10/10) | yes |
| p95 latency | < 4 s | 3098.062 ms | yes |

### Per category

| Category | Cases | Exact | Dates right | Right number |
|---|---|---|---|---|
| normal | 35 | 91.4% (32/35) | 91.4% (32/35) | 100.0% (35/35) |
| difficult | 15 | 86.7% (13/15) | 84.6% (11/13) | 100.0% (15/15) |
| adverse | 10 | 100.0% (10/10) | n/a (no expected periods) | 100.0% (10/10) |
| **all** | 60 | 91.7% (55/60) | 89.6% (43/48) | 100.0% (60/60) |

Dates right is counted over the cases that expect at least one period.

### Calls

| Measure | Value |
|---|---|
| p50 latency | 1780.408 ms |
| p95 latency | 3098.062 ms |
| Slower than 4 s (target of 06) | 0.0% |
| Slower than 10 s (the product's timeout, ADR 0016) | 0.0% |
| Calls with no valid structured output | 0 |
| Timeouts | 0 |
| Errors (process, no token) | 0 |
| Reasons | - |

### Cases not exactly right (5)

| Case | Category | Input | Expected | Actual |
|---|---|---|---|---|
| `N-04` | normal | hoje à noite não posso (2026-10-26) | 2026-10-26 20:00-08:00 | 2026-10-26..2026-10-27 20:00-08:00 |
| `N-23` | normal | sábado à noite não posso (2026-11-12) | 2026-11-14 20:00-08:00 | 2026-11-14..2026-11-15 20:00-08:00 |
| `N-31` | normal | amanhã à noite não posso (2026-12-28) | 2026-12-29 20:00-08:00 | 2026-12-29..2026-12-30 20:00-08:00 |
| `D-06` | difficult | não posso sexta a partir das 18h (2026-10-26) | 2026-10-30 18:00-23:59 | 2026-10-30..2026-10-31 18:00-00:00 |
| `D-15` | difficult | nao posso dia 31 de dezembro a noite (2026-12-28) | 2026-12-31 20:00-08:00 | 2026-12-31..2027-01-01 20:00-08:00 |

## Run 3 of 3

- **Dataset:** 60 cases; commit `70e2005a743b408dea80c542a603cb4395b0427d`; SHA-256 `658cc60c8b2fc3dba019be25bec4ec375682f2ba832abfb8eb390bfa4d808ee3`.
- **Extractor:** Claude Haiku via the Claude Code CLI, prompt v0, run 3 of 3; model reported: claude-haiku-5-5.
- **Latency:** Time of one extract call, in ms: it starts the Claude Code CLI as a new process and waits for the hosted model over the network, as the product would (request to proposal).
- **Cost per message:** 0.00022 USD on average over 58 calls (0.0126 USD in total), as estimated by the CLI.

### Measures against the targets of 06 · Responsible AI Opportunity Assessment

| Measure | Target | Result | Met |
|---|---|---|---|
| Periods exactly right (normal cases) | ≥ 85% | 91.4% (32/35) | yes |
| Dates right (normal cases) | ≥ 95% | 91.4% (32/35) | no |
| Right number of periods (all cases) | ≥ 95% | 100.0% (60/60) | yes |
| Adverse handled (adverse cases) | 100% | 100.0% (10/10) | yes |
| p95 latency | < 4 s | 2750.472 ms | yes |

### Per category

| Category | Cases | Exact | Dates right | Right number |
|---|---|---|---|---|
| normal | 35 | 91.4% (32/35) | 91.4% (32/35) | 100.0% (35/35) |
| difficult | 15 | 86.7% (13/15) | 92.3% (12/13) | 100.0% (15/15) |
| adverse | 10 | 100.0% (10/10) | n/a (no expected periods) | 100.0% (10/10) |
| **all** | 60 | 91.7% (55/60) | 91.7% (44/48) | 100.0% (60/60) |

Dates right is counted over the cases that expect at least one period.

### Calls

| Measure | Value |
|---|---|
| p50 latency | 1756.612 ms |
| p95 latency | 2750.472 ms |
| Slower than 4 s (target of 06) | 0.0% |
| Slower than 10 s (the product's timeout, ADR 0016) | 0.0% |
| Calls with no valid structured output | 0 |
| Timeouts | 0 |
| Errors (process, no token) | 0 |
| Reasons | - |

### Cases not exactly right (5)

| Case | Category | Input | Expected | Actual |
|---|---|---|---|---|
| `N-04` | normal | hoje à noite não posso (2026-10-26) | 2026-10-26 20:00-08:00 | 2026-10-26..2026-10-27 20:00-08:00 |
| `N-05` | normal | quinta à noite não posso (2026-10-26) | 2026-10-29 20:00-08:00 | 2026-10-29..2026-10-30 20:00-08:00 |
| `N-23` | normal | sábado à noite não posso (2026-11-12) | 2026-11-14 20:00-08:00 | 2026-11-14..2026-11-15 20:00-08:00 |
| `D-06` | difficult | não posso sexta a partir das 18h (2026-10-26) | 2026-10-30 18:00-23:59 | 2026-10-30 18:00-00:00 |
| `D-15` | difficult | nao posso dia 31 de dezembro a noite (2026-12-28) | 2026-12-31 20:00-08:00 | 2026-12-31..2027-01-01 20:00-08:00 |
