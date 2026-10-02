# Questions and Effective Answers

| Field | Value |
|---|---|
| Status | Draft |
| Owner | [TODO: name] |
| Contributors | [TODO: names] |
| Deliverable | Sprint 1 · #4 Product Vision (child page) |
| Sources | Deck M2; Assignment §5.4 |
| Confluence parent | 04 · Product Vision |
| Labels | `sprint-1`, `deliverable-04`, `product`, `vision` |
| Jira | [TODO: ESC-xx] |
| Last updated | YYYY-MM-DD |

> **ℹ️ About this page.** Answers the six questions an effective product vision answers (deck M2), each backed by evidence or a link. It feeds **Product Vision Board** and **04 · Product Vision**, and is read by the team and by the investors (teaching staff).

> **ℹ️ Guidance — this page already exists.** In Confluence this page exists as **Questions and Effective Answers**. Merge these sections into it instead of creating a new page; keep its history.

> **ℹ️ Guidance.** Keep each answer to two to four sentences. Every answer needs an **Evidence / link**: a research note ID, a finding, or the page where the detail lives. If there is no evidence yet, say so and mark the answer as a hypothesis.

## Who

> **ℹ️ Guidance.** Deck question: *"Who is the target customer? Who are the target users?"* The buyer is not necessarily the user: say who uses Escala every day and who would pay for it or decide to adopt it (the association? the municipality?).

**Answer**

[TODO: target customer (who pays or adopts) and target users (who uses it)]

**Evidence / link**

[TODO: research note IDs, **Personas**, stakeholder map on **01 · Problem and Opportunity**]

> **✏️ Example (illustrative — replace).** **Answer:** Target users are the duty chief who composes the *piquete* (primary user), the firefighters of the active roster (*quadro ativo*) who declare unavailability and apply to slots, and the commander. Target customer: [TODO: the corps' association or the municipality] — a hypothesis to test with CMD-01. **Evidence / link:** **Personas**; stakeholder map on **01 · Problem and Opportunity**.

## What need

> **ℹ️ Guidance.** Deck question: *"Which needs will the product address? What value does it add?"* Name the needs in priority order and the value for each user group. Link the assumptions (A1–A6) or findings (F1…) that support them.

**Answer**

[TODO: needs addressed and value added]

**Evidence / link**

[TODO: assumption and finding IDs, **Research Synthesis and Decisions**]

> **✏️ Example (illustrative — replace).** **Answer:** At the moment of assignment, the duty chief needs to know whether the person holds a valid certification, a driving licence that matches the vehicle class, and the availability they declared, and to notice early when the day's fire risk requires reinforcing. Value: gaps are filled faster with verified-qualified people, and no vehicle stays in the station for lack of a qualified crew. **Evidence / link:** assumptions A1, A2, A4 on **01 · Problem and Opportunity**; findings on **Research Synthesis and Decisions**.

## What product

> **ℹ️ Guidance.** Deck question: *"Which attributes are critical to success? What will it roughly look like and do?"* Name three to five critical attributes, then describe in one or two sentences what the user sees and does.

**Answer**

[TODO: critical attributes; what it roughly looks like and does]

**Evidence / link**

[TODO: **Product Vision Board**, **Requirements and Quality Attributes**]

> **✏️ Example (illustrative — replace).** **Answer:** Critical attributes: eligibility is decided by deterministic rules, never by AI; the product keeps working when IPMA or the AI is down; declaring unavailability is as easy as sending a message. It is a mobile-first web app (PWA) showing today's *piquete*, its open slots, eligible and ineligible candidates with reasons, and today's fire risk (RCM) from IPMA. **Evidence / link:** **Product Vision Board**; **Requirements and Quality Attributes** (RF16, RF24, RNF03).

## Versus alternatives

> **ℹ️ Guidance.** Deck question: *"How does it compare to competitor and internal products? What are the unique selling points and target price?"* Summarise the comparison, including the informal alternative (whiteboard, WhatsApp, phone), and state the unique selling points. Give a target price or sustainability model only as a hypothesis; never invent a competitor's price or feature.

**Answer**

[TODO: comparison, unique selling points, target price or sustainability model]

**Evidence / link**

[TODO: **02 · Market and Competitor Analysis**]

> **✏️ Example (illustrative — replace).** **Answer:** Portuguese tools such as Ifprotec already manage rosters (features to verify on **02 · Market and Competitor Analysis**); the informal alternative is a whiteboard plus WhatsApp and the phone. Escala competes on roster quality: "We don't tell you who is on duty. We tell you whether they can actually crew that vehicle, given the risk outside." Target price: [TODO: hypothesis — see Business model below]. **Evidence / link:** capability matrix on **02 · Market and Competitor Analysis**.

## Business model

> **ℹ️ Guidance.** Deck question: *"How will the company make money? What are the revenue sources?"* Say how Escala would make money or sustain itself (e.g. subscription per corps; municipal funding; open-source with paid hosting) and mark it clearly as a **hypothesis** until investors or stakeholders confirm it.

**Answer**

[TODO: business or sustainability model — marked as a hypothesis]

**Evidence / link**

[TODO: stakeholder conversation note ID or investor feedback (Jira *Feedback* issue)]

> **✏️ Example (illustrative — replace).** **Answer (hypothesis, not validated):** the options are (a) a subscription per corps paid by the association, (b) municipal funding, since the municipality benefits from the corps' readiness, or (c) open-source with paid hosting. Current preference: [TODO: option and why]. No price is stated until there is evidence. **Evidence / link:** [TODO: CMD-01 note; investor feedback ESC-xx].

## Feasibility

> **ℹ️ Guidance.** Deck question: *"Is the product feasible? Can the company build and sell it?"* Answer both halves: can a team of 3–4 students build the MVP in three sprints, and can it be sold or adopted? Name the biggest open risk for each.

**Answer**

[TODO: can we build it in three sprints; can it be sold or adopted]

**Evidence / link**

[TODO: **10 · Walking Skeleton**, **MVP, Success Criteria and Ethical Engagement**, **Product Roadmap (GO)**]

> **✏️ Example (illustrative — replace).** **Answer:** Build: the MVP covers one journey (J1) for one corps; the walking skeleton proves PWA → API → queue → worker → DB in Sprint 1; authentication is mocked, as the assignment allows for the MVP; IPMA data is open, with a simulator for tests. Open risk: the internal regulation (RN07, RN10) is still to obtain. Adopt: depends on A3 (firefighters accept a phone app) and on the commander's support — [TODO: evidence]. **Evidence / link:** **10 · Walking Skeleton**; **MVP, Success Criteria and Ethical Engagement**.

## Done when
- [ ] All six questions have an **Answer** and an **Evidence / link**
- [ ] The business model is marked as a hypothesis until confirmed
- [ ] Answers are consistent with **Product Vision Board** and **04 · Product Vision**
- [ ] Reviewed by another team member
- [ ] Guidance and example blocks removed
- [ ] Status set to Done
