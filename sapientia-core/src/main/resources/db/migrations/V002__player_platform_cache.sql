-- V002__player_platform_cache.sql — cache de PlatformType por UUID.
-- Preenchido no login via Floodgate; sobrevive restarts.

CREATE TABLE player_platform (
    uuid        TEXT    NOT NULL PRIMARY KEY,
    platform    TEXT    NOT NULL,
    detected_at INTEGER NOT NULL
);
