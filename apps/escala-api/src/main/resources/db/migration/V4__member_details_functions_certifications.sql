-- Member details, functions, driving licences and certifications, registered by the admin (SCRUM-77).
-- Only new columns and tables: the new columns have a default or accept NULL and the previous version (SCRUM-63)
-- never inserts members, so it keeps working on a rollback. No existing constraint changes.
-- The contact is personal data: shown only to the admin and the member themself, never logged (09).

ALTER TABLE member
    ADD COLUMN rank character varying(30),
    ADD COLUMN roster_status character varying(20) NOT NULL DEFAULT 'ACTIVE_ROSTER',
    ADD COLUMN within_age_limit boolean NOT NULL DEFAULT true,
    ADD COLUMN contact character varying(120),
    ADD CONSTRAINT member_rank_check CHECK (rank IN ('CADET', 'TRAINEE', 'FIREFIGHTER_3RD_CLASS', 'FIREFIGHTER_2ND_CLASS',
        'FIREFIGHTER_1ST_CLASS', 'SUB_CHIEF', 'CHIEF', 'OFFICER', 'COMMANDER')),
    ADD CONSTRAINT member_roster_status_check CHECK (roster_status IN ('COMMAND_ROSTER', 'ACTIVE_ROSTER', 'RESERVE_ROSTER',
        'HONORARY_ROSTER'));

CREATE TABLE certification_type (
    code character varying(10) NOT NULL,
    name character varying(80) NOT NULL,
    CONSTRAINT certification_type_pkey PRIMARY KEY (code)
);

INSERT INTO certification_type (code, name) VALUES
    ('TAS', 'Tripulante de Ambulância de Socorro'),
    ('TAT', 'Tripulante de Ambulância de Transporte');

-- Functions are the slot roles a member can take.
CREATE TABLE member_function (
    member_id uuid NOT NULL,
    service_function character varying(20) NOT NULL,
    CONSTRAINT member_function_pkey PRIMARY KEY (member_id, service_function),
    CONSTRAINT member_function_member_fkey FOREIGN KEY (member_id) REFERENCES member (id),
    CONSTRAINT member_function_check CHECK (service_function IN ('CREW_CHIEF', 'DRIVER', 'CREW_MEMBER'))
);

-- One certification per type and one licence per class: a renewal replaces the dates, with no history.
CREATE TABLE member_certification (
    id uuid NOT NULL,
    member_id uuid NOT NULL,
    type_code character varying(10) NOT NULL,
    issued_on date NOT NULL,
    expires_on date NOT NULL,
    CONSTRAINT member_certification_pkey PRIMARY KEY (id),
    CONSTRAINT member_certification_member_fkey FOREIGN KEY (member_id) REFERENCES member (id),
    CONSTRAINT member_certification_type_fkey FOREIGN KEY (type_code) REFERENCES certification_type (code),
    CONSTRAINT member_certification_type_unique UNIQUE (member_id, type_code),
    CONSTRAINT member_certification_dates_check CHECK (expires_on >= issued_on)
);

CREATE TABLE member_driving_licence (
    id uuid NOT NULL,
    member_id uuid NOT NULL,
    licence_class character varying(5) NOT NULL,
    expires_on date NOT NULL,
    CONSTRAINT member_driving_licence_pkey PRIMARY KEY (id),
    CONSTRAINT member_driving_licence_member_fkey FOREIGN KEY (member_id) REFERENCES member (id),
    CONSTRAINT member_driving_licence_class_unique UNIQUE (member_id, licence_class),
    CONSTRAINT member_driving_licence_class_check CHECK (licence_class IN ('B', 'C1', 'C', 'C1E', 'CE'))
);

-- The three synthetic users of V3 get a rank and functions for the demonstration; no contacts.
UPDATE member SET rank = 'FIREFIGHTER_3RD_CLASS' WHERE id = '00000000-0000-4000-8000-000000000001';
UPDATE member SET rank = 'CHIEF' WHERE id = '00000000-0000-4000-8000-000000000002';
UPDATE member SET rank = 'COMMANDER', roster_status = 'COMMAND_ROSTER' WHERE id = '00000000-0000-4000-8000-000000000003';

INSERT INTO member_function (member_id, service_function) VALUES
    ('00000000-0000-4000-8000-000000000001', 'CREW_MEMBER'),
    ('00000000-0000-4000-8000-000000000002', 'CREW_CHIEF');
