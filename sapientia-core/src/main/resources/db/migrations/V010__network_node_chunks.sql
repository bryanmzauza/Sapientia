-- V010__network_node_chunks.sql — chunk columns for the network node tables.
-- Chunk loads read nodes by chunk. The previous expression indexes could not
-- serve the block_x/block_z range queries, so each load scanned a whole
-- column of the world. Chunk coordinates are floor(block / 16), an arithmetic
-- shift, matching custom_blocks.

ALTER TABLE energy_nodes ADD COLUMN chunk_x INTEGER NOT NULL DEFAULT 0;
ALTER TABLE energy_nodes ADD COLUMN chunk_z INTEGER NOT NULL DEFAULT 0;
UPDATE energy_nodes SET chunk_x = block_x >> 4, chunk_z = block_z >> 4;
DROP INDEX IF EXISTS idx_energy_nodes_chunk;
CREATE INDEX idx_energy_nodes_chunk ON energy_nodes (world, chunk_x, chunk_z);

ALTER TABLE item_nodes ADD COLUMN chunk_x INTEGER NOT NULL DEFAULT 0;
ALTER TABLE item_nodes ADD COLUMN chunk_z INTEGER NOT NULL DEFAULT 0;
UPDATE item_nodes SET chunk_x = block_x >> 4, chunk_z = block_z >> 4;
DROP INDEX IF EXISTS idx_item_nodes_chunk;
CREATE INDEX idx_item_nodes_chunk ON item_nodes (world, chunk_x, chunk_z);

ALTER TABLE fluid_nodes ADD COLUMN chunk_x INTEGER NOT NULL DEFAULT 0;
ALTER TABLE fluid_nodes ADD COLUMN chunk_z INTEGER NOT NULL DEFAULT 0;
UPDATE fluid_nodes SET chunk_x = block_x >> 4, chunk_z = block_z >> 4;
DROP INDEX IF EXISTS idx_fluid_nodes_chunk;
CREATE INDEX idx_fluid_nodes_chunk ON fluid_nodes (world, chunk_x, chunk_z);
