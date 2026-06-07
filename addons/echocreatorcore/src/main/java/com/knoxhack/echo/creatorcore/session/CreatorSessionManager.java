package com.knoxhack.echo.creatorcore.session;

import com.knoxhack.echo.creatorcore.api.CreatorMode;
import com.knoxhack.echo.creatorcore.api.CreatorPermission;
import com.knoxhack.echo.creatorcore.api.CreatorSession;
import java.time.Instant;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.server.level.ServerPlayer;

public final class CreatorSessionManager {
    private final Map<UUID, CreatorSession> sessions = new LinkedHashMap<>();

    public synchronized CreatorSession open(ServerPlayer player, CreatorPermission permission, CreatorMode mode) {
        CreatorSession session = new CreatorSession(player.getUUID(), player.getScoreboardName(),
                permission, mode, Instant.now(), Instant.now());
        sessions.put(player.getUUID(), session);
        return session;
    }

    public synchronized Optional<CreatorSession> get(UUID playerId) {
        return Optional.ofNullable(sessions.get(playerId));
    }

    public synchronized void close(UUID playerId) {
        sessions.remove(playerId);
    }

    public synchronized Collection<CreatorSession> sessions() {
        return List.copyOf(sessions.values());
    }

    public synchronized void touch(UUID playerId) {
        CreatorSession session = sessions.get(playerId);
        if (session != null) {
            sessions.put(playerId, session.touch());
        }
    }
}
