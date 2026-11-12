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

CREATE TABLE piquete (
    id uuid NOT NULL,
    duty_date date NOT NULL,
    CONSTRAINT piquete_pkey PRIMARY KEY (id)
);

CREATE TABLE slot (
    id uuid NOT NULL,
    assignment_id uuid NOT NULL,
    role character varying(255) NOT NULL,
    status character varying(255) NOT NULL,
    piquete_id uuid NOT NULL,
    CONSTRAINT slot_pkey PRIMARY KEY (id),
    CONSTRAINT uk2nk0ishjxki86vnhw1rv7b0rm UNIQUE (assignment_id),
    CONSTRAINT fkfgpq5d12m1pap5oe3fn89b006 FOREIGN KEY (piquete_id) REFERENCES piquete (id),
    -- Both checks are written as pg_dump prints the ones Hibernate created, so the catalogs are identical.
    CONSTRAINT slot_role_check CHECK (((role)::text = ANY ((ARRAY['CREW_CHIEF'::character varying, 'DRIVER'::character varying,
        'CREW_MEMBER'::character varying])::text[]))),
    CONSTRAINT slot_status_check CHECK (((status)::text = ANY ((ARRAY['OPEN'::character varying, 'PENDING'::character varying,
        'ELIGIBLE'::character varying, 'PROPOSED'::character varying, 'FILLED'::character varying])::text[])))
);
