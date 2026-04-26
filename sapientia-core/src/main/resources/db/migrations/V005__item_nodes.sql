-- V005__item_nodes.sql — item logistics graph persistence (T-300 / 1.1.0).
-- Same shape as V003 (energy_nodes): networks are NOT stored, they are recomputed
-- at runtime from node adjacency. Item buffers live in adjacent vanilla
-- containers (chest / barrel / hopper / dispenser / dropper / shulker box / etc.),
-- so this table only stores the node descriptor + filter rules.
-- See ROADMAP 1.1.0 and ADR-013.

CREATE TABLE item_nodes (
    world         TEXT    NOT NULL,
    block_x       INTEGER NOT NULL,
    block_y       INTEGER NOT NULL,
    block_z       INTEGER NOT NULL,
    node_id       TEXT    NOT NULL,            -- UUID, stable across restarts
    node_type     TEXT    NOT NULL,            -- PRODUCER | CONSUMER | FILTER | CABLE | JUNCTION
    tier          TEXT    NOT NULL,            -- LOW | MID | HIGH | EXTREME
    priority      INTEGER NOT NULL DEFAULT 0,
    routing_policy TEXT,                       -- nullable; per-network override (FILTER nodes only)
    updated_at    INTEGER NOT NULL,
    PRIMARY KEY (world, block_x, block_y, block_z)
);

CREATE INDEX idx_item_nodes_chunk ON item_nodes (world, (block_x / 16), (block_z / 16));
CREATE UNIQUE INDEX idx_item_nodes_node_id ON item_nodes (node_id);

-- Filter rules: each FILTER node owns an ordered rule list.
CREATE TABLE item_filter_rules (
    filter_node_id TEXT    NOT NULL,           -- FK by value to item_nodes.node_id
    rule_index     INTEGER NOT NULL,
    mode           TEXT    NOT NULL,           -- WHITELIST | BLACKLIST | ACCEPT_ALL
    pattern        TEXT    NOT NULL,           -- e.g. "sapientia:*", "minecraft:copper_ingot"
    updated_at     INTEGER NOT NULL,
    PRIMARY KEY (filter_node_id, rule_index)
);

CREATE INDEX idx_item_filter_rules_node ON item_filter_rules (filter_node_id);
