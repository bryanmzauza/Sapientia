package dev.brmz.sapientia.core.ui;

import dev.brmz.sapientia.api.energy.EnergyNode;
import dev.brmz.sapientia.api.ui.BedrockFormRenderer;
import dev.brmz.sapientia.api.ui.JavaInventoryRenderer;
import dev.brmz.sapientia.api.ui.UIDescriptor;
import org.bukkit.NamespacedKey;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Bundled machine UI (T-145 / 0.3.0 finalisation, T-202 / 1.0.0).
 *
 * <p>Renders the live energy buffer + running flag for any {@link EnergyNode}.
 * Java side: 27-slot chest with an XP-bottle "energy bar" and start/stop button.
 * Bedrock side: {@code CustomForm} with two labels + one toggle.
 */
public final class MachineUIDescriptor implements UIDescriptor<EnergyNode> {

    public static final NamespacedKey KEY =
            NamespacedKey.fromString("sapientia:machine");

    private final MachineRunningRegistry running;
    private final JavaInventoryRenderer<EnergyNode> javaRenderer;
    private final @Nullable BedrockFormRenderer<EnergyNode> bedrockRenderer;

    public MachineUIDescriptor(
            @NotNull MachineRunningRegistry running,
            @NotNull JavaInventoryRenderer<EnergyNode> javaRenderer,
            @Nullable BedrockFormRenderer<EnergyNode> bedrockRenderer) {
        this.running = running;
        this.javaRenderer = javaRenderer;
        this.bedrockRenderer = bedrockRenderer;
    }

    @Override
    public @NotNull NamespacedKey key() {
        return KEY;
    }

    @Override
    public @NotNull JavaInventoryRenderer<EnergyNode> javaRenderer() {
        return javaRenderer;
    }

    @Override
    public @Nullable BedrockFormRenderer<EnergyNode> bedrockRenderer() {
        return bedrockRenderer;
    }

    public @NotNull MachineRunningRegistry running() {
        return running;
    }
}
