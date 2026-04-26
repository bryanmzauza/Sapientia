package dev.brmz.sapientia.content.android;

import dev.brmz.sapientia.api.Sapientia;
import dev.brmz.sapientia.api.android.AndroidType;
import dev.brmz.sapientia.api.block.SapientiaBlock;
import dev.brmz.sapientia.api.events.SapientiaBlockBreakEvent;
import dev.brmz.sapientia.api.events.SapientiaBlockInteractEvent;
import dev.brmz.sapientia.api.events.SapientiaBlockPlaceEvent;
import dev.brmz.sapientia.api.guide.GuideCategory;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

/**
 * Shared scaffolding for the eight android variants shipped in 1.9.0
 * (T-451 / T-452). Mirrors {@code LogisticsContentBlock} /
 * {@code MachineEnergyBlock}: registers a node with
 * {@code AndroidService} on place, removes it on break, and surfaces a
 * read-only inspect message on right-click.
 *
 * <p>The cap enforcement (T-456) lives in
 * {@code dev.brmz.sapientia.core.android.AndroidCapsListener}; if the
 * placement passes that gate, this block also runs a defensive
 * {@code addNode} which is a no-op when the cap is hit (e.g. simultaneous
 * placement by two players).
 */
public abstract class AndroidContentBlock implements SapientiaBlock {

    private final NamespacedKey id;
    private final Material material;
    private final String displayKey;
    private final AndroidType type;

    protected AndroidContentBlock(@NotNull Plugin plugin,
                                  @NotNull String name,
                                  @NotNull Material material,
                                  @NotNull String displayKey,
                                  @NotNull AndroidType type) {
        this.id = new NamespacedKey(plugin, name);
        this.material = material;
        this.displayKey = displayKey;
        this.type = type;
    }

    @Override public final @NotNull NamespacedKey id() { return id; }
    @Override public final @NotNull Material baseMaterial() { return material; }
    @Override public final @NotNull String displayNameKey() { return displayKey; }
    @Override public final @NotNull GuideCategory guideCategory() { return GuideCategory.MACHINE; }

    public final @NotNull AndroidType type() { return type; }

    @Override
    public void onPlace(@NotNull SapientiaBlockPlaceEvent event) {
        Player player = event.player();
        Sapientia.get().androids().addNode(event.block(), type, player.getUniqueId());
    }

    @Override
    public void onBreak(@NotNull SapientiaBlockBreakEvent event) {
        Sapientia.get().androids().removeNode(event.block());
    }

    @Override
    public void onInteract(@NotNull SapientiaBlockInteractEvent event) {
        // Open the program selector UI (T-453 / 1.9.1). Right-clicking with
        // an empty hand opens the chest; clicking a paper ticket assigns the
        // chosen logic program. Bedrock players auto-fall-back through
        // BedrockFormsUIProvider per docs/ui-strategy.md §3.2.
        Player player = event.player();
        if (Sapientia.get().androids().nodeAt(event.block()).isEmpty()) return;
        Sapientia.get().openUI(
                player,
                org.bukkit.NamespacedKey.fromString("sapientia:android_program_selector"),
                Sapientia.get().androids().nodeAt(event.block()).get());
    }
}
