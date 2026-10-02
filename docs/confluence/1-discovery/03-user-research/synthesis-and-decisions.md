# Research Synthesis and Decisions

| Field | Value |
|---|---|
| Status | Draft |
| Owner | [TODO: name] |
| Contributors | [TODO: names] |
| Deliverable | Sprint 1 · #3 User Research (child page) |
| Sources | Assignment §5.3, §7.1.2 #3, §11 |
| Confluence parent | 03 · User Research |
| Labels | `sprint-1`, `deliverable-03`, `user-research` |
| Jira | [TODO: ESC-xx] |
| Last updated | YYYY-MM-DD |

> **ℹ️ About this page.** Where research notes and observations about the *piquete* become findings, assumption statuses and product decisions. It holds the *Findings* and *Resulting product decisions* of deliverable #3 (Assignment §7.1.2 #3); **03 · User Research** summarises it, and investors read it to see that research changed the product.

> **⚠️ Rule.** "Research results must not be fabricated or misrepresented." (Assignment §11). Count participants honestly and never promote a hunch to a finding.

## Findings

> **ℹ️ Guidance.** A finding is a pattern in what people do today, stated as a fact, not a feature idea. Each one cites the notes it comes from (e.g. B-01, OBS-01) and states its strength honestly: "N of M participants + K observation sessions", never "everyone". A single-source finding is allowed if its strength says so.

| ID (F1…) | Finding | Evidence (note IDs) | Strength (how many participants / sessions) |
|---|---|---|---|
| [TODO: F1] | [TODO: finding] | [TODO: note IDs] | [TODO: participants / sessions] |

> **✏️ Example (illustrative — replace). Fictional example to show the format. Never copy into real notes.**
>
> | ID (F1…) | Finding | Evidence (note IDs) | Strength (how many participants / sessions) |
> |---|---|---|---|
> | F1 | Gaps are filled by phoning people in the order the duty chief remembers, not by qualification | CS-01, OBS-01 | 1 duty chief + 1 observation session |
> | F2 | Certification validity is not checked at the moment of assignment | B-01, CS-01, OBS-01 | 2 participants + 1 observation session |
> | F3 | Firefighters already announce unavailability as free text in the WhatsApp group | B-01, B-02 | 2 of 3 firefighters |
> | F4 | The required composition per fire-risk level is agreed verbally, not written down | CMD-01 | 1 commander |

## Assumption status

> **ℹ️ Guidance.** One row per assumption, with the same wording and status as the Assumptions table on **01 · Problem and Opportunity**; update both on the same day. Status is *untested*, *validated*, *refuted* or *inconclusive*. A refuted assumption is a result, not a failure: record what it changes (if A4 is false, the IPMA integration must change).

| Assumption (A1–A6) | Status | Evidence | Change since last update |
|---|---|---|---|
| A1 — last-minute gaps happen at least weekly | untested | [TODO: note IDs / findings] | [TODO: old → new status (YYYY-MM-DD)] |
| A2 — nobody can check certification validity at the moment of assignment | untested | [TODO: note IDs / findings] | [TODO: old → new status (YYYY-MM-DD)] |
| A3 — firefighters accept using a phone app for this | untested | [TODO: note IDs / findings] | [TODO: old → new status (YYYY-MM-DD)] |
| A4 — fire risk changes the required piquete composition | untested | [TODO: note IDs / findings] | [TODO: old → new status (YYYY-MM-DD)] |
| A5 — a written internal regulation exists | untested | [TODO: note IDs / findings] | [TODO: old → new status (YYYY-MM-DD)] |
| A6 — firefighters don't know how many statutory hours they have done | untested | [TODO: note IDs / findings] | [TODO: old → new status (YYYY-MM-DD)] |

> **✏️ Example (illustrative — replace). Fictional example to show the format. Never copy into real notes.**
>
> | Assumption (A1–A6) | Status | Evidence | Change since last update |
> |---|---|---|---|
> | A2 — nobody can check certification validity at the moment of assignment | validated | F2 (B-01, CS-01, OBS-01) | untested → validated (YYYY-MM-DD) |
> | A4 — fire risk changes the required piquete composition | inconclusive | CS-01 described adding a crew on a maximum-risk day; CMD-01 said the rule is not written down (F4); regulation requested | untested → inconclusive (YYYY-MM-DD) |
> | A6 — firefighters don't know how many statutory hours they have done | inconclusive | B-01 keeps their own count; B-02 does not know | untested → inconclusive (YYYY-MM-DD) |

## From finding to product decision

> **ℹ️ Guidance.** At least one product decision must change because of research; this is proof for the review. For each decision, name the finding and exactly what changed (page section, requirement ID or Jira issue), then copy the row to *Resulting product decisions* on **03 · User Research**.

| Finding | Decision | What changed (page / Jira) | Date |
|---|---|---|---|
| [TODO: Fx] | [TODO: decision] | [TODO: page section / requirement ID / ESC-xx] | [TODO: YYYY-MM-DD] |

> **✏️ Example (illustrative — replace). Fictional example to show the format. Never copy into real notes.**
>
> | Finding | Decision | What changed (page / Jira) | Date |
> |---|---|---|---|
> | F2 | Check eligibility at the moment of assignment and block expired certifications (RN02); moved into the MVP | **Requirements and Quality Attributes** RF08, RF24; **MVP, Success Criteria and Ethical Engagement** In scope; ESC-31 | YYYY-MM-DD |
> | F3 | Keep free-text unavailability with assisted extraction (RF13) as the primary AI use, with the form as fallback | **06 · Responsible AI Opportunity Assessment** Demonstrated need; ESC-xx | YYYY-MM-DD |

## Open questions

> **ℹ️ Guidance.** What research has not answered yet, who or what could answer it, and by when. Untested and inconclusive assumptions belong here too; an honestly documented gap is worth more than a guessed answer (Assignment §5.3).

| Question | Who or what could answer it | Owner | By when |
|---|---|---|---|
| [TODO: question] | [TODO: role code or document] | [TODO: name] | [TODO: YYYY-MM-DD] |

> **✏️ Example (illustrative — replace). Fictional example to show the format. Never copy into real notes.**
>
> | Question | Who or what could answer it | Owner | By when |
> |---|---|---|---|
> | What minimum rest between duty periods does the internal regulation set (RN07)? | CMD-01; the internal regulation | Member A | YYYY-MM-DD |
> | Who defines the required composition per fire-risk level (RN10)? | CMD-01 | Member B | YYYY-MM-DD |

## Done when
- [ ] Every finding cites note or observation IDs and states its strength honestly
- [ ] A1–A6 each have a status that matches **01 · Problem and Opportunity**
- [ ] At least one product decision changed because of research, with the page or Jira issue that changed
- [ ] Findings and decisions summarised on **03 · User Research**
- [ ] Reviewed by another team member
- [ ] Guidance and example blocks removed
- [ ] Status set to Done
