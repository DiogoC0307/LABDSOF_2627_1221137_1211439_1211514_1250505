-- Cover requests (SCRUM-72): when a member can't come, their slot opens and a cover request is made (AC1 of SCRUM-71).
-- Every other member with the slot's function is checked by the eligibility rules in elegibilidade-worker (ADR 0007),
-- and only those the rules accept are asked. The supervisor is alerted when nobody is free and qualified, and when
-- nobody accepts in time. Instants in UTC (ADR 0012); ends are exclusive.

-- One request to cover one slot. OPEN until it is filled (SCRUM-73) or its shift starts (CLOSED). At most one OPEN
-- request per slot, so a second "I can't come" makes no second request. starts_at and ends_at are the shift's, kept so
-- that the request still says which shift it was. vacated_member_id is the member who held the slot, when there was
-- one. A request goes with its slot when the slot is removed.
CREATE TABLE cover_request (
    id uuid NOT NULL,
    slot_id uuid NOT NULL,
    piquete_id uuid NOT NULL,
    role character varying(11) NOT NULL,
    starts_at timestamp(6) with time zone NOT NULL,
    ends_at timestamp(6) with time zone NOT NULL,
    vacated_member_id uuid,
    reason character varying(11) NOT NULL,
    status character varying(6) NOT NULL,
    opened_at timestamp(6) with time zone NOT NULL,
    escalation character varying(13),
    escalated_at timestamp(6) with time zone,
    CONSTRAINT cover_request_pkey PRIMARY KEY (id),
    CONSTRAINT cover_request_slot_fkey FOREIGN KEY (slot_id) REFERENCES slot (id) ON DELETE CASCADE,
    CONSTRAINT cover_request_vacated_fkey FOREIGN KEY (vacated_member_id) REFERENCES member (id),
    CONSTRAINT cover_request_role_check CHECK (role IN ('CREW_CHIEF', 'DRIVER', 'CREW_MEMBER')),
    CONSTRAINT cover_request_reason_check CHECK (reason IN ('CANNOT_COME', 'NO_ANSWER')),
    CONSTRAINT cover_request_status_check CHECK (status IN ('OPEN', 'FILLED', 'CLOSED')),
    CONSTRAINT cover_request_escalation_check CHECK (escalation IN ('NO_CANDIDATES', 'NO_ACCEPTANCE')),
    CONSTRAINT cover_request_period_check CHECK (ends_at > starts_at)
);

CREATE UNIQUE INDEX cover_request_open_slot_idx ON cover_request (slot_id) WHERE status = 'OPEN';
CREATE INDEX cover_request_status_idx ON cover_request (status);

-- One member checked for a cover request. Its id is the assignmentId of AvaliacaoPedida (purpose COVER) and of
-- AvaliacaoConcluida. CHECKING until the answer is back; then OFFERED (the member is asked) when the rules accept them,
-- NOT_OFFERED otherwise.
CREATE TABLE cover_offer (
    id uuid NOT NULL,
    cover_request_id uuid NOT NULL,
    member_id uuid NOT NULL,
    status character varying(11) NOT NULL,
    evaluation_state character varying(9) NOT NULL,
    result character varying(10),
    violated_rules character varying(255),
    engine_version character varying(64),
    created_at timestamp(6) with time zone NOT NULL,
    evaluated_at timestamp(6) with time zone,
    offered_at timestamp(6) with time zone,
    CONSTRAINT cover_offer_pkey PRIMARY KEY (id),
    CONSTRAINT cover_offer_request_fkey FOREIGN KEY (cover_request_id) REFERENCES cover_request (id) ON DELETE CASCADE,
    CONSTRAINT cover_offer_member_fkey FOREIGN KEY (member_id) REFERENCES member (id),
    CONSTRAINT cover_offer_request_member_key UNIQUE (cover_request_id, member_id),
    CONSTRAINT cover_offer_status_check CHECK (status IN ('CHECKING', 'OFFERED', 'NOT_OFFERED')),
    CONSTRAINT cover_offer_state_check CHECK (evaluation_state IN ('PENDING', 'EVALUATED', 'FAILED')),
    CONSTRAINT cover_offer_result_check CHECK (result IN ('ELIGIBLE', 'INELIGIBLE'))
);

CREATE INDEX cover_offer_member_status_idx ON cover_offer (member_id, status);

-- The notifications of a cover request: the member asked (COVER_REQUESTED), and the supervisor when the slot opens
-- (SLOT_OPENED, with why the slot opened) and when nobody covers it (COVER_ESCALATED, with why). The PWA writes the text
-- from these fields.
ALTER TABLE notification
    ADD COLUMN cover_request_id uuid,
    ADD COLUMN role character varying(11),
    ADD COLUMN reason character varying(13),
    DROP CONSTRAINT notification_type_check,
    ADD CONSTRAINT notification_type_check CHECK (type IN ('WEEKLY_SCHEDULE_PUBLISHED', 'ADDED_TO_SHIFT',
        'REMOVED_FROM_SHIFT', 'SHIFT_TIMES_CHANGED', 'SHIFT_CANCELLED', 'SLOT_OPENED', 'COVER_REQUESTED',
        'COVER_ESCALATED')),
    ADD CONSTRAINT notification_role_check CHECK (role IN ('CREW_CHIEF', 'DRIVER', 'CREW_MEMBER')),
    ADD CONSTRAINT notification_reason_check CHECK (reason IN ('CANNOT_COME', 'NO_ANSWER', 'NO_CANDIDATES',
        'NO_ACCEPTANCE'));

CREATE INDEX notification_cover_request_idx ON notification (cover_request_id);
