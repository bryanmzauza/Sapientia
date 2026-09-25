package dev.brmz.sapientia.api.mining;

import org.jetbrains.annotations.NotNull;

/**
 * One element of a mineral and how much of the mineral it makes up.
 *
 * @param element lower-case element id, e.g. {@code copper}; see
 *                {@link MiningService#registerElement}
 * @param share   primary (what the mineral is mined for), secondary or trace
 */
public record MineralComponent(@NotNull String element, @NotNull Share share) {

    /** How an element contributes to a mineral. */
    public enum Share { PRIMARY, SECONDARY, TRACE }

    public MineralComponent {
        if (element.isBlank()) throw new IllegalArgumentException("element must not be blank");
    }

    public static @NotNull MineralComponent primary(@NotNull String element) {
        return new MineralComponent(element, Share.PRIMARY);
    }

    public static @NotNull MineralComponent secondary(@NotNull String element) {
        return new MineralComponent(element, Share.SECONDARY);
    }

    public static @NotNull MineralComponent trace(@NotNull String element) {
        return new MineralComponent(element, Share.TRACE);
    }
}
