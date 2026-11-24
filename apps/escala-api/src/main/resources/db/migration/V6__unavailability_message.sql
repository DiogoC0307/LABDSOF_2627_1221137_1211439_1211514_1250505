-- A member's unavailability in their own words, until they confirm what was read (SCRUM-75, RF13, RF31 to RF33).
-- Only a new table: the previous version ignores it on a rollback. The number is fixed at merge (migration rule E):
-- if another V6 reaches main first, this file is renamed to the next free number before the merge.
-- free_text is deleted on confirmation, on discard, and 24 hours after the message if it was not confirmed (RNF09,
-- 09); the row stays without it, so the method and the latency can be counted. The proposal holds dates and times
-- only, in the ExtracaoConcluida v2 format. Instants in UTC (ADR 0012); reference_date is the day in Lisbon.

CREATE TABLE unavailability_message (
    id uuid NOT NULL,
    member_id uuid NOT NULL,
    free_text character varying(500),
    reference_date date NOT NULL,
    status character varying(10) NOT NULL,
    request_event_id uuid NOT NULL,
    proposal text,
    method character varying(10),
    model character varying(100),
    latency_ms integer,
    created_at timestamp(6) with time zone NOT NULL,
    answered_at timestamp(6) with time zone,
    closed_at timestamp(6) with time zone,
    CONSTRAINT unavailability_message_pkey PRIMARY KEY (id),
    CONSTRAINT unavailability_message_member_fkey FOREIGN KEY (member_id) REFERENCES member (id),
    CONSTRAINT unavailability_message_status_check
        CHECK (status IN ('PENDING', 'PROPOSED', 'CONFIRMED', 'DISCARDED', 'EXPIRED')),
    CONSTRAINT unavailability_message_method_check CHECK (method IN ('MODEL', 'BASELINE', 'FALLBACK')),
    CONSTRAINT unavailability_message_closed_text_check
        CHECK (status IN ('PENDING', 'PROPOSED') OR free_text IS NULL)
);

-- The limit of 10 messages per member per hour (09 T-11).
CREATE INDEX unavailability_message_member_created_idx ON unavailability_message (member_id, created_at);
-- The 24-hour job looks only at the messages that still hold text.
CREATE INDEX unavailability_message_text_created_idx ON unavailability_message (created_at)
    WHERE free_text IS NOT NULL;
