-- Schema of a database created by spring.jpa.hibernate.ddl-auto=update on main (ba0e055, release 0.3.0),
-- from pg_dump --schema-only, with one row. Used by TC-SCRUM-87-02 to check that Flyway baselines such a
-- database and keeps its data.

CREATE TABLE public.outbox_event (
    id uuid NOT NULL,
    created_at timestamp(6) with time zone NOT NULL,
    payload text NOT NULL,
    published_at timestamp(6) with time zone,
    routing_key character varying(255) NOT NULL
);

CREATE TABLE public.piquete (
    id uuid NOT NULL,
    duty_date date NOT NULL
);

CREATE TABLE public.processed_event (
    event_id uuid NOT NULL,
    processed_at timestamp(6) with time zone NOT NULL
);

CREATE TABLE public.slot (
    id uuid NOT NULL,
    assignment_id uuid NOT NULL,
    role character varying(255) NOT NULL,
    status character varying(255) NOT NULL,
    piquete_id uuid NOT NULL,
    CONSTRAINT slot_role_check CHECK (((role)::text = ANY ((ARRAY['CREW_CHIEF'::character varying, 'DRIVER'::character varying, 'CREW_MEMBER'::character varying])::text[]))),
    CONSTRAINT slot_status_check CHECK (((status)::text = ANY ((ARRAY['OPEN'::character varying, 'PENDING'::character varying, 'ELIGIBLE'::character varying, 'PROPOSED'::character varying, 'FILLED'::character varying])::text[])))
);

ALTER TABLE ONLY public.outbox_event
    ADD CONSTRAINT outbox_event_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.piquete
    ADD CONSTRAINT piquete_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.processed_event
    ADD CONSTRAINT processed_event_pkey PRIMARY KEY (event_id);

ALTER TABLE ONLY public.slot
    ADD CONSTRAINT slot_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.slot
    ADD CONSTRAINT uk2nk0ishjxki86vnhw1rv7b0rm UNIQUE (assignment_id);

ALTER TABLE ONLY public.slot
    ADD CONSTRAINT fkfgpq5d12m1pap5oe3fn89b006 FOREIGN KEY (piquete_id) REFERENCES public.piquete(id);

INSERT INTO public.piquete (id, duty_date) VALUES ('11111111-1111-4111-8111-111111111111', '2026-11-01');
INSERT INTO public.slot (id, assignment_id, role, status, piquete_id)
VALUES ('22222222-2222-4222-8222-222222222222', '33333333-3333-4333-8333-333333333333', 'CREW_MEMBER', 'ELIGIBLE',
        '11111111-1111-4111-8111-111111111111');
