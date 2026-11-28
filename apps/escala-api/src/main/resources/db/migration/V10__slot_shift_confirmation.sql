-- SCRUM-70 / RF42: shift start timestamp, confirmation prompt tracking and member presence decision.
ALTER TABLE slot
    ADD COLUMN shift_starts_at timestamp(6) with time zone,
    ADD COLUMN presence_status character varying(20) NOT NULL DEFAULT 'UNCONFIRMED',
    ADD COLUMN confirmation_prompt_sent_at timestamp(6) with time zone,
    ADD CONSTRAINT slot_presence_status_check
        CHECK (presence_status IN ('UNCONFIRMED', 'COMING', 'CANNOT_COME'));

UPDATE slot s
SET shift_starts_at = (p.duty_date::timestamp AT TIME ZONE 'Europe/Lisbon')
FROM piquete p
WHERE s.piquete_id = p.id
  AND s.shift_starts_at IS NULL;

ALTER TABLE slot
    ALTER COLUMN shift_starts_at SET NOT NULL;