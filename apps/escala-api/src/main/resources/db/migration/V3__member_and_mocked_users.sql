-- Members and their access role, with the three synthetic users of the mocked sign-in (SCRUM-63, ADR 0006).
-- Only a new table: nothing existing changes, so the previous version of the service ignores it on a rollback.
-- Rank, contact and certifications come with member registration (SCRUM-77). While sign-in is mocked, only
-- synthetic members may exist (09, T-01).

CREATE TABLE member (
    id uuid NOT NULL,
    full_name character varying(120) NOT NULL,
    access_role character varying(20) NOT NULL,
    active boolean NOT NULL DEFAULT true,
    created_at timestamp(6) with time zone NOT NULL DEFAULT now(),
    CONSTRAINT member_pkey PRIMARY KEY (id),
    CONSTRAINT member_access_role_check CHECK (access_role IN ('MEMBER', 'SUPERVISOR', 'ADMIN'))
);

INSERT INTO member (id, full_name, access_role) VALUES
    ('00000000-0000-4000-8000-000000000001', 'Synthetic Member', 'MEMBER'),
    ('00000000-0000-4000-8000-000000000002', 'Synthetic Supervisor', 'SUPERVISOR'),
    ('00000000-0000-4000-8000-000000000003', 'Synthetic Admin', 'ADMIN');
