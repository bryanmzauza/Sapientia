-- V004__unlocked_content.sql
-- Per-player unlock log (T-151 / 0.4.0). Each row marks a single entry as
-- unlocked for a single player. Multiple rows per (player, entry) are not
-- allowed. Reads are one-shot on join (hydrated into the in-memory cache).

CREATE TABLE IF NOT EXISTS unlocked_content (
    player_uuid TEXT NOT NULL,
    entry_id    TEXT NOT NULL,
    unlocked_at INTEGER NOT NULL,
    PRIMARY KEY (player_uuid, entry_id)
);

CREATE INDEX IF NOT EXISTS idx_unlocked_content_player ON unlocked_content(player_uuid);
