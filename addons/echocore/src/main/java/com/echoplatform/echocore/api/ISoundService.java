package com.echoplatform.echocore.api;

import java.util.List;
import java.util.Map;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;

public interface ISoundService {
    default boolean playEvent(Identifier eventId) {
        return eventId != null;
    }

    default boolean playEvent(Player player, Identifier eventId) {
        return playEvent(player, eventId, 1.0F, 1.0F);
    }

    default boolean playEvent(Player player, Identifier eventId, float volume, float pitch) {
        return playEvent(eventId);
    }

    default boolean stopEvent(Identifier eventId) {
        return eventId != null;
    }

    default boolean stopEvent(Player player, Identifier eventId) {
        return stopEvent(eventId);
    }

    default boolean playProfile(Player player, Identifier profileId) {
        return false;
    }

    default boolean setContext(Player player, Map<String, String> context) {
        return false;
    }

    default boolean patchContext(Player player, Map<String, String> patch) {
        return false;
    }

    default boolean clearContext(Player player) {
        return false;
    }

    default boolean stopControlledAudio(Player player) {
        return false;
    }

    default boolean available() {
        return false;
    }

    default List<Identifier> activeEvents() {
        return List.of();
    }

    default SoundServiceDiagnostics diagnostics() {
        return SoundServiceDiagnostics.unavailable();
    }
}
