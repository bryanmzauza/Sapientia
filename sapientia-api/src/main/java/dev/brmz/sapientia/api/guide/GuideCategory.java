package dev.brmz.sapientia.api.guide;

/**
 * High-level bucket used by the guide book UI (T-150 / 0.4.0) to organise
 * {@link dev.brmz.sapientia.api.item.SapientiaItem}s and
 * {@link dev.brmz.sapientia.api.block.SapientiaBlock}s into readable pages.
 */
public enum GuideCategory {
    /** Raw materials, ingredients, crafting components. */
    MATERIAL,
    /** Hand-held tools and utilities. */
    TOOL,
    /** Workbenches, processing and generation machines. */
    MACHINE,
    /** Energy network blocks (generators, cables, capacitors, consumers). */
    ENERGY,
    /** Item / fluid logistics. */
    LOGISTICS,
    /** Everything else (default). */
    MISC
}
