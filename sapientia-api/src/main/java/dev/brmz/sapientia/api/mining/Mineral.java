package dev.brmz.sapientia.api.mining;

import java.util.List;

import dev.brmz.sapientia.api.progression.Era;
import org.bukkit.NamespacedKey;
import org.jetbrains.annotations.NotNull;

/**
 * A mineral found by breaking natural rock. Minerals are mixtures: one or
 * more primary elements plus secondary and trace elements that better
 * separation methods recover.
 *
 * <p>Breaking a host block may drop the mineral fragment item
 * ({@code <id>_fragment}); separation may leave the tailings item
 * ({@code <id>_tailings}). Both items must be registered by the content that
 * registers the mineral.
 *
 * @param id          mineral id, e.g. {@code sapientia:native_copper}
 * @param era         era from which the mineral drops
 * @param composition elements and their shares; at least one primary
 * @param sources     where the mineral drops; empty for minerals that only come from processing
 */
public record Mineral(
        @NotNull NamespacedKey id,
        @NotNull Era era,
        @NotNull List<MineralComponent> composition,
        @NotNull List<MineralSource> sources) {

    public Mineral {
        composition = List.copyOf(composition);
        sources = List.copyOf(sources);
        if (composition.stream().noneMatch(c -> c.share() == MineralComponent.Share.PRIMARY)) {
            throw new IllegalArgumentException("mineral " + id + " needs a primary element");
        }
    }

    /** Id of the fragment item that drops from rock. */
    public @NotNull NamespacedKey fragmentItem() {
        return new NamespacedKey(id.getNamespace(), id.getKey() + "_fragment");
    }

    /** Id of the tailings item left by separation. */
    public @NotNull NamespacedKey tailingsItem() {
        return new NamespacedKey(id.getNamespace(), id.getKey() + "_tailings");
    }

    /** Whether some element is recovered only by better methods, so separation can leave tailings. */
    public boolean hasByproducts() {
        return composition.stream().anyMatch(c -> c.share() != MineralComponent.Share.PRIMARY);
    }
}
