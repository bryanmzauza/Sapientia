package dev.brmz.sapientia.api.logistics;

/**
 * Match strategy of an {@link ItemFilterRule}. Each rule independently decides
 * whether a stack passes through; a filter node combines its rules into a final
 * verdict (any whitelist match → allow, any blacklist match → deny).
 */
public enum ItemFilterMode {
    WHITELIST,
    BLACKLIST,
    /** Pass everything; useful for the default empty-rule state. */
    ACCEPT_ALL
}
