-- Unavailability declared by a member through the form (SCRUM-64, RF12, RN06).
-- Only a new table: the previous version ignores it on a rollback.
-- Dates and times only, never a reason (09: the reason may be health or family data). Instants in UTC (ADR 0012);
-- the end is exclusive. Periods are deleted 30 days after they end (09, UnavailabilityRetention).

CREATE TABLE unavailability (
    id uuid NOT NULL,
    member_id uuid NOT NULL,
    starts_at timestamp(6) with time zone NOT NULL,
    ends_at timestamp(6) with time zone NOT NULL,
    source character varying(10) NOT NULL DEFAULT 'FORM',
    created_at timestamp(6) with time zone NOT NULL DEFAULT now(),
    CONSTRAINT unavailability_pkey PRIMARY KEY (id),
    CONSTRAINT unavailability_member_fkey FOREIGN KEY (member_id) REFERENCES member (id),
    CONSTRAINT unavailability_period_check CHECK (ends_at > starts_at),
    CONSTRAINT unavailability_source_check CHECK (source IN ('FORM', 'FREE_TEXT'))
);

CREATE INDEX unavailability_member_starts_idx ON unavailability (member_id, starts_at);
CREATE INDEX unavailability_ends_idx ON unavailability (ends_at);
