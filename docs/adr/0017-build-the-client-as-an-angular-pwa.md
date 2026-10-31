# 0017 · Build the client as an Angular PWA

| Field | Value |
|---|---|
| Date | 2026-10-31 |
| Status | Proposed |
| Deciders | Diogo Pereira, Diogo Sousa, Pedro Costa, Diogo Costa |

## Context

Escala's members check their shifts, say when they cannot serve, swap and accept cover requests on their own phones, which are a mix of Android and iPhone. The supervisor and the station computers use the same screens on a desktop browser. Members often check the roster where the signal is weak, so the last loaded roster must stay readable offline (RNF06). The corps has no budget or process for publishing and updating apps in two stores, and the team has three sprints.

The client has been an Angular project since the repository was created (`apps/web`, Angular 21), but no ADR recorded why.

## Decision

We will build one client, `apps/web`, as an Angular Progressive Web App: installable from the browser on Android and iOS, with a service worker (`@angular/service-worker`, `ngsw-config.json`) that caches the app shell and the last roster for offline reading. The same app serves the member, supervisor and station views. It talks only to `escala-api`, over REST/HTTPS (ADR 0002, ADR 0006).

## Alternatives considered

| Alternative | Why rejected |
|---|---|
| Native apps (Kotlin and Swift) | Two code bases and two store publication processes for a team of four in three sprints; every fix waits for store review |
| Cross-platform native (Flutter, React Native) | Still needs store publication, and the station computers would need a separate web build |
| Server-rendered web pages without a PWA | No offline reading of the last roster (RNF06) and no install on the home screen |
| React or Vue PWA | Technically equivalent; Angular's CLI already includes the service worker, routing and test setup, and the client was started in Angular when the repository was created |

## Consequences

- **Positive:** one code base for phones and desktops; updates reach every member on the next load, with no store; offline reading through the service worker.
- **Negative:** push notifications on iOS work only after the PWA is added to the home screen (iOS 16.4 or later), so notifications (RF20) need an in-app fallback; the service worker cache must be invalidated on each release.
- **Trade-offs:** we give up native device features we do not need (background location, deep OS integration) in exchange for one deployable client.

## Assignment requirement served

§6.1 and §7.1.2 #7 (technology selection with rationale); RNF06 (roster readable offline).

## Related

- Jira: SCRUM-46 (today's *piquete* on the installable phone PWA)
- Requirements: RNF06, RF10, RF20
- Supersedes: —
- Other ADRs or pages: 0002; 0006; **07 · Technical Design** (technology selection); **08 · Architecture Decision Records** (index)
- Date evidence: written on 2026-10-31 and proposed in the pull request that adds it (SCRUM-40); the Angular client already exists in `apps/web`, and SCRUM-46 (installable phone PWA) is merged.
