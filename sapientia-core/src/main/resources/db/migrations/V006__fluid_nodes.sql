-- V006__fluid_nodes.sql — fluid logistics graph persistence (T-301 / 1.2.0).
-- Mirror of V005 (item_nodes) plus a per-tank buffer column. Networks are
-- NOT stored — recomputed at runtime from node adjacency. See ADR-015.

CREATE TABLE fluid_nodes (
    world          TEXT    NOT NULL,
    block_x        INTEGER NOT NULL,
    block_y        INTEGER NOT NULL,
    block_z        INTEGER NOT NULL,
    node_id        TEXT    NOT NULL,           -- UUID, stable across restarts
    node_type      TEXT    NOT NULL,           -- PIPE | PUMP | DRAIN | TANK | JUNCTION
    tier           TEXT    NOT NULL,           -- LOW | MID | HIGH | EXTREME
    fluid_type     TEXT,                       -- nullable; namespace:id of the held fluid (TANK only)
    amount_mb      INTEGER NOT NULL DEFAULT 0, -- mB held; 0 for non-tank or empty tanks
    updated_at     INTEGER NOT NULL,
    PRIMARY KEY (world, block_x, block_y, block_z)
);

CREATE INDEX idx_fluid_nodes_chunk ON fluid_nodes (world, (block_x / 16), (block_z / 16));
CREATE UNIQUE INDEX idx_fluid_nodes_node_id ON fluid_nodes (node_id);
