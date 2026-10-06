# 03 · User Research

| Field | Value |
|---|---|
| Status | Draft |
| Owner | [TODO: name] |
| Contributors | [TODO: names] |
| Deliverable | Sprint 1 · #3 User Research |
| Sources | Assignment §5.3, §7.1.2 #3, §11 |
| Confluence parent | 1 · Discovery |
| Labels | `sprint-1`, `deliverable-03`, `discovery`, `user-research` |
| Jira | [TODO: SCRUM-xx] |
| Last updated | YYYY-MM-DD |

> **ℹ️ About this page.** Entry page of Sprint 1 deliverable #3: what we asked members of the *Corpo de Bombeiros Voluntários*, how, what we found and what changed in the product. It covers the §7.1.2 content "Research method; Findings; Limitations; Resulting product decisions", lets investors judge whether the *piquete* problem is real, and is the parent of **Interview Guides and Consent**, **Observation Grid**, **Research Synthesis and Decisions** and every research note created from **Research Note — Template**.

> **⚠️ Rule.** **Never fabricate or complete participant answers.** If something was not asked or not answered, write "not asked" or "no answer". Assignment §5.3: "Limitations in access to real users must be documented. Teams must not fabricate user-research evidence."

## Research questions

> **ℹ️ Guidance.** Write one research question per assumption A1–A6 from **01 · Problem and Opportunity**, about past behaviour, and name the method that answers it. Start with the riskiest: if A4 is false, the IPMA integration must change.

- RQ1 → A1: [TODO: research question] — method: [TODO: method]
- RQ2 → A2: [TODO: research question] — method: [TODO: method]
- RQ3 → A3: [TODO: research question] — method: [TODO: method]
- RQ4 → A4: [TODO: research question] — method: [TODO: method]
- RQ5 → A5: [TODO: research question] — method: [TODO: method]
- RQ6 → A6: [TODO: research question] — method: [TODO: method]

> **✏️ Example (illustrative — replace).**
> - RQ1 → A1 (last-minute gaps happen at least weekly): How often did the *piquete* have a last-minute gap in the last month, and how was each gap filled? — interviews with duty chiefs; observation.
> - RQ2 → A2 (nobody can check certification validity at the moment of assignment): When a duty chief assigns someone, how do they know the TAS certification is still valid? — observation; interviews.
> - RQ4 → A4 (fire risk changes the required *piquete* composition): What changed in the composition on the last high fire-risk day, and who decided it? — consultation with the commander; the internal regulation, if shared.

## Method

> **ℹ️ Guidance.** One row per session or activity, using the methods allowed by §5.3: interviews, surveys, observation, prototype testing, analysis of public reports or datasets, review of documented user experiences, consultation with stakeholders. "Consent recorded" is Y only if the script on **Interview Guides and Consent** was read and accepted. Link each row to its research note.

| Method | Participants (codes) | Date | Duration | Consent recorded |
|---|---|---|---|---|
| [TODO: method] | [TODO: codes] | YYYY-MM-DD | [TODO: minutes] | [TODO: Y/N] |

> **✏️ Example (illustrative — replace). Fictional example to show the format. Never copy into real notes.**
> | Method | Participants (codes) | Date | Duration | Consent recorded |
> |---|---|---|---|---|
> | Interview | B-01 | 2026-10-10 | 20 min | Y |
> | Observation | OBS-01 (CS-01 composing the *piquete*) | 2026-10-11 | 90 min | Y (CS-01 and the members present) |
> | Consultation with stakeholders | CMD-01 | 2026-10-13 | 30 min | Y |
> | Analysis of public reports or datasets | — (bombeiros.pt FAQ on statutory hours; IPMA RCM open data) | 2026-10-08 | — | n/a |

## Participants

> **ℹ️ Guidance.** Codes only: B-01… firefighter, CS-01… duty chief, CMD-01 commander; never names. Give years of service as a range so nobody in a small corps can be recognised. If a code-to-name key is needed for follow-ups, keep it outside Confluence and Git and delete it with the notes.

| Code | Role | Years of service (range) | Method |
|---|---|---|---|
| [TODO: code] | [TODO: role] | [TODO: range] | [TODO: method] |

> **✏️ Example (illustrative — replace). Fictional example to show the format. Never copy into real notes.**
> | Code | Role | Years of service (range) | Method |
> |---|---|---|---|
> | B-01 | Firefighter | 5–10 | Interview |
> | CS-01 | Duty chief | 15–20 | Interview; observation (OBS-01) |
> | CMD-01 | Commander | 20+ | Consultation with stakeholders |

## Findings

> **ℹ️ Guidance.** Summarise the 3–5 strongest findings in a few lines, each with its ID (F1…) and the note IDs behind it, and say which assumptions they support or contradict. The full table, the strength of each finding and the assumption status live on **Research Synthesis and Decisions**.

[TODO: short summary of the findings] — full detail on **Research Synthesis and Decisions**.

> **✏️ Example (illustrative — replace). Fictional example to show the format. Never copy into real notes.**
> - F1 — In every composition described (CS-01) and in OBS-01, gaps were filled by phone in a fixed personal order. Supports A1 only in part: the frequency is still unknown.
> - F2 — During OBS-01 nobody consulted a record of TAS validity; CS-01 relies on memory. Supports A2.
> - F3 — B-01 and B-02 already announce unavailability as free text in the WhatsApp group. Basis for the AI-assisted workflow on **06 · Responsible AI Opportunity Assessment**.
> - F4 — CMD-01: the required composition per risk level is agreed verbally, not written down. Supports A4; contradicts A5.

## Limitations

> **ℹ️ Guidance.** State what limits the evidence: access to real users, sample size, and bias (e.g. a single corps; the commander chose the interviewees). A documented limitation is worth more than invented evidence; §5.3 requires it.

- **Access:** [TODO: who we could and could not reach, and why]
- **Sample size:** [TODO: number of sessions per method and role]
- **Bias:** [TODO: how participants were selected and what that may skew]

> **✏️ Example (illustrative — replace). Fictional example to show the format. Never copy into real notes.**
> - Access: one corps only; no session on a maximum fire-risk day, so A4 rests on what CMD-01 said, not on observation.
> - Sample size: four interviews and one observation session; no firefighter with under two years of service.
> - Bias: the commander chose the interviewees, who may be the most available members.

## Resulting product decisions

> **ℹ️ Guidance.** At least one row: a product decision that changed because of research, and the page or Jira issue where the change is visible. Keep it consistent with the fuller table on **Research Synthesis and Decisions**.

| Finding | Decision | Where it changed |
|---|---|---|
| [TODO: F-ID — finding] | [TODO: decision] | [TODO: page title / SCRUM-xx] |

> **✏️ Example (illustrative — replace). Fictional example to show the format. Never copy into real notes.**
> | Finding | Decision | Where it changed |
> |---|---|---|
> | F2 — TAS validity is checked from memory | The eligibility check at the moment of assignment is in the MVP, and confirmation is blocked without a valid evaluation (RF16, RF24) | **MVP, Success Criteria and Ethical Engagement**; SCRUM-999 |
> | F4 — the composition per risk level is not written down | RN10 stays "to obtain" and is configurable by the commander instead of hard-coded | **Requirements and Quality Attributes** (RN10) |

## Ethics

> **ℹ️ Guidance.** Describe what was actually done, not what was intended. §11: "User-research participants must understand the purpose of the research." · "Personal data must not be collected without a justified purpose." · "Research results must not be fabricated or misrepresented."

- **Consent:** the script on **Interview Guides and Consent** is read aloud before every session; each research note records "Consent confirmed (Y/N)"; no consent, no notes.
- **Anonymisation:** role codes only (B-01, CS-01, CMD-01, OBS-01); no names, contacts or exact ages; names of third parties removed from notes and quotes.
- **No recording:** no audio or video; the note-taker writes during the session.
- **Storage:** [TODO: where notes are stored and who can access them]
- **Deletion:** [TODO: when notes are deleted]. If a participant asks, what they said is deleted and the deletion is noted against their code.

## Done when
- [ ] Every assumption A1–A6 has a research question and a method
- [ ] Every session is listed in Method with participant codes, date and consent recorded, and has its research note; no names anywhere
- [ ] Limitations cover access, sample size and bias
- [ ] At least one resulting product decision, with where it changed
- [ ] Reviewed by another team member
- [ ] Guidance and example blocks removed
- [ ] Status set to Done
