CREATE TABLE candidates (
    id            VARCHAR(100)  NOT NULL PRIMARY KEY,
    name          VARCHAR(100)  NOT NULL,
    email         VARCHAR(255)  NOT NULL,
    phone         VARCHAR(30),
    position      VARCHAR(100)  NOT NULL,
    pos_label     VARCHAR(255),
    city          VARCHAR(100),
    telegram      VARCHAR(100),
    total_exp     VARCHAR(50),
    stack         TEXT,
    education     TEXT,
    verdict       VARCHAR(20)   NOT NULL,
    summary       TEXT,
    status        VARCHAR(20)   NOT NULL,
    criteria      JSONB         NOT NULL DEFAULT '[]',
    experience    JSONB         NOT NULL DEFAULT '[]',
    questions     JSONB         NOT NULL DEFAULT '[]',
    parsed_at     TIMESTAMPTZ,
    created_at    TIMESTAMPTZ   NOT NULL DEFAULT now(),
    updated_at    TIMESTAMPTZ   NOT NULL DEFAULT now()
);

CREATE UNIQUE INDEX uq_candidates_email ON candidates (email);
CREATE INDEX idx_candidates_verdict ON candidates (verdict);
CREATE INDEX idx_candidates_status ON candidates (status);
CREATE INDEX idx_candidates_position ON candidates (position);
CREATE INDEX idx_candidates_created_at ON candidates (created_at);
