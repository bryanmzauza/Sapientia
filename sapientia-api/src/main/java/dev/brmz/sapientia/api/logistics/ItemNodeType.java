package dev.brmz.sapientia.api.logistics;

/** Role an item logistics node plays within its network (T-300 / 1.1.0). */
public enum ItemNodeType {
    /** Pulls items from an adjacent vanilla container into the network. */
    PRODUCER,
    /** Pushes items from the network into an adjacent vanilla container. */
    CONSUMER,
    /** Transit node that filters which items may pass. */
    FILTER,
    /** Passive transit, no filtering, no buffering. */
    CABLE,
    /** Reserved for future routing junctions (1.2.0+); currently behaves like a cable. */
    JUNCTION
}
