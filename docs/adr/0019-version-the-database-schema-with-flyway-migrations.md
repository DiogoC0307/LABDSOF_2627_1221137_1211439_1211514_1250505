# 0019 · Version the database schema with Flyway migrations

| Field | Value |
|---|---|
| Date | 2026-11-09 |
| Status | Proposed |
| Deciders | [TODO: names, when the team accepts it] |

## Context

Until release 0.3.0 each service created and changed its own schema at start-up with `spring.jpa.hibernate.ddl-auto=update`. Hibernate adds tables and columns but never changes a constraint that already exists. When `PENDING` and `ELIGIBLE` were added to `SlotStatus`, the dev database kept its old `slot_status_check`, and `POST /api/v1/piquetes` answered `500` on 2026-11-01. It was fixed by deleting the dev database, which is not possible once data has to be kept. Every Sprint 2 story adds tables, columns or enum values (members, certifications, unavailability, swaps), and a rollback must keep working (RNF08). SCRUM-87.

## Decision

We will version the schema of each service with Flyway SQL migrations in `src/main/resources/db/migration` of that service, and run them when the service starts. Hibernate only validates the entities against the schema (`spring.jpa.hibernate.ddl-auto=validate`).

- `V1__baseline.sql` is the schema that `ddl-auto=update` created up to 0.3.0, with the same constraint names, taken from `pg_dump --schema-only`.
- A database created before Flyway has the tables but no `flyway_schema_history`. `spring.flyway.baseline-on-migrate=true` with `baseline-version=1` records it as version 1 without running V1, so its data is kept.
- Every migration works with the previous version of the service (expand, then contract), so rolling back the image never needs a database restore. The rule is in [Deployment and Operations](../operations/deployment-and-operations.md#deploy).
- A migration that has been merged is never edited; a fix is a new migration.

## Alternatives considered

| Alternative | Why rejected |
|---|---|
| Keep `ddl-auto=update` | It caused the dev `500` of 2026-11-01: it never changes or drops constraints, columns or types, so the schema drifts from the entities with no record of what ran where. |
| Liquibase | Does the same job, but changes are written in XML or YAML changelogs and need more configuration. Plain SQL migrations are easier to review in a pull request, and Spring Boot sets up both the same way. |
| Hand-written SQL scripts run by an operator | Only Diogo Costa can reach the databases (SCRUM-92), there would be no record of what ran in each environment, and every deploy would need a manual step. |

## Consequences

- **Positive:**
  - Every schema change is a reviewed file in the pull request, and `flyway_schema_history` records what ran in each database.
  - The integration tests (Testcontainers) build their database from the same migrations that run in dev, qa and prod.
  - A changed enum or constraint ships as a migration, so the dev `500` of 2026-11-01 cannot happen again unnoticed.
- **Negative:**
  - A change to an entity now needs a migration as well; if they disagree, the service refuses to start (`validate`).
  - Removing or renaming a column takes two releases: stop using it, then drop it.
- **Trade-offs:** we accept the extra file per change, in exchange for schema changes that are visible, repeatable and safe to roll back.

## Assignment requirement served

§6.7 and §8.5 (reproducible deployment and rollback); RNF08.

## Related

- Jira: SCRUM-87
- Requirements: RNF08
- Supersedes: —
- Other ADRs or pages: 0003 (one database per component), 0018 (Docker Compose and Kubernetes); [Deployment and Operations](../operations/deployment-and-operations.md)
- Date evidence: proposed on 2026-11-09 in the pull request for SCRUM-87, after the dev incident of 2026-11-01.
