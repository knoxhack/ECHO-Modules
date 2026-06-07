package com.knoxhack.echocursecore.api;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;

public final class CurseCoreEvents {
    private static final List<Consumer<CurseEvent>> GAINED = new CopyOnWriteArrayList<>();
    private static final List<Consumer<CurseEvent>> CLEANSED = new CopyOnWriteArrayList<>();

    private CurseCoreEvents() {
    }

    public static void onGained(Consumer<CurseEvent> listener) {
        if (listener != null) {
            GAINED.add(listener);
        }
    }

    public static void onCleansed(Consumer<CurseEvent> listener) {
        if (listener != null) {
            CLEANSED.add(listener);
        }
    }

    static void fireGained(ServerPlayer player, Identifier curseId, int stage, String source) {
        CurseEvent event = new CurseEvent(player, curseId, stage, source);
        for (Consumer<CurseEvent> listener : GAINED) {
            listener.accept(event);
        }
    }

    static void fireCleansed(ServerPlayer player, Identifier curseId, int stage, String source) {
        CurseEvent event = new CurseEvent(player, curseId, stage, source);
        for (Consumer<CurseEvent> listener : CLEANSED) {
            listener.accept(event);
        }
    }

    public record CurseEvent(ServerPlayer player, Identifier curseId, int stage, String source) {
    }
}
