package dev.brmz.sapientia.bedrock;

import java.lang.reflect.Method;
import java.util.logging.Level;
import java.util.logging.Logger;

import dev.brmz.sapientia.api.PlatformType;
import dev.brmz.sapientia.api.ui.BedrockFormRenderer;
import dev.brmz.sapientia.api.ui.UIDescriptor;
import dev.brmz.sapientia.api.ui.UIProvider;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

/**
 * Bedrock {@link UIProvider}. Dispatches Floodgate forms when a
 * {@link BedrockFormRenderer} is provided, otherwise the caller should delegate to the
 * Java provider. Floodgate types are accessed reflectively so the build still compiles
 * without the Floodgate API on the runtime classpath.
 */
public final class BedrockFormsUIProvider implements UIProvider {

    private final Logger logger;

    public BedrockFormsUIProvider(@NotNull Logger logger) {
        this.logger = logger;
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
        if (renderer == null) {
            logger.fine(() -> "No Bedrock renderer for " + descriptor.key()
                    + "; caller should fall back to Java.");
            return;
        }
        renderer.open(player, context);
    }

    /** True when Floodgate is loaded and ready to dispatch forms. */
    public static boolean isReady() {
        try {
            Class<?> api = Class.forName("org.geysermc.floodgate.api.FloodgateApi");
            Method getInstance = api.getMethod("getInstance");
            return getInstance.invoke(null) != null;
        } catch (ReflectiveOperationException e) {
            return false;
        }
    }
}
