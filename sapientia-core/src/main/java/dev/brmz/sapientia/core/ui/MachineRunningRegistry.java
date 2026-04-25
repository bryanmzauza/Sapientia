package dev.brmz.sapientia.core.ui;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import dev.brmz.sapientia.api.energy.EnergyNode;
import org.jetbrains.annotations.NotNull;

/**
 * Tracks the player-toggled "running" flag for each energy node. The energy
 * solver does not consult this registry yet (the demo pipeline always ticks);
 * the toggle is exposed primarily so the Machine UI has a meaningful control to
 * mirror across Java + Bedrock today (T-202). Persistent machine state and a
 * solver that honours the flag land in 1.2.0.
 */
public final class MachineRunningRegistry {

    private final Set<UUID> stopped = ConcurrentHashMap.newKeySet();

    public boolean isRunning(@NotNull EnergyNode node) {
        return !stopped.contains(node.nodeId());
    }

    public void setRunning(@NotNull EnergyNode node, boolean running) {
        if (running) {
            stopped.remove(node.nodeId());
        } else {
            stopped.add(node.nodeId());
        }
    }

    public void clear() {
        stopped.clear();
    }
}
