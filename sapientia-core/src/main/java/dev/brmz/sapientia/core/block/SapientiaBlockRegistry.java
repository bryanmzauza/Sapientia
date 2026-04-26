package dev.brmz.sapientia.core.block;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

import dev.brmz.sapientia.api.block.SapientiaBlock;
import org.bukkit.NamespacedKey;
import org.jetbrains.annotations.NotNull;

/**
 * In-memory registry of Java-defined Sapientia blocks (ADR-012). Resolves both by
 * block id and by the id of the item form that places them.
 */
public final class SapientiaBlockRegistry {

    private final Map<NamespacedKey, SapientiaBlock> byId = new LinkedHashMap<>();
    private final Map<NamespacedKey, SapientiaBlock> byItemId = new LinkedHashMap<>();

    public void register(@NotNull SapientiaBlock block) {
        NamespacedKey id = block.id();
        if (byId.putIfAbsent(id, block) != null) {
            throw new IllegalStateException("Duplicate Sapientia block id: " + id);
        }
        NamespacedKey itemId = block.itemId();
        if (byItemId.putIfAbsent(itemId, block) != null) {
            byId.remove(id);
            throw new IllegalStateException(
                    "Duplicate Sapientia block item id: " + itemId + " (from block " + id + ")");
        }
    }

    public @NotNull Optional<SapientiaBlock> find(@NotNull NamespacedKey id) {
        return Optional.ofNullable(byId.get(id));
    }

    public @NotNull Optional<SapientiaBlock> findByItemId(@NotNull NamespacedKey itemId) {
        return Optional.ofNullable(byItemId.get(itemId));
    }

    public @NotNull Map<NamespacedKey, SapientiaBlock> all() {
        return Collections.unmodifiableMap(byId);
    }
}
