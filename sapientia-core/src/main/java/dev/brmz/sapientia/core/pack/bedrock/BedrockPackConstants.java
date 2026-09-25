package dev.brmz.sapientia.core.pack.bedrock;

/**
 * Stable identifiers for Sapientia's bundled Bedrock resource pack.
 *
 * <p>The pack UUIDs MUST remain constant across releases — Bedrock clients
 * cache packs by UUID, so changing them would force every connected player to
 * redownload and re-trust the pack. The manifest version follows the plugin
 * version (see {@code ResourcePackBuilder}), which is what tells clients a new
 * copy is available.
 */
public final class BedrockPackConstants {

    /** Top-level header UUID. */
    public static final String HEADER_UUID = "9f3a8d2a-1c2b-4d3e-9a87-7c1d5b0c4e21";

    /** {@code modules[0]} UUID. Distinct from the header per Bedrock manifest spec. */
    public static final String MODULE_UUID = "4b7a2e10-8e92-4a17-8a44-6b9a82f5d3a8";

    /** Minimum supported Bedrock engine version (1.20.0 — Geyser baseline). */
    public static final int[] MIN_ENGINE_VERSION = {1, 20, 0};

    public static final String PACK_NAME = "Sapientia";
    public static final String PACK_DESCRIPTION = "Sapientia custom content (Bedrock)";

    private BedrockPackConstants() {}
}
