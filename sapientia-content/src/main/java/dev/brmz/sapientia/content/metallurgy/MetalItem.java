package dev.brmz.sapientia.content.metallurgy;

import dev.brmz.sapientia.api.guide.GuideCategory;
import dev.brmz.sapientia.api.item.SapientiaItem;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

/**
 * Generic Sapientia item representing one ({@link Metal}, {@link MetalForm}) pair
 * (T-402 / T-403). 54 instances are registered for the six raw metals and another
 * 24 for the three alloys, all backed by vanilla materials with custom-model-data
 * tags so the resource pack can swap textures.
 */
public final class MetalItem implements SapientiaItem {

    private final NamespacedKey id;
    private final Metal metal;
    private final MetalForm form;
    private final String displayKey;

    public MetalItem(@NotNull Plugin plugin, @NotNull Metal metal, @NotNull MetalForm form) {
        this.metal = metal;
        this.form = form;
        String base = metal.idBase() + "_" + form.suffix();
        this.id = new NamespacedKey(plugin, base);
        this.displayKey = "metal." + base + ".name";
    }

    @Override
    public @NotNull NamespacedKey id() {
        return id;
    }

    @Override
    public @NotNull Material baseMaterial() {
        return form.defaultMaterial();
    }

    @Override
    public @NotNull String displayNameKey() {
        return displayKey;
    }

    @Override
    public @NotNull GuideCategory guideCategory() {
        return GuideCategory.MATERIAL;
    }

    public @NotNull Metal metal() {
        return metal;
    }

    public @NotNull MetalForm form() {
        return form;
    }
}
