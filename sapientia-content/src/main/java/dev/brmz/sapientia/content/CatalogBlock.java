package dev.brmz.sapientia.content;

import dev.brmz.sapientia.api.block.SapientiaBlock;
import dev.brmz.sapientia.api.progression.Era;
import org.jetbrains.annotations.NotNull;

/** A built-in block whose era comes from {@link ContentEras}. */
public interface CatalogBlock extends SapientiaBlock {

    @Override
    default @NotNull Era era() {
        return ContentEras.of(id());
    }
}
