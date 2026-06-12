package com.knoxhack.echoashfallprotocol.event;

import com.echoplatform.echocore.api.EchoCoreServices;
import com.knoxhack.echoashfallprotocol.echo.QuestData;
import com.knoxhack.echoashfallprotocol.faction.AshfallBiomeFactions;
import com.knoxhack.echoashfallprotocol.faction.FactionProgressionHelper;
import com.knoxhack.echoashfallprotocol.registry.ModAttachments;
import com.knoxhack.echoashfallprotocol.registry.ModItems;
import com.knoxhack.echoashfallprotocol.research.ResearchData;
import com.knoxhack.echoashfallprotocol.world.FieldOpsData;
import com.knoxhack.echoashfallprotocol.world.POIScannerService;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;

/**
 * Repeatable field-op contracts for survival, exploration, and combat.
 */
public class FieldOpsContractHandler {

    public static void requestContract(ServerPlayer player, POIScannerService.ScanHit hit) {
        FieldOpsData data = player.getData(ModAttachments.FIELD_OPS_DATA.get());

        if (EnvironmentalEventHandler.isEventActive(player.level(), EnvironmentalEventType.RADIATION_STORM)
                || EnvironmentalEventHandler.isEventActive(player.level(), EnvironmentalEventType.TOXIC_STORM)
                || EnvironmentalEventHandler.isEventActive(player.level(), EnvironmentalEventType.BLACKOUT)
                || EnvironmentalEventHandler.isEventActive(player.level(), EnvironmentalEventType.ASH_STORM)
                || EnvironmentalEventHandler.isEventActive(player.level(), EnvironmentalEventType.CRYO_FRONT)
                || EnvironmentalEventHandler.isEventActive(player.level(), EnvironmentalEventType.NEXUS_SURGE)) {
            data.assign(FieldOpsData.ContractType.STORM_SHELTER, "storm_shelter", "Storm Shelter Audit", 3);
            player.sendSystemMessage(Component.literal(
                    "[ECHO-7] Survival contract uploaded: hold covered shelter during active weather for 30 seconds.")
                    .withStyle(ChatFormatting.GOLD));
        } else if (hit != null && !hit.discovered()) {
            data.assign(FieldOpsData.ContractType.SCANNER_SWEEP, hit.id(), hit.displayName(), 1);
            player.sendSystemMessage(Component.literal(
                    "[ECHO-7] Exploration contract uploaded: verify and archive " + hit.displayName()
                            + " (" + hit.hazardProfile() + ").")
                    .withStyle(ChatFormatting.AQUA));
        } else {
            data.assign(FieldOpsData.ContractType.CORRUPTED_BOUNTY, "corrupted_bounty", "Corrupted Bounty", 6);
            player.sendSystemMessage(Component.literal(
                    "[ECHO-7] Combat contract uploaded: eliminate 6 corrupted hostiles for Radwarden bounty credit.")
                    .withStyle(ChatFormatting.RED));
        }

        player.setData(ModAttachments.FIELD_OPS_DATA.get(), data);
    }

    public static void onPoiDiscovered(ServerPlayer player, POIScannerService.ScanHit hit) {
        FieldOpsData data = player.getData(ModAttachments.FIELD_OPS_DATA.get());
        if (data.getActiveContract() == FieldOpsData.ContractType.SCANNER_SWEEP
            && data.getTargetId().equals(hit.id())) {
            data.incrementProgress();
            completeContract(player, data);
        }
        player.setData(ModAttachments.FIELD_OPS_DATA.get(), data);
    }

    public static void onPlayerTick(Object event) {
        if (!(eventValue(event, "getEntity") instanceof ServerPlayer player)) {
            return;
        }

        FieldOpsData data = player.getData(ModAttachments.FIELD_OPS_DATA.get());
        if (data.getActiveContract() != FieldOpsData.ContractType.STORM_SHELTER) {
            return;
        }

        boolean eventActive = EnvironmentalEventHandler.isEventActive(player.level(), EnvironmentalEventType.RADIATION_STORM)
                || EnvironmentalEventHandler.isEventActive(player.level(), EnvironmentalEventType.TOXIC_STORM)
                || EnvironmentalEventHandler.isEventActive(player.level(), EnvironmentalEventType.BLACKOUT)
                || EnvironmentalEventHandler.isEventActive(player.level(), EnvironmentalEventType.ASH_STORM)
                || EnvironmentalEventHandler.isEventActive(player.level(), EnvironmentalEventType.CRYO_FRONT)
                || EnvironmentalEventHandler.isEventActive(player.level(), EnvironmentalEventType.NEXUS_SURGE);

        if (!eventActive || player.tickCount % 100 != 0) {
            return;
        }

        if (!player.level().canSeeSky(player.blockPosition())) {
            data.incrementProgress();
            player.sendSystemMessage(Component.literal(
                    "[ECHO-7] Shelter integrity holding. (" + data.getProgress() + "/" + data.getGoal() + ")")
                    .withStyle(ChatFormatting.GRAY));
            if (data.isComplete()) {
                completeContract(player, data);
            }
            player.setData(ModAttachments.FIELD_OPS_DATA.get(), data);
        }
    }

    public static void onLivingDeath(Object event) {
        Object source = eventValue(event, "getSource");
        if (!(eventValue(source, "getEntity") instanceof ServerPlayer player)) {
            return;
        }

        FieldOpsData data = player.getData(ModAttachments.FIELD_OPS_DATA.get());
        if (data.getActiveContract() != FieldOpsData.ContractType.CORRUPTED_BOUNTY) {
            return;
        }

        if (!(eventValue(event, "getEntity") instanceof LivingEntity killed)) {
            return;
        }
        String id = killed.getType().getDescriptionId();
        if (id.contains("rad_zombie")
            || id.contains("glowing_ghoul")
            || id.contains("irradiated_wolf")
            || id.contains("ash_wraith")
            || id.contains("rust_walker")
            || id.contains("city_stalker")
            || id.contains("mutated_crawler")
            || id.contains("toxic_slime")) {
            data.incrementProgress();
            if (data.isComplete()) {
                completeContract(player, data);
            } else {
                player.sendSystemMessage(Component.literal(
                        "[ECHO-7] Bounty confirmed. (" + data.getProgress() + "/" + data.getGoal() + ")")
                        .withStyle(ChatFormatting.RED));
            }
            player.setData(ModAttachments.FIELD_OPS_DATA.get(), data);
        }
    }

    private static void completeContract(ServerPlayer player, FieldOpsData data) {
        FieldOpsData.ContractType type = data.getActiveContract();
        ResearchData research = ResearchData.get(player);

        switch (type) {
            case STORM_SHELTER -> {
                giveItem(player, new ItemStack(ModItems.EMERGENCY_RATION.get(), 2));
                giveItem(player, new ItemStack(ModItems.FILTER_CARTRIDGE_BASIC.get(), 1));
                giveItem(player, new ItemStack(ModItems.CLEAN_WATER_BOTTLE.get(), 1));
                EchoCoreServices.addFactionReputation(player, AshfallBiomeFactions.SPOREBOUND_SANCTUM, 4);
                AshfallAdapterCoreExplorationRuntime.reputationUpdated(
                        player, AshfallBiomeFactions.SPOREBOUND_SANCTUM, 4, "field_ops_contract");
                research.addPoints(8);
            }
            case SCANNER_SWEEP -> {
                giveItem(player, new ItemStack(ModItems.CIRCUIT_BOARD.get(), 1));
                giveItem(player, new ItemStack(ModItems.ENERGY_CELL.get(), 1));
                EchoCoreServices.addFactionReputation(player, AshfallBiomeFactions.CRASHBREAK_SALVAGE, 4);
                AshfallAdapterCoreExplorationRuntime.reputationUpdated(
                        player, AshfallBiomeFactions.CRASHBREAK_SALVAGE, 4, "field_ops_contract");
                research.addPoints(12);
            }
            case CORRUPTED_BOUNTY -> {
                giveItem(player, new ItemStack(ModItems.POWER_CELL.get(), 1));
                giveItem(player, new ItemStack(ModItems.SCRAP_METAL.get(), 4));
                EchoCoreServices.addFactionReputation(player, AshfallBiomeFactions.RADWARDEN_COMPACT, 5);
                AshfallAdapterCoreExplorationRuntime.reputationUpdated(
                        player, AshfallBiomeFactions.RADWARDEN_COMPACT, 5, "field_ops_contract");
                research.addPoints(10);
            }
            case NONE -> {
                return;
            }
        }

        ResearchData.saveAndSync(player, research);
        FactionProgressionHelper.syncMilestones(player);

        QuestData quest = QuestData.get(player);
        quest.addToArchive("[FIELD OPS] " + type.name() + " contract completed.");
        player.setData(ModAttachments.QUEST_DATA.get(), quest);

        player.sendSystemMessage(Component.literal(
                "[ECHO-7] Contract complete: " + data.getTargetName() + ". Rewards transmitted.")
                .withStyle(ChatFormatting.GREEN));
        data.clearContract();
        player.setData(ModAttachments.FIELD_OPS_DATA.get(), data);
    }

    private static void giveItem(ServerPlayer player, ItemStack stack) {
        if (!player.getInventory().add(stack.copy())) {
            player.level().addFreshEntity(new ItemEntity(player.level(), player.getX(), player.getY(), player.getZ(), stack.copy()));
        }
    }

    private static Object eventValue(Object event, String methodName) {
        if (event == null) {
            return null;
        }
        try {
            return event.getClass().getMethod(methodName).invoke(event);
        } catch (ReflectiveOperationException | RuntimeException exception) {
            return null;
        }
    }
}
