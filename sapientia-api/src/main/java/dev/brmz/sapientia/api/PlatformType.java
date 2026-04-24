package dev.brmz.sapientia.api;

/**
 * Player platform detected at login time. Resolved through Floodgate API when present,
 * cached in SQLite for subsequent sessions. See docs/bedrock-compatibility.md for details.
 */
public enum PlatformType {
    JAVA,
    BEDROCK,
    UNKNOWN
}
