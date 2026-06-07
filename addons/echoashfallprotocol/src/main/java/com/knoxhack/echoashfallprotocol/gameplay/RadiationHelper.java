package com.knoxhack.echoashfallprotocol.gameplay;

import com.knoxhack.echoashfallprotocol.registry.ModAttachments;
import com.knoxhack.echoashfallprotocol.event.AshfallAdapterCoreHazardRuntime;
import com.knoxhack.echoashfallprotocol.research.PerkEffectHandler;
import com.knoxhack.echoashfallprotocol.survival.HazardZoneManager;
import com.knoxhack.echoashfallprotocol.survival.SurvivalData;
import net.minecraft.server.level.ServerPlayer;

/**
 * Applies all radiation multipliers in one place.
 */
public final class RadiationHelper {

    private RadiationHelper() {
    }

    public static float scaleIncomingRadiation(ServerPlayer player, float baseAmount) {
        return DifficultyProfile.scaleRadiation(player.level(), baseAmount)
                * PerkEffectHandler.getRadiationResistanceMultiplier(player);
    }

    public static void addRadiation(ServerPlayer player, float baseAmount) {
        SurvivalData survivalData = player.getData(ModAttachments.SURVIVAL_DATA.get());
        float before = survivalData.getRadiationLevel();
        survivalData.addRadiation(scaleIncomingRadiation(player, baseAmount));
        player.setData(ModAttachments.SURVIVAL_DATA.get(), survivalData);
        AshfallAdapterCoreHazardRuntime.radiationChanged(
                player, before, survivalData.getRadiationLevel(), null, "radiation_helper");
    }

    public static void addEnvironmentalRadiation(ServerPlayer player, float baseAmount) {
        SurvivalData survivalData = player.getData(ModAttachments.SURVIVAL_DATA.get());
        float before = survivalData.getRadiationLevel();
        float amount = scaleIncomingRadiation(player, baseAmount);
        amount *= Math.max(0.0f, 1.0f - HazardZoneManager.radiationResistance(player));
        if (amount > 0.0f) {
            survivalData.addRadiation(amount);
            player.setData(ModAttachments.SURVIVAL_DATA.get(), survivalData);
            player.syncData(ModAttachments.SURVIVAL_DATA.get());
            AshfallAdapterCoreHazardRuntime.radiationChanged(
                    player, before, survivalData.getRadiationLevel(), null, "environmental_radiation");
        }
    }
}
