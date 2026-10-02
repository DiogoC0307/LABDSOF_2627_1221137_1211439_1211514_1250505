# User Journeys

| Field | Value |
|---|---|
| Status | Draft |
| Owner | [TODO: name] |
| Contributors | [TODO: names] |
| Deliverable | Sprint 1 · #4 Product Vision (child page) |
| Sources | Assignment §5.4, §7.1.2 #4; Deck M3 (user journeys as BPMN with swimlanes) |
| Confluence parent | 04 · Product Vision |
| Labels | `sprint-1`, `deliverable-04`, `product`, `user-journeys` |
| Jira | [TODO: ESC-xx] |
| Last updated | YYYY-MM-DD |

> **ℹ️ About this page.** Escala's principal user journeys, as they happen today (as-is) and with the product (to-be), drawn as BPMN diagrams with swimlanes. It covers the "Principal user journeys" item of Sprint 1 deliverable #4 Product Vision (§5.4, §7.1.2). The team derives stories and requirements from it; investors see the product's value in the to-be journey.

## Journeys

> **ℹ️ Guidance.** One line per journey: ID, main personas (see **Personas**) and the steps from trigger to end. J1 is the principal journey, the one the MVP must deliver end to end; J3 stays a one-liner until it is needed.

- **J1 (principal)** — unavailability → gap detected → eligible people computed → request → application → confirmation → hours counted. Personas: Nuno (persona), Cátia (persona).
- **J2** — fire-risk-driven reinforcement. Persona: Cátia (persona).
- **J3** — checking my statutory hours. Persona: Nuno (persona).

## J1 · From unavailability to confirmed replacement (principal)

> **ℹ️ Guidance.** Draw the as-is from research (what happens today, each pain cited by note ID) and the to-be with Escala; each table is the step-by-step text of its diagram. Every to-be step points to requirement IDs on **Requirements and Quality Attributes**.

### As-is

[DIAGRAM: BPMN 2.0 with swimlanes Firefighter / Duty chief / Commander, draw.io]

| # | Lane | Step | Channel / tool | Pain point | Evidence (note ID) |
|---|---|---|---|---|---|
| 1 | [TODO: lane] | [TODO: step] | [TODO: channel] | [TODO: pain, or "none"] | [TODO: note ID, or "assumption Ax", e.g. A2] |

> **✏️ Example (illustrative — replace). Fictional example to show the format. Never copy into real notes.**
>
> | # | Lane | Step | Channel / tool | Pain point | Evidence (note ID) |
> |---|---|---|---|---|---|
> | 1 | Firefighter | Tells the duty chief he can't make the weekend slot | WhatsApp message | Free text; nothing is recorded | B-01 |
> | 2 | Duty chief | Calls members one by one, in the order remembered | Phone | No list of who is eligible for that slot | CS-01 |
> | 3 | Duty chief | Accepts the first person who says yes | Phone | Can't check TAS validity or driving licence at that moment | CS-01 |
> | 4 | Commander | Reviews the year's hours | [TODO: record used today] | Problems found only at year end | CMD-01 |

### To-be

[DIAGRAM: BPMN with lanes Firefighter / Duty chief / Commander / Escala / IPMA, draw.io]

| # | Lane | Step | System support | Requirement IDs |
|---|---|---|---|---|
| 1 | [TODO: lane] | [TODO: step] | [TODO: what Escala does] | [TODO: RF/RNF IDs] |

> **✏️ Example (illustrative — replace).**
>
> | # | Lane | Step | System support | Requirement IDs |
> |---|---|---|---|---|
> | 1 | Firefighter | Types "este fds não posso" | Extracted dates shown in editable fields, labelled "read automatically"; form if AI is off | RF13, RF31, RF32, RF35 |
> | 2 | Escala | Detects the open slot and computes who is eligible | Eligible and ineligible candidates, with reasons (e.g. RN02: expired TAS) | RF14, RF16, RF21 |
> | 3 | Duty chief | Opens a cover request; an eligible member applies | Request reaches eligible members; application recorded | RF15, RF17, RF20 |
> | 4 | Duty chief | Confirms the applicant; hours are counted | Confirmation blocked without a valid evaluation; hours progress updated | RF18, RF24, RF04 |

### Exceptions

> **ℹ️ Guidance.** For each exception, write what the user sees and what the system does, consistent with the degraded modes on **07 · Technical Design**. Add any others you find (e.g. AI extraction unavailable → form).

- **No eligible person** — Escala says so and lists every candidate with the rule that excludes them (RF16); nothing is confirmed automatically. [TODO: whether an exceptional assignment with a reason is allowed (RF19, depends on decision D-02)]
- **Eligibility engine down** (`elegibilidade-worker`) — evaluations stay "pending" and confirmation is blocked (RF24); the duty chief fills the slot by phone, as today.
- **IPMA down** — the last risk value is shown with its age (RF29); beyond [TODO: staleness threshold], Escala assumes the higher risk level.

## J2 · Fire-risk-driven reinforcement

> **ℹ️ Guidance.** Same structure as J1, shorter: the few steps between a high fire-risk forecast and a reinforced *piquete*. Start the to-be from the IPMA lane and hand over to J1 once cover requests are open.

### As-is

[DIAGRAM: BPMN 2.0 with swimlanes Firefighter / Duty chief / Commander, draw.io]

| # | Lane | Step | Channel / tool | Pain point | Evidence (note ID) |
|---|---|---|---|---|---|
| 1 | [TODO: lane] | [TODO: step] | [TODO: channel] | [TODO: pain, or "none"] | [TODO: note ID, or "assumption Ax", e.g. A2] |

> **✏️ Example (illustrative — replace). Fictional example to show the format. Never copy into real notes.**
>
> | # | Lane | Step | Channel / tool | Pain point | Evidence (note ID) |
> |---|---|---|---|---|---|
> | 1 | Duty chief | Finds out the day's fire risk | [TODO: source used today] | Need to reinforce noticed too late | CS-01 |
> | 2 | Duty chief | Calls extra members to reinforce | Phone, WhatsApp | Doesn't know whom to call first | CS-01 |

### To-be

[DIAGRAM: BPMN with lanes Firefighter / Duty chief / Commander / Escala / IPMA, draw.io]

| # | Lane | Step | System support | Requirement IDs |
|---|---|---|---|---|
| 1 | [TODO: lane] | [TODO: step] | [TODO: what Escala does] | [TODO: RF/RNF IDs] |

> **✏️ Example (illustrative — replace).**
>
> | # | Lane | Step | System support | Requirement IDs |
> |---|---|---|---|---|
> | 1 | IPMA | Publishes the rural fire risk (RCM 1–5) for the municipality and weather warnings | Hourly polling with cache; risk update event `RiscoAtualizado` | RF25, RF26 |
> | 2 | Escala | Compares the planned *piquete* with the composition required for that risk level (RN10) | Alert to the duty chief when below | RF27, RF28 |
> | 3 | Duty chief | Opens cover requests for the reinforcement slots | Continues as J1 from step 2 | RF15, RF16 |

### Exceptions

- **No eligible person** — as in J1.
- **Eligibility engine down** — as in J1.
- **IPMA down** — as in J1; the alert also says the risk data is stale and shows its age. [TODO: confirm with **07 · Technical Design**]

## Done when
- [ ] J1 and J2 each have an as-is and a to-be BPMN diagram with swimlanes
- [ ] Every as-is pain point cites a research note ID or is listed as an assumption
- [ ] Every to-be step has requirement IDs from **Requirements and Quality Attributes**
- [ ] Every exception has a behaviour consistent with **07 · Technical Design**
- [ ] Reviewed by another team member
- [ ] Guidance and example blocks removed
- [ ] Status set to Done
