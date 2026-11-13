package dev.mending.geyser.nametagfix;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.concurrent.atomic.AtomicBoolean;
import org.geysermc.geyser.api.extension.ExtensionLogger;
import org.geysermc.geyser.entity.type.Entity;
import org.geysermc.geyser.entity.type.player.PlayerEntity;
import org.geysermc.geyser.scoreboard.Team;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.entity.ClientboundAddEntityPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.entity.ClientboundSetPassengersPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.scoreboard.ClientboundSetPlayerTeamPacket;

/**
 * Puts back the nametag Geyser hid only because a player carries a passenger.
 *
 * The name is what Geyser's LivingEntity#updateNametag computes without its passengers.isEmpty() condition: no team
 * = username, visible team = team display name, hidden team = no name. It is written through Geyser's own setNametag,
 * so Geyser's stored nametag matches the client and every later hide by Geyser is sent and corrected by the next hook.
 *
 * Geyser internals: Entity#setNametag, Team, EntityCache. Runs on the session's event loop.
 */
final class NametagRestorer {

    private final Method setNametag;
    private final ExtensionLogger logger;
    private final AtomicBoolean failureLogged = new AtomicBoolean();

    private NametagRestorer(final Method setNametag, final ExtensionLogger logger) {
        this.setNametag = setNametag;
        this.logger = logger;
    }

    /** Finds Geyser's protected setNametag(String, boolean); throws if this Geyser build has none. */
    static NametagRestorer create(final ExtensionLogger logger) throws NoSuchMethodException {
        final Method method = Entity.class.getDeclaredMethod("setNametag", String.class, boolean.class);
        method.setAccessible(true);
        return new NametagRestorer(method, logger);
    }

    void afterPassengers(final GeyserSession session, final ClientboundSetPassengersPacket packet) {
        restore(session, session.getEntityCache().getEntityByJavaId(packet.getEntityId()));
    }

    /** A player spawned again after a world change keeps its old passengers in Geyser, so the name is hidden at once. */
    void afterSpawn(final GeyserSession session, final ClientboundAddEntityPacket packet) {
        restore(session, session.getEntityCache().getEntityByJavaId(packet.getEntityId()));
    }

    /**
     * Any team change can matter, also of the Bedrock player's own team (HIDE_FOR_OTHER_TEAMS, HIDE_FOR_OWN_TEAM), so
     * every player is checked; nothing is sent where the name is already right.
     */
    void afterTeam(final GeyserSession session, final ClientboundSetPlayerTeamPacket packet) {
        session.getEntityCache().forEachPlayerEntity(player -> restore(session, player));
    }

    private void restore(final GeyserSession session, final Entity entity) {
        if (!(entity instanceof PlayerEntity player) || player == session.getPlayerEntity() || !player.isValid()
            || !hasPassenger(player)) {
            return;
        }
        final String wanted = wantedName(session, player);
        if (wanted.equals(player.getNametag())) {
            return;
        }
        try {
            setNametag.invoke(player, wanted, false);
        } catch (final IllegalAccessException | InvocationTargetException exception) {
            if (failureLogged.compareAndSet(false, true)) {
                logger.error("GeyserNametagFix could not set a nametag (update the extension)", exception);
            }
            return;
        }
        player.updateBedrockMetadata();
    }

    private static String wantedName(final GeyserSession session, final PlayerEntity player) {
        final String name = player.getUsername();
        final Team team = session.getWorldCache().getScoreboard().getTeamFor(player.teamIdentifier());
        if (team == null) {
            return name;
        }
        return team.isVisibleFor(session.getPlayerEntity().getUsername()) ? team.displayName(name) : "";
    }

    private static boolean hasPassenger(final Entity entity) {
        for (final Entity passenger : entity.getPassengers()) {
            if (passenger != null) {
                return true;
            }
        }
        return false;
    }
}
