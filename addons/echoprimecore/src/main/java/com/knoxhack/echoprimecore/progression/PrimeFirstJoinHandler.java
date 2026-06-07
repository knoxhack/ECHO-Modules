package com.knoxhack.echoprimecore.progression;

import com.knoxhack.echo.adaptercore.EchoBackendWorldEventBridge;
import com.knoxhack.echoprimecore.EchoPrimeCore;
import com.knoxhack.echocore.api.EchoCoreServices;
import com.knoxhack.echoprimecore.config.PrimeConfig;
import com.knoxhack.echoprimecore.registry.ModItems;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

public final class PrimeFirstJoinHandler {
    private PrimeFirstJoinHandler() {
    }

    public static void onPlayerLoggedIn(Object event) {
        ServerPlayer player = EchoBackendWorldEventBridge.loggedInServerPlayer(event);
        if (player == null) {
            return;
        }
        if (isGameTestServer(player)) {
            return;
        }
        PrimePlayerData data = PrimePlayerData.get(player);
        if (!data.firstJoinInitialized()) {
            data.setFirstJoinInitialized(true);
            data.setStage("Prime Survival: Begin");
            data.setObjective("Find a Signal Shard, then craft a Crude Scanner.");
            PrimePlayerData.saveAndSync(player, data);
            PrimeProgressionService.unlock(player, EchoPrimeCore.id("started"), false);
            PrimeProgressionService.unlock(player, EchoPrimeCore.id("terminal_online"), false);
            PrimeProgressionService.unlock(player, EchoPrimeCore.id("index_online"), false);
            EchoCoreServices.startMission(player, EchoPrimeCore.id("mission/prime_survival_begin"));
            if (PrimeConfig.giveFieldManualOnFirstJoin()) {
                giveFieldManual(player);
            }
            player.sendSystemMessage(Component.literal(
                    "ECHO: Prime initialized. World state: Stable. Signal activity: Low. Objective: survive, explore, and bring ECHO systems online.")
                    .withStyle(ChatFormatting.AQUA));
        }
        PrimeStarterFlow.refreshStarterLoop(player);
    }

    private static void giveFieldManual(ServerPlayer player) {
        if (hasItem(player, ModItems.PRIME_FIELD_MANUAL.get())) {
            return;
        }
        ItemStack manual = new ItemStack(ModItems.PRIME_FIELD_MANUAL.get());
        if (!player.getInventory().add(manual)) {
            player.drop(manual, false);
        }
    }

    private static boolean hasItem(ServerPlayer player, net.minecraft.world.item.Item item) {
        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
            ItemStack stack = player.getInventory().getItem(slot);
            if (!stack.isEmpty() && stack.is(item)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isGameTestServer(ServerPlayer player) {
        String serverClassName = player.level().getServer() == null
                ? ""
                : player.level().getServer().getClass().getName();
        return serverClassName.contains("GameTest") || serverClassName.contains("gametest");
    }
}
