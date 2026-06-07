package com.knoxhack.echoholomap.integration;

import com.knoxhack.echoholomap.Config;
import com.knoxhack.echoholomap.HoloMapIds;
import com.knoxhack.echoholomap.network.HoloMapSync;
import com.knoxhack.echoholomap.world.HoloMapSavedData;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.Permissions;
import com.knoxhack.echoterminal.api.TerminalActionRegistry;

public final class HoloMapTerminalCommonIntegration {
    private HoloMapTerminalCommonIntegration() {
    }

    public static void register() {
        TerminalActionRegistry.register(HoloMapIds.TAB, HoloMapIds.REFRESH_ACTION, (player, payload) -> {
            if (player instanceof ServerPlayer serverPlayer) {
                HoloMapSync.send(serverPlayer);
            }
        });
        TerminalActionRegistry.register(HoloMapIds.TAB, HoloMapIds.TEST_MARKER_ACTION, (player, payload) -> {
            if (!(player instanceof ServerPlayer serverPlayer) || !debugEnabled()
                    || !serverPlayer.createCommandSourceStack().permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER)
                    || !(serverPlayer.level() instanceof ServerLevel serverLevel)) {
                return;
            }
            Identifier layer = HoloMapIds.layerFromInput(payload);
            HoloMapSavedData.get(serverLevel).addDebugMarker(serverPlayer, layer);
            serverPlayer.sendSystemMessage(Component.literal("ECHO HoloMap // Test marker added to " + layer + "."));
            HoloMapSync.send(serverPlayer);
        });
    }

    private static boolean debugEnabled() {
        try {
            return Config.DEBUG_MARKERS.get();
        } catch (RuntimeException exception) {
            return true;
        }
    }
}
