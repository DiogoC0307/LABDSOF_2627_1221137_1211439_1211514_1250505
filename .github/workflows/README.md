# CI/CD workflows

This folder holds the GitHub Actions workflows for Escala.

## Workflows

| Workflow | Trigger | What it does |
|---|---|---|
| [ci.yml](build.yml) | Pull request to `main`, push to `main`, manual | For `apps/escala-api` and `apps/elegibilidade-worker` (Maven, Java 21): build, tests with JaCoCo coverage, SonarQube analysis. For `apps/web` (npm, Node.js 24): build, Vitest tests with lcov coverage, SonarQube analysis. Test and coverage reports are uploaded as run artefacts. |

### SonarQube configuration

The SonarQube steps need two repository secrets (*Settings → Secrets and variables → Actions*):

| Secret | Value |
|---|---|
| `SONAR_HOST_URL` | URL of the SonarQube server (e.g. `https://sonarcloud.io` or the self-hosted instance) |
| `SONAR_TOKEN` | Analysis token generated in SonarQube |

While `SONAR_TOKEN` is not set (or on pull requests from forks, which do not receive secrets), the SonarQube steps are skipped and the rest of the pipeline still runs. Project keys: `escala-escala-api`, `escala-elegibilidade-worker` (in each `pom.xml`) and `escala-web` (in `apps/web/sonar-project.properties`). When using SonarCloud, also set `sonar.organization`.

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

## Where this is documented

- Rules for branches, commits and pull requests: `docs/confluence/0-team-and-process/git-pr-and-jira-rules.md`
- Pipeline, deployment and rollback: `docs/operations/deployment-and-operations.md`
- Test types and CI stages: `docs/testing/test-strategy.md`
