package com.knoxhack.echoashfallprotocol.faction;

import com.knoxhack.echocore.api.EchoFactionActionResult;
import com.knoxhack.echocore.api.EchoFactionDefinition;
import com.knoxhack.echocore.api.EchoFactionProfile;
import com.knoxhack.echoashfallprotocol.registry.ModItems;
import com.knoxhack.echoashfallprotocol.research.ResearchData;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

/**
 * Cooldown-gated practical services exposed by Ashfall faction NPCs.
 */
public final class AshfallFactionServices {
    private static final int COOLDOWN_TICKS = 20 * 60 * 8;

    private AshfallFactionServices() {
    }

    public static EchoFactionActionResult perform(ServerPlayer player, EchoFactionProfile profile, String roleId) {
        EchoFactionDefinition definition = profile.definition();
        AshfallFactionContracts.ServiceKind kind = AshfallFactionContracts.serviceKind(definition.id());
        AshfallFactionContractData data = AshfallFactionContractData.get(player);
        long now = player.level().getGameTime();
        long readyAt = data.serviceCooldownUntil(definition.id(), kind.name());
        if (readyAt > now) {
            long seconds = Math.max(1L, (readyAt - now) / 20L);
            return EchoFactionActionResult.failure("Service Cooling", definition.shortName()
                    + " support can reopen in " + seconds + "s.");
        }

        switch (kind) {
            case SUPPLY -> {
                give(player, new ItemStack(ModItems.CLEAN_WATER_BOTTLE.get(), 1));
                give(player, new ItemStack(ModItems.BANDAGE.get(), 1));
            }
            case HAZARD -> {
                give(player, new ItemStack(ModItems.FILTER_CARTRIDGE_BASIC.get(), 1));
                give(player, new ItemStack(ModItems.CLEAN_WATER_BOTTLE.get(), 1));
            }
            case REPAIR -> {
                give(player, new ItemStack(ModItems.SCRAP_METAL.get(), 4));
                give(player, new ItemStack(ModItems.ENERGY_CELL.get(), 1));
            }
            case MEDICAL -> {
                give(player, new ItemStack(ModItems.RAD_AWAY.get(), 1));
                give(player, new ItemStack(ModItems.BANDAGE.get(), 1));
            }
            case COLD -> {
                give(player, new ItemStack(ModItems.HAND_WARMER.get(), 1));
                give(player, new ItemStack(ModItems.EMERGENCY_RATION.get(), 1));
            }
            case RADIATION -> {
                give(player, new ItemStack(ModItems.RAD_AWAY.get(), 1));
                give(player, new ItemStack(ModItems.FILTER_CARTRIDGE_ADVANCED.get(), 1));
            }
            case SALVAGE -> {
                give(player, new ItemStack(ModItems.SCRAP_WIRE.get(), 3));
                give(player, new ItemStack(ModItems.SCRAP_CIRCUIT.get(), 1));
            }
            case ARCHIVE -> {
                ResearchData research = ResearchData.get(player);
                research.addPoints(10);
                ResearchData.saveAndSync(player, research);
            }
            case NEXUS -> {
                give(player, new ItemStack(ModItems.FILTER_CARTRIDGE_ELITE.get(), 1));
                give(player, new ItemStack(ModItems.RAD_AWAY.get(), 1));
            }
        }

        data.setServiceCooldown(definition.id(), kind.name(), now + COOLDOWN_TICKS);
        AshfallFactionContractData.saveAndSync(player, data);
        String message = definition.shortName() + " support delivered: " + definition.serviceSummary();
        player.sendSystemMessage(Component.literal("[ECHO-7] " + message), true);
        return EchoFactionActionResult.success("Service Delivered", message);
    }

    private static void give(ServerPlayer player, ItemStack stack) {
        com.knoxhack.echoashfallprotocol.research.PerkEffectHandler.applyLootBonus(player, stack);
        if (!player.addItem(stack)) {
            player.drop(stack, false);
        }
    }
}
