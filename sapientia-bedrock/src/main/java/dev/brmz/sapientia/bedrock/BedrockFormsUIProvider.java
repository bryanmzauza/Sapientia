package dev.brmz.sapientia.bedrock;

import java.util.logging.Logger;

import dev.brmz.sapientia.api.PlatformType;
import dev.brmz.sapientia.api.ui.BedrockFormRenderer;
import dev.brmz.sapientia.api.ui.JavaInventoryRenderer;
import dev.brmz.sapientia.api.ui.UIDescriptor;
import dev.brmz.sapientia.api.ui.UIProvider;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

/**
 * Bedrock {@link UIProvider}. Dispatches Floodgate forms when a
 * {@link BedrockFormRenderer} is provided; otherwise falls back to an
 * auto-generated {@code SimpleForm} synthesised from the Java renderer's
 * inventory (T-206). Forms themselves are wrapped by
 * {@link dev.brmz.sapientia.bedrock.forms.SapientiaSimpleForm} and friends.
 */
public final class BedrockFormsUIProvider implements UIProvider {

    private final Logger logger;
    private final BedrockFallbackForm fallback;

    public BedrockFormsUIProvider(@NotNull Logger logger) {
        this.logger = logger;
        this.fallback = new BedrockFallbackForm(logger);
    }

    @Override
    public @NotNull PlatformType targetPlatform() {
        return PlatformType.BEDROCK;
    }

    @Override
    public <C> void openCustom(@NotNull Player player,
                                @NotNull UIDescriptor<C> descriptor,
                                @NotNull C context) {
        BedrockFormRenderer<C> renderer = descriptor.bedrockRenderer();
        if (renderer != null) {
            renderer.open(player, context);
            return;
        }
        JavaInventoryRenderer<C> javaRenderer = descriptor.javaRenderer();
        logger.info(() -> "Bedrock fallback form for " + descriptor.key());
        fallback.openFromJava(player, descriptor, javaRenderer, context);
    }

    /** True when the Floodgate plugin is loaded and ready to dispatch forms. */
    public static boolean isReady() {
        return Bukkit.getPluginManager().getPlugin("floodgate") != null;
    }
}
