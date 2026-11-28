-- Who can take a role on a shift the supervisor is building (SCRUM-171): one row per member with that role, checked by
-- the eligibility rules in elegibilidade-worker (ADR 0007). Its id is the assignmentId of AvaliacaoPedida (purpose
-- CANDIDATE) and AvaliacaoConcluida; request_id groups the members of one question. The shift is only a draft in the
-- browser, so it has no foreign key. Rows are deleted a day after they are made. Instants in UTC (ADR 0012).

CREATE TABLE candidate_check (
    id uuid NOT NULL,
    request_id uuid NOT NULL,
    member_id uuid NOT NULL,
    role character varying(11) NOT NULL,
    starts_at timestamp(6) with time zone NOT NULL,
    ends_at timestamp(6) with time zone NOT NULL,
    evaluation_state character varying(9) NOT NULL,
    result character varying(10),
    violated_rules character varying(255),
    engine_version character varying(64),
    created_at timestamp(6) with time zone NOT NULL,
    evaluated_at timestamp(6) with time zone,
    CONSTRAINT candidate_check_pkey PRIMARY KEY (id),
    CONSTRAINT candidate_check_member_fkey FOREIGN KEY (member_id) REFERENCES member (id),
    CONSTRAINT candidate_check_role_check CHECK (role IN ('CREW_CHIEF', 'DRIVER', 'CREW_MEMBER')),
    CONSTRAINT candidate_check_state_check CHECK (evaluation_state IN ('PENDING', 'EVALUATED', 'FAILED')),
    CONSTRAINT candidate_check_result_check CHECK (result IN ('ELIGIBLE', 'INELIGIBLE')),
    CONSTRAINT candidate_check_period_check CHECK (ends_at > starts_at)
);

CREATE INDEX candidate_check_request_idx ON candidate_check (request_id);
CREATE INDEX candidate_check_created_idx ON candidate_check (created_at);
