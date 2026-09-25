package dev.brmz.sapientia.core.engine;

import java.util.Map;

import dev.brmz.sapientia.api.block.SapientiaBlock;
import dev.brmz.sapientia.api.events.SapientiaBlockPlaceEvent;
import dev.brmz.sapientia.core.block.ChunkBlockIndex;
import dev.brmz.sapientia.core.i18n.Messages;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.jetbrains.annotations.NotNull;

/** Cancels placements that would break a per-chunk limit and tells the player why. */
public final class ChunkLimitListener implements Listener {

    private final ChunkBlockIndex index;
    private final SapientiaEngine engine;
    private final Messages messages;

    public ChunkLimitListener(@NotNull ChunkBlockIndex index, @NotNull SapientiaEngine engine,
                              @NotNull Messages messages) {
        this.index = index;
        this.engine = engine;
        this.messages = messages;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlace(@NotNull SapientiaBlockPlaceEvent event) {
        Block block = event.block();
        SapientiaBlock placed = event.definition();
        Map<SapientiaBlock, Integer> counts = index.countsInChunk(
                block.getWorld().getName(), block.getX() >> 4, block.getZ() >> 4);

        int total = 0;
        int processors = 0;
        int sameType = 0;
        for (Map.Entry<SapientiaBlock, Integer> entry : counts.entrySet()) {
            SapientiaBlock existing = entry.getKey();
            total += entry.getValue();
            if (engine.isProcessor(existing.id())) processors += entry.getValue();
            if (existing.id().equals(placed.id())) sameType += entry.getValue();
        }
        ChunkLimits.Violation violation = engine.config().limits().check(
                new ChunkLimits.Counts(total, processors, sameType),
                placed.id().toString(), engine.isProcessor(placed.id()), placed.chunkLimit());
        if (violation == null) {
            return;
        }
        event.setCancelled(true);
        String key = switch (violation.kind()) {
            case BLOCKS -> "limit.chunk.blocks";
            case PROCESSORS -> "limit.chunk.processors";
            case SAME_TYPE -> "limit.chunk.same-type";
        };
        event.player().sendMessage(messages.component(key,
                Placeholder.unparsed("limit", Integer.toString(violation.limit())),
                Placeholder.component("block", messages.component(placed.displayNameKey()))));
    }
}
