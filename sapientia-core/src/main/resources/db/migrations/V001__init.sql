-- V001__init.sql — schema inicial do Sapientia.
-- Ver docs/persistence-schema.md §3.

CREATE TABLE custom_blocks (
    world       TEXT    NOT NULL,
    chunk_x     INTEGER NOT NULL,
    chunk_z     INTEGER NOT NULL,
    block_x     INTEGER NOT NULL,
    block_y     INTEGER NOT NULL,
    block_z     INTEGER NOT NULL,
    item_id     TEXT    NOT NULL,
    state_blob  BLOB,
    updated_at  INTEGER NOT NULL,
    PRIMARY KEY (world, block_x, block_y, block_z)
);

CREATE INDEX idx_blocks_chunk   ON custom_blocks (world, chunk_x, chunk_z);
CREATE INDEX idx_blocks_item_id ON custom_blocks (item_id);
