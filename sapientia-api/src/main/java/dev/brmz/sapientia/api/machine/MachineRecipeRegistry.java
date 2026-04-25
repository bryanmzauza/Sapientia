package dev.brmz.sapientia.api.machine;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * In-memory registry of {@link MachineRecipe}s keyed by machine kind (T-404 / 1.4.1).
 *
 * <p>Lives in the public API so addons can register their own recipes against
 * existing machine kinds. The core plugin owns a singleton instance available
 * through {@code SapientiaAPI#machineRecipes()}.
 */
public final class MachineRecipeRegistry {

    private final Map<NamespacedKey, List<MachineRecipe>> byMachine = new HashMap<>();

    public synchronized void register(@NotNull MachineRecipe recipe) {
        byMachine.computeIfAbsent(recipe.machineId(), k -> new ArrayList<>()).add(recipe);
    }

    public synchronized @NotNull Collection<MachineRecipe> recipesFor(@NotNull NamespacedKey machineId) {
        List<MachineRecipe> list = byMachine.get(machineId);
        return list == null ? List.of() : Collections.unmodifiableList(new ArrayList<>(list));
    }

    /** Returns the first recipe for {@code machineId} that matches {@code candidate}, or {@code null}. */
    public synchronized @Nullable MachineRecipe findMatching(@NotNull NamespacedKey machineId,
                                                             @Nullable ItemStack candidate) {
        if (candidate == null) return null;
        List<MachineRecipe> list = byMachine.get(machineId);
        if (list == null) return null;
        for (MachineRecipe r : list) {
            if (r.matches(candidate)) return r;
        }
        return null;
    }

    public synchronized int totalRecipes() {
        int total = 0;
        for (List<MachineRecipe> list : byMachine.values()) total += list.size();
        return total;
    }
}
