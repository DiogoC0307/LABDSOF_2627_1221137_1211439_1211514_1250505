-- The corps municipality's last rural fire risk (SCRUM-76, ADR 0010, ADR 0013). One row per DICO.
-- rcm is null until a usable value has been seen. valid_at is when that value was taken, in UTC (ADR 0012).
-- assumed_higher is the display warning: the schedule is not changed. Only a new table.

CREATE TABLE risk_observation (
    dico character varying(4) NOT NULL,
    rcm integer,
    forecast_date date,
    valid_at timestamp(6) with time zone,
    source character varying(16) NOT NULL,
    assumed_higher boolean NOT NULL,
    last_outcome character varying(16) NOT NULL,
    last_polled_at timestamp(6) with time zone NOT NULL,
    CONSTRAINT risk_observation_pkey PRIMARY KEY (dico),
    CONSTRAINT risk_observation_rcm_check CHECK (rcm IS NULL OR (rcm >= 1 AND rcm <= 5)),
    CONSTRAINT risk_observation_source_check CHECK (source IN ('IPMA', 'SIMULATOR')),
    CONSTRAINT risk_observation_outcome_check
        CHECK (last_outcome IN ('OK', 'UNAVAILABLE', 'DELAYED', 'INCOMPLETE', 'INVALID'))
);
