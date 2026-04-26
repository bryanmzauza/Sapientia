package dev.brmz.sapientia.api.events;

import static org.assertj.core.api.Assertions.assertThat;

import org.bukkit.event.HandlerList;
import org.junit.jupiter.api.Test;

/**
 * T-450 / 1.8.0: catalogue contract for {@link SapientiaItemPackagedEvent}.
 *
 * <p>Pure scaffolding test — no Bukkit instance required. Verifies the
 * static handler list is wired correctly and the event class implements
 * {@code Cancellable}, since both are part of the published 1.8.0 API
 * surface.
 */
class SapientiaItemPackagedEventTest {

    @Test
    void handlerListIsExposed() {
        HandlerList list = SapientiaItemPackagedEvent.getHandlerList();
        assertThat(list).isNotNull();
    }

    @Test
    void eventIsCancellable() {
        // The 1.8.0 API contract: addons must be able to veto a packager bundle.
        assertThat(org.bukkit.event.Cancellable.class
                .isAssignableFrom(SapientiaItemPackagedEvent.class)).isTrue();
    }
}
