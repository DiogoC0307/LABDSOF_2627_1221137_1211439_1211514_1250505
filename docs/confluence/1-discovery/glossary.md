# Glossary

| Field | Value |
|---|---|
| Status | Draft |
| Owner | [TODO: name] |
| Contributors | [TODO: names] |
| Deliverable | — |
| Sources | Assignment §3, §5.1, §5.3; Deck M5 (business rules); research notes |
| Confluence parent | 1 · Discovery |
| Labels | `discovery`, `glossary` |
| Jira | [TODO: SCRUM-xx] |
| Last updated | YYYY-MM-DD |

> **ℹ️ About this page.** The shared vocabulary of Escala: every Portuguese domain term and acronym used in this space, Jira and the code, with its English equivalent and what it means in the product. Other pages keep Portuguese terms in italics on first use and rely on this page for the definition; the team, reviewers and investors read it to speak the same language as the fire corps.

## Terms

| Term (PT) | English | Definition in Escala | Notes | Validated in (note ID) |
|---|---|---|---|---|
| *Corpo de Bombeiros Voluntários* | volunteer fire corps | The community Escala serves; its active roster crews the corps' vehicles | Our corps: [TODO: which corps] | [TODO: note ID] |
| *Piquete* | standby duty roster | A readiness period with a defined crew composition; the core unit of the product | Required composition depends on the day's fire-risk level (RN10, internal regulation still to obtain) | [TODO: note ID] |
| *Quadro ativo* | active roster | The only members who can be scheduled | RN01 — verify the article | [TODO: note ID] |
| TAS | emergency ambulance crew certification | Has an expiry date; expired = not eligible | RN02 — confirm the validity period | [TODO: note ID] |
| TAT | ambulance crew certification | A crew certification with an expiry date, checked like TAS | Verify the definition and the TAS/TAT difference; RN02 | [TODO: note ID] |
| RCM | IPMA rural fire risk index | 1–5 per municipality per day | Fetched hourly from IPMA open data for today and the next two days (`rcm-d0`, `rcm-d1`, `rcm-d2`) | [TODO: IPMA documentation, access date] |
| DICO | municipality code used by IPMA | Identifies the corps' municipality when fetching its RCM | Our municipality's code: [TODO: DICO] | [TODO: IPMA documentation, access date] |
| *Vaga* | slot | One position to fill in a *piquete*: role, vehicle and required certification (RF07) | No overlapping slots for the same person (RN05) | [TODO: note ID] |
| *Escalação* | assignment | Placing a member in a slot; confirmed by the duty chief only when a valid eligibility evaluation exists (RF18, RF24) | Never during a declared unavailability (RN06) | [TODO: note ID] |
| *Indisponibilidade* | unavailability | A period in which a member declares they cannot be scheduled, by form (RF12) or free text with assisted extraction and human confirmation (RF13) | The AI extracts start, end and type; the member confirms | [TODO: note ID] |
| *Horas estatutárias* | statutory annual hours | ≥275 h, of which ≥140 h emergency/standby and ≥70 h training | Source: bombeiros.pt FAQ — verify the article in DL 241/2007 (RN09) | [TODO: note ID] |
| IPMA (*Instituto Português do Mar e da Atmosfera*) | Portuguese weather and climate institute | The external data provider for fire risk (RCM) and weather warnings | Terms of use: cite the source and notify `webmaster@ipma.pt` (see **AI Usage and Third-Party Attribution**) | [TODO: IPMA documentation, access date] |
| ANEPC (*Autoridade Nacional de Emergência e Proteção Civil*) | national civil-protection authority | A stakeholder on **01 · Problem and Opportunity**; not a user of Escala | No integration in the MVP | [TODO: note ID or document] |
| DL 241/2007 | Decree-Law 241/2007 | The Decree-Law cited for the statutory annual hours | Verify the article before quoting it (RN09) | [TODO: document, access date] |

> **ℹ️ Guidance.** Validate each term in interviews and record the research note ID in *Validated in*; acronyms and laws are validated against the document instead. When participants use a term differently, correct the row and start its Notes with **Corrected:**. Add a row whenever a new Portuguese term or acronym appears on any page; everywhere else, write the English column.

> **✏️ Example (illustrative — replace). Fictional example to show the format. Never copy into real notes.**
>
> | Term (PT) | English | Definition in Escala | Notes | Validated in (note ID) |
> |---|---|---|---|---|
> | *Vaga* | slot | One position to fill in a *piquete*: role, vehicle and required certification | **Corrected:** we had used it for the whole shift; the participant used it for a single position | CS-01 · YYYY-MM-DD · Interview |

## Done when
- [ ] Every Portuguese term and acronym used anywhere in this space has a row
- [ ] Every term has been checked in at least one research note (or document for acronyms and laws), or its *Validated in* cell says "not yet validated"
- [ ] Every "verify" item (DL 241/2007 articles, TAS/TAT definitions and validity) is either confirmed with a source or still visibly flagged
- [ ] Reviewed by another team member
- [ ] Guidance and example blocks removed
- [ ] Status set to Done
