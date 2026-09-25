package dev.brmz.sapientia.core.energy;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.UUID;

import dev.brmz.sapientia.api.energy.EnergyNetwork;
import dev.brmz.sapientia.api.energy.EnergyNode;
import dev.brmz.sapientia.api.energy.EnergyNodeType;
import dev.brmz.sapientia.api.energy.EnergySpecs;
import dev.brmz.sapientia.api.energy.EnergyTier;
import dev.brmz.sapientia.core.block.BlockKey;
import dev.brmz.sapientia.core.network.NodeGraph;
import dev.brmz.sapientia.core.network.NodeGroup;
import org.jetbrains.annotations.NotNull;

/**
 * Energy networks: {@link SimpleEnergyNode}s grouped into connected
 * {@link EnergyNetwork}s. Generators, capacitors and consumers are objects
 * tracked per network, so the solver never walks cables; cables are primitive
 * entries. Keeps role snapshots because the energy solver runs on its own
 * thread (see {@link EnergySolver}).
 */
public final class NetworkGraph extends NodeGraph<SimpleEnergyNode, NetworkGraph.Network> {

    static final int GENERATORS = 0;
    static final int CAPACITORS = 1;
    static final int CONSUMERS = 2;
    private static final EnergyTier[] TIERS = EnergyTier.values();

    public NetworkGraph() {
        super(3, true);
    }

    @Override
    protected @NotNull Network newGroup() {
        return new Network();
    }

    @Override
    protected byte transitKind(@NotNull SimpleEnergyNode node) {
        return (byte) node.tier().ordinal();
    }

    @Override
    protected @NotNull SimpleEnergyNode transitView(@NotNull BlockKey key, byte kind) {
        EnergyTier tier = TIERS[kind];
        return new SimpleEnergyNode(transitId(key), key, EnergyNodeType.CABLE, tier, 0L,
                EnergySpecs.bufferMax(EnergyNodeType.CABLE, tier));
    }

    /** Stable id of the cable at {@code key}; cables are not stored as objects, so their id is derived. */
    public static @NotNull UUID transitId(@NotNull BlockKey key) {
        return UUID.nameUUIDFromBytes(("sapientia:energy:" + key.world() + ":" + key.x() + ":" + key.y() + ":" + key.z())
                .getBytes(StandardCharsets.UTF_8));
    }

    @Override
    protected int roleOf(@NotNull SimpleEnergyNode node) {
        return switch (node.type()) {
            case GENERATOR -> GENERATORS;
            case CAPACITOR -> CAPACITORS;
            case CONSUMER -> CONSUMERS;
            case CABLE -> -1;
        };
    }

    public @NotNull Collection<EnergyNetwork> networks() {
        return Collections.unmodifiableCollection(groups());
    }

    public EnergyNetwork networkOf(@NotNull EnergyNode node) {
        return node instanceof SimpleEnergyNode simple ? groupOf(simple) : null;
    }

    /** Members of a network of this graph. */
    public @NotNull Collection<SimpleEnergyNode> membersOf(@NotNull EnergyNetwork network) {
        return network instanceof Network n && !n.isRemoved() ? n.members() : Collections.emptyList();
    }

    /** One energy network. The public surface goes through {@link EnergyNetwork}. */
    public static final class Network extends NodeGroup<SimpleEnergyNode> implements EnergyNetwork {

        Network() {
            super(3);
        }

        @Override
        public Collection<EnergyNode> nodes() {
            return Collections.unmodifiableCollection(new ArrayList<>(members()));
        }

        @Override
        public long totalStored() {
            long total = 0;
            for (SimpleEnergyNode m : members()) total += m.bufferCurrent();
            return total;
        }

        @Override
        public long totalCapacity() {
            long total = 0;
            for (SimpleEnergyNode m : members()) total += m.bufferMax();
            return total;
        }
    }
}
