package dev.brmz.sapientia.core.android;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import dev.brmz.sapientia.api.android.AndroidNode;
import dev.brmz.sapientia.api.android.AndroidType;
import dev.brmz.sapientia.api.android.AndroidUpgrade;
import dev.brmz.sapientia.api.android.AndroidUpgradeKind;
import org.bukkit.block.Block;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Mutable in-memory implementation of {@link AndroidNode} (T-451 / 1.9.0).
 *
 * <p>State is mutated by {@link AndroidServiceImpl}; the kinetic loop
 * (1.9.1) reads it through the public {@link AndroidNode} surface only.
 */
final class SimpleAndroidNode implements AndroidNode {

    private final AndroidType type;
    private final Block block;
    private final UUID ownerUuid;
    private volatile @Nullable String programName;
    private final ConcurrentHashMap<AndroidUpgradeKind, Integer> upgrades = new ConcurrentHashMap<>();
    private volatile long fuelBuffer;
    private volatile int health;
    private volatile long lastTickMs;

    SimpleAndroidNode(@NotNull AndroidType type,
                      @NotNull Block block,
                      @Nullable UUID ownerUuid) {
        this.type = type;
        this.block = block;
        this.ownerUuid = ownerUuid;
        // Defaults: tier 1 in every slot, full health, empty fuel.
        upgrades.put(AndroidUpgradeKind.AI_CHIP, 1);
        upgrades.put(AndroidUpgradeKind.MOTOR, 1);
        upgrades.put(AndroidUpgradeKind.ARMOUR, 1);
        upgrades.put(AndroidUpgradeKind.FUEL_MODULE, 1);
        this.fuelBuffer = 0L;
        this.health = 100;
    }

    @Override public @NotNull AndroidType type() { return type; }
    @Override public @NotNull Block block() { return block; }
    @Override public @NotNull Optional<UUID> ownerUuid() { return Optional.ofNullable(ownerUuid); }
    @Override public @NotNull Optional<String> programName() { return Optional.ofNullable(programName); }

    @Override public int chipTier()   { return upgrades.getOrDefault(AndroidUpgradeKind.AI_CHIP, 1); }
    @Override public int motorTier()  { return upgrades.getOrDefault(AndroidUpgradeKind.MOTOR, 1); }
    @Override public int armourTier() { return upgrades.getOrDefault(AndroidUpgradeKind.ARMOUR, 1); }
    @Override public int fuelTier()   { return upgrades.getOrDefault(AndroidUpgradeKind.FUEL_MODULE, 1); }

    @Override public long fuelBuffer() { return fuelBuffer; }
    @Override public int health() { return health; }

    void setProgramName(@Nullable String programName) { this.programName = programName; }
    void setUpgrade(@NotNull AndroidUpgrade upgrade) {
        upgrades.put(upgrade.kind(), upgrade.tier());
    }
    int upgradeTier(@NotNull AndroidUpgradeKind kind) {
        return upgrades.getOrDefault(kind, 1);
    }
    void setFuelBuffer(long fuelBuffer) { this.fuelBuffer = Math.max(0L, fuelBuffer); }
    void setHealth(int health) { this.health = Math.max(0, health); }
    long lastTickMs() { return lastTickMs; }
    void setLastTickMs(long lastTickMs) { this.lastTickMs = lastTickMs; }
}
