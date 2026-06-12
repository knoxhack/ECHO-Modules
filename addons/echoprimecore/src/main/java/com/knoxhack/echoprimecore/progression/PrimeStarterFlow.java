package com.knoxhack.echoprimecore.progression;

import com.knoxhack.echo.adaptercore.EchoBackendWorldEventBridge;
import com.echoplatform.echocore.api.EchoCoreServices;
import com.echoplatform.echocore.api.mission.MissionObjectiveType;
import com.knoxhack.echoprimecore.EchoPrimeCore;
import com.knoxhack.echoprimecore.registry.ModItems;
import com.knoxhack.echoprimecore.world.PrimeRelayPostGenerator;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.Map;

public final class PrimeStarterFlow {
    private PrimeStarterFlow() {
    }

    public static void onPlayerTick(Object event) {
        ServerPlayer player = EchoBackendWorldEventBridge.postTickServerPlayer(event);
        if (player == null) {
            return;
        }
        if (player.tickCount % 80 != 0) {
            return;
        }
        refreshStarterLoop(player);
    }

    public static void onItemCrafted(Object event) {
        if (!(EchoBackendWorldEventBridge.itemCraftedPlayer(event) instanceof ServerPlayer player)) {
            return;
        }
        ItemStack crafted = EchoBackendWorldEventBridge.itemCraftedStack(event);
        if (crafted.is(ModItems.CRUDE_SCANNER.get())) {
            PrimeProgressionService.unlock(player, EchoPrimeCore.id("lens_online"));
            EchoCoreServices.recordMissionObjective(player, MissionObjectiveType.CRAFT_ITEM,
                    EchoPrimeCore.id("crude_scanner"), 1, Map.of("source", "echoprimecore:starter_loop"));
        }
        refreshStarterLoop(player);
    }

    public static void refreshStarterLoop(ServerPlayer player) {
        if (player == null) {
            return;
        }
        if (has(player, ModItems.SIGNAL_SHARD.get())) {
            PrimeProgressionService.setStage(player, "First Signal",
                    "Craft a Crude Scanner from Signal Shard, Scanner Handle, Basic Lens, and Circuit Plate.");
        }
        if (has(player, ModItems.CRUDE_SCANNER.get())) {
            PrimeProgressionService.unlock(player, EchoPrimeCore.id("lens_online"), false);
        }
        boolean hasRelayLoot = has(player, ModItems.RELAY_FRAGMENT.get()) && has(player, ModItems.CIRCUIT_PLATE.get());
        if (hasRelayLoot) {
            PrimeProgressionService.unlock(player, EchoPrimeCore.id("first_signal"));
            PrimeProgressionService.unlock(player, EchoPrimeCore.id("first_ruin"), false);
            PrimeProgressionService.setStage(player, "First Tech",
                    "Index has revealed starter tech recipes. Build toward a Prime Circuit and Machine Frame.");
        }
        if (has(player, ModItems.MACHINE_FRAME.get()) || has(player, ModItems.PRIME_CIRCUIT.get())) {
            PrimeProgressionService.unlock(player, EchoPrimeCore.id("first_machine"));
        }
        maybeMarkNearbyRelay(player);
    }

    public static void useCrudeScanner(ServerPlayer player) {
        if (!(player.level() instanceof ServerLevel level)) {
            return;
        }
        PrimeProgressionService.unlock(player, EchoPrimeCore.id("lens_online"));
        PrimePlayerData data = PrimePlayerData.get(player);
        BlockPos relay = data.starterRelayPlaced()
                ? data.relayPos()
                : PrimeRelayPostGenerator.placeStarterRelay(level, player.blockPosition(), level.getRandom());
        data.setRelayPos(relay);
        PrimePlayerData.saveAndSync(player, data);
        PrimeProgressionService.unlock(player, EchoPrimeCore.id("holomap_online"));
        EchoCoreServices.structureDiscoveryService().recordStructureScan(player,
                EchoPrimeCore.id("abandoned_relay_post"),
                relay,
                "Abandoned Relay Post",
                "Weak Prime signal source found by crude scanner.");
        EchoCoreServices.recordMissionObjective(player, MissionObjectiveType.SCAN_BLOCK,
                EchoPrimeCore.id("weak_signal"), 1, Map.of("source", "echoprimecore:starter_loop"));
        player.sendSystemMessage(Component.literal("Crude Scanner // Weak ECHO signal resolved.")
                .withStyle(ChatFormatting.AQUA));
        player.sendSystemMessage(Component.literal("HoloMap marker recorded: Abandoned Relay Post at "
                + relay.getX() + " " + relay.getY() + " " + relay.getZ())
                .withStyle(ChatFormatting.GREEN));
    }

    private static void maybeMarkNearbyRelay(ServerPlayer player) {
        PrimePlayerData data = PrimePlayerData.get(player);
        if (!data.starterRelayPlaced()) {
            return;
        }
        if (player.blockPosition().closerThan(data.relayPos(), 9.0D)) {
            PrimeProgressionService.unlock(player, EchoPrimeCore.id("first_ruin"));
            EchoCoreServices.recordMissionObjective(player, MissionObjectiveType.DISCOVER_STRUCTURE,
                    EchoPrimeCore.id("abandoned_relay_post"), 1, Map.of("source", "echoprimecore:starter_loop"));
        }
    }

    private static boolean has(ServerPlayer player, Item item) {
        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
            ItemStack stack = player.getInventory().getItem(slot);
            if (!stack.isEmpty() && stack.is(item)) {
                return true;
            }
        }
        return false;
    }
}
