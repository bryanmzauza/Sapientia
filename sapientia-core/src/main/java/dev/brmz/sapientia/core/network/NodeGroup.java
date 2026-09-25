package dev.brmz.sapientia.core.network;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import dev.brmz.sapientia.core.collect.IntList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * One connected network of a {@link NodeGraph}. Members are kept as node ids
 * (constant-time removal), and working nodes are also kept per role (for
 * energy: generators, capacitors, consumers), so solvers never walk cables or
 * pipes.
 *
 * <p>Solvers run a network only when it is due: after a change woke it up, or
 * when its idle back-off expires (see {@link NodeGraph#collectDue}).
 */
public abstract class NodeGroup<N extends GraphNode> {

    private final UUID networkId = UUID.randomUUID();
    final IntList memberIds = new IntList();
    final ArrayList<N>[] roles;

    @Nullable NodeGraph<N, ?> graph;
    int gid = -1;
    int groupSlot = -1;
    int version;

    // Pacing: written by the solving thread, except the two volatile flags.
    volatile boolean removed;
    volatile boolean wakeQueued;
    long dueCycle = -1;
    long scheduledCycle = -1;
    int idleCycles;

    // Role lists as seen by a solver on another thread (see NodeGraph#refreshSnapshots).
    volatile Object @Nullable [][] snapshot;
    boolean snapshotStale;

    @SuppressWarnings({"unchecked", "rawtypes"})
    protected NodeGroup(int roleCount) {
        this.roles = new ArrayList[roleCount];
        for (int i = 0; i < roleCount; i++) {
            roles[i] = new ArrayList<>();
        }
    }

    public final UUID networkId() {
        return networkId;
    }

    public final int size() {
        return memberIds.size();
    }

    /**
     * Every member, transit nodes included (as detached views); main thread
     * only. Builds a new list, so it is meant for commands and addons, not
     * for per-tick work.
     */
    public final @NotNull List<N> members() {
        NodeGraph<N, ?> g = graph;
        return g == null || removed ? List.of() : g.materialize(memberIds);
    }

    /** Live members with the given role; main thread only. */
    public final @NotNull List<N> role(int role) {
        return Collections.unmodifiableList(roles[role]);
    }

    /**
     * Members with the given role as of the last {@link NodeGraph#refreshSnapshots},
     * readable from any thread; {@code null} before the first refresh.
     */
    @SuppressWarnings("unchecked")
    public final @Nullable List<N> roleSnapshot(int role) {
        Object[][] snap = snapshot;
        return snap == null ? null : (List<N>) (List<?>) Arrays.asList(snap[role]);
    }

    /** Increments on every membership change; lets solvers cache derived data. */
    public final int version() {
        return version;
    }

    /** Whether this network was merged into another, split, or emptied. */
    public final boolean isRemoved() {
        return removed;
    }

    /** Schedules this network for the next solver cycle. Safe from any thread. */
    public final void wake() {
        NodeGraph<N, ?> g = graph;
        if (g == null || removed || wakeQueued) return;
        wakeQueued = true;
        g.wakeQueue.add(this);
    }
}
