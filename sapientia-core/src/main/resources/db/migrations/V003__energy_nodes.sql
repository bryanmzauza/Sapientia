-- V003__energy_nodes.sql — energy graph persistence (T-140 / 0.3.0).
-- Networks are NOT stored: they are recomputed at runtime from node adjacency
-- (chunk hydration → NetworkGraph). Each row represents one node placed in the
-- world. See docs/persistence-schema.md and ROADMAP 0.3.0.

CREATE TABLE energy_nodes (
    world         TEXT    NOT NULL,
    block_x       INTEGER NOT NULL,
    block_y       INTEGER NOT NULL,
    block_z       INTEGER NOT NULL,
    node_id       TEXT    NOT NULL,            -- UUID, stable across restarts
    node_type     TEXT    NOT NULL,            -- GENERATOR | CONSUMER | CAPACITOR | CABLE
    tier          TEXT    NOT NULL,            -- LOW | MID | HIGH | EXTREME
    buffer_curr   INTEGER NOT NULL DEFAULT 0,
    buffer_max    INTEGER NOT NULL DEFAULT 0,
    updated_at    INTEGER NOT NULL,
    PRIMARY KEY (world, block_x, block_y, block_z)
);

CREATE INDEX idx_energy_nodes_chunk ON energy_nodes (world, (block_x / 16), (block_z / 16));
CREATE UNIQUE INDEX idx_energy_nodes_node_id ON energy_nodes (node_id);
