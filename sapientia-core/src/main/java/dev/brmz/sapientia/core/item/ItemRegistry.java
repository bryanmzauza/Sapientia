package dev.brmz.sapientia.core.item;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import dev.brmz.sapientia.api.item.SapientiaItem;
import dev.brmz.sapientia.api.overrides.ContentOverrides;
import dev.brmz.sapientia.api.overrides.ItemOverride;
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
 * carries its identity via a {@link PersistentDataContainer} tag. See ADR-007 / ADR-012.
 *
 * <p>Two registration entry points coexist:
 * <ul>
 *   <li>{@link #register(ItemDefinition)} — lightweight definition used by legacy
 *       built-ins and tests.</li>
 *   <li>{@link #register(SapientiaItem)} — the Slimefun-style Java API addons and
 *       the built-in catalog use going forward (ADR-012). Internally it is bridged
 *       to an {@link ItemDefinition} so the existing give/tab-complete code keeps
 *       working unchanged.</li>
 * </ul>
 */
public final class ItemRegistry {

    private final Plugin plugin;
    private final Messages messages;
    private final NamespacedKey idKey;
    private final Map<String, ItemDefinition> definitions = new LinkedHashMap<>();
    private final Map<NamespacedKey, SapientiaItem> sapientiaItems = new LinkedHashMap<>();
    private @Nullable ContentOverrides overrides;

    public ItemRegistry(@NotNull Plugin plugin, @NotNull Messages messages) {
        this.plugin = plugin;
        this.messages = messages;
        this.idKey = new NamespacedKey(plugin, "item_id");
    }

    /** Injects the override source. Safe to call post-construction / on reload wiring (T-160). */
    public void setOverrides(@Nullable ContentOverrides overrides) {
        this.overrides = overrides;
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

    /** Registers a Slimefun-style item (ADR-012). */
    public void register(@NotNull SapientiaItem item) {
        NamespacedKey key = item.id();
        if (sapientiaItems.putIfAbsent(key, item) != null) {
            throw new IllegalStateException("Duplicate Sapientia item id: " + key);
        }
        register(new ItemDefinition(
                key.toString(),
                item.baseMaterial(),
                item.displayNameKey(),
                item.loreKeys()));
    }

    public @NotNull Optional<ItemDefinition> get(@NotNull String id) {
        return Optional.ofNullable(definitions.get(id));
    }

    public @NotNull Optional<SapientiaItem> find(@NotNull NamespacedKey id) {
        return Optional.ofNullable(sapientiaItems.get(id));
    }

    public @NotNull Map<String, ItemDefinition> all() {
        return Collections.unmodifiableMap(definitions);
    }

    public @NotNull Map<NamespacedKey, SapientiaItem> allSapientiaItems() {
        return Collections.unmodifiableMap(sapientiaItems);
    }

    /** Extracts the Sapientia id tag from an item stack, if present. */
    public @Nullable String idOf(@Nullable ItemStack stack) {
        if (stack == null || !stack.hasItemMeta()) {
            return null;
        }
        PersistentDataContainer pdc = stack.getItemMeta().getPersistentDataContainer();
        return pdc.get(idKey, PersistentDataType.STRING);
    }

    /** Resolves an {@link ItemStack} back to the {@link SapientiaItem} it represents. */
    public @Nullable SapientiaItem resolve(@Nullable ItemStack stack) {
        String id = idOf(stack);
        if (id == null) {
            return null;
        }
        NamespacedKey key = NamespacedKey.fromString(id);
        return key == null ? null : sapientiaItems.get(key);
    }

    /** Produces a new stack for the given id. Returns null if the id is unknown. */
    public @Nullable ItemStack createStack(@NotNull String id, int amount) {
        ItemDefinition def = definitions.get(id);
        if (def == null) {
            return null;
        }
        Material material = def.material();
        String displayNameKey = def.displayNameKey();
        List<String> loreKeys = def.loreKeys();
        if (overrides != null) {
            NamespacedKey key = NamespacedKey.fromString(id);
            if (key != null) {
                ItemOverride itemOv = overrides.forItem(key).orElse(null);
                if (itemOv != null) {
                    if (itemOv.material().isPresent())       material = itemOv.material().get();
                    if (itemOv.displayNameKey().isPresent()) displayNameKey = itemOv.displayNameKey().get();
                    if (itemOv.loreKeys().isPresent())       loreKeys = itemOv.loreKeys().get();
                } else {
                    // Block companion items share the block's id; let BlockOverride retune them too.
                    dev.brmz.sapientia.api.overrides.BlockOverride blockOv =
                            overrides.forBlock(key).orElse(null);
                    if (blockOv != null) {
                        if (blockOv.material().isPresent())       material = blockOv.material().get();
                        if (blockOv.displayNameKey().isPresent()) displayNameKey = blockOv.displayNameKey().get();
                    }
                }
            }
        }
        final String finalDisplayNameKey = displayNameKey;
        final List<String> finalLoreKeys = loreKeys;
        ItemStack stack = new ItemStack(material, Math.max(1, amount));
        stack.editMeta(meta -> {
            Component displayName = messages.component(finalDisplayNameKey)
                    .style(Style.style().decoration(TextDecoration.ITALIC, false).build());
            meta.displayName(displayName);
            if (!finalLoreKeys.isEmpty()) {
                List<Component> lore = finalLoreKeys.stream()
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
