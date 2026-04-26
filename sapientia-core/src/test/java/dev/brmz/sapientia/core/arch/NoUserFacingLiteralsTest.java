package dev.brmz.sapientia.core.arch;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchRule;
import dev.brmz.sapientia.core.i18n.AllowLiteral;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

/**
 * ArchUnit gate T-106 — forbids callers in {@code dev.brmz.sapientia.core} from
 * invoking {@code CommandSender.sendMessage(String)} directly. All user-facing
 * text must be resolved through {@link dev.brmz.sapientia.core.i18n.Messages}
 * (which sends Adventure {@code Component}s). Annotate with {@link AllowLiteral}
 * to grant a deliberate exception.
 */
final class NoUserFacingLiteralsTest {

    private static final JavaClasses CORE_CLASSES = new ClassFileImporter()
            .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
            .importPackages("dev.brmz.sapientia.core");

    @Test
    void noCallsToSendMessageWithRawString() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("dev.brmz.sapientia.core..")
                .and().areNotAnnotatedWith(AllowLiteral.class)
                .should().callMethod(org.bukkit.command.CommandSender.class, "sendMessage", String.class)
                .orShould().callMethod(org.bukkit.command.CommandSender.class, "sendMessage", String[].class)
                .orShould().callMethod(org.bukkit.entity.Player.class, "sendMessage", String.class)
                .because("user-facing text must be resolved through Messages (see docs/i18n-strategy.md).")
                .allowEmptyShould(true);
        rule.check(CORE_CLASSES);
    }
}
