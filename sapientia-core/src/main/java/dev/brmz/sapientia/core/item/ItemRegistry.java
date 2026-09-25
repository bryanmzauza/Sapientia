package dev.brmz.sapientia.core.item;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import dev.brmz.sapientia.api.item.SapientiaItem;
import dev.brmz.sapientia.api.overrides.BlockOverride;
import dev.brmz.sapientia.api.overrides.ContentOverrides;
import dev.brmz.sapientia.api.overrides.ItemOverride;
import dev.brmz.sapientia.core.i18n.Messages;
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.components.CustomModelDataComponent;
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

    /** Namespace of the built-in catalogue, whose textures ship inside the plugin jar. */
    public static final String BUNDLED_NAMESPACE = "sapientia";

    private final Plugin plugin;
    private final Messages messages;
    private final NamespacedKey idKey;
    private final Map<String, ItemDefinition> definitions = new LinkedHashMap<>();
    private final Map<NamespacedKey, SapientiaItem> sapientiaItems = new LinkedHashMap<>();
    private @Nullable ContentOverrides overrides;
    private Set<String> itemModelIds = Set.of();

    public ItemRegistry(@NotNull Plugin plugin, @NotNull Messages messages) {
        this.plugin = plugin;
        this.messages = messages;
        this.idKey = new NamespacedKey(plugin, "item_id");
    }

    /** Injects the override source. Safe to call post-construction / on reload wiring (T-160). */
    public void setOverrides(@Nullable ContentOverrides overrides) {
        this.overrides = overrides;
    }

    /**
     * Ids (without namespace) of built-in items that have a bundled model. New
     * stacks of these items carry a {@code minecraft:item_model} component
     * pointing at {@code sapientia:<id>}. Pass an empty set to disable.
     */
    public void setItemModels(@NotNull Set<String> ids) {
        this.itemModelIds = Set.copyOf(ids);
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
                item.loreKeys(),
                item.customModelData()));
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
        Effective eff = effective(def);
        NamespacedKey key = NamespacedKey.fromString(id);
        boolean bundledModel = key != null
                && BUNDLED_NAMESPACE.equals(key.getNamespace())
                && itemModelIds.contains(key.getKey());
        Style plain = Style.style().decoration(TextDecoration.ITALIC, false).build();
        ItemStack stack = new ItemStack(eff.material(), Math.max(1, amount));
        stack.editMeta(meta -> {
            meta.displayName(messages.component(eff.displayNameKey()).style(plain));
            if (!eff.loreKeys().isEmpty()) {
                meta.lore(eff.loreKeys().stream()
                        .map(loreKey -> messages.component(loreKey).style(plain))
                        .toList());
            }
            if (bundledModel) {
                meta.setItemModel(key);
            }
            if (eff.customModelData() > 0) {
                CustomModelDataComponent cmd = meta.getCustomModelDataComponent();
                cmd.setFloats(List.of((float) eff.customModelData()));
                meta.setCustomModelDataComponent(cmd);
            }
            meta.getPersistentDataContainer().set(idKey, PersistentDataType.STRING, def.id());
        });
        return stack;
    }

    /** Base material for the given id after overrides, or throws if the id is unknown. */
    public @NotNull Material materialOf(@NotNull String id) {
        ItemDefinition def = definitions.get(id);
        if (def == null) {
            throw new IllegalArgumentException("Unknown Sapientia item id: " + id);
        }
        return effective(def).material();
    }

    private record Effective(Material material, String displayNameKey, List<String> loreKeys, int customModelData) {}

    /** Applies item (or, for block companion items, block) overrides to a definition. */
    private Effective effective(ItemDefinition def) {
        Material material = def.material();
        String displayNameKey = def.displayNameKey();
        List<String> loreKeys = def.loreKeys();
        int customModelData = def.customModelData();
        NamespacedKey key = overrides == null ? null : NamespacedKey.fromString(def.id());
        if (key != null) {
            ItemOverride itemOv = overrides.forItem(key).orElse(null);
            if (itemOv != null) {
                if (itemOv.material().isPresent())         material = itemOv.material().get();
                if (itemOv.displayNameKey().isPresent())   displayNameKey = itemOv.displayNameKey().get();
                if (itemOv.loreKeys().isPresent())         loreKeys = itemOv.loreKeys().get();
                if (itemOv.customModelData().isPresent())  customModelData = itemOv.customModelData().get();
            } else {
                // Block companion items share the block's id; let BlockOverride retune them too.
                BlockOverride blockOv = overrides.forBlock(key).orElse(null);
                if (blockOv != null) {
                    if (blockOv.material().isPresent())       material = blockOv.material().get();
                    if (blockOv.displayNameKey().isPresent()) displayNameKey = blockOv.displayNameKey().get();
                }
            }
        }
        return new Effective(material, displayNameKey, loreKeys, customModelData);
    }

    /** Immutable description of a Sapientia item. */
    public record ItemDefinition(
            String id,
            Material material,
            String displayNameKey,
            List<String> loreKeys,
            int customModelData) {

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
            if (customModelData < 0) {
                throw new IllegalArgumentException("customModelData must be >= 0");
            }
            loreKeys = loreKeys == null ? List.of() : List.copyOf(loreKeys);
        }

        /** Convenience constructor for legacy callers without custom-model-data. */
        public ItemDefinition(String id, Material material, String displayNameKey, List<String> loreKeys) {
            this(id, material, displayNameKey, loreKeys, 0);
        }
    }
}
