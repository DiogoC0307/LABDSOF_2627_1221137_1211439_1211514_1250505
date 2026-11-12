-- SCRUM-95: a slot whose evaluation could not be made or applied shows EVALUATION_FAILED (RF24).
-- Written as pg_dump prints the check Hibernate creates, as in V1.
ALTER TABLE slot DROP CONSTRAINT slot_status_check;
ALTER TABLE slot ADD CONSTRAINT slot_status_check CHECK (((status)::text = ANY ((ARRAY['OPEN'::character varying,
    'PENDING'::character varying, 'ELIGIBLE'::character varying, 'PROPOSED'::character varying,
    'FILLED'::character varying, 'EVALUATION_FAILED'::character varying])::text[])));
