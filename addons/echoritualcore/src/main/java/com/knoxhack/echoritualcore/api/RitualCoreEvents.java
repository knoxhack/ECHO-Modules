package com.knoxhack.echoritualcore.api;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

public final class RitualCoreEvents {
    private static final List<Consumer<RitualCompleteEvent>> COMPLETE_LISTENERS = new ArrayList<>();
    private static final List<Consumer<RitualFailedEvent>> FAILURE_LISTENERS = new ArrayList<>();

    private RitualCoreEvents() {
    }

    public static void onComplete(Consumer<RitualCompleteEvent> listener) {
        COMPLETE_LISTENERS.add(listener);
    }

    public static void onFailure(Consumer<RitualFailedEvent> listener) {
        FAILURE_LISTENERS.add(listener);
    }

    public static void fireComplete(ServerPlayer player, Identifier ritualId, Identifier subjectId, ItemStack focus, BlockPos pos) {
        RitualCompleteEvent event = new RitualCompleteEvent(player, ritualId, subjectId, focus, pos);
        for (Consumer<RitualCompleteEvent> listener : new ArrayList<>(COMPLETE_LISTENERS)) {
            listener.accept(event);
        }
    }

    public static void fireFailure(ServerPlayer player, Identifier ritualId, Identifier subjectId, String reason, BlockPos pos) {
        RitualFailedEvent event = new RitualFailedEvent(player, ritualId, subjectId, reason, pos);
        for (Consumer<RitualFailedEvent> listener : new ArrayList<>(FAILURE_LISTENERS)) {
            listener.accept(event);
        }
    }

    public record RitualCompleteEvent(ServerPlayer player, Identifier ritualId, Identifier subjectId, ItemStack focus, BlockPos pos) {
    }

    public record RitualFailedEvent(ServerPlayer player, Identifier ritualId, Identifier subjectId, String reason, BlockPos pos) {
    }
}
