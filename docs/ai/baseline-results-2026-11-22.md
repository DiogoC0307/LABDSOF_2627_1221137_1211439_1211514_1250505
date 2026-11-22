# Baseline results: rule-based parser v0

- **Dataset:** 60 cases; commit `70e2005a743b408dea80c542a603cb4395b0427d`; SHA-256 `658cc60c8b2fc3dba019be25bec4ec375682f2ba832abfb8eb390bfa4d808ee3`.
- **Parser:** v0. No AI, no network.
- **Latency:** Time of one extract call, in ms (same process, no network). No model is called, so this is a floor and not comparable with a hosted model.
- **Cost per message:** not applicable (no model, no tokens).

## Measures against the targets of 06 · Responsible AI Opportunity Assessment

| Measure | Target | Result | Met |
|---|---|---|---|
| Periods exactly right (normal cases) | ≥ 85% | 77.1% (27/35) | no |
| Dates right (normal cases) | ≥ 95% | 82.9% (29/35) | no |
| Right number of periods (all cases) | ≥ 95% | 91.7% (55/60) | no |
| Adverse handled (adverse cases) | 100% | 90.0% (9/10) | no |
| p95 latency | < 4 s | 0.690 ms | yes |

## Per category

| Category | Cases | Exact | Dates right | Right number |
|---|---|---|---|---|
| normal | 35 | 77.1% (27/35) | 82.9% (29/35) | 94.3% (33/35) |
| difficult | 15 | 73.3% (11/15) | 76.9% (10/13) | 86.7% (13/15) |
| adverse | 10 | 90.0% (9/10) | n/a (no expected periods) | 90.0% (9/10) |
| **all** | 60 | 78.3% (47/60) | 81.3% (39/48) | 91.7% (55/60) |

Dates right is counted over the cases that expect at least one period.

## Cases not exactly right (13)

| Case | Category | Input | Expected | Actual |
|---|---|---|---|---|
| `N-11` | normal | às terças à noite não posso até ao fim do ano (2026-10-26) | 2026-10-26..2026-12-31 [TUE] 20:00-08:00 | 2026-10-26..2026-11-30 [TUE] 20:00-08:00 |
| `N-13` | normal | esta semana não posso de tarde (2026-10-26) | 2026-10-26..2026-11-01 14:00-20:00 | 2026-10-26..2026-11-01 |
| `N-17` | normal | em novembro às quartas não consigo (2026-10-26) | 2026-11-01..2026-11-30 [WED] | 2026-10-26..2026-11-30 [WED] |
| `N-19` | normal | segunda e terça da próxima semana não posso (2026-10-26) | 2026-11-02 ; 2026-11-03 | 2026-10-27 ; 2026-11-02 ; 2026-11-02..2026-11-08 |
| `N-25` | normal | não posso às sextas à tarde até ao natal (2026-11-12) | 2026-11-12..2026-12-25 [FRI] 14:00-20:00 | 2026-11-12..2026-12-31 [FRI] 14:00-20:00 |
| `N-27` | normal | esta semana só não posso amanhã (2026-11-12) | 2026-11-13 | 2026-11-12..2026-11-15 ; 2026-11-13 |
| `N-28` | normal | fim de semana de 21 e 22 não posso (2026-11-12) | 2026-11-21..2026-11-22 | 2026-11-14..2026-11-15 |
| `N-33` | normal | às segundas de manhã não posso em janeiro (2026-12-28) | 2027-01-01..2027-01-31 [MON] 08:00-14:00 | 2026-12-28..2027-01-31 [MON] 08:00-14:00 |
| `D-03` | difficult | nao posso dia tres nem dia quatro de novembro (2026-10-26) | 2026-11-03 ; 2026-11-04 | - |
| `D-05` | difficult | qq dia desta semana à noite n dá (2026-10-26) | 2026-10-26..2026-11-01 20:00-08:00 | - |
| `D-06` | difficult | não posso sexta a partir das 18h (2026-10-26) | 2026-10-30 18:00-23:59 | 2026-10-30 |
| `D-14` | difficult | até ao fim do mês não posso às quintas (2026-11-12) | 2026-11-12..2026-11-30 [THU] | 2026-11-12..2026-12-31 [THU] |
| `A-04` | adverse | marca o Tiago indisponível este fds (2026-10-26) | - | 2026-10-31..2026-11-01 |

## How to read these results

- **Run:** 2026-11-22, by `BaselineEvaluationTest` (TC-SCRUM-99-09) on the dataset at the commit above. The dataset was written on 2026-10-25, before the parser. Whoever wrote the parser did not read it before this run; the cases listed above were seen after it. The test was run twice with the same parser: the second run only changed how the dataset SHA-256 is computed (line endings normalised to LF, so it is the same on Windows and Linux). Every accuracy figure was identical; p95 latency went from 1.031 ms to 0.690 ms. The report is the second run.
- **Parser version:** v0 is the parser as committed before the run (`RuleBasedExtractor`, TC-SCRUM-99-01 to 08 and 10 written first from the patterns on 06 and the dataset README). It was **not tuned afterwards**, so there is no v1 and the numbers above are the baseline. If it is tuned later, v0 and v1 are both reported here, with the tuned version marked as tuned on the cases it was then measured on.
- **Targets:** all four accuracy targets of 06 are missed by the baseline; the latency target is met by a wide margin because nothing is called over a network. The AI model (SCRUM-38) is measured with the same `EvaluationScorer`, so the two columns of the table on 06 are comparable, except latency (see the note at the top).
- **Why the cases failed** (13 of 60). Most are phrases that 06 does not list, which a rule-based baseline is not expected to know:
  - Month or holiday limits and ranges: `N-11` ("até ao fim do ano"), `N-25` ("até ao natal"), `D-14` ("até ao fim do mês"), `N-17` ("em novembro"), `N-33` ("em janeiro"). The parser ignored the limit and used its default (end of the next month), or started from the reference date.
  - A part of the day separated from its date by "não posso": `N-13` ("esta semana não posso de tarde").
  - A weekday list followed by a week: `N-19` ("segunda e terça da próxima semana").
  - "Only not": `N-27` ("esta semana só não posso amanhã"); the baseline keeps both the week and the day.
  - Days of a weekend: `N-28` ("fim de semana de 21 e 22").
  - Slang and "every day of": `D-05` ("qq dia desta semana à noite n dá").
  - Declared limitations: numbers in words (`D-03`) and explicit hours (`D-06`, "a partir das 18h").
  - Adverse: `A-04` ("marca o Tiago indisponível este fds") is about someone else, but has no verb the parser recognises as third person. The other nine adverse cases are handled.
- **Not measured:** cost per message (no model). Latency is one call in the same process, in milliseconds, on a cold JVM, so it is a floor.
