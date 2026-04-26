package dev.brmz.sapientia.core.i18n;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.jetbrains.annotations.NotNull;

/**
 * Converts Adventure {@link Component}s to flat strings suitable for the surfaces
 * that cannot render the full Adventure tree (T-205 / 1.0.0).
 *
 * <p>{@link #toPlainBedrock(Component)} produces a string with legacy {@code §}
 * colour codes preserved while {@code hover} / {@code click} events, gradients
 * and other Adventure-only effects are silently dropped — this is the format
 * Floodgate forms accept on Bedrock clients.
 */
public final class TextAdapter {

    private static final LegacyComponentSerializer LEGACY =
            LegacyComponentSerializer.builder()
                    .character(LegacyComponentSerializer.SECTION_CHAR)
                    .hexColors()
                    .build();

    private TextAdapter() {}

    /**
     * Renders {@code component} to a Bedrock-friendly string. Colours and basic
     * decorations (bold, italic, underline) are preserved as legacy {@code §}
     * codes; everything else (hover, click, fonts, gradients) is dropped.
     */
    public static @NotNull String toPlainBedrock(@NotNull Component component) {
        return LEGACY.serialize(component);
    }

    /** Strips every formatting code, leaving only the textual content. */
    public static @NotNull String toPlain(@NotNull Component component) {
        return PlainTextComponentSerializer.plainText().serialize(component);
    }
}
