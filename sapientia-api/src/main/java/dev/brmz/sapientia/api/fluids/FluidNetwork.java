package dev.brmz.sapientia.api.fluids;

import java.util.Collection;
import java.util.UUID;

import org.jetbrains.annotations.NotNull;

/**
 * Read-only view of a fluid network (T-301 / 1.2.0). Networks are immutable
 * snapshots in the API; mutation happens through {@link FluidService}.
 */
public interface FluidNetwork {

    @NotNull UUID networkId();

    @NotNull Collection<FluidNode> nodes();

    int size();
}
