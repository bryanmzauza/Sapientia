package dev.brmz.sapientia.api.android;

import java.util.Optional;
import java.util.UUID;

import org.bukkit.block.Block;
import org.jetbrains.annotations.NotNull;

/**
 * Live read-only snapshot of an android placed in the world (T-451 / 1.9.0).
 *
 * <p>Returned by {@link AndroidService#nodeAt(Block)} and carried by the
 * {@link dev.brmz.sapientia.api.events.SapientiaAndroidTickEvent}. Mutation
 * goes through the {@link AndroidService} so the persistence layer can keep
 * up.
 */
public interface AndroidNode {

    @NotNull AndroidType type();

    /** World location. Never null. */
    @NotNull Block block();

    /** Owner / placer UUID. Empty when the android was spawned by an op tool. */
    @NotNull Optional<UUID> ownerUuid();

    /**
     * Currently assigned logic program name (matches
     * {@link dev.brmz.sapientia.api.logic.LogicProgram#name()}); empty when
     * the android has no program assigned yet.
     */
    @NotNull Optional<String> programName();

    int chipTier();
    int motorTier();
    int armourTier();
    int fuelTier();

    long fuelBuffer();
    int health();
}
