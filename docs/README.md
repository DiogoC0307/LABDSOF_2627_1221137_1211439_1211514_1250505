# Documentation README

Documentation templates for **Escala**, the LABDSOF 2026/27 product (MSc in Computer Engineering, ISEP). Read this file first. It covers how the templates work, where each page goes in Confluence, and the order to fill them in.

## What this folder is

- Templates for all project documentation. Each template states what the page is for and which assignment requirement it satisfies. It also lists the sections, says what to write in each, gives an illustrative example, and ends with a *Done when* checklist.
- **`confluence/`**: one file = one Confluence page. They are copied into the Confluence space linked to Jira project `SCRUM` (see [Moving a page to Confluence](#moving-a-page-to-confluence)).
- **Everything else** (`adr/`, `architecture/`, `testing/`, `operations/` and this file) stays in Git next to the code (see [What stays in Git and why](#what-stays-in-git-and-why)).
- Every template starts as `Draft`. Nothing here is final until a human has reviewed it.

## Conventions

### Page header

Every Confluence template starts with the page title as H1, a metadata table and an About block (`> **ℹ️ About this page.** …`). The table fields are always in this order: Status · Owner · Contributors · Deliverable · Sources · Confluence parent · Labels · Jira · Last updated.

- Status values: `Draft` → `In review` → `Done`.
- Pages that are neither a Sprint 1 deliverable nor one of its child pages use `—` in the Deliverable field; child pages use `Sprint 1 · #N <Deliverable> (child page)`.
- The three Git docs with a header ([technical design](architecture/technical-design.md), [test strategy](testing/test-strategy.md), [deployment and operations](operations/deployment-and-operations.md)) use a shorter table with a *Confluence page* field.

### Blocks

| Block | Format | Meaning |
|---|---|---|
| Guidance | `> **ℹ️ Guidance.** …` | What to write. Delete it, or turn it into a Confluence Info panel, once the section is filled |
| Example | `> **✏️ Example (illustrative — replace).** …` | What good content looks like. Delete it once real content exists. Research-note and observation examples also say *"Fictional example to show the format. Never copy into real notes."* |
| Rule | `> **⚠️ Rule.** …` | A rule that must not be broken (e.g. "Never fabricate or complete participant answers."). Keep it; in Confluence turn it into a Warning panel |
| Existing page | `> **ℹ️ Guidance — this page already exists.** …` | Only on the seven pages that already exist in Confluence. Merge into that page (and rename it if told to) instead of creating a new one |
| Placeholder | `[TODO: …]` | Something the team must fill |
| Jira | `[JIRA: <JQL>]` | Replace with a Jira Issues macro using that JQL |
| Diagram | `[DIAGRAM: <what to draw, which tool>]` | Replace with a draw.io or whiteboard diagram, or an exported image |
| Image | `[IMAGE: <description>]` | Replace with an image; never a photo of a real person without consent |
| Macro | `[MACRO: <macro name>]` | Replace with that Confluence macro, e.g. `[MACRO: Children display]` |

### Done-when checklist

Every Confluence template ends with its page-specific criteria followed by three standard lines:

```markdown
## Done when
- [ ] …
- [ ] Reviewed by another team member
- [ ] Guidance and example blocks removed
- [ ] Status set to Done
```

The only exception is **Sprint 1 · Meeting Minutes**, a parent-only page. The three Git docs with a header end the same way.

### Titles, numbering and cross-references

- Pages that are Sprint 1 deliverables carry the deliverable number in the title (`01 · …` to `10 · …`), so a reviewer can audit the sprint from the page tree.
- Titles use the middle dot `·` and the em dash `—` exactly as in the [map](#map-to-confluence).
- Confluence pages refer to other pages by their exact title in **bold**, never by relative `.md` links, because paths break in Confluence. They contain no Mermaid, because Confluence does not render it natively; they use `[DIAGRAM: …]` instead. Git docs may use both.
- English only, with British spelling. The Portuguese domain terms *Corpo de Bombeiros Voluntários*, *piquete*, *quadro ativo*, *vaga*, *escalação*, *indisponibilidade* and *horas estatutárias* stay in *italics* on first use in each page and are defined in the **Glossary**.

### Container pages

Ten pages have no template file. Create each one as a page whose body is only `[MACRO: Children display]`:

- children of **Escala — Home**: `0 · Team and Process`, `1 · Discovery`, `2 · Product`, `3 · Responsible AI`, `4 · Architecture and Security`, `5 · Sprints`, `6 · Investor Decisions`
- children of **5 · Sprints**: `Sprint 1`, `Sprint 2`, `Sprint 3`

The folders `confluence/0-team-and-process/templates/` and `confluence/1-discovery/03-user-research/notes/` only group files and are not Confluence pages. Pages that the team creates later from templates go here:

- filled minutes: children of **Sprint N · Meeting Minutes**
- filled research notes: children of **03 · User Research**
- filled decisions: children of **Investor Decision Log**

## Map to Confluence

The map has one row for each of the 39 files under `confluence/`. Create the container pages first. **Yes** under *Exists already?* means: merge into the existing page instead of creating a second one.

| File | Confluence parent | Page title | Deliverable | Exists already? |
|---|---|---|---|---|
| [00-home.md](confluence/00-home.md) | — (space home page) | Escala — Home | — | No |
| [team-and-roles.md](confluence/0-team-and-process/team-and-roles.md) | 0 · Team and Process | Team and Roles | — | No |
| [scrum-methodology.md](confluence/0-team-and-process/scrum-methodology.md) | 0 · Team and Process | Scrum Methodology | — | Yes, as *Scrum Methodology*: merge |
| [definition-of-ready.md](confluence/0-team-and-process/definition-of-ready.md) | 0 · Team and Process | Definition of Ready (DoR) | — | Yes, as *Definition of Ready (DoR)*: merge |
| [definition-of-done.md](confluence/0-team-and-process/definition-of-done.md) | 0 · Team and Process | Definition of Done (DoD) | — | Yes, as *Definition of Done (DoD)*: merge |
| [git-pr-and-jira-rules.md](confluence/0-team-and-process/git-pr-and-jira-rules.md) | 0 · Team and Process | Git, Pull Request and Jira Rules | — | No |
| [ai-usage-and-attribution.md](confluence/0-team-and-process/ai-usage-and-attribution.md) | 0 · Team and Process | AI Usage and Third-Party Attribution | — | No |
| [templates/meeting-minutes-template.md](confluence/0-team-and-process/templates/meeting-minutes-template.md) | 0 · Team and Process | Meeting Minutes — Template | — | Yes, as *Ata de Reuniões – Template*: merge and rename |
| [01-problem-and-opportunity.md](confluence/1-discovery/01-problem-and-opportunity.md) | 1 · Discovery | 01 · Problem and Opportunity | Sprint 1 · #1 Problem and Opportunity Report | No |
| [glossary.md](confluence/1-discovery/glossary.md) | 1 · Discovery | Glossary | — | No |
| [02-market-and-competitor-analysis.md](confluence/1-discovery/02-market-and-competitor-analysis.md) | 1 · Discovery | 02 · Market and Competitor Analysis | Sprint 1 · #2 Market and Competitor Analysis | No |
| [03-user-research.md](confluence/1-discovery/03-user-research/03-user-research.md) | 1 · Discovery | 03 · User Research | Sprint 1 · #3 User Research | No |
| [interview-guides-and-consent.md](confluence/1-discovery/03-user-research/interview-guides-and-consent.md) | 03 · User Research | Interview Guides and Consent | Sprint 1 · #3 User Research (child page) | No |
| [observation-grid.md](confluence/1-discovery/03-user-research/observation-grid.md) | 03 · User Research | Observation Grid | Sprint 1 · #3 User Research (child page) | No |
| [synthesis-and-decisions.md](confluence/1-discovery/03-user-research/synthesis-and-decisions.md) | 03 · User Research | Research Synthesis and Decisions | Sprint 1 · #3 User Research (child page) | No |
| [notes/research-note-template.md](confluence/1-discovery/03-user-research/notes/research-note-template.md) | 03 · User Research | Research Note — Template | Sprint 1 · #3 User Research (child page) | No |
| [04-product-vision.md](confluence/2-product/04-product-vision/04-product-vision.md) | 2 · Product | 04 · Product Vision | Sprint 1 · #4 Product Vision | No |
| [vision-questions.md](confluence/2-product/04-product-vision/vision-questions.md) | 04 · Product Vision | Questions and Effective Answers | Sprint 1 · #4 Product Vision (child page) | Yes, as *Questions and Effective Answers*: merge |
| [product-vision-board.md](confluence/2-product/04-product-vision/product-vision-board.md) | 04 · Product Vision | Product Vision Board | Sprint 1 · #4 Product Vision (child page) | Yes, as *Vision, Roman's Checklists and Product …*: merge and rename |
| [personas.md](confluence/2-product/04-product-vision/personas.md) | 04 · Product Vision | Personas | Sprint 1 · #4 Product Vision (child page) | No |
| [user-journeys.md](confluence/2-product/04-product-vision/user-journeys.md) | 04 · Product Vision | User Journeys | Sprint 1 · #4 Product Vision (child page) | No |
| [mvp-success-criteria-and-ethical-engagement.md](confluence/2-product/04-product-vision/mvp-success-criteria-and-ethical-engagement.md) | 04 · Product Vision | MVP, Success Criteria and Ethical Engagement | Sprint 1 · #4 Product Vision (child page) | No |
| [requirements-and-quality-attributes.md](confluence/2-product/requirements-and-quality-attributes.md) | 2 · Product | Requirements and Quality Attributes | — | No |
| [05-product-backlog.md](confluence/2-product/05-product-backlog/05-product-backlog.md) | 2 · Product | 05 · Product Backlog | Sprint 1 · #5 Product Backlog | Yes, as *Product Backlog – Template*: merge and rename |
| [product-roadmap.md](confluence/2-product/05-product-backlog/product-roadmap.md) | 05 · Product Backlog | Product Roadmap (GO) | Sprint 1 · #5 Product Backlog (child page) | No |
| [06-ai-opportunity-assessment.md](confluence/3-responsible-ai/06-ai-opportunity-assessment.md) | 3 · Responsible AI | 06 · Responsible AI Opportunity Assessment | Sprint 1 · #6 Responsible AI Opportunity Assessment | No |
| [architecture/technical-design.md](architecture/technical-design.md) | 4 · Architecture and Security | 07 · Technical Design | Sprint 1 · #7 Technical Design | Yes, as *07 · Technical Design*: paste this Git file |
| [08-architecture-decision-records.md](confluence/4-architecture-and-security/08-architecture-decision-records.md) | 4 · Architecture and Security | 08 · Architecture Decision Records | Sprint 1 · #8 Architecture Decision Records | No |
| [09-security-and-privacy.md](confluence/4-architecture-and-security/09-security-and-privacy.md) | 4 · Architecture and Security | 09 · Security and Privacy Assessment | Sprint 1 · #9 Security and Privacy Assessment | No |
| [10-walking-skeleton.md](confluence/4-architecture-and-security/10-walking-skeleton.md) | 4 · Architecture and Security | 10 · Walking Skeleton | Sprint 1 · #10 Walking Skeleton | No |
| [sprint-1-deliverables.md](confluence/5-sprints/sprint-1/sprint-1-deliverables.md) | Sprint 1 | Sprint 1 · Deliverables | — | No |
| [sprint-1-planning.md](confluence/5-sprints/sprint-1/sprint-1-planning.md) | Sprint 1 | Sprint 1 · Planning | — | No |
| [sprint-1-meeting-minutes.md](confluence/5-sprints/sprint-1/sprint-1-meeting-minutes.md) | Sprint 1 | Sprint 1 · Meeting Minutes | — | No |
| [sprint-1-review.md](confluence/5-sprints/sprint-1/sprint-1-review.md) | Sprint 1 | Sprint 1 · Review | — | No |
| [sprint-1-retrospective.md](confluence/5-sprints/sprint-1/sprint-1-retrospective.md) | Sprint 1 | Sprint 1 · Retrospective | — | No |
| [sprint-2-deliverables.md](confluence/5-sprints/sprint-2/sprint-2-deliverables.md) | Sprint 2 | Sprint 2 · Deliverables | — | No |
| [sprint-3-deliverables.md](confluence/5-sprints/sprint-3/sprint-3-deliverables.md) | Sprint 3 | Sprint 3 · Deliverables | — | No |
| [investor-decision-log.md](confluence/6-investor-decisions/investor-decision-log.md) | 6 · Investor Decisions | Investor Decision Log | — | No |
| [decision-template.md](confluence/6-investor-decisions/decision-template.md) | 6 · Investor Decisions | Decision — Template | — | No |

## What stays in Git and why

Assignment §9: *"Teams should prefer documentation generated from or maintained close to the source code where appropriate."* §12 evaluates the repository itself, and its list includes "Source-code repository", "Deployment instructions", "Architecture documentation", "API documentation" and "Test and quality evidence". The documents below change together with the code, so they are reviewed in the same pull request. Confluence keeps a summary or index page that points to them.

| Document | What it holds | Why in Git (§9 / §12) | Confluence counterpart |
|---|---|---|---|
| [adr/README.md](adr/README.md), [adr/0000-adr-template.md](adr/0000-adr-template.md) and `adr/NNNN-*.md` | One file per architecture decision | Written the day the decision is taken and reviewed with the code it affects; §12 "Architecture documentation" | **08 · Architecture Decision Records** (index) |
| [architecture/technical-design.md](architecture/technical-design.md) | Technical design (source of truth): C4 views, data model, API, async workflows | Diagrams as code, updated with the code; §12 "Architecture documentation" | **07 · Technical Design** (paste of this file) |
| [testing/test-strategy.md](testing/test-strategy.md) | Test types, test case convention, boundary cases, what remains untested | Tied to the CI stages and the test code; §12 "Test and quality evidence" | Linked from **Definition of Done (DoD)** and **07 · Technical Design** |
| [operations/deployment-and-operations.md](operations/deployment-and-operations.md) | Run, configure, deploy, roll back; runbook for degraded modes | Commands must match the repository; §12 "Deployment instructions" | **10 · Walking Skeleton**, **07 · Technical Design** |
| `contracts/openapi.yaml`, `contracts/events/` (repository, outside `docs/`) | API and event contracts | Maintained next to the code that implements them; §12 "API documentation" | **07 · Technical Design** (API outline) |
| [Repository README](../README.md) (already exists; not a template) | Product in one sentence, quick start | First thing an evaluator reads; §12 "Source-code repository" | **Escala — Home** (links to the repository) |

## Fill order

0. **Team and process:** **Team and Roles**, **Scrum Methodology**, **Definition of Ready (DoR)**, **Definition of Done (DoD)**, **Git, Pull Request and Jira Rules**, **AI Usage and Third-Party Attribution**, **Meeting Minutes — Template**. Set up the space at the same time: **Escala — Home**, the container pages, **Sprint 1 · Planning**, **Sprint 1 · Deliverables**, **Sprint 1 · Meeting Minutes**.
1. **Context:** **01 · Problem and Opportunity**, **Glossary**.
2. **Research and competitors, in parallel:** **03 · User Research** (with **Interview Guides and Consent**, **Observation Grid**, **Research Note — Template**, **Research Synthesis and Decisions**) and **02 · Market and Competitor Analysis**.
3. **Vision:** **04 · Product Vision** and its children (**Questions and Effective Answers**, **Product Vision Board**, **Personas**, **User Journeys**, **MVP, Success Criteria and Ethical Engagement**), then **Product Roadmap (GO)**.
4. **Requirements:** **Requirements and Quality Attributes**.
5. **Backlog:** **05 · Product Backlog**.
6. **Design:** **06 · Responsible AI Opportunity Assessment**, Git [technical design](architecture/technical-design.md) (paste to **07 · Technical Design**), **08 · Architecture Decision Records**, **09 · Security and Privacy Assessment**, and the [ADRs](adr/README.md).
7. **Test strategy:** [Test Strategy](testing/test-strategy.md) (Git).

- **In parallel from day 1:** **10 · Walking Skeleton** and [Deployment and Operations](operations/deployment-and-operations.md) (Git).
- **End of sprint:** **Sprint 1 · Review**, **Sprint 1 · Retrospective**, **Investor Decision Log** (one page per decision, from **Decision — Template**).

Fill **Sprint 2 · Deliverables** and **Sprint 3 · Deliverables** when those sprints are planned. Update the other pages at every review.

This order follows the deck's *Analysis → Design → Test plan → Code → Test execution*, but it is not a waterfall: the skeleton starts on day 1.

## Moving a page to Confluence

1. Create the container pages first (see [Container pages](#container-pages)).
2. Create a page with the exact title (the file's H1) under the right parent (see [Map to Confluence](#map-to-confluence)). Each member creates and edits their own pages with their own account, because page history is contribution evidence.
3. Paste the Markdown. Check that the tables and task lists came through.
4. Turn Guidance and Example blocks into Info/Note panels, or delete them. Turn Rule blocks into Warning panels and keep them.
5. Replace `[JIRA: …]` with the Jira Issues macro using that JQL.
6. Replace `[DIAGRAM: …]` with a draw.io diagram. Replace `[IMAGE: …]` with the image and `[MACRO: …]` with the named macro.
7. Add the labels from the header.
8. For pages marked **Yes** under *Exists already?* in the map, merge into the existing page instead of creating a new one, and rename it when the map says so. Page history is kept.
9. Register **Meeting Minutes — Template** as a Confluence space template, as its own guidance explains.
10. From then on, the Confluence page is the living document. Do not keep editing the Markdown copy in parallel.

## Rules

- **No fabricated research.** §5.3: *"Teams must not fabricate user-research evidence."* §11: *"Research results must not be fabricated or misrepresented."* A documented limitation is better than invented evidence.
- **Examples are illustrative.** Every Example block is fictional and only shows the format. Replace it with real content or delete it, and never copy it into real notes. Anything that looks like data (quotes, counts, minutes, dates) must come from a real, referenced source, such as a research note ID.
- **No real personal data.** Use participant codes (B-01, CS-01, CMD-01, OBS-01). No names, phone numbers or photos of people without consent (§11).
- **AI-assisted content is reviewed by a human.** §11: *"AI-generated content must be reviewed before being treated as authoritative."* Record the tool, where it was used and the reviewer on **AI Usage and Third-Party Attribution**. These templates were themselves drafted with an AI assistant, so record that there too.
- **Unverified facts stay marked.** Anything marked "(verify)", ❓ or `[TODO: …]` must be confirmed or resolved before a page is set to Done.
