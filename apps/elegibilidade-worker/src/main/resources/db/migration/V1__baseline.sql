-- Baseline: the schema that spring.jpa.hibernate.ddl-auto=update created up to release 0.3.0 (SCRUM-87, ADR 0019).
-- Taken from pg_dump --schema-only of a database created by main (ba0e055), with the same constraint names,
-- so later migrations can change them. A database that already has these tables is baselined at version 1
-- (spring.flyway.baseline-on-migrate) and this script does not run on it.

CREATE TABLE outbox_event (
    id uuid NOT NULL,
    created_at timestamp(6) with time zone NOT NULL,
    payload text NOT NULL,
    published_at timestamp(6) with time zone,
    routing_key character varying(255) NOT NULL,
    CONSTRAINT outbox_event_pkey PRIMARY KEY (id)
);

CREATE TABLE processed_event (
    event_id uuid NOT NULL,
    processed_at timestamp(6) with time zone NOT NULL,
    CONSTRAINT processed_event_pkey PRIMARY KEY (event_id)
);

CREATE TABLE evaluation (
    id uuid NOT NULL,
    assignment_id uuid NOT NULL,
    created_at timestamp(6) with time zone NOT NULL,
    engine_version character varying(255) NOT NULL,
    request_event_id uuid NOT NULL,
    result character varying(255) NOT NULL,
    trace_id character varying(255) NOT NULL,
    CONSTRAINT evaluation_pkey PRIMARY KEY (id),
    CONSTRAINT uk6qo0pvlupqraced8qm6ga7oyd UNIQUE (request_event_id)
);
