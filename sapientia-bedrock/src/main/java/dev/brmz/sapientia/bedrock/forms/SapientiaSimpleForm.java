package dev.brmz.sapientia.bedrock.forms;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import org.geysermc.cumulus.form.SimpleForm;
import org.geysermc.cumulus.response.SimpleFormResponse;
import org.geysermc.floodgate.api.FloodgateApi;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

/**
 * Tiny wrapper around Cumulus {@link SimpleForm} (T-201). Builds a menu form
 * with a title, content body and an arbitrary list of click-able buttons,
 * then dispatches it through {@link FloodgateApi#sendForm}.
 *
 * <p>Keeping the form construction in a Sapientia-owned class makes call-sites
 * read uniformly across UIs and lets the auto-fallback path (T-206) reuse the
 * same builder.
 */
public final class SapientiaSimpleForm {

    private String title = "";
    private String content = "";
    private final List<String> buttonLabels = new ArrayList<>();
    private Consumer<Integer> onClick = i -> {};
    private Runnable onClose = () -> {};

    public @NotNull SapientiaSimpleForm title(@NotNull String title) {
        this.title = title;
        return this;
    }

    public @NotNull SapientiaSimpleForm content(@NotNull String content) {
        this.content = content;
        return this;
    }

    public @NotNull SapientiaSimpleForm button(@NotNull String label) {
        this.buttonLabels.add(label);
        return this;
    }

    /** Click callback receives the 0-based button index that was pressed. */
    public @NotNull SapientiaSimpleForm onClick(@NotNull Consumer<Integer> handler) {
        this.onClick = handler;
        return this;
    }

    public @NotNull SapientiaSimpleForm onClose(@NotNull Runnable handler) {
        this.onClose = handler;
        return this;
    }

    /** Sends the form to the player. No-op if Floodgate is not loaded. */
    public void send(@NotNull Player player) {
        FloodgateApi api;
        try {
            api = FloodgateApi.getInstance();
        } catch (Throwable t) {
            return;
        }
        if (api == null) return;

        SimpleForm.Builder builder = SimpleForm.builder()
                .title(title)
                .content(content);
        for (String label : buttonLabels) {
            builder.button(label);
        }
        builder.validResultHandler((form, response) -> {
            int idx = response.clickedButtonId();
            onClick.accept(idx);
        });
        builder.closedOrInvalidResultHandler((form, response) -> onClose.run());

        api.sendForm(player.getUniqueId(), builder.build());
    }

    /** Convenience accessor used by tests / fallback to inspect button labels. */
    public @NotNull List<String> buttonLabels() {
        return List.copyOf(buttonLabels);
    }

    /** Convenience accessor for the resolved title. */
    public @NotNull String title() { return title; }

    /** Convenience accessor for the resolved content body. */
    public @NotNull String content() { return content; }

    /** Internal hook used by {@link #send(Player)}; visible for testing. */
    @SuppressWarnings("unused")
    static SimpleFormResponse __responseTypeMarker() { return null; }
}
