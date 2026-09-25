package dev.brmz.sapientia.core.engine;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import dev.brmz.sapientia.api.block.SapientiaBlock;
import dev.brmz.sapientia.core.block.BlockKey;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/** The engine's glue: machines follow chunk indexing, player activity and chunk unloads. */
final class SapientiaEngineTest {

    private static final NamespacedKey MACHINE = new NamespacedKey("sapientia", "test_machine");
    private static final NamespacedKey CABLE = new NamespacedKey("sapientia", "test_cable");

    private final EngineConfig config = new EngineConfig(5.0, 1, 40, ChunkLimits.DEFAULT);
    private final SapientiaEngine engine = new SapientiaEngine(Logger.getLogger("SapientiaEngineTest"), config);
    private final List<BlockKey> runs = new ArrayList<>();

    private static SapientiaBlock block(NamespacedKey id) {
        return new SapientiaBlock() {
            @Override public @NotNull NamespacedKey id() { return id; }
            @Override public @NotNull Material baseMaterial() { return Material.STONE; }
            @Override public @NotNull String displayNameKey() { return "block.test.name"; }
        };
    }

    private void registerMachine() {
        engine.registerBehavior(MACHINE, 1, context -> {
            runs.add(engine.keyOf(context));
            return 5;
        });
    }

    private void tick(int times) {
        for (int i = 0; i < times; i++) engine.tick();
    }

    @Test
    void onlyBlocksWithABehaviourBecomeMachines() {
        registerMachine();
        engine.onBlockAdded(new BlockKey("world", 0, 64, 0), block(MACHINE));
        engine.onBlockAdded(new BlockKey("world", 1, 64, 0), block(CABLE));

        assertThat(engine.isProcessor(MACHINE)).isTrue();
        assertThat(engine.isProcessor(CABLE)).isFalse();
        assertThat(engine.scheduler().registeredCount()).isEqualTo(1);
    }

    @Test
    void machinesStayPausedUntilAPlayerComesClose() {
        registerMachine();
        BlockKey key = new BlockKey("world", 8, 64, 8);
        engine.onBlockAdded(key, block(MACHINE));

        tick(30);
        assertThat(runs).isEmpty();

        engine.activity().addViewer("world", 1, 0, engine.currentTick()); // chunk 0 is within radius 1
        tick(30);
        assertThat(runs).isNotEmpty().allMatch(key::equals);
    }

    @Test
    void machinesPauseAfterThePlayerLeavesAndTheDelayPasses() {
        registerMachine();
        engine.activity().addViewer("world", 0, 0, 0);
        engine.onBlockAdded(new BlockKey("world", 0, 64, 0), block(MACHINE));
        tick(20);
        assertThat(runs).isNotEmpty();

        engine.activity().removeViewer("world", 0, 0, engine.currentTick());
        tick(60); // delay is 40 ticks; the sweep runs every 20
        int afterPause = runs.size();
        tick(100);

        assertThat(runs).hasSize(afterPause);
        assertThat(engine.machineCounts().get("paused")).isEqualTo(1);
    }

    @Test
    void unloadingAChunkRemovesItsMachines() {
        registerMachine();
        BlockKey key = new BlockKey("world", 0, 64, 0);
        engine.onBlockAdded(key, block(MACHINE));

        engine.onChunkUnloaded("world", 0, 0);

        assertThat(engine.scheduler().registeredCount()).isZero();
    }

    @Test
    void breakingAMachineUnregistersIt() {
        registerMachine();
        BlockKey key = new BlockKey("world", 0, 64, 0);
        engine.onBlockAdded(key, block(MACHINE));

        engine.onBlockRemoved(key, block(MACHINE));

        assertThat(engine.scheduler().registeredCount()).isZero();
    }

    @Test
    void systemTasksRunOnTheirPeriodAndAreTimed() {
        int[] calls = {0};
        engine.addSystemTask("network", 10, 5, () -> calls[0]++);

        tick(45);

        assertThat(calls[0]).isEqualTo(5); // ticks 5, 15, 25, 35, 45
        assertThat(engine.perf().sections()).containsKeys("network", "machines");
    }

    @Test
    void registeringTheSameBehaviourTwiceFails() {
        registerMachine();
        org.assertj.core.api.Assertions.assertThatThrownBy(this::registerMachine)
                .isInstanceOf(IllegalStateException.class);
    }
}
