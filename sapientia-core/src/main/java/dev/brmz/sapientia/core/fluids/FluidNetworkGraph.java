package dev.brmz.sapientia.core.fluids;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.UUID;

import dev.brmz.sapientia.api.energy.EnergyTier;
import dev.brmz.sapientia.api.fluids.FluidNetwork;
import dev.brmz.sapientia.api.fluids.FluidNode;
import dev.brmz.sapientia.api.fluids.FluidNodeType;
import dev.brmz.sapientia.core.block.BlockKey;
import dev.brmz.sapientia.core.network.NodeGraph;
import dev.brmz.sapientia.core.network.NodeGroup;
import org.jetbrains.annotations.NotNull;

/**
 * Fluid networks: {@link SimpleFluidNode}s grouped into connected
 * {@link FluidNetwork}s. Tanks, pumps and drains are objects tracked per
 * network, so the solver never walks pipes or junctions; those are primitive
 * entries.
 */
public final class FluidNetworkGraph extends NodeGraph<SimpleFluidNode, FluidNetworkGraph.Network> {

    static final int TANKS = 0;
    static final int PUMPS = 1;
    static final int DRAINS = 2;
    private static final FluidNodeType[] TYPES = FluidNodeType.values();
    private static final EnergyTier[] TIERS = EnergyTier.values();

    public FluidNetworkGraph() {
        super(3, false);
    }

    @Override
    protected @NotNull Network newGroup() {
        return new Network();
    }

    @Override
    protected byte transitKind(@NotNull SimpleFluidNode node) {
        return (byte) (node.type().ordinal() << 3 | node.tier().ordinal());
    }

    @Override
    protected @NotNull SimpleFluidNode transitView(@NotNull BlockKey key, byte kind) {
        return new SimpleFluidNode(transitId(key), key, TYPES[kind >> 3], TIERS[kind & 7], null, 0L);
    }

    /** Stable id of the pipe or junction at {@code key}; they are not stored as objects. */
    public static @NotNull UUID transitId(@NotNull BlockKey key) {
        return UUID.nameUUIDFromBytes(("sapientia:fluids:" + key.world() + ":" + key.x() + ":" + key.y() + ":" + key.z())
                .getBytes(StandardCharsets.UTF_8));
    }

    @Override
    protected int roleOf(@NotNull SimpleFluidNode node) {
        return switch (node.type()) {
            case TANK -> TANKS;
            case PUMP -> PUMPS;
            case DRAIN -> DRAINS;
            default -> -1; // pipes and junctions only carry fluid
        };
    }

    public @NotNull Collection<FluidNetwork> networks() {
        return Collections.unmodifiableCollection(groups());
    }

    public FluidNetwork networkOf(@NotNull FluidNode node) {
        return node instanceof SimpleFluidNode simple ? groupOf(simple) : null;
    }

    public @NotNull Collection<SimpleFluidNode> membersOf(@NotNull FluidNetwork network) {
        return network instanceof Network n && !n.isRemoved() ? n.members() : Collections.emptyList();
    }

    /** One fluid network. The public surface goes through {@link FluidNetwork}. */
    public static final class Network extends NodeGroup<SimpleFluidNode> implements FluidNetwork {

        Network() {
            super(3);
        }

        @Override
        public Collection<FluidNode> nodes() {
            return Collections.unmodifiableCollection(new ArrayList<>(members()));
        }
    }
}
