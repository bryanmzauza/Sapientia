-- V009__androids.sql — Programmable autonomous worker persistence (T-451 / 1.9.0).
-- One row per placed android. The kinetic AI behaviour (loot simulation,
-- crop / log / ore scans, builder pattern execution, slayer / trader logic)
-- ships with the kinetic loop in 1.9.1; 1.9.0 only persists placement +
-- assigned program + upgrade slots so caps and program assignment survive
-- a server restart.
-- See ROADMAP 1.9.0 (T-451..T-456) and docs/content-spec-T-45x.md.

CREATE TABLE androids (
    world          TEXT    NOT NULL,
    block_x        INTEGER NOT NULL,
    block_y        INTEGER NOT NULL,
    block_z        INTEGER NOT NULL,
    type           TEXT    NOT NULL,            -- AndroidType.name() (FARMER, LUMBERJACK, ...)
    owner_uuid     TEXT,                        -- placer's UUID (nullable for /sapientia give world-edit)
    program_name   TEXT,                        -- references logic_programs(name); nullable until programmed
    chip_tier      INTEGER NOT NULL DEFAULT 1,  -- AI chip tier (1..4) — radius scaling
    motor_tier     INTEGER NOT NULL DEFAULT 1,  -- motor tier (1..4) — instructions/tick scaling (capped at 1 in 1.9.0)
    armour_tier    INTEGER NOT NULL DEFAULT 1,  -- armour tier (1..4) — HP scaling
    fuel_tier      INTEGER NOT NULL DEFAULT 1,  -- fuel module tier (1..4) — biofuel ↔ SU conversion
    fuel_buffer    INTEGER NOT NULL DEFAULT 0,  -- current SU buffer
    health         INTEGER NOT NULL DEFAULT 100,-- current HP
    last_tick_ms   INTEGER NOT NULL DEFAULT 0,
    PRIMARY KEY (world, block_x, block_y, block_z)
);

-- Helper index for the per-chunk cap enforcement (T-456): four androids per
-- chunk maximum. The composite PRIMARY KEY is (world, x, y, z) so the
-- typical cap query falls back to a chunk-bucketed COUNT(*) — a chunk index
-- speeds it up without bloating the table.
CREATE INDEX idx_androids_chunk
    ON androids (world, (block_x >> 4), (block_z >> 4));
