package dev.brmz.sapientia.core.mining;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.random.RandomGenerator;

import dev.brmz.sapientia.api.mining.Mineral;
import dev.brmz.sapientia.api.mining.MineralComponent;
import dev.brmz.sapientia.api.mining.SeparationMethod;
import dev.brmz.sapientia.api.mining.SeparationResult;
import dev.brmz.sapientia.api.progression.Era;
import org.jetbrains.annotations.NotNull;

/**
 * Separates a mineral into elements with a method: each primary element yields
 * {@code primaryYield} units (the fraction is a chance), each secondary and
 * trace element one unit with the method's chance. Whatever is not recovered,
 * including elements of locked eras, stays in the tailings, which a better
 * method can reprocess once. Reprocessed tailings yield only byproducts and
 * never leave tailings again.
 */
public final class Separator {

    private final Function<String, Era> elementEra;
    private final Predicate<Era> unlocked;

    /**
     * @param elementEra era of an element, {@code null} if unregistered (treated as available)
     * @param unlocked   whether an era is unlocked on the server
     */
    public Separator(@NotNull Function<String, Era> elementEra, @NotNull Predicate<Era> unlocked) {
        this.elementEra = elementEra;
        this.unlocked = unlocked;
    }

    public @NotNull SeparationResult separate(@NotNull Mineral mineral, @NotNull SeparationMethod method,
                                              boolean fromTailings, @NotNull RandomGenerator random) {
        Map<String, Integer> out = new LinkedHashMap<>();
        boolean leftover = false;
        for (MineralComponent component : mineral.composition()) {
            Era era = elementEra.apply(component.element());
            boolean locked = era != null && !unlocked.test(era);
            switch (component.share()) {
                case PRIMARY -> {
                    if (fromTailings) continue;
                    if (locked) {
                        leftover = true;
                        continue;
                    }
                    int units = (int) Math.floor(method.primaryYield());
                    if (random.nextDouble() < method.primaryYield() - units) units++;
                    if (units > 0) out.merge(component.element(), units, Integer::sum);
                }
                case SECONDARY, TRACE -> {
                    double chance = component.share() == MineralComponent.Share.SECONDARY
                            ? method.secondaryChance() : method.traceChance();
                    if (!locked && random.nextDouble() < chance) {
                        out.merge(component.element(), 1, Integer::sum);
                    } else {
                        leftover = true;
                    }
                }
            }
        }
        return new SeparationResult(out, leftover && !fromTailings);
    }
}
