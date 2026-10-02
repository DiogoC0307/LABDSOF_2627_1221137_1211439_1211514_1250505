# AI Usage and Third-Party Attribution

| Field | Value |
|---|---|
| Status | Draft |
| Owner | [TODO: name] |
| Contributors | [TODO: names] |
| Deliverable | — |
| Sources | Assignment §11 (also §5.6.1, §5.6.2, §6.5); Deck M2–M4, M8 (attributed templates) |
| Confluence parent | 0 · Team and Process |
| Labels | `team-process`, `ethics`, `attribution` |
| Jira | [TODO: ESC-xx] |
| Last updated | YYYY-MM-DD |

> **ℹ️ About this page.** The single record of every AI tool the team used, who reviewed its output, and every third-party dataset, API, template, method and library Escala relies on. The teaching staff read it to check §11; team members update it whenever they use a new tool or source.

## Why this page exists

Assignment §11 requires, among other rules:

> "AI-generated content must be reviewed before being treated as authoritative."
>
> "Third-party code, datasets, tools, and generated content must be acknowledged appropriately."

> **ℹ️ Guidance.** Keep this section as it is. Every other section on this page answers one of these two sentences; the **Definition of Done (DoD)** for documentation items also requires "AI-assisted content reviewed by a human (§11)".

## AI tools used by the team

| Tool | Used for | Where (pages / code paths) | Reviewed by | Date |
|---|---|---|---|---|
| [TODO: tool (provider)] | [TODO: what it produced] | [TODO: page titles or repo paths] | [TODO: names] | YYYY-MM-DD |

> **ℹ️ Guidance.** One row per tool and purpose, added the day the tool is used, not at the end of the sprint. *Where* is specific enough to find the content (bold page title or repo path); *Reviewed by* is the person who checked it, never the person who generated it; *Date* is the review date. The templates in this space were drafted with Claude (Anthropic), so that row belongs here once the team has reviewed them.

> **✏️ Example (illustrative — replace).**
>
> | Tool | Used for | Where (pages / code paths) | Reviewed by | Date |
> |---|---|---|---|---|
> | Claude (Anthropic) | drafting documentation templates and structure | this Confluence space's templates | whole team | YYYY-MM-DD |
> | Claude (Anthropic) | first draft of unit tests for the IPMA RCM parser | `apps/elegibilidade-worker` | Member B (PR review) | YYYY-MM-DD |

## Rule

> **⚠️ Rule.** No AI-generated content is treated as authoritative before human review; the reviewer is recorded.

- **Documentation:** the reviewer is recorded in the table above and ticks "Reviewed by another team member" in the page's Done when.
- **Code:** mention AI-assisted parts in the PR's "What changes"; the approving reviewer is the recorded reviewer (see **Git, Pull Request and Jira Rules**).
- **Research:** AI is never used to write, complete or "clean up" participant answers (see **03 · User Research**).

> **ℹ️ Guidance.** Review means checking facts, sources and fit with Escala, not skimming. If nobody can vouch for a statement, mark it `(verify)` or remove it.

## Third-party data and APIs

**IPMA open data** (*Instituto Português do Mar e da Atmosfera*):
- **Data used:** rural fire risk RCM 1–5 per municipality (DICO code), `https://api.ipma.pt/open-data/forecast/meteorology/rcm/rcm-d{0,1,2}.json`, hourly; weather warnings, `https://api.ipma.pt/open-data/forecast/warnings/warnings_www.json`.
- **Attribution text (product and docs):** [TODO: exact text, checked against IPMA's terms of use]
- **Where it is shown:** [TODO: screens in the app, and docs such as the repository README]
- **Usage notified to `webmaster@ipma.pt`:** [TODO: date sent] by [TODO: name]
- **Simulator:** `tools/ipma-sim` serves synthetic data in IPMA's format for tests and the evaluation environment; it is not IPMA data.

**Language model used by `elegibilidade-worker`:** [TODO: model and provider, after the hosting spike (ADR 0011)]; record its terms of use and whether input text leaves our infrastructure.

> **ℹ️ Guidance.** IPMA's terms of use ask us to cite the source and notify `webmaster@ipma.pt`; keep the date the notification was sent as evidence. Show the attribution wherever risk data or warnings appear, including when the value comes from cache.

> **✏️ Example (illustrative — replace).** Attribution under the risk panel: *"Fire risk and weather warnings: IPMA — Instituto Português do Mar e da Atmosfera (open data). Updated HH:MM."* (verify the wording against IPMA's terms)

## Templates and methods

- **Product Vision Board** — Roman Pichler, romanpichler.com, CC BY-SA 4.0. Used on **Product Vision Board**.
- **Persona Template** — Roman Pichler, romanpichler.com, CC BY-SA 4.0. Used on **Personas**.
- **GO Product Roadmap** — Roman Pichler, romanpichler.com, CC BY-SA 4.0. Used on **Product Roadmap (GO)**.
- **The 2020 Scrum Guide** — scrumguides.org. Used on **Scrum Methodology** and **Team and Roles**.
- Also credited where used: walking skeleton (Alistair Cockburn) on **10 · Walking Skeleton**; retrospective prime directive (Norm Kerth) on **Sprint 1 · Retrospective**.

> **ℹ️ Guidance.** Each page that uses a CC BY-SA 4.0 template carries its own attribution line (author, source, licence); our adapted versions are shared under the same licence. Add an item here for any other method or template you adopt.

> **✏️ Example (illustrative — replace).** *"Based on the Product Vision Board by Roman Pichler (romanpichler.com), licensed under CC BY-SA 4.0."*

## Libraries and licences

- Dependency and licence report generated in CI: [TODO: link to the latest report in GitHub Actions]
- Last checked for release: [TODO: release, e.g. R0]

> **ℹ️ Guidance.** Do not list libraries by hand; link the report the CI dependency-check stage produces, so it stays current. Note here any licence that needs action (attribution in the app, notice file, or replacing the library).

## Datasets

- **AI evaluation dataset** — team-written and synthetic, never copied from real messages. Its origin, scope, size and limitations are on **06 · Responsible AI Opportunity Assessment**; the file lives in the repository at [TODO: path].
- **Test and demo data** — synthetic members, certifications and rosters only; no real corps member appears in the repository or the deployed environment.
- **IPMA simulator data** — synthetic, see above.

> **ℹ️ Guidance.** Assignment §11: "Real personal or sensitive data should not be used when synthetic or anonymized data is sufficient." If any third-party dataset is added later, record its source, licence and access date here.

## AI output in the product

AI outputs are labelled in the UI as **"read automatically"** until the user confirms them. Extracted values appear in editable fields; ineligibility explanations are generated from rule codes only, and the AI never decides eligibility.

> **ℹ️ Guidance.** Assignment §5.6.2: "The interface must not present uncertain AI-generated content as verified fact." Add a screenshot of the label once the screen exists, and link the requirement (RF33) and its test case.

[IMAGE: screenshot of an extracted unavailability shown in editable fields with the "read automatically" label]

> **✏️ Example (illustrative — replace).** Message "este fds não posso" → the form shows *Unavailable · Saturday 00:00 → Sunday 23:59*, labelled "read automatically", with **Confirm** and **Edit**. After confirmation the label disappears and the original free text is deleted.

## Done when
- [ ] Every AI tool used so far has a row with a named reviewer and review date
- [ ] IPMA attribution text is shown in the product and the date the notification was sent to `webmaster@ipma.pt` is recorded
- [ ] Every page based on a Roman Pichler template carries its attribution line
- [ ] The CI dependency report link points to the current release
- [ ] Reviewed by another team member
- [ ] Guidance and example blocks removed
- [ ] Status set to Done
