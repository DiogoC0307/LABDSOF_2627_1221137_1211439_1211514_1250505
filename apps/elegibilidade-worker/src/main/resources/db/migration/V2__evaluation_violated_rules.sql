-- The eligibility rules (ADR 0007) replace SEMPRE_ELEGIVEL. Evaluation now keeps the codes of the rules
-- it found broken (RN01, RN05, RN06), comma separated, empty when ELIGIBLE. 
-- Nullable: evaluations made before this have none, and the previous version ignores the column on a rollback.

ALTER TABLE evaluation
    ADD COLUMN violated_rules character varying(255);
