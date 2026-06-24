CREATE TABLE candidate_status_history (
    id            UUID          NOT NULL PRIMARY KEY,
    candidate_id  VARCHAR(100)  NOT NULL,
    from_status   VARCHAR(20)   NOT NULL,
    to_status     VARCHAR(20)   NOT NULL,
    comment       TEXT,
    changed_at    TIMESTAMPTZ   NOT NULL DEFAULT now(),
    CONSTRAINT fk_status_history_candidate
        FOREIGN KEY (candidate_id) REFERENCES candidates (id)
        ON DELETE CASCADE
);

CREATE INDEX idx_status_history_candidate_id ON candidate_status_history (candidate_id);
CREATE INDEX idx_status_history_changed_at ON candidate_status_history (changed_at);
