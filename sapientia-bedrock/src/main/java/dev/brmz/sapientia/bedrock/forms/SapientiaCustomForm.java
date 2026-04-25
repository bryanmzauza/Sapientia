package dev.brmz.sapientia.bedrock.forms;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import org.geysermc.cumulus.component.LabelComponent;
import org.geysermc.cumulus.component.ToggleComponent;
import org.geysermc.cumulus.form.CustomForm;
import org.geysermc.cumulus.response.CustomFormResponse;
import org.geysermc.floodgate.api.FloodgateApi;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

/**
 * Wrapper around Cumulus {@link CustomForm} (T-201). Supports the small subset
 * of components Sapientia uses on Bedrock today: {@link LabelComponent labels}
 * and {@link ToggleComponent toggles}. The submit handler receives a
 * {@link CustomFormResponse} the caller can index into in declaration order.
 *
 * <p>Adding sliders / dropdowns / inputs is a localized extension — keep this
 * class small until a real use case arrives.
 */
public final class SapientiaCustomForm {

    private String title = "";
    private final List<Component> components = new ArrayList<>();
    private Consumer<CustomFormResponse> onSubmit = r -> {};
    private Runnable onClose = () -> {};

    public @NotNull SapientiaCustomForm title(@NotNull String title) {
        this.title = title;
        return this;
    }

    public @NotNull SapientiaCustomForm label(@NotNull String text) {
        components.add(new Component(Kind.LABEL, text, false));
        return this;
    }

    public @NotNull SapientiaCustomForm toggle(@NotNull String text, boolean defaultValue) {
        components.add(new Component(Kind.TOGGLE, text, defaultValue));
        return this;
    }

    public @NotNull SapientiaCustomForm onSubmit(@NotNull Consumer<CustomFormResponse> handler) {
        this.onSubmit = handler;
        return this;
    }

    public @NotNull SapientiaCustomForm onClose(@NotNull Runnable handler) {
        this.onClose = handler;
        return this;
    }

    public void send(@NotNull Player player) {
        FloodgateApi api;
        try {
            api = FloodgateApi.getInstance();
        } catch (Throwable t) {
            return;
        }
        if (api == null) return;

        CustomForm.Builder builder = CustomForm.builder().title(title);
        for (Component c : components) {
            switch (c.kind) {
                case LABEL -> builder.label(c.text);
                case TOGGLE -> builder.toggle(c.text, c.toggleDefault);
            }
        }
        builder.validResultHandler((form, response) -> onSubmit.accept(response));
        builder.closedOrInvalidResultHandler((form, response) -> onClose.run());
        api.sendForm(player.getUniqueId(), builder.build());
    }

    /** Read-only view of declared components, useful for tests + auto-fallback. */
    public @NotNull List<Component> components() {
        return List.copyOf(components);
    }

    public @NotNull String title() { return title; }

    public enum Kind { LABEL, TOGGLE }

    /** Snapshot of one declared component (label or toggle). */
    public record Component(@NotNull Kind kind, @NotNull String text, boolean toggleDefault) {}
}
