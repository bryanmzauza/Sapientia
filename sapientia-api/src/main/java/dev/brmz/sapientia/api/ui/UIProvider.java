package dev.brmz.sapientia.api.ui;

import dev.brmz.sapientia.api.PlatformType;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

/**
 * Platform-specific renderer backend. Each implementation handles one
 * {@link PlatformType} and delegates custom UIs through {@link UIDescriptor}.
 * See docs/ui-strategy.md §1.1.
 */
public interface UIProvider {

    @NotNull PlatformType targetPlatform();

    /** Opens an addon-declared UI with the supplied context. */
    <C> void openCustom(@NotNull Player player, @NotNull UIDescriptor<C> descriptor, @NotNull C context);
}
