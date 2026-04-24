package dev.brmz.sapientia.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class VersionTest {

    @Test
    void parsesSemverString() {
        Version v = Version.parse("1.2.3");
        assertThat(v.major()).isEqualTo(1);
        assertThat(v.minor()).isEqualTo(2);
        assertThat(v.patch()).isEqualTo(3);
        assertThat(v.toString()).isEqualTo("1.2.3");
    }

    @Test
    void stripsPreReleaseQualifier() {
        Version v = Version.parse("0.1.0-SNAPSHOT");
        assertThat(v).isEqualTo(new Version(0, 1, 0));
    }

    @Test
    void rejectsShortVersions() {
        assertThatThrownBy(() -> Version.parse("1.2"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
