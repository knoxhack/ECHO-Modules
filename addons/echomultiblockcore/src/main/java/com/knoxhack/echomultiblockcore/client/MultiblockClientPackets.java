package com.knoxhack.echomultiblockcore.client;

import com.knoxhack.echomultiblockcore.api.MultiblockBuildAssistSnapshot;
import com.knoxhack.echomultiblockcore.network.MultiblockDefinitionMetadataPacket;
import com.knoxhack.echomultiblockcore.network.AutomationRecipeMetadataPacket;
import com.knoxhack.echomultiblockcore.network.MultiblockBuildAssistPacket;
import com.knoxhack.echomultiblockcore.network.RobotAnimationPacket;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.Level;

public final class MultiblockClientPackets {
    private static final Map<Identifier, MultiblockDefinitionMetadataPacket.Entry> DEFINITIONS = new LinkedHashMap<>();
    private static final Map<Identifier, AutomationRecipeMetadataPacket.Entry> RECIPES = new LinkedHashMap<>();
    private static final Map<Identifier, MultiblockBuildAssistSnapshot> BUILD_ASSIST = new LinkedHashMap<>();

    private MultiblockClientPackets() {
    }

    public static void handleRobotAnimation(RobotAnimationPacket packet) {
        Minecraft minecraft = Minecraft.getInstance();
        Level level = minecraft.level;
        if (level == null || packet == null) {
            return;
        }
        RobotAnimationClientState.accept(packet);
        double sx = packet.robotPos().getX() + 0.5D;
        double sy = packet.robotPos().getY() + 0.8D;
        double sz = packet.robotPos().getZ() + 0.5D;
        double tx = packet.targetPos().getX() + 0.5D;
        double ty = packet.targetPos().getY() + 0.6D;
        double tz = packet.targetPos().getZ() + 0.5D;
        for (int i = 0; i < 18; i++) {
            double t = i / 17.0D;
            double x = sx + (tx - sx) * t;
            double y = sy + (ty - sy) * t;
            double z = sz + (tz - sz) * t;
            level.addParticle(ParticleTypes.ELECTRIC_SPARK, x, y, z, 0.0D, 0.015D, 0.0D);
        }
        level.addParticle(ParticleTypes.END_ROD, tx, ty + 0.2D, tz, 0.0D, 0.04D, 0.0D);
    }

    public static void handleDefinitionMetadata(MultiblockDefinitionMetadataPacket packet) {
        DEFINITIONS.clear();
        if (packet != null) {
            for (MultiblockDefinitionMetadataPacket.Entry entry : packet.entries()) {
                DEFINITIONS.put(entry.id(), entry);
            }
        }
    }

    public static void handleAutomationRecipeMetadata(AutomationRecipeMetadataPacket packet) {
        RECIPES.clear();
        if (packet != null) {
            for (AutomationRecipeMetadataPacket.Entry entry : packet.entries()) {
                RECIPES.put(entry.id(), entry);
            }
        }
    }

    public static void handleBuildAssistMetadata(MultiblockBuildAssistPacket packet) {
        BUILD_ASSIST.clear();
        if (packet != null) {
            for (MultiblockBuildAssistSnapshot snapshot : packet.snapshots()) {
                BUILD_ASSIST.put(snapshot.definitionId(), snapshot);
            }
        }
    }

    public static MultiblockDefinitionMetadataPacket.Entry metadata(Identifier id) {
        return DEFINITIONS.get(id);
    }

    public static AutomationRecipeMetadataPacket.Entry recipeMetadata(Identifier id) {
        return RECIPES.get(id);
    }

    public static List<AutomationRecipeMetadataPacket.Entry> recipeMetadataEntries() {
        return List.copyOf(RECIPES.values());
    }

    public static MultiblockBuildAssistSnapshot buildAssist(Identifier id) {
        return BUILD_ASSIST.get(id);
    }
}
