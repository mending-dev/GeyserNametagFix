package dev.mending.geyser.nametagfix;

import org.geysermc.event.subscribe.Subscribe;
import org.geysermc.geyser.api.event.lifecycle.GeyserPostInitializeEvent;
import org.geysermc.geyser.api.extension.Extension;

/**
 * Keeps the nametags of players with a passenger (e.g. a cosmetic's invisible armor stand) visible for Bedrock players.
 * Geyser hides them because Java does so for mobs. The passenger stays a real rider, so it moves without delay; no
 * timer, no extra entity, nothing changes for Java players.
 */
public final class GeyserNametagFixExtension implements Extension {

    /** After Geyser's initialisation, when its translators are registered. */
    @Subscribe
    public void onPostInitialize(final GeyserPostInitializeEvent event) {
        final NametagRestorer restorer;
        try {
            restorer = NametagRestorer.create(logger());
        } catch (final ReflectiveOperationException | RuntimeException | LinkageError exception) {
            logger().error("GeyserNametagFix is disabled: this Geyser build does not match (update the extension)", exception);
            return;
        }
        final int hooked = TranslatorHooks.install(restorer, logger());
        if (hooked == TranslatorHooks.HOOKS) {
            logger().info("GeyserNametagFix enabled: nametags of players with passengers stay visible for Bedrock players");
        } else {
            logger().warning("GeyserNametagFix hooked " + hooked + " of " + TranslatorHooks.HOOKS
                + " translators; names may still be hidden in some cases (update the extension)");
        }
    }
}
