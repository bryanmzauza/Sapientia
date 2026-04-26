package dev.brmz.sapientia.api.machine;

import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * A single processing recipe attached to a machine block kind (T-404 / 1.4.1).
 *
 * <p>Each recipe pairs a vanilla {@code input} stack with a vanilla {@code output}
 * stack and the energy cost ({@code energyCost} in SU) plus the number of
 * scheduler ticks it takes to complete ({@code ticksRequired}).
 *
 * <p>For the 1.4.x baseline the matcher is intentionally simple: input and
 * output are matched by {@link ItemStack#isSimilar(ItemStack)} (material + meta),
 * which works for both vanilla items and Sapientia items (which use PDC tags
 * embedded in {@link ItemStack#getItemMeta()}).
 *
 * <p>Recipes are registered against a machine kind (its
 * {@link NamespacedKey block id}) via the core {@code MachineRecipeRegistry}.
 *
 * @param machineId      block id of the machine that runs this recipe
 *                       (e.g. {@code sapientia:macerator})
 * @param input          required input stack (consumed from the chest above)
 * @param output         produced output stack (deposited in the chest below)
 * @param energyCost     SU drained from the machine's own buffer to complete
 * @param ticksRequired  number of scheduler iterations to complete (≥ 1)
 */
public record MachineRecipe(
        @NotNull NamespacedKey machineId,
        @NotNull ItemStack input,
        @NotNull ItemStack output,
        long energyCost,
        int ticksRequired) {

    public MachineRecipe {
        if (input.getAmount() <= 0) {
            throw new IllegalArgumentException("input amount must be > 0");
        }
        if (output.getAmount() <= 0) {
            throw new IllegalArgumentException("output amount must be > 0");
        }
        if (energyCost < 0L) {
            throw new IllegalArgumentException("energyCost must be >= 0");
        }
        if (ticksRequired <= 0) {
            throw new IllegalArgumentException("ticksRequired must be > 0");
        }
    }

    /** Returns true when {@code candidate} is similar to and at-least-as-large as {@link #input}. */
    public boolean matches(@Nullable ItemStack candidate) {
        if (candidate == null || candidate.getAmount() < input.getAmount()) return false;
        return candidate.isSimilar(input);
    }
}
