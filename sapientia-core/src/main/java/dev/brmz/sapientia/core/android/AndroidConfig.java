package dev.brmz.sapientia.core.android;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.jetbrains.annotations.NotNull;

/**
 * Operator-facing android caps + tick configuration (T-456 / 1.9.0).
 *
 * <p>Reads under the {@code androids:} section of {@code config.yml}:
 * <ul>
 *   <li>{@code androids.cap.server} (default 200) — hard server-wide
 *       maximum number of androids that can be placed. Once reached,
 *       further placement events are cancelled.</li>
 *   <li>{@code androids.cap.chunk} (read-only, locked at 4) — surfaced
 *       here for documentation only. The cap is enforced by
 *       {@link AndroidCaps#CHUNK_CAP} and cannot be changed without a
 *       roadmap revision (see T-456).</li>
 * </ul>
 *
 * <p>Pure POJO so it can be unit-tested without Bukkit.
 */
public final class AndroidConfig {

    /** Default server-wide cap when {@code config.yml} does not override it. */
    public static final int DEFAULT_SERVER_CAP = 200;

    /** Hard floor — never let an op set the server cap below 1. */
    public static final int MIN_SERVER_CAP = 1;

    /** Hard ceiling — way above what TPS can reasonably handle (P-020). */
    public static final int MAX_SERVER_CAP = 5_000;

    private final int serverCap;

    public AndroidConfig(int serverCap) {
        if (serverCap < MIN_SERVER_CAP) {
            this.serverCap = MIN_SERVER_CAP;
        } else if (serverCap > MAX_SERVER_CAP) {
            this.serverCap = MAX_SERVER_CAP;
        } else {
            this.serverCap = serverCap;
        }
    }

    public int serverCap() {
        return serverCap;
    }

    public static @NotNull AndroidConfig defaults() {
        return new AndroidConfig(DEFAULT_SERVER_CAP);
    }

    /**
     * Read the config from a Bukkit {@link FileConfiguration}. Missing
     * sections / keys silently fall back to {@link #DEFAULT_SERVER_CAP}.
     */
    public static @NotNull AndroidConfig from(@NotNull FileConfiguration config) {
        ConfigurationSection androids = config.getConfigurationSection("androids");
        if (androids == null) return defaults();
        ConfigurationSection cap = androids.getConfigurationSection("cap");
        if (cap == null) return defaults();
        int server = cap.getInt("server", DEFAULT_SERVER_CAP);
        return new AndroidConfig(server);
    }
}
