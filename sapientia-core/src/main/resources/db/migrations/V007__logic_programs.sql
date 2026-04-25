-- V007__logic_programs.sql — programmable-logic DAG persistence (T-302 / 1.3.0).
-- One row per registered program. The compiled DAG is kept in memory; only the
-- canonical YAML source + its enabled flag + the last compilation/run error
-- are stored so the runtime can rehydrate after a restart.
-- See ROADMAP 1.3.0 and docs/persistence-schema.md.

CREATE TABLE logic_programs (
    name        TEXT    NOT NULL PRIMARY KEY,
    yaml_source TEXT    NOT NULL,
    enabled     INTEGER NOT NULL DEFAULT 1,
    last_error  TEXT,
    updated_at  INTEGER NOT NULL
);
