package dev.brmz.sapientia.core.network;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Consumer;

import dev.brmz.sapientia.core.block.BlockKey;
import dev.brmz.sapientia.core.collect.IntList;
import dev.brmz.sapientia.core.collect.LongObjectMap;
import dev.brmz.sapientia.core.engine.ActivityFilter;
import dev.brmz.sapientia.core.engine.ActivityMap;
import dev.brmz.sapientia.core.engine.BlockPositions;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Adjacency graph shared by the energy, item and fluid networks. Nodes are
 * adjacent when they touch face to face in the same world; each connected set
 * of nodes is one {@link NodeGroup}.
 *
 * <p>Built for millions of nodes:
 * <ul>
 *   <li>Every node is an int id with its data in parallel primitive arrays
 *       (27 bytes per node). Transit nodes (cables, pipes, junctions) have no
 *       object at all; asking for one returns a detached view. Working nodes
 *       keep their object, which holds their state.</li>
 *   <li>Per chunk, a sorted array maps positions to ids, so lookups allocate
 *       nothing and unloading a chunk costs the nodes of that chunk only.</li>
 *   <li>Adding a node merges the smaller neighbouring networks into the
 *       largest. Removing nodes checks connectivity with a search that stops
 *       as soon as the cut ends meet again, and splits only when they do not.</li>
 *   <li>Solvers ask {@link #collectDue} for the networks that need work this
 *       cycle: those woken by a change and those whose idle back-off expired.
 *       A network with nothing to do costs nothing.</li>
 * </ul>
 *
 * <p>Topology changes happen on the main thread. {@link #collectDue},
 * {@link #worked} and {@link #idle} belong to the thread that runs the
 * solver; {@link NodeGroup#wake()} and {@link GraphNode#markDirty()} are safe
 * from any thread.
 */
public abstract class NodeGraph<N extends GraphNode, G extends NodeGroup<N>> implements ActivityMap.Listener {

    /** Longest idle back-off, in solver cycles. */
    public static final int MAX_DELAY = 63;
    private static final int WHEEL_SIZE = 64;
    private static final int NONE = -1;
    private static final int FREE_SLOT = -2;

    private final int roleCount;
    private final boolean snapshots;

    private final Map<String, Integer> worldIds = new HashMap<>();
    private final List<String> worldNames = new ArrayList<>();
    private final List<LongObjectMap<ChunkNodes>> chunksByWorld = new ArrayList<>();

    // Per node id. Free ids are chained through group[] and marked by memberSlot == FREE_SLOT.
    private long[] pos = new long[0];
    private short[] world = new short[0];
    private int[] group = new int[0];
    private int[] memberSlot = new int[0];
    private int[] visit = new int[0];
    private byte[] kind = new byte[0];
    private Object[] objects = new Object[0];
    private int capacity;
    private int freeHead = NONE;
    private int nodeCount;

    private Object[] groupTable = new Object[16];
    private int groupTableSize;
    private final IntList freeGids = new IntList();
    private final ArrayList<G> groups = new ArrayList<>();

    private final ArrayList<G> staleSnapshots = new ArrayList<>();
    private final ArrayList<G>[] wheel;
    private final int[] scratch = new int[6];
    private ActivityFilter activity = ActivityFilter.ALWAYS;
    private int visitEpoch;

    final ConcurrentLinkedQueue<GraphNode> dirtyQueue = new ConcurrentLinkedQueue<>();
    final ConcurrentLinkedQueue<NodeGroup<?>> wakeQueue = new ConcurrentLinkedQueue<>();

    /**
     * @param roleCount number of working roles (see {@link #roleOf})
     * @param snapshots keep per-network role snapshots for a solver on another thread
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    protected NodeGraph(int roleCount, boolean snapshots) {
        this.roleCount = roleCount;
        this.snapshots = snapshots;
        this.wheel = new ArrayList[WHEEL_SIZE];
        for (int i = 0; i < WHEEL_SIZE; i++) {
            wheel[i] = new ArrayList<>();
        }
    }

    /** Creates an empty network. */
    protected abstract @NotNull G newGroup();

    /** Working role of a node, or {@code -1} for transit nodes (cables, pipes, junctions). */
    protected abstract int roleOf(@NotNull N node);

    /** Compact code of a transit node's type and tier, stored in one byte. */
    protected abstract byte transitKind(@NotNull N node);

    /** Rebuilds a detached view of a transit node from its position and code. */
    protected abstract @NotNull N transitView(@NotNull BlockKey key, byte kind);

    /** Copies per-network settings when {@code from} splits and {@code into} is one of the parts. */
    protected void inherit(@NotNull G from, @NotNull G into) {}

    // --- Topology ------------------------------------------------------------------------

    /** Adds a node, merging the networks it touches. A node already at that position wins. */
    public void addNode(@NotNull N node) {
        BlockKey key = node.location();
        int w = worldId(key.world());
        LongObjectMap<ChunkNodes> chunks = chunksByWorld.get(w);
        long chunkKey = BlockPositions.chunk(key.chunkX(), key.chunkZ());
        ChunkNodes chunk = chunks.get(chunkKey);
        int local = local(key.x(), key.y(), key.z());
        if (chunk != null && chunk.get(local) != NONE) {
            return;
        }
        if (chunk == null) {
            chunk = new ChunkNodes();
            chunks.put(chunkKey, chunk);
        }
        int id = allocate();
        pos[id] = node.packed;
        world[id] = (short) w;
        group[id] = NONE;
        memberSlot[id] = NONE;
        visit[id] = 0;
        chunk.put(local, id);
        nodeCount++;
        if (roleOf(node) >= 0) {
            objects[id] = node;
            node.id = id;
            node.owner = this;
            node.setActive(activity.isActive(key.world(), key.chunkX(), key.chunkZ()));
            if (node.takeDirty()) {
                node.markDirty(); // changed before it joined: queue it now
            }
        } else {
            objects[id] = null;
            kind[id] = transitKind(node);
        }

        int count = neighbours(id);
        G host = null;
        for (int i = 0; i < count; i++) {
            G g = groupById(group[scratch[i]]);
            if (g != null && (host == null || g.memberIds.size() > host.memberIds.size())) {
                host = g;
            }
        }
        if (host == null) {
            host = createGroup();
        } else {
            for (int i = 0; i < count; i++) {
                G g = groupById(group[scratch[i]]);
                if (g != null && g != host) {
                    absorb(host, g);
                }
            }
        }
        attach(host, id);
        host.wake();
    }

    /** Removes the node at {@code key}, splitting its network if it was a bridge. */
    public void removeNode(@NotNull BlockKey key) {
        int id = idAt(key.world(), key.x(), key.y(), key.z());
        if (id == NONE) return;
        unindex(id);
        IntList ids = new IntList();
        ids.add(id);
        removeIds(ids);
    }

    /**
     * Removes every node in a chunk (chunk unload). Returns the working nodes
     * removed, so their unsaved state can be written.
     */
    public @NotNull List<N> removeChunk(@NotNull String worldName, int chunkX, int chunkZ) {
        Integer w = worldIds.get(worldName);
        ChunkNodes chunk = w == null ? null : chunksByWorld.get(w).remove(BlockPositions.chunk(chunkX, chunkZ));
        if (chunk == null) {
            return new ArrayList<>();
        }
        return removeIds(chunk.ids());
    }

    /** Detaches already unindexed nodes, then re-checks the networks they were part of. */
    @SuppressWarnings("unchecked")
    private List<N> removeIds(IntList ids) {
        Set<G> affected = new LinkedHashSet<>();
        for (int i = 0; i < ids.size(); i++) {
            int id = ids.get(i);
            G g = groupById(group[id]);
            if (g != null) {
                detach(g, id);
                affected.add(g);
            }
        }
        // Surviving neighbours of the removed nodes are where a network may have been cut.
        Map<G, IntList> cutEnds = new HashMap<>();
        int epoch = ++visitEpoch;
        for (int i = 0; i < ids.size(); i++) {
            int count = neighbours(ids.get(i));
            for (int j = 0; j < count; j++) {
                int n = scratch[j];
                G g = groupById(group[n]);
                if (g != null && visit[n] != epoch) {
                    visit[n] = epoch;
                    cutEnds.computeIfAbsent(g, k -> new IntList()).add(n);
                }
            }
        }
        List<N> removedObjects = new ArrayList<>();
        for (int i = 0; i < ids.size(); i++) {
            int id = ids.get(i);
            Object o = objects[id];
            if (o != null) {
                N node = (N) o;
                node.id = -1;
                node.owner = null;
                removedObjects.add(node);
                objects[id] = null;
            }
            free(id);
            nodeCount--;
        }
        for (G g : affected) {
            if (g.memberIds.isEmpty()) {
                removeGroup(g);
                continue;
            }
            IntList ends = cutEnds.get(g);
            if (ends != null && ends.size() > 1) {
                split(g, ends);
            }
            g.wake();
        }
        return removedObjects;
    }

    /**
     * Checks whether the cut ends of {@code g} are still connected. A search
     * from one end stops as soon as it has reached all the others; if it runs
     * out first, the part it covered becomes a network of its own and the
     * remaining ends are checked the same way.
     */
    private void split(G g, IntList ends) {
        IntList pending = new IntList();
        for (int i = 0; i < ends.size(); i++) pending.add(ends.get(i));
        IntList component = new IntList(); // also the search queue, read from `head`
        while (pending.size() > 1) {
            int target = ++visitEpoch;
            for (int i = 0; i < pending.size(); i++) {
                visit[pending.get(i)] = target;
            }
            int seen = ++visitEpoch;
            int start = pending.get(0);
            int remaining = pending.size() - 1;
            component.clear();
            visit[start] = seen;
            component.add(start);
            int head = 0;
            while (head < component.size() && remaining > 0) {
                int current = component.get(head++);
                int count = neighbours(current);
                for (int i = 0; i < count; i++) {
                    int n = scratch[i];
                    if (group[n] != g.gid || visit[n] == seen) continue;
                    if (visit[n] == target) remaining--;
                    visit[n] = seen;
                    component.add(n);
                }
            }
            if (remaining == 0) {
                return; // every cut end is still reachable
            }
            G part = createGroup();
            inherit(g, part);
            for (int i = 0; i < component.size(); i++) {
                int id = component.get(i);
                detach(g, id);
                attach(part, id);
            }
            part.wake();
            IntList rest = new IntList();
            for (int i = 0; i < pending.size(); i++) {
                int id = pending.get(i);
                if (group[id] == g.gid) rest.add(id);
            }
            pending = rest;
        }
    }

    // --- Lookups -------------------------------------------------------------------------

    /** The node at {@code key}: the object for working nodes, a detached view for transit nodes. */
    public @Nullable N nodeAt(@NotNull BlockKey key) {
        return nodeAt(key.world(), key.x(), key.y(), key.z());
    }

    public @Nullable N nodeAt(@NotNull String worldName, int x, int y, int z) {
        int id = idAt(worldName, x, y, z);
        return id == NONE ? null : node(id);
    }

    public boolean contains(@NotNull BlockKey key) {
        return idAt(key.world(), key.x(), key.y(), key.z()) != NONE;
    }

    /** Every node, transit nodes as views. Builds a new list: for commands and tests. */
    public @NotNull List<N> nodes() {
        List<N> out = new ArrayList<>(nodeCount);
        for (int id = 0; id < capacity; id++) {
            if (memberSlot[id] != FREE_SLOT) out.add(node(id));
        }
        return out;
    }

    /** The nodes of one chunk, transit nodes as views. */
    public @NotNull List<N> nodesInChunk(@NotNull String worldName, int chunkX, int chunkZ) {
        Integer w = worldIds.get(worldName);
        ChunkNodes chunk = w == null ? null : chunksByWorld.get(w).get(BlockPositions.chunk(chunkX, chunkZ));
        return chunk == null ? new ArrayList<>() : materialize(chunk.ids());
    }

    public @NotNull List<G> groups() {
        return Collections.unmodifiableList(groups);
    }

    /** Network of a node (object or view) of this graph, or {@code null}. */
    @SuppressWarnings("unchecked")
    public @Nullable G groupOf(@NotNull GraphNode node) {
        if (node.owner == this) {
            return (G) node.group;
        }
        BlockKey key = node.location();
        int id = idAt(key.world(), key.x(), key.y(), key.z());
        return id == NONE ? null : groupById(group[id]);
    }

    public int networkCount() {
        return groups.size();
    }

    public int nodeCount() {
        return nodeCount;
    }

    List<N> materialize(IntList ids) {
        List<N> out = new ArrayList<>(ids.size());
        for (int i = 0; i < ids.size(); i++) {
            out.add(node(ids.get(i)));
        }
        return out;
    }

    // --- Activity ------------------------------------------------------------------------

    /** Sets the filter that decides whether a newly added node starts active. */
    @SuppressWarnings("unchecked")
    public void setActivityFilter(@NotNull ActivityFilter activity) {
        this.activity = activity;
        for (int id = 0; id < capacity; id++) {
            if (memberSlot[id] != FREE_SLOT && objects[id] != null) {
                N node = (N) objects[id];
                BlockKey key = node.location();
                node.setActive(activity.isActive(key.world(), key.chunkX(), key.chunkZ()));
            }
        }
    }

    /** Marks a chunk's working nodes active or inactive; activation wakes their networks. */
    public void setChunkActive(@NotNull String worldName, int chunkX, int chunkZ, boolean active) {
        Integer w = worldIds.get(worldName);
        ChunkNodes chunk = w == null ? null : chunksByWorld.get(w).get(BlockPositions.chunk(chunkX, chunkZ));
        if (chunk == null) return;
        for (int i = 0; i < chunk.size; i++) {
            Object o = objects[chunk.ids[i]];
            if (o instanceof GraphNode node) {
                node.setActive(active);
                NodeGroup<?> g = node.group;
                if (active && g != null) g.wake();
            }
        }
    }

    @Override
    public void onChunkActivated(@NotNull String world, int chunkX, int chunkZ) {
        setChunkActive(world, chunkX, chunkZ, true);
    }

    @Override
    public void onChunkDeactivated(@NotNull String world, int chunkX, int chunkZ) {
        setChunkActive(world, chunkX, chunkZ, false);
    }

    // --- Pacing (solver thread) ----------------------------------------------------------

    /** Networks to run this cycle: the ones woken since the last cycle and the ones whose back-off expired. */
    @SuppressWarnings("unchecked")
    public @NotNull List<G> collectDue(long cycle) {
        ArrayList<G> due = new ArrayList<>();
        NodeGroup<?> woken;
        while ((woken = wakeQueue.poll()) != null) {
            woken.wakeQueued = false;
            if (woken.removed || woken.dueCycle == cycle) continue;
            woken.dueCycle = cycle;
            woken.idleCycles = 0;
            due.add((G) woken);
        }
        ArrayList<G> bucket = wheel[(int) (cycle & (WHEEL_SIZE - 1))];
        for (G g : bucket) {
            if (!g.removed && g.scheduledCycle == cycle && g.dueCycle != cycle) {
                g.dueCycle = cycle;
                due.add(g);
            }
        }
        bucket.clear();
        return due;
    }

    /** The network did work: run it again next cycle. */
    public void worked(@NotNull G g, long cycle) {
        g.idleCycles = 0;
        schedule(g, cycle + 1);
    }

    /**
     * The network had nothing to do: back off exponentially up to {@code maxDelay}
     * cycles, or sleep until woken when {@code maxDelay} is 0.
     */
    public void idle(@NotNull G g, long cycle, int maxDelay) {
        if (maxDelay <= 0) {
            g.scheduledCycle = -1;
            return;
        }
        int delay = Math.min(Math.min(maxDelay, MAX_DELAY), 1 << Math.min(g.idleCycles, 6));
        g.idleCycles++;
        schedule(g, cycle + delay);
    }

    /** Runs the network again at {@code cycle + delay} without touching its back-off. */
    public void retry(@NotNull G g, long cycle, int delay) {
        schedule(g, cycle + Math.max(1, Math.min(delay, MAX_DELAY)));
    }

    private void schedule(G g, long at) {
        g.scheduledCycle = at;
        wheel[(int) (at & (WHEEL_SIZE - 1))].add(g);
    }

    // --- Persistence and snapshots (main thread) -----------------------------------------

    /** Hands every node changed since the last call (and still in the graph) to {@code sink}. */
    @SuppressWarnings("unchecked")
    public void drainDirty(@NotNull Consumer<? super N> sink) {
        GraphNode node;
        while ((node = dirtyQueue.poll()) != null) {
            if (node.owner == this && node.takeDirty()) {
                sink.accept((N) node);
            }
        }
    }

    /** Publishes the role lists of networks that changed, for a solver on another thread. */
    public void refreshSnapshots() {
        for (G g : staleSnapshots) {
            g.snapshotStale = false;
            if (g.removed) continue;
            Object[][] snap = new Object[roleCount][];
            for (int r = 0; r < roleCount; r++) {
                snap[r] = g.roles[r].toArray();
            }
            g.snapshot = snap;
        }
        staleSnapshots.clear();
    }

    // --- Internals: groups -------------------------------------------------------------

    @SuppressWarnings("unchecked")
    private @Nullable G groupById(int gid) {
        return gid < 0 ? null : (G) groupTable[gid];
    }

    private G createGroup() {
        G g = newGroup();
        g.graph = this;
        int gid;
        if (!freeGids.isEmpty()) {
            gid = freeGids.removeLast();
        } else {
            if (groupTableSize == groupTable.length) {
                groupTable = Arrays.copyOf(groupTable, groupTableSize + (groupTableSize >> 1) + 16);
            }
            gid = groupTableSize++;
        }
        groupTable[gid] = g;
        g.gid = gid;
        g.groupSlot = groups.size();
        groups.add(g);
        markStale(g);
        return g;
    }

    private void removeGroup(G g) {
        g.removed = true;
        groupTable[g.gid] = null;
        freeGids.add(g.gid);
        int slot = g.groupSlot;
        G last = groups.remove(groups.size() - 1);
        if (last != g) {
            groups.set(slot, last);
            last.groupSlot = slot;
        }
        g.groupSlot = -1;
    }

    private void absorb(G host, G other) {
        for (int i = 0; i < other.memberIds.size(); i++) {
            attach(host, other.memberIds.get(i));
        }
        other.memberIds.clear();
        for (ArrayList<N> role : other.roles) {
            role.clear();
        }
        removeGroup(other);
    }

    @SuppressWarnings("unchecked")
    private void attach(G g, int id) {
        group[id] = g.gid;
        memberSlot[id] = g.memberIds.size();
        g.memberIds.add(id);
        Object o = objects[id];
        if (o != null) {
            N node = (N) o;
            ArrayList<N> list = g.roles[roleOf(node)];
            node.roleSlot = list.size();
            list.add(node);
            node.group = g;
        }
        g.version++;
        markStale(g);
    }

    @SuppressWarnings("unchecked")
    private void detach(G g, int id) {
        int slot = memberSlot[id];
        int last = g.memberIds.removeLast();
        if (last != id) {
            g.memberIds.set(slot, last);
            memberSlot[last] = slot;
        }
        memberSlot[id] = NONE;
        Object o = objects[id];
        if (o != null) {
            N node = (N) o;
            ArrayList<N> list = g.roles[roleOf(node)];
            int roleSlot = node.roleSlot;
            N lastInRole = list.remove(list.size() - 1);
            if (lastInRole != node) {
                list.set(roleSlot, lastInRole);
                lastInRole.roleSlot = roleSlot;
            }
            node.roleSlot = -1;
            node.group = null;
        }
        group[id] = NONE;
        g.version++;
        markStale(g);
    }

    private void markStale(G g) {
        if (snapshots && !g.snapshotStale) {
            g.snapshotStale = true;
            staleSnapshots.add(g);
        }
    }

    // --- Internals: nodes and index ----------------------------------------------------

    @SuppressWarnings("unchecked")
    private N node(int id) {
        Object o = objects[id];
        if (o != null) return (N) o;
        long p = pos[id];
        return transitView(new BlockKey(worldNames.get(world[id]),
                BlockPositions.x(p), BlockPositions.y(p), BlockPositions.z(p)), kind[id]);
    }

    private int worldId(String name) {
        Integer id = worldIds.get(name);
        if (id == null) {
            if (worldNames.size() > Short.MAX_VALUE) {
                throw new IllegalStateException("Too many worlds");
            }
            id = worldNames.size();
            worldNames.add(name);
            chunksByWorld.add(new LongObjectMap<>());
            worldIds.put(name, id);
        }
        return id;
    }

    private int idAt(String worldName, int x, int y, int z) {
        Integer w = worldIds.get(worldName);
        return w == null ? NONE : idAt(w, x, y, z);
    }

    private int idAt(int w, int x, int y, int z) {
        ChunkNodes chunk = chunksByWorld.get(w).get(BlockPositions.chunk(x >> 4, z >> 4));
        return chunk == null ? NONE : chunk.get(local(x, y, z));
    }

    private void unindex(int id) {
        long p = pos[id];
        int x = BlockPositions.x(p);
        int z = BlockPositions.z(p);
        LongObjectMap<ChunkNodes> chunks = chunksByWorld.get(world[id]);
        long chunkKey = BlockPositions.chunk(x >> 4, z >> 4);
        ChunkNodes chunk = chunks.get(chunkKey);
        chunk.remove(local(x, BlockPositions.y(p), z));
        if (chunk.size == 0) {
            chunks.remove(chunkKey);
        }
    }

    /** Fills {@link #scratch} with the ids of the face neighbours of {@code id}; returns how many. */
    private int neighbours(int id) {
        long p = pos[id];
        int w = world[id];
        int x = BlockPositions.x(p);
        int y = BlockPositions.y(p);
        int z = BlockPositions.z(p);
        int count = 0;
        count = probe(w, x + 1, y, z, count);
        count = probe(w, x - 1, y, z, count);
        count = probe(w, x, y + 1, z, count);
        count = probe(w, x, y - 1, z, count);
        count = probe(w, x, y, z + 1, count);
        count = probe(w, x, y, z - 1, count);
        return count;
    }

    private int probe(int w, int x, int y, int z, int count) {
        int n = idAt(w, x, y, z);
        if (n != NONE) {
            scratch[count++] = n;
        }
        return count;
    }

    private int allocate() {
        if (freeHead == NONE) {
            grow(Math.max(64, capacity + (capacity >> 1)));
        }
        int id = freeHead;
        freeHead = group[id];
        return id;
    }

    private void free(int id) {
        memberSlot[id] = FREE_SLOT;
        group[id] = freeHead;
        freeHead = id;
    }

    private void grow(int newCapacity) {
        int old = capacity;
        pos = Arrays.copyOf(pos, newCapacity);
        world = Arrays.copyOf(world, newCapacity);
        group = Arrays.copyOf(group, newCapacity);
        memberSlot = Arrays.copyOf(memberSlot, newCapacity);
        visit = Arrays.copyOf(visit, newCapacity);
        kind = Arrays.copyOf(kind, newCapacity);
        objects = Arrays.copyOf(objects, newCapacity);
        for (int id = newCapacity - 1; id >= old; id--) {
            memberSlot[id] = FREE_SLOT;
            group[id] = freeHead;
            freeHead = id;
        }
        capacity = newCapacity;
    }

    /** Position inside the chunk: 4 bits of x, 4 of z, 12 of y (−2048..2047). */
    private static int local(int x, int y, int z) {
        return ((y + 2048) & 0xFFF) << 8 | (x & 15) << 4 | (z & 15);
    }

    /** One chunk's nodes: positions sorted for binary search, with their ids alongside. */
    private static final class ChunkNodes {
        int[] locals = new int[4];
        int[] ids = new int[4];
        int size;

        int get(int local) {
            int i = find(local);
            return i < 0 ? NONE : ids[i];
        }

        void put(int local, int id) {
            int i = -(find(local) + 1);
            if (size == locals.length) {
                int grown = size + (size >> 1) + 4;
                locals = Arrays.copyOf(locals, grown);
                ids = Arrays.copyOf(ids, grown);
            }
            System.arraycopy(locals, i, locals, i + 1, size - i);
            System.arraycopy(ids, i, ids, i + 1, size - i);
            locals[i] = local;
            ids[i] = id;
            size++;
        }

        void remove(int local) {
            int i = find(local);
            if (i < 0) return;
            System.arraycopy(locals, i + 1, locals, i, size - i - 1);
            System.arraycopy(ids, i + 1, ids, i, size - i - 1);
            size--;
        }

        IntList ids() {
            IntList out = new IntList();
            for (int i = 0; i < size; i++) out.add(ids[i]);
            return out;
        }

        private int find(int local) {
            int lo = 0;
            int hi = size - 1;
            while (lo <= hi) {
                int mid = (lo + hi) >>> 1;
                int key = locals[mid];
                if (key < local) {
                    lo = mid + 1;
                } else if (key > local) {
                    hi = mid - 1;
                } else {
                    return mid;
                }
            }
            return -(lo + 1);
        }
    }
}
