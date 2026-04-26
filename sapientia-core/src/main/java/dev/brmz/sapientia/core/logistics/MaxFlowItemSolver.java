package dev.brmz.sapientia.core.logistics;

import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Deque;

/**
 * Pure-data Edmonds-Karp implementation of the Ford-Fulkerson method
 * (T-444 / 1.8.1). Operates on an integer adjacency matrix with capacities
 * and returns the maximum flow from {@code source} to {@code sink}.
 *
 * <p>Used by {@link ItemSolver} when the operator opts in to
 * {@code network.solver: maxflow} via {@link LogisticsConfig} for HV+ item
 * networks. The legacy greedy solver (1.1.0) remains the default. The full
 * adapter that turns an {@link ItemNetworkGraph} into an instance of this
 * class lives behind the same flag and is tested through
 * {@code MaxFlowItemSolverTest}; the per-tick wiring on live worlds lands
 * with the routing improvements (T-445) — see ADR-020.
 *
 * <p>Algorithmic notes:
 * <ul>
 *   <li>Time complexity: {@code O(V·E²)} (Edmonds-Karp guarantee).</li>
 *   <li>For the targeted 1000-node HV item network (P-019) this is well
 *       inside the 2 ms / cycle budget — see the benchmark in
 *       {@code sapientia-benchmarks}.</li>
 *   <li>Capacities are {@code long} to match {@code ItemSpecs} throughputs;
 *       the BFS layer stays {@code int}-indexed.</li>
 * </ul>
 */
public final class MaxFlowItemSolver {

    private final int n;
    private final long[][] capacity;
    private final long[][] residual;

    /**
     * Build a max-flow solver from an {@code n×n} capacity matrix.
     *
     * <p>{@code capacity[u][v]} is the maximum flow allowed from {@code u}
     * to {@code v} in items per cycle. Self-loops are ignored. The matrix
     * is copied defensively.
     */
    public MaxFlowItemSolver(long[][] capacity) {
        if (capacity == null) {
            throw new IllegalArgumentException("capacity matrix must not be null");
        }
        this.n = capacity.length;
        for (long[] row : capacity) {
            if (row.length != n) {
                throw new IllegalArgumentException("capacity matrix must be square");
            }
        }
        this.capacity = new long[n][n];
        this.residual = new long[n][n];
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                long c = capacity[i][j];
                if (c < 0) {
                    throw new IllegalArgumentException("capacity must be non-negative");
                }
                this.capacity[i][j] = c;
                this.residual[i][j] = c;
            }
        }
    }

    /** Number of vertices. */
    public int size() {
        return n;
    }

    /**
     * Compute the maximum flow from {@code source} to {@code sink}. Resets
     * the internal residual graph on every call so the result is
     * deterministic regardless of previous invocations.
     */
    public long maxFlow(int source, int sink) {
        if (source < 0 || source >= n || sink < 0 || sink >= n) {
            throw new IllegalArgumentException("source/sink out of bounds");
        }
        if (source == sink) {
            return 0L;
        }
        // Reset residual graph (fresh run each call).
        for (int i = 0; i < n; i++) {
            System.arraycopy(capacity[i], 0, residual[i], 0, n);
        }
        long total = 0L;
        int[] parent = new int[n];
        while (true) {
            long bottleneck = bfsAugment(source, sink, parent);
            if (bottleneck <= 0L) break;
            total += bottleneck;
            int v = sink;
            while (v != source) {
                int u = parent[v];
                residual[u][v] -= bottleneck;
                residual[v][u] += bottleneck;
                v = u;
            }
        }
        return total;
    }

    private long bfsAugment(int source, int sink, int[] parent) {
        Arrays.fill(parent, -1);
        parent[source] = source;
        long[] minCap = new long[n];
        minCap[source] = Long.MAX_VALUE;
        Deque<Integer> queue = new ArrayDeque<>();
        queue.add(source);
        while (!queue.isEmpty()) {
            int u = queue.poll();
            for (int v = 0; v < n; v++) {
                if (parent[v] == -1 && residual[u][v] > 0L) {
                    parent[v] = u;
                    minCap[v] = Math.min(minCap[u], residual[u][v]);
                    if (v == sink) {
                        return minCap[v];
                    }
                    queue.add(v);
                }
            }
        }
        return 0L;
    }
}
