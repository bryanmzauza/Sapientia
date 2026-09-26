package dev.brmz.sapientia.content.stoneage;

import dev.brmz.sapientia.api.events.SapientiaBlockPlaceEvent;
import net.kyori.adventure.text.Component;
import org.bukkit.Nameable;
import org.bukkit.block.BlockState;
import org.jetbrains.annotations.NotNull;

/** Shared helpers for Stone Age blocks that stand on vanilla containers. */
final class StoneAgeBlocks {

    private StoneAgeBlocks() {}

    /** Gives the vanilla container the item's name, so its window shows it as the title. */
    static void nameContainer(@NotNull SapientiaBlockPlaceEvent event) {
        Component name = event.placedStack().getItemMeta().displayName();
        BlockState state = event.block().getState();
        if (name != null && state instanceof Nameable nameable) {
            nameable.customName(name);
            state.update(true, false);
        }
    }
}
