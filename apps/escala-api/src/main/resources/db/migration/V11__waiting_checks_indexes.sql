-- The gauge escala_checks_waiting{purpose} (SCRUM-98) counts, every 15 s, the requests still waiting for
-- elegibilidade-worker. One index on (state, date) per table, so each count reads the index and never the whole table.

CREATE INDEX slot_status_starts_idx ON slot (status, shift_starts_at);
CREATE INDEX assignment_evaluation_state_proposed_idx ON assignment (evaluation_state, proposed_at);
CREATE INDEX candidate_check_state_created_idx ON candidate_check (evaluation_state, created_at);
CREATE INDEX unavailability_message_status_created_idx ON unavailability_message (status, created_at);
