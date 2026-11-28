-- Publishing a week (SCRUM-66): the supervisor sends the whole week, each member on it is checked by the eligibility
-- rules in elegibilidade-worker (ADR 0007), and only when every check is back is the week published, or blocked with
-- its conflicts. Members are notified of what changed for them. Instants in UTC (ADR 0012); ends are exclusive.

-- One attempt to publish a week. plan is the week as the supervisor sent it (JSON), with the IDs its new shifts and
-- slots will have, so the week can be applied exactly as it was checked.
CREATE TABLE week_publication (
    id uuid NOT NULL,
    week character varying(8) NOT NULL,
    status character varying(10) NOT NULL,
    requested_by uuid NOT NULL,
    plan text NOT NULL,
    created_at timestamp(6) with time zone NOT NULL,
    completed_at timestamp(6) with time zone,
    CONSTRAINT week_publication_pkey PRIMARY KEY (id),
    CONSTRAINT week_publication_requested_by_fkey FOREIGN KEY (requested_by) REFERENCES member (id),
    CONSTRAINT week_publication_status_check CHECK (status IN ('CHECKING', 'PUBLISHED', 'BLOCKED', 'SUPERSEDED'))
);

CREATE INDEX week_publication_week_idx ON week_publication (week, status);

-- A member on a slot (domain model: Slot 1 -- 0..* Assignment). Its id is the assignmentId of AvaliacaoPedida and
-- AvaliacaoConcluida. PROPOSED while its publication is checked, CONFIRMED once the week is published, CANCELLED when
-- a later publication replaces it or its publication is blocked. piquete_id and slot_id have no foreign key: a new
-- shift is only created when its week is published, and a published one keeps its history after it is deleted.
CREATE TABLE assignment (
    id uuid NOT NULL,
    publication_id uuid NOT NULL,
    piquete_id uuid NOT NULL,
    slot_id uuid NOT NULL,
    member_id uuid NOT NULL,
    role character varying(11) NOT NULL,
    starts_at timestamp(6) with time zone NOT NULL,
    ends_at timestamp(6) with time zone NOT NULL,
    status character varying(9) NOT NULL,
    evaluation_state character varying(9) NOT NULL,
    result character varying(10),
    violated_rules character varying(255),
    engine_version character varying(64),
    proposed_at timestamp(6) with time zone NOT NULL,
    evaluated_at timestamp(6) with time zone,
    confirmed_at timestamp(6) with time zone,
    CONSTRAINT assignment_pkey PRIMARY KEY (id),
    CONSTRAINT assignment_publication_fkey FOREIGN KEY (publication_id) REFERENCES week_publication (id),
    CONSTRAINT assignment_member_fkey FOREIGN KEY (member_id) REFERENCES member (id),
    CONSTRAINT assignment_role_check CHECK (role IN ('CREW_CHIEF', 'DRIVER', 'CREW_MEMBER')),
    CONSTRAINT assignment_status_check CHECK (status IN ('PROPOSED', 'CONFIRMED', 'CANCELLED')),
    CONSTRAINT assignment_evaluation_state_check CHECK (evaluation_state IN ('PENDING', 'EVALUATED', 'FAILED')),
    CONSTRAINT assignment_result_check CHECK (result IN ('ELIGIBLE', 'INELIGIBLE')),
    CONSTRAINT assignment_period_check CHECK (ends_at > starts_at)
);

CREATE INDEX assignment_publication_idx ON assignment (publication_id);
CREATE INDEX assignment_piquete_status_idx ON assignment (piquete_id, status);
CREATE INDEX assignment_member_status_idx ON assignment (member_id, status);

-- What a member is told (RF20, in the app; ADR 0017): the week was published with their shifts, or one of their
-- shifts changed. The text is written by the PWA from these fields, so no other member's data is stored here.
CREATE TABLE notification (
    id uuid NOT NULL,
    member_id uuid NOT NULL,
    type character varying(32) NOT NULL,
    week character varying(8) NOT NULL,
    piquete_id uuid,
    starts_at timestamp(6) with time zone,
    ends_at timestamp(6) with time zone,
    previous_starts_at timestamp(6) with time zone,
    previous_ends_at timestamp(6) with time zone,
    shift_count integer,
    created_at timestamp(6) with time zone NOT NULL,
    read_at timestamp(6) with time zone,
    CONSTRAINT notification_pkey PRIMARY KEY (id),
    CONSTRAINT notification_member_fkey FOREIGN KEY (member_id) REFERENCES member (id),
    CONSTRAINT notification_type_check CHECK (type IN ('WEEKLY_SCHEDULE_PUBLISHED', 'ADDED_TO_SHIFT', 'REMOVED_FROM_SHIFT',
        'SHIFT_TIMES_CHANGED', 'SHIFT_CANCELLED'))
);

CREATE INDEX notification_member_created_idx ON notification (member_id, created_at DESC);
