package dev.brmz.sapientia.core.item;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import dev.brmz.sapientia.core.i18n.Messages;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * In-memory registry of custom items. Each item is identified by a stable string key and
 * carries its identity via a {@link PersistentDataContainer} tag. See ADR-010.
 */
public final class ItemRegistry {

    private final Plugin plugin;
    private final Messages messages;
    private final NamespacedKey idKey;
    private final Map<String, ItemDefinition> definitions = new LinkedHashMap<>();

    public ItemRegistry(@NotNull Plugin plugin, @NotNull Messages messages) {
        this.plugin = plugin;
        this.messages = messages;
        this.idKey = new NamespacedKey(plugin, "item_id");
    }

    public @NotNull NamespacedKey idKey() {
        return idKey;
    }

    /** Registers a definition. Throws if another item already claimed the same key. */
    public void register(@NotNull ItemDefinition definition) {
        if (definitions.putIfAbsent(definition.id(), definition) != null) {
            throw new IllegalStateException("Duplicate Sapientia item id: " + definition.id());
        }
    }

    public @NotNull Optional<ItemDefinition> get(@NotNull String id) {
        return Optional.ofNullable(definitions.get(id));
    }

    public @NotNull Map<String, ItemDefinition> all() {
        return Collections.unmodifiableMap(definitions);
    }

    /** Extracts the Sapientia id tag from an item stack, if present. */
    public @Nullable String idOf(@Nullable ItemStack stack) {
        if (stack == null || !stack.hasItemMeta()) {
            return null;
        }
        PersistentDataContainer pdc = stack.getItemMeta().getPersistentDataContainer();
        return pdc.get(idKey, PersistentDataType.STRING);
    }

    /** Produces a new stack for the given id. Returns null if the id is unknown. */
    public @Nullable ItemStack createStack(@NotNull String id, int amount) {
        ItemDefinition def = definitions.get(id);
        if (def == null) {
            return null;
        }
        ItemStack stack = new ItemStack(def.material(), Math.max(1, amount));
        stack.editMeta(meta -> {
            Component displayName = messages.component(def.displayNameKey())
                    .style(Style.style().decoration(TextDecoration.ITALIC, false).build());
            meta.displayName(displayName);
            if (!def.loreKeys().isEmpty()) {
                List<Component> lore = def.loreKeys().stream()
                        .map(key -> (Component) messages.component(key)
                                .style(Style.style().decoration(TextDecoration.ITALIC, false).build()))
                        .toList();
                meta.lore(lore);
            }
            meta.getPersistentDataContainer().set(idKey, PersistentDataType.STRING, def.id());
        });
        return stack;
    }

    /** Immutable description of a Sapientia item. */
    public record ItemDefinition(
            String id,
            Material material,
            String displayNameKey,
            List<String> loreKeys) {

        public ItemDefinition {
            if (id == null || id.isBlank()) {
                throw new IllegalArgumentException("id must not be blank");
            }
            if (material == null) {
                throw new IllegalArgumentException("material must not be null");
            }
            if (displayNameKey == null || displayNameKey.isBlank()) {
                throw new IllegalArgumentException("displayNameKey must not be blank");
            }
            loreKeys = loreKeys == null ? List.of() : List.copyOf(loreKeys);
        }
    }
}
