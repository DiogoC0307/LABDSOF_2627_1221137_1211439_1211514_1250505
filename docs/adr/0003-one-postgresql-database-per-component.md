# 0003 · Give each component its own PostgreSQL database

| Field | Value |
|---|---|
| Date | 2026-10-11 |
| Status | Accepted |
| Deciders | Diogo Pereira, Diogo Sousa, Pedro Costa, Diogo Costa |

## Context

§6.4 asks teams not to introduce multiple database technologies unless the product requires it, and to be explicit about ownership. Cross-database foreign keys would couple the two deployables from ADR 0002.

## Decision

We will use PostgreSQL for both stores: `escala-db` owned only by `escala-api`, and `elegibilidade-db` owned only by `elegibilidade-worker`. No component reads or writes the other component's database. Cross-service identifiers travel on the events between the two services.

## Alternatives considered

| Alternative | Why rejected |
|---|---|
| One shared PostgreSQL schema | Either service could break the other's data; independent deploy and restore would be unsafe |
| PostgreSQL for the API and a different store for the worker | Two technologies with no requirement that justifies the complexity (§6.4) |

## Consequences

- **Positive:** one technology to operate; each service can migrate and restore its own data.
- **Negative:** no SQL join across people and evaluations; the worker must receive the facts it needs in the event.
- **Trade-offs:** we duplicate a few identifiers in events rather than share a schema.

## Assignment requirement served

§6.4 (data management); supports §6.2 (component boundaries).

## Related

- Jira: SCRUM-39
- Requirements: RNF09
- Supersedes: —
- Other ADRs or pages: 0002; later 0008 defines what the events carry
- Date evidence: ADR 0015 (2026-10-11) states that the two services each keep their own database.
- Drafted by Pedro Costa (branch `docs/SCRUM-39-technical`, 2026-10-26); revised for SCRUM-40.
