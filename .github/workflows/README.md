# CI/CD workflows

This folder holds the GitHub Actions workflows for Escala. They build and test every change, and deploy `main` to dev, release branches to qa and GitHub Releases to prod.

## Workflows

| Workflow | Trigger | What it does |
|---|---|---|
| [build.yml](build.yml) (*Build*) | Pull request to `main`, push to `main`, manual | Three jobs. `backend` (one run each for `apps/escala-api` and `apps/elegibilidade-worker`: Maven, Java 21): build, `verify` with tests and JaCoCo coverage, SonarQube analysis. `web` (`apps/web`: npm, Node.js 24): build, Vitest tests with lcov coverage, SonarQube analysis. `security`: secret scan, dependency checks and SBOMs, see [Security job](#security-job); it never fails the build. Test and coverage reports are uploaded as run artefacts (`escala-api-reports`, `elegibilidade-worker-reports`, `web-reports`). A new run on the same ref cancels the previous one. |
| [build-images.yml](build-images.yml) (*Build images*) | Called by `deploy-dev.yml` and `deploy-qa.yml` (`workflow_call`) | Builds the three images (`escala-api`, `elegibilidade-worker`, `escala-web`), each from the Dockerfile in its `apps/` folder, and pushes them to `ghcr.io/<owner>/<image>`. Every image is tagged with the commit SHA; optionally also with a version tag and `latest`. |
| [deploy.yml](deploy.yml) (*Publish manifests*) | Called by the three deploy workflows (`workflow_call`) with `environment`, `overlay` and `tag` | Sets the image tags in `infra/k8s/<overlay>` (in the runner only), renders the manifests with `kubectl kustomize` and pushes them as `manifests.yaml` to the branch `deploy/<overlay>`, with the commit message `deploy(<overlay>): <tag> (source <sha>)`. Argo CD applies that branch. No cluster credentials are used. One publish at a time per overlay. |
| [deploy-dev.yml](deploy-dev.yml) (*Deploy DEV*) | Push to `main`, manual | Builds the images (tags: commit SHA and `latest`) and publishes `deploy/dev`. It does not wait for *Build*: both start on the same push. |
| [deploy-qa.yml](deploy-qa.yml) (*Deploy QA*) | Push to a `release/**` branch | Requires the branch name `release/MAJOR.MINOR.PATCH`. Builds the images (tags: commit SHA and `X.Y.Z-rc.<run number>`) and publishes `deploy/qa`. |
| [deploy-prod.yml](deploy-prod.yml) (*Deploy PROD*) | A GitHub Release is published | Requires a tag `vX.Y.Z` on the branch `release/X.Y.Z`. Retags the images of that commit as `X.Y.Z` (nothing is rebuilt; it fails if QA never built that commit) and publishes `deploy/prod`. The GitHub Environment `production` can require approval. |

How the deployment works end to end (Argo CD, environments, releasing, rollback) is in the [README](../../README.md#environments-and-deployment) and in [deployment-and-operations.md](../../docs/operations/deployment-and-operations.md).

## Stages as they are today

| Stage | Where | Tool | Runs on |
|---|---|---|---|
| Build | `build.yml`, jobs `backend` and `web` | Maven wrapper (Temurin 21); npm (Node.js 24) | every pull request and push to `main` |
| Tests | same jobs | JUnit with Testcontainers and JaCoCo (Java services); Vitest with coverage (`apps/web`) | every pull request and push to `main` |
| Static analysis | same jobs, SonarQube step | SonarQube | every pull request and push to `main`, only when `SONAR_TOKEN` is set |
| Secret scan, dependency check, SBOM | `build.yml`, job `security` | gitleaks, `npm audit`, OSV-Scanner, CycloneDX | every pull request and push to `main`; reports only, never fails |
| Image | `build-images.yml` | Docker Buildx, GitHub Container Registry | push to `main` (dev), push to `release/**` (qa); prod retags |
| Deploy | `deploy*.yml` | Kustomize, Argo CD | dev: push to `main`; qa: push to `release/**`; prod: Release published |

[TODO: are the *Build* jobs required status checks in the ruleset of `main` (SCRUM-20 asks for it), and is the `production` GitHub Environment configured with required reviewers, Diogo Costa]

### Security job

The `security` job of `build.yml` is non-blocking on purpose: the job and every step use `continue-on-error`, and gitleaks exits 0. It runs:

- **gitleaks** over the history of the checked-out commit, with `--redact`. [`.gitleaks.toml`](../../.gitleaks.toml) extends the default rules and exempts two verified false positives, each explained in the file: the SonarQube project key in `build.yml`, and the Sealed Secrets manifests (`infra/k8s/**/sealed-secret*.yaml`, encrypted by design). The generated `deploy/*` branches are not scanned.
- **`npm audit`** in `apps/web`, for production dependencies and for all dependencies.
- **OSV-Scanner** on `apps/web/package-lock.json` and on the CycloneDX SBOMs of the two Maven services (it cannot evaluate their `pom.xml` files directly, because the versions are inherited from `spring-boot-starter-parent`).
- **CycloneDX SBOMs** of the three components, uploaded as the artefact `sbom`; the JSON reports are uploaded as `security-reports`.
- A summary with the findings per tool in the job summary, produced by [`.github/scripts/security-summary.py`](../scripts/security-summary.py).

Making these steps blocking, and from which severity, is still to be decided (SCRUM-20).

### SonarQube configuration

The SonarQube steps need two repository secrets (*Settings → Secrets and variables → Actions*):

| Secret | Value |
|---|---|
| `SONAR_HOST_URL` | URL of the SonarQube server |
| `SONAR_TOKEN` | Analysis token generated in SonarQube |

While `SONAR_TOKEN` is not set (or on pull requests from forks, which do not receive secrets), the SonarQube steps are skipped and the rest of the pipeline still runs. The organisation is `diogoc0307` (in each `pom.xml` and in the `web` step).

**Known problem: one project key for three components.** `build.yml` passes the same `-Dsonar.projectKey=DiogoC0307_LABDSOF_2627_1221137_1211439_1211514_1250505` to the two Maven services and to the web app, while the `pom.xml` files and `apps/web/sonar-project.properties` declare `escala-escala-api`, `escala-elegibilidade-worker` and `escala-web`. The value on the command line wins, so the three analyses go to the same project and each one overwrites the previous one. The keys in the `pom.xml` files and in `sonar-project.properties` are not changed because that needs three projects to be created first. [TODO: create the three projects in SonarCloud and then remove the `-Dsonar.projectKey` override from `build.yml`, Diogo Costa]

## What the pipeline is for

Assignment §8.5 asks for an automated pipeline that gives "rapid and visible feedback when a change introduces a defect". Every pull request to `main` must pass it before it is merged ("CI green" in the pull-request rules and in the Definition of Done).

## Where this is documented

- Rules for branches, commits and pull requests: the Confluence page *Git, Pull Request and Jira Rules* in the [Confluence space](https://isep-team-apn94608.atlassian.net/wiki/spaces/L).
- Pipeline, deployment and rollback: `docs/operations/deployment-and-operations.md`
- Test types and CI stages: `docs/testing/test-strategy.md`
