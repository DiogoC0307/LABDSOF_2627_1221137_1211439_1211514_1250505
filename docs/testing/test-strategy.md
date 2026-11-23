# Test Strategy

| Field | Value |
|---|---|
| Status | In review |
| Owner | Diogo Sousa |
| Contributors | Diogo Pereira |
| Deliverable | Sprint 2 · #10 Automated unit, integration, and relevant end-to-end tests; Final submission · #7 Test and quality evidence |
| Sources | Assignment §8.3, §9, §11, §12; Deck (Analysis -> Design -> Test plan -> Code -> Test execution, Definition of Ready, Definition of Done) |
| Confluence page | **Definition of Done (DoD)** and **07 · Technical Design** |

## About this document

How Escala is tested: which risks drive the tests, which test types run where, how test cases are named and traced, and where the evidence lives.

## Approach

Test cases are planned before code, following the deck's *Analysis -> Design -> Test plan -> Code -> Test execution*. 

```mermaid
flowchart LR
  A["Analysis: story and RN IDs"] --> D["Design: technical design and ADRs"]
  D --> T["Test plan: TC IDs in Jira (DoR)"]
  T --> C["Code: tests written with the code"]
  C --> E["Test execution: CI and recorded functional tests (DoD)"]
```

## Risks

These are the top risks, most severe first (also covered in [What remains untested and why](#what-remains-untested-and-why) section).

| # | Risk | Test type(s) that cover it | Requirement/Rule |
|---|---|---|---|
| 1 | Someone who is not qualified, not available or not rested is placed in a slot | Unit (eligibility rules, with clock and date boundaries), end-to-end | RN01–RN08, RN12–RN14, RF21 |
| 2 | An assignment, swap or cover is applied without a valid evaluation. Worker down, evaluation failed or out of date | Integration (evaluation states), resilience (worker stopped) | RF24, RNF04 |
| 3 | A message is lost, or a duplicate is applied twice | Integration (real RabbitMQ and PostgreSQL) | RNF05, §6.3 |
| 4 | Personal data reaches someone who should not see it: another member, the logs or the AI provider | Integration (role access, log leaks), security | RF40, RNF07, RNF09, T-07, T-08, T-12 |
| 5 | The AI reads the wrong dates and the member does not notice | AI evaluation, unit (schema and date guards), end-to-end (confirm step) | RF31–RF33, RF35 |
| 6 | `escala-api`, `elegibilidade-worker` and `apps/web` disagree on an event or a REST response | Contract | `contracts/events/`, `contracts/openapi.yaml` |
| 7 | IPMA is down or its data is unusable, and the risk shown is wrong | Unit (response parser), resilience (simulator modes) | RF25, RF29, RNF03, ADR 0013 |

## Test types

The eligibility rules are deterministic (ADR 0007) and full of date boundaries, so most cases are cheap unit tests. The two services share no database and meet only at RabbitMQ, where mocks would hide the real failures (retries, duplicates, restarts), so integration tests run against real containers, and contract tests guard the messages between them. 

| Type | What it covers | Risks | Tool | When it runs | State and owner |
|---|---|---|---|---|---|
| Unit | Eligibility rules RN01–RN08 and RN11–RN14 (positive, negative, boundary); date and time calculations; extraction post-processing; IPMA response parsing | 1, 5, 7 | JUnit Jupiter (`escala-api`, `elegibilidade-worker`); Vitest with jsdom (`apps/web`, run with `ng test`) | `build.yml`, every pull request to and push on `main` | Extraction and web tests exist. The rules are not implemented yet (the worker answers `SEMPRE_ELEGIVEL`), so their tests come with SCRUM-65, SCRUM-69, SCRUM-160 and SCRUM-161. Owner: the developer of each story |
| Integration | Each service with its real PostgreSQL and RabbitMQ: transactional outbox, idempotency key, retries 5s/30s/2 min then DLQ, evaluation states, migrations, role access, log leaks; free text deleted after confirmation (RNF09, planned in SCRUM-75) | 2, 3, 4 | Testcontainers (PostgreSQL 17 and RabbitMQ 4.0 containers), Awaitility | `build.yml` | Exists. Owner: the developer of each story |
| Contract | Event schemas (`AvaliacaoPedida`, `AvaliacaoConcluida`, `ExtracaoPedida`, `ExtracaoConcluida`, `RiscoAtualizado`) against `contracts/events/`; REST responses against `contracts/openapi.yaml` | 6 | Chosen in SCRUM-96 | `build.yml` | Not yet: SCRUM-96, in progress |
| End-to-end | The principal journey on the Docker Compose stack: the supervisor publishes a week, a member sees their shift and says they can't come, a free and qualified colleague accepts the cover, the slot shows the colleague | 1 | Browser test, e.g. Playwright (SCRUM-100) | `build.yml` | Not yet: SCRUM-100 |
| Security | Static analysis; dependency check; secret scanning (RNF07); role checks on the endpoints that exist (`RoleAccessIT`, `MemberAccessIT`, `UnavailabilityIT`) | 4 | SonarQube; `npm audit` and OSV-Scanner; gitleaks; JUnit | `build.yml`; the `security` job only reports and does not fail the build yet | Exists |
| Performance | Unavailability by form saved in < 500 ms (RNF02, TC-SCRUM-64-03); evaluation p95 < 5 s (RNF01) | 2 | JUnit in `UnavailabilityIT` (p95 of 30 saves) | RNF02 in `build.yml` | RNF02 exists. RNF01 is not tested |
| Resilience | Worker stopped: swaps and covers stay pending and nothing is applied (RNF04); IPMA unavailable, delayed, incomplete or invalid; AI off: the form still works (RF35) | 2, 5, 7 | Docker Compose; IPMA simulator (`tools/ipma-sim`) | By hand, from the runbook in [Deployment and Operations](../operations/deployment-and-operations.md) | Retries, the DLQ and a restart of the service in the middle are automated (SCRUM-95). The worker demo is SCRUM-98, the form fallback SCRUM-89, the IPMA tests SCRUM-76 |
| AI evaluation | Extraction on the 60-case synthetic dataset, compared with the non-AI baseline `RuleBasedExtractor` (SCRUM-99), against the targets of **06 · Responsible AI Opportunity Assessment**: periods exactly right ≥ 85% and dates right ≥ 95% of normal cases, right number of periods ≥ 95%, adverse handled 100%, p95 latency < 4s | 5 | `EvaluationScorer` in `apps/elegibilidade-worker/src/main/java/pt/isep/escala/elegibilidadeworker/extracao/evaluation/` (the same scorer for both); `BaselineEvaluationTest` (TC-SCRUM-99-09) writes the baseline report to `target/ai-eval/`; `AiEvaluationMain` runs the model through the `ai-eval` service of `compose.yaml` (SCRUM-38) | Baseline in `build.yml`; model by hand when the prompt, model or dataset changes, with the login token from the shell of whoever runs it (never in CI) | Exists. Dated results in `docs/ai/`; the targets are reported, never enforced. Two targets are not met yet (see [Evidence](#evidence)). Owner: Diogo Pereira (SCRUM-38) |

### Coverage target

At least 70% of the lines of new code, measured with JaCoCo (Java services) and Vitest with lcov (`apps/web`) and reported by SonarQube. This is the quality bar defined in the Definition of Done.

## Test case convention

- **ID:** `TC-SCRUM-<key>-nn`, e.g. `TC-SCRUM-46-01`, `TC-SCRUM-46-02`.
- **Format:** Given/When/Then, written in the Jira story's acceptance criteria before coding (DoR: "a test plan and test cases are outlined").
- **Coverage:** every business rule (`RN`) gets at least one positive, one negative and one **boundary** test case.
- **Traceability:** the TC IDs are linked in Jira; each automated test carries its TC ID in its name, so a CI report can be traced back to the story. In `apps/web` the ID starts the test name, e.g. `it('TC-SCRUM-46-01 lists today's date, slot and evaluation status', …)`; for the Java tests the form is `@DisplayName("TC-SCRUM-<key>-nn …")`.
- **Manual functional tests** (DoD: "functional tests performed and recorded"): record TC ID, date, version, who and result [TODO: where, e.g. a comment on the Jira story].

Test cases of story SCRUM-46 (the installable phone PWA), in `apps/web/src/app/schedule/schedule.spec.ts` and `apps/web/src/app/app.spec.ts`:

- `TC-SCRUM-46-01`: lists today's date, slot and evaluation status.
- `TC-SCRUM-46-02`: offers install when the browser fires `beforeinstallprompt`.

## Business rule test cases

One row per business rule of **Requirements and Quality Attributes**, with at least one positive, one negative and one boundary case, or the reason why not. The rules run in the worker (ADR 0007); a refusal is `INELIGIBLE` with the rule's code in `violatedRules`.

State of each case: 

- **[A]** automated and in CI; 
- **[J]** in the Jira story, not automated yet; 
- **[P]** proposed here, to be added to the story before it is coded. 

| Rule | Positive | Negative | Boundary |
|---|---|---|---|
| RN01 · active roster only | A member on `ACTIVE_ROSTER` is offered: TC-SCRUM-65-06 [P] | A member off the active roster is not offered: TC-SCRUM-65-04 [J] | A member on `COMMAND_ROSTER`, e.g. the seeded Synthetic Admin, is not offered: TC-SCRUM-65-07 [P] |
| RN02 · certification until its expiry date | TAS that expires a month after the slot: qualified: TC-SCRUM-65-08 [P] | TAS expired yesterday: not qualified, with the reason: TC-SCRUM-65-05 [J] | TAS that expires on the day the slot starts: qualified: TC-SCRUM-65-09 [P]. Night slot that starts on the expiry day: qualified, the start counts: TC-SCRUM-65-10 [P]. Slot at 00:30 in Lisbon on the day after, in summer (still the expiry day in UTC): not qualified: TC-SCRUM-65-11 [P] |
| RN03 · licence matches the vehicle | Valid class C licence, class C slot: qualified: TC-SCRUM-161-01 [J] | Class B licence, class C slot: not qualified: TC-SCRUM-161-02 [J] | Licence that expires on the day the slot starts: qualified; expired the day before: not qualified (Lisbon date, as RN02): TC-SCRUM-161-03 [J]. Only CE recorded, class C slot: not qualified: TC-SCRUM-161-04 [J]. Slot with no required licence (`requiredLicenceClass` null): the licence is not checked: TC-SCRUM-161-05 [J] |
| RN04 · minimum crew | A *piquete* with every required function is saved with no warning: TC-SCRUM-65-15 [P] | A swap that leaves the *piquete* with no driver is rejected: TC-SCRUM-69-02 [J] | The only driver is removed: "The Saturday piquete has no driver": TC-SCRUM-65-03 [J] |
| RN05 · no overlapping slots | Two slots of one member on different days: TC-SCRUM-65-16 [P] | Two slots at the same time: TC-SCRUM-65-17 [P] | A slot that starts exactly when the other ends is not an overlap (RN07 may still refuse it): TC-SCRUM-65-18 [P]. 20:00–08:00 and 06:00–14:00 the next day overlap across midnight: TC-SCRUM-65-19 [P] |
| RN06 · declared unavailability | Unavailable on another day: offered: TC-SCRUM-65-20 [P] | Unavailable that Saturday: TC-SCRUM-65-02, TC-SCRUM-69-03, TC-SCRUM-72-02 [J] | The unavailability ends exactly when the slot starts: offered: TC-SCRUM-65-21 [P] |
| RN07 · minimum rest | 12 h between two shifts under 12 h: allowed: TC-SCRUM-160-01 [J] | 10 h between them: refused: TC-SCRUM-160-02 [J] | Exactly 11 h: allowed; 10 h 59 min: refused, for professionals and volunteers: TC-SCRUM-160-03 [J]. Professionals: exactly 24 h after a 12 h day shift, 48 h after a 12 h night shift, 72 h after a 24 h shift: allowed; one minute less: refused; a shift longer than 12 h and shorter than 24 h needs the rest of a 12 h shift of the same period: TC-SCRUM-160-04 [J]. Clock changes: TC-SCRUM-160-05 [J] (see the [catalogue](#boundary-cases-catalogue)). Values from configuration: TC-SCRUM-160-10 [J] |
| RN08 · age limit | Within the age limit: qualified: TC-SCRUM-161-06 [J] | `withinAgeLimit` false: not qualified: TC-SCRUM-161-06 [J] | The 65th birthday cannot be tested in the worker: the date of birth is not stored and the event carries only yes or no (ADR 0008) |
| RN09 · annual service | — | — | Out of the MVP (RF04, v2): see *What remains untested* |
| RN10 · risk sets the composition | — | — | Off by default (D-00; RF27 and RF28 are Won't): see *What remains untested* |
| RN11 · supervisor informed, approval optional | A valid swap is applied and the supervisor is informed: TC-SCRUM-69-01 [J] | Approval on and the supervisor rejects: nothing changes: TC-SCRUM-74-02 [J] | Approval on: the swap waits and nothing changes yet: TC-SCRUM-74-01 [J]. Approval off: applied at once: TC-SCRUM-74-03 [J] |
| RN12 · weekly hours limit | Below the limit: allowed: TC-SCRUM-65-22 [P] | Above the limit: refused: TC-SCRUM-65-23 [P] | Exactly at the limit: allowed: TC-SCRUM-65-24 [P] |
| RN13 · volunteers, no two night shifts in a row | A professional on two consecutive night *piquetes*: RN13 does not apply: TC-SCRUM-160-07 [J] | A volunteer on night *piquetes* on two consecutive nights: refused: TC-SCRUM-160-06 [J] | One free night between two night *piquetes*: allowed (if RN07 allows it): TC-SCRUM-160-07 [J] |
| RN14 · volunteers, shifts under 12 h | 8 h slot: allowed: TC-SCRUM-160-08 [J] | 24 h slot: refused: TC-SCRUM-160-08 [J] | 11 h 59 min: allowed; exactly 12 h: refused; 21:00–08:00 on the night the clocks go back is 12 h: refused; a professional on 12 h: RN14 does not apply: TC-SCRUM-160-09 [J] |

RN07, RN13 and RN14 need to know whether a member is a professional or a volunteer: SCRUM-159 adds that to `Member` and to `AvaliacaoPedida` (TC-SCRUM-159-01 to -04) and blocks SCRUM-160. `TC-SCRUM-65-12` to `-14` are not used: RN03 moved to SCRUM-161.

## Boundary cases catalogue

One row per boundary, and a new one whenever a rule is refined.

| Boundary case | Rule / requirement | Expected behaviour | Test case(s) |
|---|---|---|---|
| Certification expiring on the slot date | RN02 | It counts on its expiry day. The check uses the date in Lisbon (`Europe/Lisbon`) at the start of the slot (domain model; decided 2026-11-23, SCRUM-42) | TC-SCRUM-65-09, -10, -11 [P] |
| Rest period ending exactly at the next start | RN07 | Exactly the minimum is enough: 11 h after a shift under 12 h (24 h, 48 h and 72 h after the long shifts of professionals). One minute less is refused | TC-SCRUM-160-03, -04 [J] |
| Duty period crossing midnight | RN05, RN07, RN14, ADR 0012 | Overlap, rest and duration use the start and end instants, not calendar dates | TC-SCRUM-65-19 [P] |
| Daylight-saving change, rest | RN07, ADR 0012 | Real duration counts. Spring: 22:00 on 2027-03-27 to 09:00 on 2027-03-28 is 11 h on the clock and 10 h of rest: refused. Autumn: 23:00 on 2027-10-30 to 09:00 on 2027-10-31 is 10 h on the clock and 11 h of rest: allowed | TC-SCRUM-160-05 [J] |
| Daylight-saving change, duration | RN14, ADR 0012 | 21:00 on 2027-10-30 to 08:00 on 2027-10-31 is 11 h on the clock and 12 h of service: refused for a volunteer | TC-SCRUM-160-09 [J] |
| Daylight-saving change, unavailability | RN06, ADR 0012 | Lisbon times are sent in UTC, on the day the clocks go forward too. The day they go back has no test yet | `unavailability.spec.ts` [A], no `TC-` ID |
| Duplicate event delivered twice | RNF05 | The second delivery changes nothing: one evaluation, one result applied | TC-SCRUM-95-02 [A] (both services) |
| Slot starting exactly when another ends | RN05, RN07 | Not an overlap; RN07 decides | TC-SCRUM-65-18 [P] |
| Unavailability ending exactly at the slot start | RN06 | The member can be offered | TC-SCRUM-65-21 [P] |
| Volunteer shift of exactly 12 h | RN14 | Refused; 11 h 59 min is allowed | TC-SCRUM-160-09 [J] |
| Licence expiring on the slot date | RN03 | As RN02: it counts on its expiry day, using the Lisbon date at the start of the slot | TC-SCRUM-161-03 [J] |
| Second night *piquete* in a row | RN13 | Refused for a volunteer; one free night between them is allowed; a professional is not refused | TC-SCRUM-160-06, -07 [J] |
| Weekly hours exactly at the limit | RN12 | Allowed; above the limit is refused | TC-SCRUM-65-24 [P] |
| Evaluation older than the one stored | RF24 | Ignored; the slot does not change | TC-SCRUM-79-05 [J] |
| Two members accept a cover in the same second | RF17 | Only the first stored answer takes the slot | TC-SCRUM-73-02 [J] |
| Unavailability end before or equal to its start | RF12 | `400` and nothing is saved | TC-SCRUM-64-02 [A] |
| Certification expiry before its issue date | RF02 | `400` and nothing is saved | TC-SCRUM-77-02 [A] |
| IPMA silent for 3 h | RF29, ADR 0013 | Last value with its age and a warning; the schedule is unchanged | TC-SCRUM-76-02 [J] |

## Test data

Every test uses synthetic data only (assignment §11). Real rosters, certifications or messages are never copied, even anonymised, and while sign-in is mocked only synthetic members may exist (09, T-01).

- **Seed data:** three synthetic users (Synthetic Member, Synthetic Supervisor, Synthetic Admin), with ranks and no contacts, in `apps/escala-api/src/main/resources/db/migration/V3__member_and_mocked_users.sql` and `V4__member_details_functions_certifications.sql`. Other members, certifications and licences are created by the tests that need them.
- **IPMA fixtures (SCRUM-43):** `tools/ipma-sim/fixtures/`. `rcm-d0.json` and `warnings_www.json` are the bodies fetched on 2026-11-21. `rcm-d0-municipality-missing.json`, `invalid.json` and `empty.json` are fixed bodies for the unusable responses the live API did not return that day: the corps municipality missing, a body that is not JSON, and an empty body (ADR 0013). The simulator container and the unavailable and delayed modes are still to build.
- **AI evaluation dataset:** 60 team-written cases (35 normal / 15 difficult / 10 adverse), in `apps/elegibilidade-worker/src/test/resources/ai-eval/unavailability.jsonl`; its origin, conventions and limitations are in the `README.md` next to it and on **06 · Responsible AI Opportunity Assessment**.
- **Clock:** `escala-api` has a `Clock` bean (`ClockConfig`), which a test replaces with `Clock.fixed`, as `UnavailabilityIT` does (2026-11-22 10:00 UTC). The AI cases carry their own reference date. The worker has no `Clock` bean yet; where a rule needs the current time, it needs one before its boundary tests.

## Evidence

| Evidence | Produced by | Where to find it |
|---|---|---|
| Unit and integration test reports | `build.yml` | GitHub Actions run → artefacts `escala-api-reports`, `elegibilidade-worker-reports` (Surefire) and `web-reports` |
| Coverage report | `build.yml` | Same artefacts (JaCoCo HTML for the Java services, `coverage/` for `apps/web`) and SonarQube [TODO: link to the SonarQube projects, Diogo Sousa] |
| Static analysis and dependency check reports | SonarQube step and `security` job of `build.yml` | SonarQube [TODO: link, Diogo Sousa]; job summary and artefacts `security-reports` and `sbom` of the run |
| End-to-end report | `build.yml` (SCRUM-100) | Not available yet |
| Recorded manual functional tests | Team members | The smoke tests of 2026-11-01 are in [Manual smoke tests](#manual-smoke-tests) below [TODO: where the others are recorded, e.g. Jira story comments, Diogo Sousa] |
| Performance results | `UnavailabilityIT`, TC-SCRUM-64-03 (RNF02) | Artefact `escala-api-reports`. RNF01: not available yet |
| Resilience and degraded-mode demonstration | Runbook in [Deployment and Operations](../operations/deployment-and-operations.md) | Not recorded yet: SCRUM-98 |
| AI evaluation results | Dataset runner | **06 · Responsible AI Opportunity Assessment**; reports in Git: [baseline v0](../ai/baseline-results-2026-11-22.md) (SCRUM-99) and [Claude Haiku, prompt v0, 3 runs](../ai/haiku-results-2026-11-22.md) (SCRUM-38). In the headline run the model meets the targets for the number of periods, adverse cases and latency, and misses two: periods exactly right (82.9%, target 85%) and dates right (82.9%, target 95%) |

## What remains untested and why

Assignment §8.3: "Teams must justify the types of tests selected and identify important areas that remain untested." Reviewed at every sprint review.

| Area | Why it is untested | What could go wrong | Mitigation / when |
|---|---|---|---|
| Eligibility rules RN01, RN02, RN04–RN06, RN11 and RN12 | Not implemented: the worker answers `SEMPRE_ELEGIVEL` | Someone not qualified or not available is marked eligible | Cases planned above; tests come with SCRUM-65, SCRUM-69, SCRUM-72 and SCRUM-74. Synthetic data only until then |
| RN03, RN07, RN08, RN13, RN14 | Not implemented; their stories (SCRUM-159, SCRUM-160, SCRUM-161) are in the Backlog, not estimated or planned yet, and need three additive fields in `AvaliacaoPedida` | A member is scheduled without rest, a volunteer on a shift the corps does not allow, or a driver without a valid licence | Cases planned above, with IDs; planned at the next refinement |
| Internal regulation behind RN07, RN13 and RN14 | The document was not obtained; the values were confirmed by one member of Corps 1 (B-02) | Another corps has other values | The rules are configurable (ADR 0014) |
| RN09 annual service; RN10 risk-based composition | RN09 belongs to RF04, out of the MVP; RN10 is off (D-00) | None in the MVP: neither runs | Tests when the feature enters a release |
| Confirmation blocked without a valid evaluation (RF24) | SCRUM-79 is not built; today only the slot states are tested (TC-SCRUM-95-03) | An assignment, swap or cover is applied unchecked | SCRUM-79; worker-down demo in SCRUM-98 |
| Principal journey end to end | The stories it crosses are not finished | Each part works and the whole does not | SCRUM-100; manual smoke tests until then |
| Contracts between services and with the web app | SCRUM-96 is in progress | A renamed field breaks a consumer after the merge | SCRUM-96; integration tests use the current messages; a contract changes in the same pull request as its producer and consumer (`contracts/README.md`) |
| Evaluation p95 < 5 s (RNF01) and load | No load test; the rules are not implemented | Slow answers with many requests | Check before the Sprint 3 review, once the rules exist |
| Live IPMA failures | The live API cannot be forced to fail; the simulator container is not built | A real response differs from the fixtures | Fixtures from the live API (SCRUM-43); unusable bodies keep the last valid value (ADR 0013); SCRUM-76 |
| Live AI provider in CI | Needs a personal login token, which never enters CI | The CLI or the model changes and the extraction breaks | Fake `claude` process in CI (TC-SCRUM-38-01 to -06 and -08); evaluation by hand; form fallback (RF35, SCRUM-89) |
| Real authentication | Mocked in the MVP (§6.6 footnote); roles are tested, login is not | Integration problems with a real identity provider | Synthetic data only; open risk on **09 · Security and Privacy Assessment** |
| Accessibility and real phones (RNF06, RNF10, RNF11) | No device lab; the guided test is in Sprint 3 | Layout, offline or screen-reader problems | Automated check, keyboard pass and guided test in Sprint 3 |
| Restore of the backups | Not tested yet | Data cannot be recovered | SCRUM-91 (T-14) |
