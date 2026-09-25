-- V011__progression.sql — server era and per-player research (Foundation 2).
-- server_state holds single values such as the current era. player_discoveries
-- replaces unlocked_content: a row means the player has discovered the item.

CREATE TABLE IF NOT EXISTS server_state (
    state_key   TEXT    PRIMARY KEY,
    state_value TEXT    NOT NULL,
    updated_at  INTEGER NOT NULL
);

CREATE TABLE IF NOT EXISTS player_discoveries (
    player_uuid   TEXT    NOT NULL,
    item_id       TEXT    NOT NULL,
    discovered_at INTEGER NOT NULL,
    PRIMARY KEY (player_uuid, item_id)
);

INSERT OR IGNORE INTO player_discoveries (player_uuid, item_id, discovered_at)
    SELECT player_uuid, entry_id, unlocked_at FROM unlocked_content;

DROP TABLE IF EXISTS unlocked_content;
