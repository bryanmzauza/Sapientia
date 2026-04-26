package dev.brmz.sapientia.core.logistics;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

/**
 * T-444 / T-446 / 1.8.1 — pure-arithmetic invariants for the
 * {@link MaxFlowItemSolver} (Edmonds-Karp). Pinned values double as the
 * regression suite for the "maxflow correctness vs reference" task.
 */
class MaxFlowItemSolverTest {

    /**
     * Canonical 4-vertex example from CLRS §26.2 — known max flow = 23.
     *
     * <pre>
     *      ┌──16──┐
     *  s ──┤      ├── t      (s=0, t=3)
     *      └──13──┘
     *  with intermediate edges 12 / 4 / 9 / 14 / 7
     * </pre>
     */
    @Test
    void solvesClassicFourNodeNetwork() {
        // 6-vertex CLRS network: s=0, v1=1, v2=2, v3=3, v4=4, t=5
        long[][] cap = new long[6][6];
        cap[0][1] = 16; cap[0][2] = 13;
        cap[1][2] = 10; cap[1][3] = 12;
        cap[2][1] =  4; cap[2][4] = 14;
        cap[3][2] =  9; cap[3][5] = 20;
        cap[4][3] =  7; cap[4][5] =  4;

        MaxFlowItemSolver solver = new MaxFlowItemSolver(cap);
        assertThat(solver.maxFlow(0, 5)).isEqualTo(23L);
    }

    @Test
    void selfLoopReturnsZero() {
        long[][] cap = new long[3][3];
        cap[0][1] = 100; cap[1][2] = 100;
        MaxFlowItemSolver solver = new MaxFlowItemSolver(cap);
        assertThat(solver.maxFlow(1, 1)).isEqualTo(0L);
    }

    @Test
    void disconnectedSinkReturnsZero() {
        long[][] cap = new long[4][4];
        cap[0][1] = 50; // 0 -> 1, but 2 and 3 are isolated
        MaxFlowItemSolver solver = new MaxFlowItemSolver(cap);
        assertThat(solver.maxFlow(0, 3)).isEqualTo(0L);
    }

    @Test
    void singleEdgeFlowIsCapacity() {
        long[][] cap = new long[2][2];
        cap[0][1] = 42;
        MaxFlowItemSolver solver = new MaxFlowItemSolver(cap);
        assertThat(solver.maxFlow(0, 1)).isEqualTo(42L);
    }

    @Test
    void parallelPathsSumCapacities() {
        // s -> a -> t (cap 5)   s -> b -> t (cap 7)
        long[][] cap = new long[4][4];
        int s = 0, a = 1, b = 2, t = 3;
        cap[s][a] = 5; cap[a][t] = 5;
        cap[s][b] = 7; cap[b][t] = 7;
        MaxFlowItemSolver solver = new MaxFlowItemSolver(cap);
        assertThat(solver.maxFlow(s, t)).isEqualTo(12L);
    }

    @Test
    void runIsDeterministicAcrossInvocations() {
        long[][] cap = new long[6][6];
        cap[0][1] = 16; cap[0][2] = 13;
        cap[1][2] = 10; cap[1][3] = 12;
        cap[2][1] =  4; cap[2][4] = 14;
        cap[3][2] =  9; cap[3][5] = 20;
        cap[4][3] =  7; cap[4][5] =  4;
        MaxFlowItemSolver solver = new MaxFlowItemSolver(cap);
        long first  = solver.maxFlow(0, 5);
        long second = solver.maxFlow(0, 5);
        assertThat(first).isEqualTo(second).isEqualTo(23L);
    }

    @Test
    void rejectsNegativeCapacities() {
        long[][] cap = new long[2][2];
        cap[0][1] = -1L;
        assertThatThrownBy(() -> new MaxFlowItemSolver(cap))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsNonSquareMatrix() {
        long[][] cap = new long[][] { {0L, 1L}, {0L, 0L, 0L} };
        assertThatThrownBy(() -> new MaxFlowItemSolver(cap))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
