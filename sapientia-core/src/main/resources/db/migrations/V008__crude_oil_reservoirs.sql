-- T-412 / 1.5.1 — Per-chunk crude_oil reservoirs.
-- Each loaded chunk that has ever been queried by a pumpjack receives a row.
-- The reserve regenerates very slowly (configurable; ~1 mB/min default) so
-- abandoned wells eventually refill but cannot sustain a single pumpjack.
CREATE TABLE crude_oil_reservoirs (
    world         TEXT    NOT NULL,
    chunk_x       INTEGER NOT NULL,
    chunk_z       INTEGER NOT NULL,
    amount_mb     INTEGER NOT NULL DEFAULT 0,
    initial_mb    INTEGER NOT NULL DEFAULT 0,
    last_tick_ms  INTEGER NOT NULL,
    PRIMARY KEY (world, chunk_x, chunk_z)
);
