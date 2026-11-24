-- Display read model for today's fire risk (SCRUM-76, ADR 0010). Written from RiscoAtualizado.
-- It does not change slots. Instants in UTC (ADR 0012). Only a new table.

CREATE TABLE daily_risk (
    dico character varying(4) NOT NULL,
    rcm integer NOT NULL,
    forecast_date date NOT NULL,
    fetched_at timestamp(6) with time zone NOT NULL,
    source character varying(16) NOT NULL,
    assumed_higher boolean NOT NULL,
    updated_at timestamp(6) with time zone NOT NULL,
    CONSTRAINT daily_risk_pkey PRIMARY KEY (dico),
    CONSTRAINT daily_risk_rcm_check CHECK (rcm >= 1 AND rcm <= 5),
    CONSTRAINT daily_risk_source_check CHECK (source IN ('IPMA', 'SIMULATOR'))
);
