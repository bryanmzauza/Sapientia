package dev.brmz.sapientia.bedrock.forms;

import java.util.function.Consumer;

import org.geysermc.cumulus.form.ModalForm;
import org.geysermc.floodgate.api.FloodgateApi;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

/**
 * Wrapper around Cumulus {@link ModalForm}: title + content + two labelled
 * buttons that resolve to a {@code boolean} (button1 = {@code true}). T-201.
 */
public final class SapientiaModalForm {

    private String title = "";
    private String content = "";
    private String button1 = "Yes";
    private String button2 = "No";
    private Consumer<Boolean> onClick = b -> {};
    private Runnable onClose = () -> {};

    public @NotNull SapientiaModalForm title(@NotNull String title) {
        this.title = title;
        return this;
    }

    public @NotNull SapientiaModalForm content(@NotNull String content) {
        this.content = content;
        return this;
    }

    public @NotNull SapientiaModalForm button1(@NotNull String label) {
        this.button1 = label;
        return this;
    }

    public @NotNull SapientiaModalForm button2(@NotNull String label) {
        this.button2 = label;
        return this;
    }

    public @NotNull SapientiaModalForm onClick(@NotNull Consumer<Boolean> handler) {
        this.onClick = handler;
        return this;
    }

    public @NotNull SapientiaModalForm onClose(@NotNull Runnable handler) {
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

        ModalForm.Builder builder = ModalForm.builder()
                .title(title)
                .content(content)
                .button1(button1)
                .button2(button2);
        builder.validResultHandler((form, response) -> onClick.accept(response.clickedFirst()));
        builder.closedOrInvalidResultHandler((form, response) -> onClose.run());
        api.sendForm(player.getUniqueId(), builder.build());
    }
}
