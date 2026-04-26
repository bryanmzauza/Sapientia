package dev.brmz.sapientia.core.ui;

import java.util.Locale;
import java.util.Map;

import dev.brmz.sapientia.api.energy.EnergyNode;
import dev.brmz.sapientia.api.ui.BedrockFormRenderer;
import dev.brmz.sapientia.bedrock.forms.SapientiaCustomForm;
import dev.brmz.sapientia.core.i18n.Messages;
import dev.brmz.sapientia.core.i18n.TextAdapter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

/**
 * Bedrock counterpart of {@link MachineJavaRenderer} (T-202). Builds a
 * {@link SapientiaCustomForm} with two informational labels (current / max
 * stored energy plus a percentage) and a single {@code running} toggle the
 * player can flip to start or stop the machine.
 */
public final class MachineBedrockRenderer implements BedrockFormRenderer<EnergyNode> {

    private final Messages messages;
    private final MachineRunningRegistry running;

    public MachineBedrockRenderer(@NotNull Messages messages,
                                  @NotNull MachineRunningRegistry running) {
        this.messages = messages;
        this.running = running;
    }

    @Override
    public void open(@NotNull Player player, @NotNull EnergyNode node) {
        long stored = node.bufferCurrent();
        long capacity = Math.max(node.bufferMax(), 1);
        int percent = (int) ((stored * 100L) / capacity);

        boolean isRunning = running.isRunning(node);

        String title = TextAdapter.toPlainBedrock(messages.component("ui.machine.title"));
        String content = TextAdapter.toPlainBedrock(messages.component("ui.machine.bedrock.content",
                Map.of("stored", String.valueOf(stored),
                        "capacity", String.valueOf(capacity),
                        "percent", String.valueOf(percent))));
        String toggleLabel = TextAdapter.toPlainBedrock(messages.component("ui.machine.bedrock.toggle"));
        String runningLabel = TextAdapter.toPlainBedrock(messages.component(isRunning
                ? "ui.machine.running"
                : "ui.machine.stopped"));

        new SapientiaCustomForm()
                .title(title)
                .label(content)
                .label(runningLabel)
                .toggle(toggleLabel, isRunning)
                .onSubmit(response -> {
                    // First two components are labels (no response value). The
                    // toggle is the third declared component, hence index 2.
                    Boolean wantRunning = response.asToggle(2);
                    if (wantRunning == null) return;
                    running.setRunning(node, wantRunning);
                })
                .send(player);
    }

    /** Visible for tests — formatted percentage of stored vs capacity (0..100). */
    @SuppressWarnings("unused")
    static int percent(long stored, long capacity) {
        if (capacity <= 0) return 0;
        long pct = (stored * 100L) / capacity;
        return (int) Math.max(0, Math.min(100, pct));
    }

    /** Visible for tests — locale-stable lower-cased lookup of running state. */
    @SuppressWarnings("unused")
    static String stateKey(boolean running) {
        return running ? "ui.machine.running".toLowerCase(Locale.ROOT)
                       : "ui.machine.stopped".toLowerCase(Locale.ROOT);
    }
}
