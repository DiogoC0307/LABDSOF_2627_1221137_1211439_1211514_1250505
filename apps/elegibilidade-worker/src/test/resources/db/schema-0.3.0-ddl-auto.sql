-- Schema of a database created by spring.jpa.hibernate.ddl-auto=update on main (ba0e055, release 0.3.0),
-- from pg_dump --schema-only, with one row. Used by TC-SCRUM-87-02 to check that Flyway baselines such a
-- database and keeps its data.

CREATE TABLE public.evaluation (
    id uuid NOT NULL,
    assignment_id uuid NOT NULL,
    created_at timestamp(6) with time zone NOT NULL,
    engine_version character varying(255) NOT NULL,
    request_event_id uuid NOT NULL,
    result character varying(255) NOT NULL,
    trace_id character varying(255) NOT NULL
);

CREATE TABLE public.outbox_event (
    id uuid NOT NULL,
    created_at timestamp(6) with time zone NOT NULL,
    payload text NOT NULL,
    published_at timestamp(6) with time zone,
    routing_key character varying(255) NOT NULL
);

CREATE TABLE public.processed_event (
    event_id uuid NOT NULL,
    processed_at timestamp(6) with time zone NOT NULL
);

ALTER TABLE ONLY public.evaluation
    ADD CONSTRAINT evaluation_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.outbox_event
    ADD CONSTRAINT outbox_event_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.processed_event
    ADD CONSTRAINT processed_event_pkey PRIMARY KEY (event_id);

ALTER TABLE ONLY public.evaluation
    ADD CONSTRAINT uk6qo0pvlupqraced8qm6ga7oyd UNIQUE (request_event_id);

INSERT INTO public.evaluation (id, assignment_id, created_at, engine_version, request_event_id, result, trace_id)
VALUES ('44444444-4444-4444-8444-444444444444', '33333333-3333-4333-8333-333333333333', '2026-11-01T10:00:00Z',
        'SEMPRE_ELEGIVEL', '55555555-5555-4555-8555-555555555555', 'ELIGIBLE', 'trace-0.3.0');
