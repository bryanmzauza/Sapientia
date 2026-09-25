package dev.brmz.sapientia.content;

import dev.brmz.sapientia.api.item.SapientiaItem;
import dev.brmz.sapientia.api.progression.Era;
import org.jetbrains.annotations.NotNull;

/** A built-in item whose era comes from {@link ContentEras}. */
public interface CatalogItem extends SapientiaItem {

    @Override
    default @NotNull Era era() {
        return ContentEras.of(id());
    }
}
