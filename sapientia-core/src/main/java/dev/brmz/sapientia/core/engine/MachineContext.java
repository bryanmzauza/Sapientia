package dev.brmz.sapientia.core.engine;

/**
 * The machine currently being run. The scheduler reuses a single instance, so
 * behaviours must not keep a reference to it after {@code run} returns.
 */
public final class MachineContext {

    private int handle;
    private int worldId;
    private long position;
    private long tick;

    void bind(int handle, int worldId, long position, long tick) {
        this.handle = handle;
        this.worldId = worldId;
        this.position = position;
        this.tick = tick;
    }

    public int handle() { return handle; }

    public int worldId() { return worldId; }

    /** Block position packed with {@link BlockPositions#pack}. */
    public long position() { return position; }

    public int x() { return BlockPositions.x(position); }

    public int y() { return BlockPositions.y(position); }

    public int z() { return BlockPositions.z(position); }

    /** Current scheduler tick. */
    public long tick() { return tick; }
}
