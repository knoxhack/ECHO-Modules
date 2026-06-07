package com.knoxhack.echoorbitalremnants.faction;

import com.knoxhack.echo.adaptercore.EchoBackendWorldEventBridge;
import com.knoxhack.echoorbitalremnants.entity.OrbitalFactionNpcEntity;
import com.knoxhack.echoorbitalremnants.item.FactionPledgeItem;
import com.knoxhack.echoorbitalremnants.progression.EchoTerminalProgress;
import com.knoxhack.echoorbitalremnants.registry.ModBlocks;
import com.knoxhack.echoorbitalremnants.registry.ModEntities;
import com.knoxhack.echoorbitalremnants.world.ModDimensions;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.AABB;

public final class OrbitalOutpostSpawner {
    private static final int TICK_INTERVAL = 300;
    private static final int SCAN_RADIUS = 14;
    private static final int NEARBY_RADIUS = 48;
    private static final int MAX_NEARBY_TOTAL = 3;
    private static final int MAX_NEARBY_FACTION = 1;

    private OrbitalOutpostSpawner() {
    }

    public static void onPlayerTick(Object event) {
        ServerPlayer player = EchoBackendWorldEventBridge.postTickServerPlayer(event);
        if (player == null || !(player.level() instanceof ServerLevel level)) {
            return;
        }
        long gameTime = level.getGameTime();
        if (gameTime % TICK_INTERVAL != Math.floorMod(player.getUUID().getLeastSignificantBits(), TICK_INTERVAL)) {
            return;
        }
        FactionPledgeItem.Faction faction = OrbitalOutpostProfiles.factionForDimension(level.dimension());
        if (faction == null) {
            return;
        }
        BlockPos anchor = nearbyOutpostAnchor(player, faction);
        if (anchor == null) {
            return;
        }
        if (countNearby(level, player, null) >= MAX_NEARBY_TOTAL || countNearby(level, player, faction) >= MAX_NEARBY_FACTION) {
            return;
        }
        EchoTerminalProgress progress = EchoTerminalProgress.get(player);
        String siteKey = siteKey(level, faction, anchor);
        if (progress.hasOutpostNpcSeeded(siteKey)) {
            return;
        }
        BlockPos spawnPos = findSpawnPos(level, anchor);
        if (spawnPos == null) {
            return;
        }
        OrbitalFactionNpcEntity npc = ModEntities.ORBITAL_FACTION_NPC.get().create(level, EntitySpawnReason.EVENT);
        if (npc == null) {
            return;
        }
        npc.configure(faction, OrbitalOutpostProfiles.roleId(faction));
        npc.setPos(spawnPos.getX() + 0.5D, spawnPos.getY(), spawnPos.getZ() + 0.5D);
        npc.setYRot(level.getRandom().nextFloat() * 360.0F);
        npc.setXRot(0.0F);
        npc.setPersistenceRequired();
        if (level.addFreshEntity(npc)) {
            progress.markOutpostNpcSeeded(player, siteKey);
        }
    }

    private static BlockPos nearbyOutpostAnchor(ServerPlayer player, FactionPledgeItem.Faction faction) {
        BlockPos center = player.blockPosition();
        for (BlockPos pos : BlockPos.betweenClosed(center.offset(-SCAN_RADIUS, -5, -SCAN_RADIUS),
                center.offset(SCAN_RADIUS, 6, SCAN_RADIUS))) {
            Block block = player.level().getBlockState(pos).getBlock();
            if (isOutpostAnchor(block, faction)) {
                return pos.immutable();
            }
        }
        return null;
    }

    private static boolean isOutpostAnchor(Block block, FactionPledgeItem.Faction faction) {
        if (block == ModBlocks.FACTION_RELAY_HUB.get() || block == ModBlocks.FACTION_VENDOR_KIOSK.get()) {
            return true;
        }
        return switch (faction) {
            case VOID_SALVAGERS -> block == ModBlocks.SATURN_RING_RELAY.get();
            case ORBITAL_REMNANT -> block == ModBlocks.TITAN_METHANE_PUMP.get();
            case NEXUS_CHOIR -> block == ModBlocks.NEXUS_ANCHOR.get() || block == ModBlocks.NEXUS_GROWTH.get();
        };
    }

    private static int countNearby(ServerLevel level, ServerPlayer player, FactionPledgeItem.Faction faction) {
        return level.getEntitiesOfClass(OrbitalFactionNpcEntity.class,
                new AABB(player.blockPosition()).inflate(NEARBY_RADIUS),
                npc -> !npc.isRemoved() && (faction == null || npc.faction() == faction)).size();
    }

    private static BlockPos findSpawnPos(ServerLevel level, BlockPos anchor) {
        for (int radius = 1; radius <= 5; radius++) {
            for (BlockPos pos : BlockPos.betweenClosed(anchor.offset(-radius, 0, -radius),
                    anchor.offset(radius, 2, radius))) {
                if (isSpawnable(level, pos)) {
                    return pos.immutable();
                }
            }
        }
        BlockPos above = anchor.above();
        return isSpawnable(level, above) ? above : null;
    }

    private static boolean isSpawnable(ServerLevel level, BlockPos pos) {
        return level.isLoaded(pos)
                && level.getBlockState(pos).isAir()
                && level.getBlockState(pos.above()).isAir()
                && level.getBlockState(pos.below()).canOcclude();
    }

    private static String siteKey(ServerLevel level, FactionPledgeItem.Faction faction, BlockPos anchor) {
        return level.dimension().identifier() + ":" + OrbitalOutpostProfiles.factionId(faction)
                + ":" + (anchor.getX() >> 4) + ":" + (anchor.getZ() >> 4);
    }
}
