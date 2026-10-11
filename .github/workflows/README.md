# CI/CD workflows

This folder holds the GitHub Actions workflows for Escala. **No workflow exists yet**: the first one is created by the Jira task "CI pipeline v0".

## What the pipeline is for

Assignment §8.5 asks for an automated pipeline that gives "rapid and visible feedback when a change introduces a defect". Every pull request to `main` must pass it before it is merged ("CI green" in the pull-request rules and in the Definition of Done).

## Planned stages

| Stage | What it does | Runs on |
|---|---|---|
| Build | Compiles and packages `apps/escala-api`, `apps/elegibilidade-worker` and `apps/web` | every pull request |
| Tests | Unit and integration tests | every pull request |
| Static analysis | Linters and code-quality checks | every pull request |
| Dependency check | Dependency and secret scanning | every pull request |
| Image | Builds the container images | merge to `main` |
| Deploy | Deploys to the evaluation environment | merge to `main` |

The split between pull request and merge is a proposal; confirm it when the first workflow is written.

## Until the first workflow exists

"CI green" cannot be checked, so a pull request needs only the approval of another member.

## Where this is documented

- Rules for branches, commits and pull requests: `docs/confluence/0-team-and-process/git-pr-and-jira-rules.md`
- Pipeline, deployment and rollback: `docs/operations/deployment-and-operations.md`
- Test types and CI stages: `docs/testing/test-strategy.md`
