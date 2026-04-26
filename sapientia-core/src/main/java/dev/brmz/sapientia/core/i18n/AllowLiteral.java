package dev.brmz.sapientia.core.i18n;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Escape hatch for the {@code NoUserFacingLiteralsTest} ArchUnit rule (T-106).
 * Annotate a method when you deliberately need to send a raw string to a player or
 * command sender — for example in emergency log bootstrapping or internal debug
 * commands that never reach production users.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.TYPE})
public @interface AllowLiteral {
    /** Human-readable justification. Required. */
    String value();
}
