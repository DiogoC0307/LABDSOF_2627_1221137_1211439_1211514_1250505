# Git, Pull Request and Jira Rules

| Field | Value |
|---|---|
| Status | Draft |
| Owner | [TODO: name] |
| Contributors | [TODO: names] |
| Deliverable | — |
| Sources | Assignment §8.1, §8.2; Deck (Git and smart commits) |
| Confluence parent | 0 · Team and Process |
| Labels | `team-process`, `git`, `jira` |
| Jira | [TODO: ESC-xx] |
| Last updated | YYYY-MM-DD |

> **ℹ️ About this page.** The team's rules for the repository, branches, commits, pull requests and code reviews, and how the Jira project `ESC` is set up and linked to GitHub (Assignment §8.1, §8.2). Every member reads it before their first commit; reviewers use it to check that each piece of work is traceable from Jira issue to merged code.

## Repository layout

One monorepo; one folder per deployable component.

```text
.
├── apps/
│   ├── escala-api/             Spring Boot — people, rosters, slots, unavailability, requests, audit
│   ├── elegibilidade-worker/   Python — evaluations, extractions, observed risk; talks to IPMA and the language model
│   └── web/                    Angular PWA
├── contracts/                  openapi.yaml and events/ (shared API and event schemas)
├── tools/
│   └── ipma-sim/               IPMA simulator container
├── infra/                      Docker Compose, Prometheus, Grafana
├── docs/                       Git-only docs (ADRs, technical design, test strategy, operations) and the Confluence page templates (confluence/)
├── .github/                    GitHub Actions workflows, pull_request_template.md
└── README.md                   how to build, run and test
```

> **ℹ️ Guidance.** Keep the tree in step with the repository and change it in the same PR that moves a folder. The monorepo choice is ADR 0004 (Monorepo), indexed on **08 · Architecture Decision Records**. Contracts live in one place so that a schema change reaches `escala-api` and `elegibilidade-worker` in the same pull request.

## Branching

- **Trunk-based.** `main` is always releasable; every deployment is built from it.
- **`main` is protected:** changes only through a pull request with **one approval** from another member and **CI green**; no direct pushes, no force-pushes.
- **Short-lived branches** from `main`, named `<type>/ESC-<n>-<short-description>`: `feat/ESC-23-create-piquete`, `fix/…`, `docs/…`, `chore/…`. The Jira key in the name is what links the branch to the issue.
- No PR stays open more than 48 h (working agreement on **Team and Roles**); delete the branch after merging. Bring `main` into your branch at least daily.

> **ℹ️ Guidance.** Configure the protection once in GitHub (branch protection rule or ruleset on `main`) and add a screenshot: [IMAGE: `main` branch protection settings]. Choose one merge method, [TODO: squash or merge commit], and set it in the repository; with squash, also set the default squash commit message to the pull request title (verify in the repository settings) so the PR title becomes the commit on `main`; the title must therefore follow the commit format below.

> **✏️ Example (illustrative — replace).** Member A picks ESC-23 (create a *piquete* with one slot), branches `feat/ESC-23-create-piquete` from `main`, opens a draft PR on day 1, asks for review on day 2; Member B approves, CI is green, the branch is merged and deleted.

## Commits

Format: [Conventional Commits](https://www.conventionalcommits.org/) plus the Jira key — `<type>(<scope>): <summary> (ESC-<n>)`.
- **Types:** `feat`, `fix`, `docs`, `test`, `refactor`, `chore`, `ci`.
- **Scope:** the area touched, e.g. `roster`, `eligibility`, `risk`, `ai`, `infra`.

Deck rules:
- "Every commit must reference a ticket"
- "Pull frequently" · "Commit frequently" · "Push frequently"
- "Do not commit broken code"
- "Always test before pushing"

> **ℹ️ Guidance.** Write the summary in the imperative and say what changed, not how long it took. When two members pair, add a `Co-authored-by:` trailer for the partner so both appear as authors (§10 counts "Authored or co-authored pull requests"). Commit from your own account only: "Raw commit counts will not be treated as reliable evidence of contribution" (§8.1), but authorship still has to be right.

> **✏️ Example (illustrative — replace).**
> - ✅ Good: `feat(roster): create piquete with one slot (ESC-23)` — type, scope, what changed, Jira key.
> - ❌ Bad: `changes` — no type, no scope, no ticket; nobody can tell what changed or why.

## Smart commits

"Use smart commits for traceability and automated workflow" (deck). A smart-commit line starts with the Jira key, followed by a command:

- `ESC-23 #comment add POST /piquetes` — adds a comment to ESC-23
- `ESC-23 #time 1h 30m` — logs 1 h 30 min of work on ESC-23 (sprint tasks are estimated in hours, deck M7)
- `ESC-23 #<transition>` — moves the issue through the workflow; transitions use the ESC workflow names, e.g. `#in-review` [TODO: verify the exact transition names and how Jira expects spaces in them]

> **ℹ️ Guidance.** Keep the Conventional Commit line as the subject and put smart-commit lines in the commit body. Smart commits only run when the commit author's email matches the member's Jira account (verify in the Jira settings). Never move an issue to *Done* or *Ready* by smart commit: *Done* needs the **Definition of Done (DoD)** check and only the Product Owner moves items to *Ready*.

> **✏️ Example (illustrative — replace).**
> ```text
> feat(roster): create piquete with one slot (ESC-23)
>
> ESC-23 #comment add POST /piquetes
> ESC-23 #time 1h 30m
> ```

## Pull requests

- One PR per Jira issue; the title follows the commit format and carries the key.
- Open a draft PR early; mark it ready for review when CI is green and the author's checklist items are ticked.
- At least one approval from another member; nobody approves their own PR (**Team and Roles**).
- Small enough to review in one sitting; split it otherwise.
- Mention AI-assisted parts in "What changes"; the approving reviewer is the recorded human reviewer (**AI Usage and Third-Party Attribution**).
- The author merges after approval and deletes the branch.

PR template, stored in the repository as `.github/pull_request_template.md`:

```markdown
### What changes
<!-- One or two sentences: what this PR changes and why. Mention any AI-assisted parts. -->

### Jira issue
<!-- Jira key and issue title, e.g. ESC-31 — Reject assigning someone whose TAS has expired. -->

### How it was tested
<!-- Unit tests added or changed; functional tests run, by TC- ID, and where the result is recorded. -->

### Checklist — Definition of Done (code items)
- [ ] Analysis and design updated
- [ ] Code committed with a proper message referencing the Jira key
- [ ] Unit tests developed and passing
- [ ] Functional tests performed and recorded
- [ ] Acceptance criteria verified
- [ ] PR reviewed and approved by another member
- [ ] CI green (build, tests, static analysis, dependency check)
- [ ] ADR or docs updated if a decision changed
- [ ] No secrets in code
- [ ] Demonstrable without hidden manual steps
```

> **ℹ️ Guidance.** The checklist is "DoD for code items" on **Definition of Done (DoD)** word for word, which already covers "analysis and design updated" and "no secrets"; change both together. The reviewer ticks "PR reviewed and approved by another member" when approving. An unticked item needs a one-line reason in the PR, or the PR is not merged.

## Code review checklist

- **Scope.** The change does what the Jira issue and its acceptance criteria say, and nothing unrelated.
- **Tests.** Every acceptance criterion, including the boundary case, has a test with its `TC-` ID; the tests would fail if the rule were removed.
- **Business rules.** Eligibility is decided by the deterministic engine and returns rule codes (`RNxx`); AI output is never used to decide it (ADR 0007).
- **Contracts and messaging.** API or event changes are made in `contracts/` first; consumers are idempotent (idempotency key) and events go through the outbox.
- **Security and privacy.** No secrets, no real personal data in fixtures or logs; new endpoints check the role; free-text unavailability is never logged and is deleted after confirmation (RNF09).
- **Operability and docs.** Logs are structured and carry the trace ID; failures show a visible state (pending / failed); the ADR or docs changed if a decision changed.

> **ℹ️ Guidance.** Read the diff and run it locally when it is not trivial. Write comments that name the problem and a way forward; use "Request changes" only for something that breaks the DoD. Reviews are contribution evidence (§10 "Code reviews"), so review in GitHub, not in chat.

> **✏️ Example (illustrative — replace).** Review comment on the ESC-31 PR: "The RN02 check rejects only when the TAS expiry date is before the slot date, so a TAS that expires on the slot date passes. `TC-ESC-31-02` expects a rejection. Please treat the expiry date itself as expired and add that test to the CI run."

## Jira

Project key: **`ESC`**. Jira is where the team manages the backlog, plans sprints, assigns and tracks work, records decisions, documents sprint goals, monitors progress and records stakeholder feedback (§8.2).

### Issue types

| Type | Use for |
|---|---|
| Epic | One of the nine product areas E1–E9 below; groups stories across sprints |
| Story | A vertical slice of user value in story format; meets the **Definition of Ready (DoR)** before a sprint; estimated in story points, its sprint sub-tasks in hours |
| Task | Work with no user-facing value of its own, e.g. writing a Sprint 1 deliverable page or setting up CI |
| Spike | A time-boxed investigation that reduces a risk and ends in a finding or an ADR, e.g. where the AI model runs (ADR 0011) |
| Bug | A defect against an acceptance criterion or business rule, with steps to reproduce |
| **Decision** | A product or technical decision: options, choice, rationale and who decided; investor decisions are also recorded on **Investor Decision Log** |
| **Feedback** | Stakeholder or investor feedback, e.g. from a Sprint Review: source, what was said, what the team did about it |

> **ℹ️ Guidance.** Decision and Feedback are custom issue types: create them in the project settings, because §8.2 requires Jira to record decisions and stakeholder feedback. Link each Feedback issue to the Decision or Story it caused.

[JIRA: project = ESC AND issuetype in (Decision, Feedback) ORDER BY created DESC]

### Custom fields

- **Story Points** — Fibonacci 1, 2, 3, 5, 8, 13, 21; set by the developers in Planning Poker (**Scrum Methodology**).
- **Business rule** — `RN` IDs, e.g. `RN02` (**Requirements and Quality Attributes**).
- **Requirement** — `RF` / `RNF` IDs, e.g. `RF08`, `RNF04`.
- **Deliverable** — the assignment deliverable the item feeds, e.g. `Sprint 1 · #10 Walking Skeleton` (**Sprint 1 · Deliverables**).

### Workflow

`Backlog → Ready → In Progress → In Review → Done`. Only the Product Owner moves items to *Ready* (**Definition of Ready (DoR)**); *In Review* means a PR is open; *Done* requires the **Definition of Done (DoD)**. Entry rules and the board are on **Scrum Methodology**.

### Labels

- `risk` — on the risk list; ordered first ("Risky, uncertain items go first", deck M6)
- `deliverable-s1` — produces or feeds a Sprint 1 deliverable (the Deliverable field says which)
- `external` — waits on someone outside the team, e.g. IPMA, the corps or the teaching staff
- `decision` — implements, or is blocked by, a Decision issue
- `feedback` — created or changed because of a Feedback issue

### Components

`escala-api`, `elegibilidade-worker`, `web`, `infra`, `docs` — one per area of the repository layout above.

### Epics

- E1 People & certifications — [TODO: ESC-xx]
- E2 Roster & slots — [TODO: ESC-xx]
- E3 Unavailability & coverage — [TODO: ESC-xx]
- E4 Eligibility — [TODO: ESC-xx]
- E5 Risk & reinforcement (IPMA) — [TODO: ESC-xx]
- E6 Responsible AI — [TODO: ESC-xx]
- E7 Platform & operations — [TODO: ESC-xx]
- E8 Discovery & product — [TODO: ESC-xx]
- E9 Architecture, security & privacy — [TODO: ESC-xx]

[JIRA: project = ESC AND issuetype = Epic ORDER BY key ASC]

> **✏️ Example (illustrative — replace).** ESC-23 "Create a *piquete* with one slot": Type Story · Epic E2 Roster & slots · Components `escala-api`, `web` · Requirement RF06 · Deliverable `Sprint 1 · #10 Walking Skeleton` · Label `deliverable-s1` · Story Points from Planning Poker.

## GitHub ↔ Jira

Install *GitHub for Jira* and connect the repository. Any branch, commit or PR whose name or message contains the Jira key (e.g. `ESC-23`) is linked to that issue, and so are the GitHub Actions builds that run on it. The issue's Development panel then lists the linked branches, the commits with their authors and the pull requests with their authors and reviewers (verify what your Jira site shows), which gives individual contribution evidence for free (see **Team and Roles**).

> **ℹ️ Guidance.** Installing needs admin rights on both the Jira site and the GitHub organisation (verify); record who connected it and when: [TODO: name, date]. Check it once with a test branch: if the branch does not appear in the issue's Development panel, the key is missing or misspelt.

## Done when
- [ ] `main` is protected in GitHub (one approval, CI green, no direct pushes) and the screenshot is on this page
- [ ] `.github/pull_request_template.md` in the repository matches the template above and "DoD for code items" on **Definition of Done (DoD)**
- [ ] The ESC project has the seven issue types, four custom fields, workflow, labels, components and epics E1–E9 listed here, and the smart-commit transition names are verified
- [ ] *GitHub for Jira* is connected and a branch, commit and PR for one ESC issue appear in its Development panel
- [ ] Reviewed by another team member
- [ ] Guidance and example blocks removed
- [ ] Status set to Done
