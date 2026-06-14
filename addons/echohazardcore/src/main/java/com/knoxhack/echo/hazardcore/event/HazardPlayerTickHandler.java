package com.knoxhack.echo.hazardcore.event;

import com.knoxhack.echo.adaptercore.EchoBackendWorldEventBridge;
import com.knoxhack.echo.hazardcore.api.HazardExposure;
import com.knoxhack.echo.hazardcore.api.HazardService;
import com.knoxhack.echo.hazardcore.api.HazardType;
import com.knoxhack.echo.hazardcore.player.HazardPlayerData;
import java.util.List;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;

public final class HazardPlayerTickHandler {
    private HazardPlayerTickHandler() {}

    public static void onPlayerTick(Object event) {
        Player tickPlayer = EchoBackendWorldEventBridge.postTickPlayer(event);
        if (!(tickPlayer instanceof ServerPlayer player) || player.level().isClientSide()) {
            return;
        }

        HazardPlayerData data = HazardPlayerData.get(player);
        HazardService service = HazardService.find();
        List<HazardExposure> exposures = service.tickPlayer(player);

        for (HazardExposure exposure : exposures) {
            applyExposure(player, data, exposure);
        }

        data.updateLastDepth(player.getY());
    }

    private static void applyExposure(ServerPlayer player, HazardPlayerData data, HazardExposure exposure) {
        if (!exposure.isDangerous()) {
            return;
        }
        float overflow = exposure.overflow();
        HazardType hazard = exposure.hazard();
        ServerLevel level = (ServerLevel) player.level();

        if (hazard.equals(HazardType.PRESSURE)) {
            player.hurtServer(level, player.damageSources().generic(), overflow * 0.5f);
        } else if (hazard.equals(HazardType.OXYGEN_DEPRIVATION)) {
            player.addEffect(new MobEffectInstance(MobEffects.MINING_FATIGUE, 40, 0, true, false));
            if (overflow > 2.0f) {
                player.hurtServer(level, player.damageSources().generic(), overflow * 0.25f);
            }
        } else if (hazard.equals(HazardType.COLD)) {
            player.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 60, 0, true, false));
            if (overflow > 2.0f) {
                player.hurtServer(level, player.damageSources().generic(), overflow * 0.15f);
            }
        } else if (hazard.equals(HazardType.HEAT)) {
            player.setRemainingFireTicks((int) (overflow * 10));
            if (overflow > 2.0f) {
                player.hurtServer(level, player.damageSources().generic(), overflow * 0.2f);
            }
        } else if (hazard.equals(HazardType.CORRUPTION)) {
            data.addCorruption(overflow * 0.05f);
            if (data.getCorruption() > 50.0f) {
                player.addEffect(new MobEffectInstance(MobEffects.NAUSEA, 80, 0, true, false));
            }
        } else if (hazard.equals(HazardType.DECOMPRESSION_SICKNESS)) {
            data.addDecompressionSeverity(overflow * 0.1f);
            if (data.getDecompressionSeverity() > 20.0f) {
                player.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 60, 1, true, false));
                player.hurtServer(level, player.damageSources().generic(), overflow * 0.1f);
            }
        }
    }
}
