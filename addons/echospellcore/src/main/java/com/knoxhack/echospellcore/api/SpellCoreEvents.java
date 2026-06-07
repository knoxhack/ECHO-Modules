package com.knoxhack.echospellcore.api;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

public final class SpellCoreEvents {
    private static final List<Consumer<SpellCastEvent>> CAST_LISTENERS = new CopyOnWriteArrayList<>();

    private SpellCoreEvents() {
    }

    public static void onCast(Consumer<SpellCastEvent> listener) {
        if (listener != null) {
            CAST_LISTENERS.add(listener);
        }
    }

    public static void fireCast(ServerPlayer player, Identifier spellId, ItemStack focus) {
        SpellCastEvent event = new SpellCastEvent(player, spellId, focus.copy());
        for (Consumer<SpellCastEvent> listener : CAST_LISTENERS) {
            listener.accept(event);
        }
    }

    public record SpellCastEvent(ServerPlayer player, Identifier spellId, ItemStack focus) {
    }
}
