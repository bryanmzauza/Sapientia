package dev.brmz.sapientia.core.block;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;

import dev.brmz.sapientia.api.block.SapientiaBlock;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.junit.jupiter.api.Test;

final class SapientiaBlockRegistryTest {

    @Test
    void registersAndFindsByIdAndItemId() {
        SapientiaBlockRegistry registry = new SapientiaBlockRegistry();
        NamespacedKey id = key("pedestal");
        SapientiaBlock block = stub(id, id);

        registry.register(block);

        assertThat(registry.find(id)).containsSame(block);
        assertThat(registry.findByItemId(id)).containsSame(block);
        assertThat(registry.all()).containsEntry(id, block);
    }

    @Test
    void rejectsDuplicateIds() {
        SapientiaBlockRegistry registry = new SapientiaBlockRegistry();
        NamespacedKey id = key("console");
        registry.register(stub(id, id));

        assertThatThrownBy(() -> registry.register(stub(id, key("other"))))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining(id.toString());
    }

    @Test
    void rejectsDuplicateItemIds() {
        SapientiaBlockRegistry registry = new SapientiaBlockRegistry();
        NamespacedKey item = key("shared-item");
        registry.register(stub(key("a"), item));

        assertThatThrownBy(() -> registry.register(stub(key("b"), item)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("item id");
        // First registration must stay.
        assertThat(registry.find(key("a"))).isPresent();
        assertThat(registry.find(key("b"))).isEmpty();
    }

    private static NamespacedKey key(String name) {
        NamespacedKey k = NamespacedKey.fromString("sapientia:" + name);
        if (k == null) throw new AssertionError("invalid key: " + name);
        return k;
    }

    private static SapientiaBlock stub(NamespacedKey id, NamespacedKey itemId) {
        return new SapientiaBlock() {
            @Override public NamespacedKey id() { return id; }
            @Override public NamespacedKey itemId() { return itemId; }
            @Override public Material baseMaterial() { return Material.STONE; }
            @Override public String displayNameKey() { return "x"; }
        };
    }

    @SuppressWarnings("unused")
    private static List<String> unused() { return List.of(); }
}
