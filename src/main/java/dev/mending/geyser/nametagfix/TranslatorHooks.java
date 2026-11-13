package dev.mending.geyser.nametagfix;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiConsumer;
import org.geysermc.geyser.api.extension.ExtensionLogger;
import org.geysermc.geyser.registry.Registries;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.geyser.translator.protocol.PacketTranslator;
import org.geysermc.mcprotocollib.network.packet.Packet;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.entity.ClientboundAddEntityPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.entity.ClientboundSetPassengersPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.scoreboard.ClientboundSetPlayerTeamPacket;

/**
 * Runs the restorer after three of Geyser's Java packet translators; Geyser's translator itself runs unchanged first.
 * A missing translator (renamed in a newer Geyser) is skipped with a warning, an already hooked one is left alone.
 *
 * Geyser internals: Registries.JAVA_PACKET_TRANSLATORS, PacketTranslator.
 */
final class TranslatorHooks {

    static final int HOOKS = 3;

    private TranslatorHooks() {
    }

    /** Returns how many translators are hooked (also those hooked before). */
    static int install(final NametagRestorer restorer, final ExtensionLogger logger) {
        int hooked = 0;
        hooked += hook(ClientboundSetPassengersPacket.class, restorer::afterPassengers, logger);
        hooked += hook(ClientboundSetPlayerTeamPacket.class, restorer::afterTeam, logger);
        hooked += hook(ClientboundAddEntityPacket.class, restorer::afterSpawn, logger);
        return hooked;
    }

    @SuppressWarnings("unchecked")
    private static <P extends Packet> int hook(final Class<P> type, final BiConsumer<GeyserSession, P> after,
        final ExtensionLogger logger) {
        final PacketTranslator<P> original = (PacketTranslator<P>) Registries.JAVA_PACKET_TRANSLATORS.get(type);
        if (original == null) {
            logger.warning("GeyserNametagFix: Geyser has no translator for " + type.getSimpleName());
            return 0;
        }
        if (original.getClass().getName().equals(After.class.getName())) {
            return 1;
        }
        Registries.JAVA_PACKET_TRANSLATORS.register(type, new After<>(original, after, logger));
        return 1;
    }

    /** Geyser's translator, then the restorer; a restorer failure is logged once and never breaks Geyser. */
    private static final class After<P extends Packet> extends PacketTranslator<P> {

        private final PacketTranslator<P> original;
        private final BiConsumer<GeyserSession, P> after;
        private final ExtensionLogger logger;
        private final AtomicBoolean failureLogged = new AtomicBoolean();

        After(final PacketTranslator<P> original, final BiConsumer<GeyserSession, P> after,
            final ExtensionLogger logger) {
            this.original = original;
            this.after = after;
            this.logger = logger;
        }

        @Override
        public void translate(final GeyserSession session, final P packet) {
            original.translate(session, packet);
            try {
                after.accept(session, packet);
            } catch (final RuntimeException | LinkageError exception) {
                if (failureLogged.compareAndSet(false, true)) {
                    logger.error("GeyserNametagFix failed after " + packet.getClass().getSimpleName()
                        + " (logged once; update the extension)", exception);
                }
            }
        }

        @Override
        public boolean shouldExecuteInEventLoop() {
            return original.shouldExecuteInEventLoop();
        }
    }
}
