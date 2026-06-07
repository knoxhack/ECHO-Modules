package com.knoxhack.echoashfallprotocol.endgame;

import com.knoxhack.echoashfallprotocol.dimension.ModDimensions;
import com.knoxhack.echoashfallprotocol.echo.QuestData;
import com.knoxhack.echoashfallprotocol.entity.ModEntities;
import com.knoxhack.echoashfallprotocol.entity.boss.WardenBossEntity;
import com.knoxhack.echoashfallprotocol.registry.ModItems;
import com.knoxhack.echoashfallprotocol.world.NexusWorldData;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.Relative;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Server-only helpers for entering, maintaining, and leaving the Pre-Fall Archives arena.
 */
public final class PrefallArchivesArenaService {
    public static final ResourceKey<Level> PREFALL_ARCHIVES_LEVEL = ModDimensions.PREFALL_ARCHIVES;

    public static final BlockPos ARENA_CENTER = new BlockPos(0, 71, 0);
    public static final BlockPos ENTRY_POS = new BlockPos(0, 71, 16);
    public static final BlockPos WARDEN_POS = new BlockPos(0, 71, -8);
    public static final int ARENA_RADIUS = 18;
    public static final int WALL_RADIUS = 21;
    public static final AABB ARENA_BOX = new AABB(-32, 60, -32, 32, 90, 32);

    private static final int ARENA_CLEAR_HEIGHT = 8;
    private static final int WALL_HEIGHT = 5;
    private static final double ACTIVE_PLAYER_RADIUS = 34.0D;

    private static final Set<PostNexusData.NexusPath> SUPPORTED_PATHS = Set.of(
            PostNexusData.NexusPath.RESTORE,
            PostNexusData.NexusPath.DESTROY,
            PostNexusData.NexusPath.CONTROL);

    private PrefallArchivesArenaService() {}

    public record ArenaReport(
            int wardenCount,
            int activePlayerCount,
            int duplicateWardenCount,
            boolean activeFight,
            boolean shellReady,
            boolean ready) {}

    private record ReturnTarget(ServerLevel level, Vec3 position) {}

    public static InteractionResult enter(ServerPlayer player, ItemStack keyStack) {
        ServerLevel currentLevel = (ServerLevel) player.level();
        PostNexusData post = PostNexusData.get(player);
        if (!post.hasMadeChoice()) {
            inferPath(currentLevel).ifPresent(post::setSelectedPath);
        }
        if (!post.hasMadeChoice()) {
            tell(player, Component.translatable("message.echoashfallprotocol.prefall_archives.locked")
                    .withStyle(ChatFormatting.RED));
            return InteractionResult.FAIL;
        }
        if (ModDimensions.isPrefallArchives(currentLevel)) {
            tell(player, Component.translatable("message.echoashfallprotocol.prefall_archives.already_inside")
                    .withStyle(ChatFormatting.YELLOW));
            return InteractionResult.FAIL;
        }
        if (currentLevel.dimension() != Level.OVERWORLD) {
            tell(player, Component.translatable("message.echoashfallprotocol.prefall_archives.overworld_only")
                    .withStyle(ChatFormatting.YELLOW));
            return InteractionResult.FAIL;
        }
        if (!SUPPORTED_PATHS.contains(post.getSelectedPath())) {
            inferPath(currentLevel).ifPresent(post::setSelectedPath);
        }
        if (!SUPPORTED_PATHS.contains(post.getSelectedPath())) {
            tell(player, Component.translatable("message.echoashfallprotocol.prefall_archives.path_missing")
                    .withStyle(ChatFormatting.RED));
            return InteractionResult.FAIL;
        }

        ServerLevel archivesLevel = player.level().getServer().getLevel(PREFALL_ARCHIVES_LEVEL);
        if (archivesLevel == null) {
            tell(player, Component.translatable("message.echoashfallprotocol.prefall_archives.dimension_missing")
                    .withStyle(ChatFormatting.RED));
            return InteractionResult.FAIL;
        }

        Vec3 returnPoint = player.position();
        post.setArchivesReturnPoint(
                "minecraft:overworld",
                returnPoint.x(),
                returnPoint.y(),
                returnPoint.z());
        PostNexusData.saveAndSync(player, post);

        ArenaReport before = inspectArena(archivesLevel);
        if (before.activeFight()) {
            repairArenaShell(archivesLevel, post.getSelectedPath());
            cleanupDuplicateWardens(archivesLevel);
        } else {
            removeAllWardens(archivesLevel);
            prepareArena(archivesLevel, post.getSelectedPath());
        }

        if (!post.isWardenDefeated()) {
            spawnWardenIfMissing(archivesLevel);
        }
        ensureReturnKeystone(player);

        BlockPos entry = ensureSafeArenaEntry(archivesLevel);
        player.teleportTo(archivesLevel, entry.getX() + 0.5D, entry.getY(), entry.getZ() + 0.5D,
                Set.of(), player.getYRot(), player.getXRot(), false);
        archivesLevel.playSound(null, entry, SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 0.8F, 0.7F);
        if (!player.getAbilities().instabuild) {
            keyStack.shrink(1);
        }

        post.setArchivesEntered(true);
        PostNexusData.saveAndSync(player, post);
        QuestData quest = QuestData.get(player);
        quest.visitLocation("dimension", "echoashfallprotocol:prefall_archives");
        quest.repairMissionState(player);
        QuestData.saveAndSync(player, quest);
        tell(player, entryMessage(post.getSelectedPath()).withStyle(ChatFormatting.AQUA));
        return InteractionResult.SUCCESS;
    }

    public static InteractionResult returnToSavedPoint(ServerPlayer player, ItemStack keystoneStack) {
        if (!ModDimensions.isPrefallArchives(player.level())) {
            tell(player, Component.translatable("message.echoashfallprotocol.prefall_archives.return_wrong_dimension")
                    .withStyle(ChatFormatting.YELLOW));
            return InteractionResult.FAIL;
        }

        PostNexusData post = PostNexusData.get(player);
        ReturnTarget target = resolveReturnTarget(player, post);
        player.teleportTo(target.level(), target.position().x(), target.position().y(), target.position().z(),
                Set.of(), player.getYRot(), player.getXRot(), false);
        target.level().playSound(null, BlockPos.containing(target.position()), SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 0.8F, 1.2F);
        if (!player.getAbilities().instabuild) {
            keystoneStack.shrink(1);
        }

        post.clearArchivesReturnPoint();
        PostNexusData.saveAndSync(player, post);
        tell(player, Component.translatable("message.echoashfallprotocol.prefall_archives.returned")
                .withStyle(ChatFormatting.GREEN));
        return InteractionResult.SUCCESS;
    }

    public static void prepareArena(ServerLevel level, PostNexusData.NexusPath path) {
        int floorY = ARENA_CENTER.getY() - 1;
        for (int x = -WALL_RADIUS; x <= WALL_RADIUS; x++) {
            for (int z = -WALL_RADIUS; z <= WALL_RADIUS; z++) {
                double dist = Math.sqrt(x * x + z * z);
                BlockPos base = new BlockPos(ARENA_CENTER.getX() + x, floorY, ARENA_CENTER.getZ() + z);
                if (dist <= ARENA_RADIUS) {
                    level.setBlock(base, floorBlock(path), 3);
                    for (int y = 1; y <= ARENA_CLEAR_HEIGHT; y++) {
                        level.setBlock(base.above(y), Blocks.AIR.defaultBlockState(), 3);
                    }
                } else if (dist <= WALL_RADIUS) {
                    level.setBlock(base, Blocks.BEDROCK.defaultBlockState(), 3);
                    for (int y = 1; y <= WALL_HEIGHT; y++) {
                        level.setBlock(base.above(y), wallBlock(path), 3);
                    }
                }
            }
        }
        buildPillars(level, path);
        buildArchiveDefenderLanes(level, path);
        level.setBlock(ARENA_CENTER.below(), centerBlock(path), 3);
        level.setBlock(ARENA_CENTER, Blocks.BEACON.defaultBlockState(), 3);
        ensureSafeArenaEntry(level);
    }

    public static void repairArenaShell(ServerLevel level, PostNexusData.NexusPath path) {
        int floorY = ARENA_CENTER.getY() - 1;
        for (int x = -WALL_RADIUS; x <= WALL_RADIUS; x++) {
            for (int z = -WALL_RADIUS; z <= WALL_RADIUS; z++) {
                double dist = Math.sqrt(x * x + z * z);
                BlockPos base = new BlockPos(ARENA_CENTER.getX() + x, floorY, ARENA_CENTER.getZ() + z);
                if (dist <= ARENA_RADIUS && level.getBlockState(base).isAir()) {
                    level.setBlock(base, floorBlock(path), 3);
                } else if (dist > ARENA_RADIUS && dist <= WALL_RADIUS) {
                    if (level.getBlockState(base).isAir()) {
                        level.setBlock(base, Blocks.BEDROCK.defaultBlockState(), 3);
                    }
                    for (int y = 1; y <= WALL_HEIGHT; y++) {
                        BlockPos wall = base.above(y);
                        if (level.getBlockState(wall).isAir()) {
                            level.setBlock(wall, wallBlock(path), 3);
                        }
                    }
                }
            }
        }
        buildPillars(level, path);
        buildArchiveDefenderLanes(level, path);
        if (level.getBlockState(ARENA_CENTER.below()).isAir()) {
            level.setBlock(ARENA_CENTER.below(), centerBlock(path), 3);
        }
        if (level.getBlockState(ARENA_CENTER).isAir()) {
            level.setBlock(ARENA_CENTER, Blocks.BEACON.defaultBlockState(), 3);
        }
        ensureSafeArenaEntry(level);
    }

    public static int resetArena(ServerLevel level, PostNexusData.NexusPath path, boolean spawnWarden) {
        int removed = removeAllWardens(level);
        prepareArena(level, path);
        if (spawnWarden) {
            spawnWardenIfMissing(level);
        }
        return removed;
    }

    public static boolean spawnWardenIfMissing(ServerLevel level) {
        cleanupDuplicateWardens(level);
        if (!getLivingWardensInArena(level).isEmpty()) {
            return false;
        }
        WardenBossEntity warden = ModEntities.WARDEN_BOSS.get().create(level, EntitySpawnReason.EVENT);
        if (warden == null) {
            return false;
        }
        BlockPos spawn = safeWardenSpawn(level);
        warden.setPos(spawn.getX() + 0.5D, spawn.getY(), spawn.getZ() + 0.5D);
        warden.setYRot(180.0F);
        warden.setXRot(0.0F);
        warden.setPersistenceRequired();
        level.addFreshEntity(warden);
        return true;
    }

    public static int cleanupDuplicateWardens(ServerLevel level) {
        List<WardenBossEntity> wardens = getLivingWardensInArena(level);
        if (wardens.size() <= 1) {
            return 0;
        }
        wardens.sort(Comparator.comparingDouble(WardenBossEntity::getHealth).reversed());
        int removed = 0;
        for (int i = 1; i < wardens.size(); i++) {
            wardens.get(i).discard();
            removed++;
        }
        return removed;
    }

    public static int removeAllWardens(ServerLevel level) {
        int removed = 0;
        for (WardenBossEntity warden : getWardensInArena(level)) {
            warden.discard();
            removed++;
        }
        return removed;
    }

    public static int getWardenCount(ServerLevel level) {
        return getLivingWardensInArena(level).size();
    }

    public static int getActivePlayerCount(ServerLevel level) {
        return getActivePlayersInArena(level).size();
    }

    public static ArenaReport inspectArena(ServerLevel level) {
        int wardens = getLivingWardensInArena(level).size();
        int players = getActivePlayersInArena(level).size();
        int duplicates = Math.max(0, wardens - 1);
        boolean activeFight = wardens > 0 && players > 0;
        boolean shellReady = isArenaShellReady(level);
        return new ArenaReport(wardens, players, duplicates, activeFight, shellReady, shellReady && duplicates == 0);
    }

    public static boolean isInsideArena(Entity entity) {
        return ARENA_BOX.contains(entity.position());
    }

    public static Vec3 clampInsideArena(double x, double y, double z) {
        double centerX = ARENA_CENTER.getX() + 0.5D;
        double centerZ = ARENA_CENTER.getZ() + 0.5D;
        double dx = x - centerX;
        double dz = z - centerZ;
        double maxRadius = ARENA_RADIUS - 2.0D;
        double distSqr = dx * dx + dz * dz;
        if (distSqr > maxRadius * maxRadius) {
            double dist = Math.sqrt(distSqr);
            x = centerX + dx / dist * maxRadius;
            z = centerZ + dz / dist * maxRadius;
        }
        double clampedY = Math.max(ARENA_CENTER.getY(), Math.min(y, ARENA_CENTER.getY() + 4.0D));
        return new Vec3(x, clampedY, z);
    }

    public static BlockPos safeWardenSpawn(ServerLevel level) {
        ensureClearLanding(level, WARDEN_POS, centerBlock(PostNexusData.NexusPath.RESTORE));
        return WARDEN_POS;
    }

    private static List<WardenBossEntity> getWardensInArena(ServerLevel level) {
        return level.getEntitiesOfClass(WardenBossEntity.class, ARENA_BOX, Entity::isAlive);
    }

    private static List<WardenBossEntity> getLivingWardensInArena(ServerLevel level) {
        return level.getEntitiesOfClass(WardenBossEntity.class, ARENA_BOX, WardenBossEntity::isAlive);
    }

    private static List<ServerPlayer> getActivePlayersInArena(ServerLevel level) {
        return level.getEntitiesOfClass(ServerPlayer.class, ARENA_BOX, player -> player.isAlive()
                && player.distanceToSqr(Vec3.atCenterOf(ARENA_CENTER)) <= ACTIVE_PLAYER_RADIUS * ACTIVE_PLAYER_RADIUS);
    }

    private static boolean isArenaShellReady(ServerLevel level) {
        return !level.getBlockState(ARENA_CENTER.below()).isAir()
                && !level.getBlockState(ENTRY_POS.below()).isAir()
                && !level.getBlockState(WARDEN_POS.below()).isAir();
    }

    private static BlockPos ensureSafeArenaEntry(ServerLevel level) {
        ensureClearLanding(level, ENTRY_POS, floorBlock(PostNexusData.NexusPath.RESTORE));
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                ensureClearLanding(level, ENTRY_POS.offset(dx, 0, dz), floorBlock(PostNexusData.NexusPath.RESTORE));
            }
        }
        level.setBlock(ENTRY_POS.above(2), Blocks.SEA_LANTERN.defaultBlockState(), 3);
        return ENTRY_POS;
    }

    private static void ensureClearLanding(ServerLevel level, BlockPos pos, net.minecraft.world.level.block.state.BlockState floorState) {
        if (level.getBlockState(pos.below()).isAir()) {
            level.setBlock(pos.below(), floorState, 3);
        }
        level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
        level.setBlock(pos.above(), Blocks.AIR.defaultBlockState(), 3);
    }

    private static ReturnTarget resolveReturnTarget(ServerPlayer player, PostNexusData post) {
        if (post.hasArchivesReturnPoint()) {
            ServerLevel savedLevel = resolveReturnLevel(player, post);
            if (savedLevel != null && !ModDimensions.isPrefallArchives(savedLevel)) {
                return new ReturnTarget(savedLevel, new Vec3(
                        post.getArchivesReturnX(),
                        post.getArchivesReturnY(),
                        post.getArchivesReturnZ()));
            }
        }
        ServerLevel overworld = player.level().getServer().getLevel(Level.OVERWORLD);
        if (overworld == null) {
            return new ReturnTarget((ServerLevel) player.level(), player.position());
        }
        return new ReturnTarget(overworld, safeSpawnPosition(overworld));
    }

    private static ServerLevel resolveReturnLevel(ServerPlayer player, PostNexusData post) {
        if (post.hasArchivesReturnPoint()) {
            Identifier id = Identifier.tryParse(post.getArchivesReturnDimension());
            if (id != null) {
                ResourceKey<Level> key = ResourceKey.create(Registries.DIMENSION, id);
                ServerLevel level = player.level().getServer().getLevel(key);
                if (level != null && !ModDimensions.isPrefallArchives(level)) {
                    return level;
                }
            }
        }
        return player.level().getServer().getLevel(Level.OVERWORLD);
    }

    private static Vec3 safeSpawnPosition(ServerLevel level) {
        BlockPos spawn = BlockPos.ZERO;
        BlockPos surface = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, spawn);
        return new Vec3(surface.getX() + 0.5D, surface.getY(), surface.getZ() + 0.5D);
    }

    private static void buildPillars(ServerLevel level, PostNexusData.NexusPath path) {
        int[][] offsets = {{14, 14}, {-14, 14}, {14, -14}, {-14, -14}};
        for (int[] offset : offsets) {
            BlockPos base = ARENA_CENTER.offset(offset[0], 0, offset[1]);
            for (int y = 0; y < 5; y++) {
                level.setBlock(base.above(y), wallBlock(path), 3);
            }
            level.setBlock(base.above(5), Blocks.LANTERN.defaultBlockState(), 3);
        }
    }

    private static void buildArchiveDefenderLanes(ServerLevel level, PostNexusData.NexusPath path) {
        int floorY = ARENA_CENTER.getY();
        for (int i = -12; i <= 12; i++) {
            if (Math.abs(i) <= 2) {
                continue;
            }
            BlockPos eastLane = new BlockPos(ARENA_CENTER.getX() + 8, floorY, ARENA_CENTER.getZ() + i);
            BlockPos westLane = new BlockPos(ARENA_CENTER.getX() - 8, floorY, ARENA_CENTER.getZ() + i);
            level.setBlock(eastLane.below(), laneBlock(path), 3);
            level.setBlock(westLane.below(), laneBlock(path), 3);
            if (i % 4 == 0) {
                level.setBlock(eastLane.above(), Blocks.IRON_BARS.defaultBlockState(), 3);
                level.setBlock(westLane.above(), Blocks.IRON_BARS.defaultBlockState(), 3);
                level.setBlock(new BlockPos(ARENA_CENTER.getX() + i, floorY + 1, ARENA_CENTER.getZ() + 8),
                        Blocks.IRON_BARS.defaultBlockState(), 3);
                level.setBlock(new BlockPos(ARENA_CENTER.getX() + i, floorY + 1, ARENA_CENTER.getZ() - 8),
                        Blocks.IRON_BARS.defaultBlockState(), 3);
            }
        }
        level.setBlock(ARENA_CENTER.offset(0, 0, 11), Blocks.REDSTONE_LAMP.defaultBlockState(), 3);
        level.setBlock(ARENA_CENTER.offset(0, 0, -11), Blocks.REDSTONE_LAMP.defaultBlockState(), 3);
    }

    private static net.minecraft.world.level.block.state.BlockState floorBlock(PostNexusData.NexusPath path) {
        return switch (path) {
            case RESTORE -> Blocks.POLISHED_DEEPSLATE.defaultBlockState();
            case DESTROY -> Blocks.CRACKED_DEEPSLATE_TILES.defaultBlockState();
            case CONTROL -> Blocks.SMOOTH_BASALT.defaultBlockState();
            default -> Blocks.DEEPSLATE.defaultBlockState();
        };
    }

    private static net.minecraft.world.level.block.state.BlockState wallBlock(PostNexusData.NexusPath path) {
        return switch (path) {
            case RESTORE -> Blocks.DEEPSLATE_BRICKS.defaultBlockState();
            case DESTROY -> Blocks.RED_NETHER_BRICKS.defaultBlockState();
            case CONTROL -> Blocks.PURPUR_BLOCK.defaultBlockState();
            default -> Blocks.DEEPSLATE_TILES.defaultBlockState();
        };
    }

    private static net.minecraft.world.level.block.state.BlockState centerBlock(PostNexusData.NexusPath path) {
        return switch (path) {
            case RESTORE -> Blocks.LAPIS_BLOCK.defaultBlockState();
            case DESTROY -> Blocks.REDSTONE_BLOCK.defaultBlockState();
            case CONTROL -> Blocks.AMETHYST_BLOCK.defaultBlockState();
            default -> Blocks.OBSIDIAN.defaultBlockState();
        };
    }

    private static net.minecraft.world.level.block.state.BlockState laneBlock(PostNexusData.NexusPath path) {
        return switch (path) {
            case RESTORE -> Blocks.LAPIS_BLOCK.defaultBlockState();
            case DESTROY -> Blocks.REDSTONE_BLOCK.defaultBlockState();
            case CONTROL -> Blocks.PURPUR_PILLAR.defaultBlockState();
            default -> Blocks.DEEPSLATE_TILES.defaultBlockState();
        };
    }

    private static java.util.Optional<PostNexusData.NexusPath> inferPath(ServerLevel level) {
        NexusWorldData world = NexusWorldData.get(level);
        return switch (world.getState()) {
            case RESTORED -> java.util.Optional.of(PostNexusData.NexusPath.RESTORE);
            case DESTROYED -> java.util.Optional.of(PostNexusData.NexusPath.DESTROY);
            case CONTROLLED -> java.util.Optional.of(PostNexusData.NexusPath.CONTROL);
            default -> java.util.Optional.empty();
        };
    }

    private static void ensureReturnKeystone(Player player) {
        if (player.getInventory().contains(new ItemStack(ModItems.RETURN_KEYSTONE.get()))) {
            return;
        }
        player.addItem(new ItemStack(ModItems.RETURN_KEYSTONE.get()));
    }

    private static net.minecraft.network.chat.MutableComponent entryMessage(PostNexusData.NexusPath path) {
        return switch (path) {
            case RESTORE -> Component.translatable("message.echoashfallprotocol.prefall_archives.enter_restore");
            case DESTROY -> Component.translatable("message.echoashfallprotocol.prefall_archives.enter_destroy");
            case CONTROL -> Component.translatable("message.echoashfallprotocol.prefall_archives.enter_control");
            default -> Component.translatable("message.echoashfallprotocol.prefall_archives.enter");
        };
    }

    private static void tell(ServerPlayer player, Component component) {
        player.sendSystemMessage(component);
    }
}
