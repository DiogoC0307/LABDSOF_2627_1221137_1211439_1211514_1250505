-- A piquete is a shift the supervisor defines (SCRUM-65, decision D1 of 2026-11-23): its day, its period (DAY or
-- NIGHT), its start and end, and the minimum number of firefighters it needs.
-- Only new nullable columns: piquetes created before this have none of them (shown as "time not set"), and the
-- previous version ignores them on a rollback. All four are set together or not at all.
-- Instants in UTC (ADR 0012); the end is exclusive.

ALTER TABLE piquete
    ADD COLUMN period character varying(5),
    ADD COLUMN starts_at timestamp(6) with time zone,
    ADD COLUMN ends_at timestamp(6) with time zone,
    ADD COLUMN minimum_crew integer,
    ADD CONSTRAINT piquete_period_check CHECK (period IN ('DAY', 'NIGHT')),
    ADD CONSTRAINT piquete_shift_period_check CHECK (ends_at > starts_at),
    ADD CONSTRAINT piquete_minimum_crew_check CHECK (minimum_crew >= 1),
    ADD CONSTRAINT piquete_shift_complete_check CHECK (
        (period IS NULL AND starts_at IS NULL AND ends_at IS NULL AND minimum_crew IS NULL)
        OR (period IS NOT NULL AND starts_at IS NOT NULL AND ends_at IS NOT NULL AND minimum_crew IS NOT NULL)
    );
