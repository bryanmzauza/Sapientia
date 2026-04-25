package dev.brmz.sapientia.api.overrides;

import java.util.List;
import java.util.Optional;

import org.bukkit.NamespacedKey;
import org.jetbrains.annotations.NotNull;

/**
 * Read-only view of operator-supplied YAML tweaks (T-160..T-162 / 0.5.0).
 *
 * <p>The Sapientia runtime consults this service every time it renders a stack
 * or computes a recipe result, so hot-reloads take effect on the next lookup.
 */
public interface ContentOverrides {

    @NotNull Optional<ItemOverride> forItem(@NotNull NamespacedKey id);

    @NotNull Optional<BlockOverride> forBlock(@NotNull NamespacedKey id);

    @NotNull Optional<RecipeOverride> forRecipe(@NotNull NamespacedKey id);

    /** Re-reads the YAML files from disk and replaces the current snapshot. */
    @NotNull ReloadReport reload();

    /**
     * Summary of the last reload call, used by {@code /sapientia reload content}.
     *
     * @param items   number of item overrides accepted
     * @param blocks  number of block overrides accepted
     * @param recipes number of recipe overrides accepted
     * @param issues  actionable validation issues (never {@code null})
     */
    record ReloadReport(int items, int blocks, int recipes, @NotNull List<String> issues) {
        public ReloadReport {
            issues = List.copyOf(issues);
        }
    }
}
