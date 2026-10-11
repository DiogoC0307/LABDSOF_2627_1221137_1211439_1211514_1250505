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
| Jira | SCRUM-10, SCRUM-21 |
| Last updated | YYYY-MM-DD |

> **ℹ️ About this page.** The team's rules for the repository, branches, commits, pull requests and code reviews, and how the Jira project `SCRUM` is set up and linked to GitHub (Assignment §8.1, §8.2). Every member reads it before their first commit; reviewers use it to check that each piece of work is traceable from Jira issue to merged code.

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

> **ℹ️ Guidance.** This is the target layout: a folder is created by the first PR that needs it. Keep the tree in step with the repository and change it in the same PR that moves a folder. The monorepo choice is ADR 0004 (Monorepo), indexed on **08 · Architecture Decision Records**. Contracts live in one place so that a schema change reaches `escala-api` and `elegibilidade-worker` in the same pull request.

## Branching

- **Trunk-based.** `main` is always releasable; every deployment is built from it.
- **Pull requests only.** `main` changes only through a pull request with **one approval** from another member and **CI green**. No direct pushes, no force-pushes. Until the pipeline exists (SCRUM-20, "CI pipeline v0"), the approval alone is enough.
- **Documentation too.** A change under `docs/` follows the same rules as code: a branch, a Jira key and a reviewed pull request.
- **Merge commits.** A PR is merged with a merge commit, so each member's commits stay on `main` under their own name. Squash and rebase merging are turned off.
- **Short-lived branches** from `main`, named `<type>/SCRUM-<n>-<short-description>`: `feat/SCRUM-45-create-piquete`, `fix/…`, `docs/…`, `chore/…`. The Jira key in the name is what links the branch to the issue.
- **48 hours.** No PR stays open longer (working agreement on **Team and Roles**). Bring `main` into your branch at least daily and delete the branch after merging.

> **✏️ Example (illustrative — replace).** Member A picks SCRUM-45 (create a *piquete* with one slot), branches `feat/SCRUM-45-create-piquete` from `main`, opens a draft PR on day 1, asks for review on day 2; Member B approves, CI is green, the branch is merged and deleted.

### How the pull-request rule is checked

GitHub does not enforce the rule for us: the repository is private on a GitHub Free personal account, where branch protection is not available. The team enforces it instead.

- **In effect since:** [TODO: date]
- **Check:** at every Sprint Review, run `git log --first-parent main`. Every commit after that date must be a pull-request merge.
- **Exceptions:** `182ae6f` — the documentation templates, committed before these rules were agreed

> **ℹ️ Guidance.** Set the repository once: allow merge commits only (turn off squash and rebase merging) and turn on *Automatically delete head branches*. If the repository ever moves to a plan with branch protection, or becomes public, protect `main` and add a screenshot here.

## Commits

Format: [Conventional Commits](https://www.conventionalcommits.org/) plus the Jira key — `<type>(<scope>): <summary> (SCRUM-<n>)`.
- **Types:** `feat`, `fix`, `docs`, `test`, `refactor`, `chore`, `ci`.
- **Scope:** the area touched, e.g. `roster`, `eligibility`, `risk`, `ai`, `infra`.

Deck rules:
- "Every commit must reference a ticket"
- "Pull frequently" · "Commit frequently" · "Push frequently"
- "Do not commit broken code"
- "Always test before pushing"

> **ℹ️ Guidance.** Write the summary in the imperative and say what changed, not how long it took. When two members pair, add a `Co-authored-by:` trailer for the partner so both appear as authors (§10 counts "Authored or co-authored pull requests"). Commit from your own account only: "Raw commit counts will not be treated as reliable evidence of contribution" (§8.1), but authorship still has to be right.

> **✏️ Example (illustrative — replace).**
> - ✅ Good: `feat(roster): create piquete with one slot (SCRUM-45)` — type, scope, what changed, Jira key.
> - ❌ Bad: `changes` — no type, no scope, no ticket; nobody can tell what changed or why.

## Smart commits

"Use smart commits for traceability and automated workflow" (deck). A smart-commit line starts with the Jira key, followed by a command. The team uses one command:

- `SCRUM-45 #comment add POST /piquetes` — adds a comment to SCRUM-45

The team does not log time in Jira, so `#time` is not used. Issues are not moved by smart commit either: the board is updated by hand, so that *Ready* and *Done* always pass through a person.

> **ℹ️ Guidance.** Keep the Conventional Commit line as the subject and put smart-commit lines in the commit body. Smart commits only run when the commit author's email matches the member's Jira account (verify in the Jira settings). Moving an issue stays manual because *Done* needs the **Definition of Done (DoD)** check and only the Product Owner moves items to *Ready*.

> **✏️ Example (illustrative — replace).** A commit message with a smart-commit line in its body:

```text
feat(roster): create piquete with one slot (SCRUM-45)

SCRUM-45 #comment add POST /piquetes
```

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
<!-- Jira key and issue title. The key must also be in the branch name and the PR title. -->

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

> **✏️ Example (illustrative — replace).** Review comment on the SCRUM-999 PR: "The RN02 check rejects only when the TAS expiry date is before the slot date, so a TAS that expires on the slot date passes. `TC-SCRUM-999-02` expects a rejection. Please treat the expiry date itself as expired and add that test to the CI run."

## Jira

Project key: **`SCRUM`**. Jira is where the team manages the backlog, plans sprints, assigns and tracks work, records decisions, documents sprint goals, monitors progress and records stakeholder feedback (§8.2).

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

> **ℹ️ Guidance.** Decision and Feedback are custom issue types, already created in the project; §8.2 requires Jira to record decisions and stakeholder feedback. Link each Feedback issue to the Decision or Story it caused. Sub-tasks also exist, for the hour-level tasks of a story. In Jira the standard types have Portuguese names (Story is *História*, Task is *Tarefa*, Sub-task is *Subtarefa*, Bug is *Problema*); JQL needs those names, except for Epic.

[JIRA: project = SCRUM AND issuetype in (Decision, Feedback) ORDER BY created DESC]

### Custom fields

- **Story point estimate** — Fibonacci 1, 2, 3, 5, 8, 13, 21; set by the developers in Planning Poker (**Scrum Methodology**).
- **Business rule** — `RN` IDs, comma-separated, e.g. `RN07, RN10` (**Requirements and Quality Attributes**).
- **Requirement** — `RF` / `RNF` IDs, comma-separated, e.g. `RF31, RF32`.
- **Deliverable** — the number of the Sprint 1 deliverable the item feeds, e.g. `10` for the walking skeleton (**Sprint 1 · Deliverables**).

### Workflow

`Backlog → Ready → In Progress → In Review → Done`. Only the Product Owner moves items to *Ready* (**Definition of Ready (DoR)**); *In Review* means a PR is open; *Done* requires the **Definition of Done (DoD)**. Entry rules and the board are on **Scrum Methodology**.

### Labels

- `risk` — on the risk list; ordered first ("Risky, uncertain items go first", deck M6)
- `deliverable-s1` — produces or feeds a Sprint 1 deliverable (the Deliverable field says which)
- `external` — waits on someone outside the team, e.g. IPMA, the corps or the teaching staff
- `decision` — implements, or is blocked by, a Decision issue
- `feedback` — created or changed because of a Feedback issue
- `comp-escala-api`, `comp-elegibilidade-worker`, `comp-web`, `comp-infra`, `comp-docs` — the area of the repository the item touches; the project has no Jira components, so the area is a label

### Epics

| Epic | Name | Jira key |
|---|---|---|
| E1 | People & certifications | SCRUM-8 |
| E2 | Roster & slots | SCRUM-9 |
| E3 | Unavailability & coverage | SCRUM-11 |
| E4 | Eligibility | SCRUM-12 |
| E5 | Risk & reinforcement (IPMA) | SCRUM-13 |
| E6 | Responsible AI | SCRUM-14 |
| E7 | Platform & operations | SCRUM-15 |
| E8 | Discovery & product | SCRUM-16 |
| E9 | Architecture, security & privacy | SCRUM-17 |

[JIRA: project = SCRUM AND issuetype = Epic ORDER BY key ASC]

> **✏️ Example (illustrative — replace).** SCRUM-45 "Create a *piquete* with one slot": Type Story · Epic E2 Roster & slots · Labels `comp-escala-api`, `comp-web`, `deliverable-s1` · Requirement RF06 · Deliverable `10` · Story point estimate from Planning Poker.

## GitHub ↔ Jira

*GitHub for Jira* is installed and connected to the repository. Any branch, commit or PR whose name or message contains the Jira key (e.g. `SCRUM-45`) is linked to that issue, and so are the GitHub Actions builds that run on it. The issue's Development panel then lists the linked branches, the commits with their authors and the pull requests with their authors and reviewers (verify what your Jira site shows), which gives individual contribution evidence for free (see **Team and Roles**).

> **ℹ️ Guidance.** Record who connected it and when: [TODO: name, date]. No issue shows development information yet, so check it once with a test branch: if the branch does not appear in the issue's Development panel, the key is missing or misspelt.

## Done when
- [ ] The repository allows merge commits only and deletes head branches automatically
- [ ] Every commit on `main` since the rules took effect came from an approved pull request with CI green, or is listed as an exception on this page
- [ ] `.github/pull_request_template.md` in the repository matches the template above and "DoD for code items" on **Definition of Done (DoD)**
- [ ] The SCRUM project has the seven issue types, four custom fields, workflow, labels and epics E1–E9 listed here
- [ ] *GitHub for Jira* is connected and a branch, commit and PR for one SCRUM issue appear in its Development panel
- [ ] Reviewed by another team member
- [ ] Guidance and example blocks removed
- [ ] Status set to Done
