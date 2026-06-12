package com.knoxhack.echoashfallprotocol.event;

import com.knoxhack.echoashfallprotocol.EchoAshfallProtocol;
import com.knoxhack.echoashfallprotocol.guardian.BiomeGuardianProfile;
import com.knoxhack.echoashfallprotocol.guardian.BiomeGuardianProfiles;
import com.knoxhack.echoashfallprotocol.registry.ModBlocks;
import com.knoxhack.echoashfallprotocol.world.BiomeGuardianSiteData;
import com.knoxhack.echoashfallprotocol.worldgen.ProceduralStructureGenerator;
import com.knoxhack.echoashfallprotocol.worldgen.StructureType;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.Permissions;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import java.util.Comparator;
import java.util.List;

/**
 * Debug command to place procedural POIs at the player location.
 * Usage: /genpoi <bio_lab|data_center|military_vault>
 * This helps testing until proper NBT files are created.
 */
public class StructureGenCommand {

    public static void onRegisterCommands(Object event) {
        Object dispatcher = eventValue(event, "getDispatcher");
        if (dispatcher instanceof CommandDispatcher<?> commandDispatcher) {
            registerUnchecked(commandDispatcher);
        }
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
            Commands.literal("genpoi")
                .requires(StructureGenCommand::hasCommandPermission)
                .then(Commands.literal("bio_lab")
                    .executes(ctx -> {
                        if (ctx.getSource().getEntity() instanceof ServerPlayer player) {
                            generateBioLab(player);
                            return 1;
                        }
                        return 0;
                    }))
                .then(Commands.literal("data_center")
                    .executes(ctx -> {
                        if (ctx.getSource().getEntity() instanceof ServerPlayer player) {
                            generateDataCenter(player);
                            return 1;
                        }
                        return 0;
                    }))
                .then(Commands.literal("military_vault")
                    .executes(ctx -> {
                        if (ctx.getSource().getEntity() instanceof ServerPlayer player) {
                            generateMilitaryVault(player);
                            return 1;
                        }
                        return 0;
                    }))
                .then(Commands.literal("reactor_ruin")
                    .executes(ctx -> {
                        if (ctx.getSource().getEntity() instanceof ServerPlayer player) {
                            generateReactorRuin(player);
                            return 1;
                        }
                        return 0;
                    }))
                // === FACTION HUBS WITH VANILLA HOUSE INTEGRATION ===
                .then(Commands.literal("radwarden_outpost")
                    .executes(ctx -> {
                        if (ctx.getSource().getEntity() instanceof ServerPlayer player) {
                            generateFactionHub(player, StructureType.RADWARDEN_OUTPOST, "Radwarden Outpost");
                            return 1;
                        }
                        return 0;
                    }))
                .then(Commands.literal("crashbreak_salvage")
                    .executes(ctx -> {
                        if (ctx.getSource().getEntity() instanceof ServerPlayer player) {
                            generateFactionHub(player, StructureType.CRASHBREAK_SALVAGE_YARD, "Crashbreak Salvage Yard");
                            return 1;
                        }
                        return 0;
                    }))
                .then(Commands.literal("sporebound_sanctum")
                    .executes(ctx -> {
                        if (ctx.getSource().getEntity() instanceof ServerPlayer player) {
                            generateFactionHub(player, StructureType.SPOREBOUND_SANCTUM, "Sporebound Sanctum");
                            return 1;
                        }
                        return 0;
                    }))
                .then(Commands.literal("procedural")
                    .then(Commands.argument("type", StringArgumentType.word())
                        .suggests((ctx, builder) -> SharedSuggestionProvider.suggest(
                                java.util.Arrays.stream(StructureType.values()).map(StructureType::getName),
                                builder))
                        .executes(ctx -> {
                            if (ctx.getSource().getEntity() instanceof ServerPlayer player) {
                                String typeName = StringArgumentType.getString(ctx, "type");
                                return generateProcedural(player, typeName);
                            }
                            return 0;
                        })))
                .then(Commands.literal("biome_main")
                    .then(Commands.argument("biome", StringArgumentType.word())
                        .suggests((ctx, builder) -> SharedSuggestionProvider.suggest(
                                ProceduralStructureGenerator.getProfileBiomes(),
                                builder))
                        .executes(ctx -> {
                            if (ctx.getSource().getEntity() instanceof ServerPlayer player) {
                                String biome = StringArgumentType.getString(ctx, "biome");
                                return generateBiomeMain(player, biome);
                            }
                            return 0;
                        })))
                .then(Commands.literal("guardian_sites")
                    .executes(ctx -> {
                        if (ctx.getSource().getEntity() instanceof ServerPlayer player) {
                            return listGuardianSites(player, null);
                        }
                        return 0;
                    })
                    .then(Commands.argument("guardian", StringArgumentType.word())
                        .suggests((ctx, builder) -> SharedSuggestionProvider.suggest(
                                BiomeGuardianProfiles.all().stream().map(BiomeGuardianProfile::bossPath),
                                builder))
                        .executes(ctx -> {
                            if (ctx.getSource().getEntity() instanceof ServerPlayer player) {
                                return listGuardianSites(player, StringArgumentType.getString(ctx, "guardian"));
                            }
                            return 0;
                        })))
        );
    }

    public static boolean hasCommandPermission(net.minecraft.commands.CommandSourceStack source) {
        return source != null && source.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER);
    }

    private static int generateProcedural(ServerPlayer player, String typeName) {
        StructureType type = StructureType.byName(typeName);
        if (type == null) {
            player.sendSystemMessage(Component.literal("§cUnknown procedural structure: " + typeName));
            return 0;
        }

        ServerLevel level = (ServerLevel) player.level();
        BlockPos center = player.blockPosition();
        ProceduralStructureGenerator.generateStructure(level, center, type, level.getRandom());
        player.sendSystemMessage(Component.literal("§a[PROCEDURAL]§r Generated " + type.getName() + " at " + center.toShortString()));
        return 1;
    }

    private static int generateBiomeMain(ServerPlayer player, String biomePath) {
        StructureType type = ProceduralStructureGenerator.getMainStructureForBiome(biomePath);
        if (type == null) {
            player.sendSystemMessage(Component.literal("§cUnknown biome profile: " + biomePath));
            return 0;
        }

        ServerLevel level = (ServerLevel) player.level();
        BlockPos center = player.blockPosition();
        BiomeGuardianProfile profile = BiomeGuardianProfiles.byBiome(biomePath).orElse(null);
        ProceduralStructureGenerator.generateStructure(level, center, type, level.getRandom(), biomePath);
        player.sendSystemMessage(Component.literal("§a[BIOME MAIN]§r Generated " + type.getName()
                + " for " + biomePath + " at " + center.toShortString()));
        sendGuardianGenerationSummary(player, level, profile, center);
        return 1;
    }

    private static void sendGuardianGenerationSummary(ServerPlayer player, ServerLevel level,
                                                      BiomeGuardianProfile profile, BlockPos origin) {
        if (profile == null) {
            return;
        }
        BiomeGuardianSiteData data = BiomeGuardianSiteData.get(level);
        data.nearestActive(origin, profile.bossPath()).ifPresentOrElse(site -> {
            int distance = (int) Math.sqrt(site.entrance().distSqr(origin));
            player.sendSystemMessage(Component.literal("[GUARDIAN QA] " + profile.title()
                    + " entrance " + site.entrance().toShortString()
                    + " | arena " + site.arena().toShortString()
                    + " | distance " + distance + "m"));
            player.sendSystemMessage(Component.literal("[GUARDIAN QA] Scanner mission "
                    + profile.missionId() + " will route to the saved surface entrance."));
        }, () -> player.sendSystemMessage(Component.literal("[GUARDIAN QA] No active saved site found for "
                + profile.title() + ". Check GuardianSiteReport logs for boss placement failure.")));
    }

    private static int listGuardianSites(ServerPlayer player, String guardianId) {
        ServerLevel level = (ServerLevel) player.level();
        String normalized = guardianId == null || guardianId.isBlank() ? null : guardianId;
        if (normalized != null && BiomeGuardianProfiles.byBossPath(normalized).isEmpty()) {
            player.sendSystemMessage(Component.literal("[GUARDIAN QA] Unknown guardian: " + normalized));
            return 0;
        }

        BlockPos origin = player.blockPosition();
        List<BiomeGuardianSiteData.Entry> sites = BiomeGuardianSiteData.get(level).allSites().stream()
                .filter(entry -> normalized == null || entry.guardianId().equals(normalized))
                .filter(entry -> !entry.defeated())
                .sorted(Comparator.comparingDouble(entry -> entry.entrance().distSqr(origin)))
                .limit(8)
                .toList();
        if (sites.isEmpty()) {
            player.sendSystemMessage(Component.literal("[GUARDIAN QA] No active saved guardian sites"
                    + (normalized == null ? "." : " for " + normalized + ".")));
            return 0;
        }

        player.sendSystemMessage(Component.literal("[GUARDIAN QA] Active saved guardian sites: " + sites.size()));
        for (BiomeGuardianSiteData.Entry site : sites) {
            String title = BiomeGuardianProfiles.byBossPath(site.guardianId())
                    .map(BiomeGuardianProfile::title)
                    .orElse(site.guardianId());
            int distance = (int) Math.sqrt(site.entrance().distSqr(origin));
            player.sendSystemMessage(Component.literal("- " + title
                    + " | entrance " + site.entrance().toShortString()
                    + " | arena " + site.arena().toShortString()
                    + " | " + distance + "m"));
        }
        return sites.size();
    }

    private static void generateFactionHub(ServerPlayer player, StructureType type, String name) {
        ServerLevel level = (ServerLevel) player.level();
        BlockPos center = player.blockPosition();

        EchoAshfallProtocol.LOGGER.info("[StructureGenCommand] Generating {} at {}", name, center);

        // Generate using the procedural generator
        ProceduralStructureGenerator.generateStructure(level, center, type, level.getRandom());

        player.sendSystemMessage(Component.literal("§a[" + name.toUpperCase() + "]§r Generated at " + center.toShortString()));
        player.sendSystemMessage(Component.literal("§7Check console logs for vanilla house placement details."));
    }

    private static void generateBioLab(ServerPlayer player) {
        ServerLevel level = (ServerLevel) player.level();
        BlockPos center = player.blockPosition();

        // Clear area
        for (int x = -8; x <= 8; x++) {
            for (int y = -1; y <= 4; y++) {
                for (int z = -8; z <= 8; z++) {
                    level.setBlockAndUpdate(center.offset(x, y, z), Blocks.AIR.defaultBlockState());
                }
            }
        }

        // Floor - contaminated soil
        for (int x = -7; x <= 7; x++) {
            for (int z = -7; z <= 7; z++) {
                level.setBlockAndUpdate(center.offset(x, -1, z), ModBlocks.CONTAMINATED_SOIL.get().defaultBlockState());
            }
        }

        // Walls - concrete rubble
        for (int x = -7; x <= 7; x++) {
            for (int z = -7; z <= 7; z++) {
                if (Math.abs(x) == 7 || Math.abs(z) == 7) {
                    for (int y = 0; y <= 3; y++) {
                        if (level.getRandom().nextFloat() > 0.3f) {
                            level.setBlockAndUpdate(center.offset(x, y, z), ModBlocks.CONCRETE_RUBBLE.get().defaultBlockState());
                        }
                    }
                }
            }
        }

        // Containment cells - Echo glass and radiation blocks
        level.setBlockAndUpdate(center.offset(-4, 0, -4), ModBlocks.SHATTERED_GLASS.get().defaultBlockState());
        level.setBlockAndUpdate(center.offset(-4, 1, -4), ModBlocks.SHATTERED_GLASS.get().defaultBlockState());
        level.setBlockAndUpdate(center.offset(-4, 0, -3), ModBlocks.RADIATION_BLOCK.get().defaultBlockState());

        placeLootCache(level, center.offset(0, 0, 0), "chests/bio_lab_cache");

        player.sendSystemMessage(Component.literal("§a[BIO LAB]§r Generated at " + center.toShortString()));
    }

    private static void generateDataCenter(ServerPlayer player) {
        ServerLevel level = (ServerLevel) player.level();
        BlockPos center = player.blockPosition();

        // Clear area
        for (int x = -10; x <= 10; x++) {
            for (int y = -1; y <= 5; y++) {
                for (int z = -8; z <= 8; z++) {
                    level.setBlockAndUpdate(center.offset(x, y, z), Blocks.AIR.defaultBlockState());
                }
            }
        }

        // Floor
        for (int x = -9; x <= 9; x++) {
            for (int z = -7; z <= 7; z++) {
                level.setBlockAndUpdate(center.offset(x, -1, z), ModBlocks.ASH_STONE.get().defaultBlockState());
            }
        }

        // Server rack rows - ore grinder blocks as server racks
        for (int x = -6; x <= 6; x += 3) {
            for (int z = -5; z <= 5; z++) {
                level.setBlockAndUpdate(center.offset(x, 0, z), ModBlocks.ORE_GRINDER.get().defaultBlockState());
                level.setBlockAndUpdate(center.offset(x, 1, z), ModBlocks.ORE_GRINDER.get().defaultBlockState());
            }
        }

        // Damaged walls - debris blocks with gaps
        for (int x = -9; x <= 9; x++) {
            for (int z = -7; z <= 7; z++) {
                if (Math.abs(x) == 9 || Math.abs(z) == 7) {
                    for (int y = 0; y <= 4; y++) {
                        if (level.getRandom().nextFloat() > 0.4f) {
                            level.setBlockAndUpdate(center.offset(x, y, z), ModBlocks.DEBRIS_BLOCK.get().defaultBlockState());
                        }
                    }
                }
            }
        }

        placeLootCache(level, center.offset(0, 0, 0), "chests/data_center_cache");

        player.sendSystemMessage(Component.literal("§a[DATA CENTER]§r Generated at " + center.toShortString()));
    }

    private static void generateMilitaryVault(ServerPlayer player) {
        ServerLevel level = (ServerLevel) player.level();
        BlockPos center = player.blockPosition().below(3); // Partially underground

        // Clear underground area
        for (int x = -7; x <= 7; x++) {
            for (int y = -3; y <= 3; y++) {
                for (int z = -7; z <= 7; z++) {
                    level.setBlockAndUpdate(center.offset(x, y, z), Blocks.AIR.defaultBlockState());
                }
            }
        }

        // Vault walls - rusted metal (strong)
        for (int x = -7; x <= 7; x++) {
            for (int z = -7; z <= 7; z++) {
                if (Math.abs(x) == 7 || Math.abs(z) == 7) {
                    for (int y = -3; y <= 3; y++) {
                        level.setBlockAndUpdate(center.offset(x, y, z), ModBlocks.RUSTED_METAL_SHEET.get().defaultBlockState());
                    }
                }
            }
        }

        // Floor and ceiling
        for (int x = -6; x <= 6; x++) {
            for (int z = -6; z <= 6; z++) {
                level.setBlockAndUpdate(center.offset(x, -3, z), ModBlocks.NEXUS_SCAR_STONE.get().defaultBlockState());
                level.setBlockAndUpdate(center.offset(x, 3, z), ModBlocks.RUSTED_METAL_SHEET.get().defaultBlockState());
            }
        }

        // Radiation hazard in corner
        level.setBlockAndUpdate(center.offset(5, -2, 5), ModBlocks.RADIATION_BLOCK.get().defaultBlockState());
        level.setBlockAndUpdate(center.offset(5, -2, 4), ModBlocks.RADIATION_BLOCK.get().defaultBlockState());
        level.setBlockAndUpdate(center.offset(4, -2, 5), ModBlocks.RADIATION_BLOCK.get().defaultBlockState());

        // Equipment - battery bank and power node
        level.setBlockAndUpdate(center.offset(-5, -2, -5), ModBlocks.BATTERY_BANK.get().defaultBlockState());
        level.setBlockAndUpdate(center.offset(-3, -2, -5), ModBlocks.POWER_NODE.get().defaultBlockState());

        placeLootCache(level, center.offset(0, -2, 0), "chests/military_vault_cache");
        placeLootCache(level, center.offset(3, -2, 3), "chests/military_vault_cache");

        // Entrance tunnel
        for (int z = 7; z <= 12; z++) {
            for (int x = -2; x <= 2; x++) {
                for (int y = -3; y <= 2; y++) {
                    if (Math.abs(x) == 2 || y == -3 || y == 2) {
                        level.setBlockAndUpdate(center.offset(x, y, z), ModBlocks.CONCRETE_RUBBLE.get().defaultBlockState());
                    } else {
                        level.setBlockAndUpdate(center.offset(x, y, z), Blocks.AIR.defaultBlockState());
                    }
                }
            }
        }

        player.sendSystemMessage(Component.literal("§a[MILITARY VAULT]§r Generated at " + center.toShortString()));
    }

    private static void generateReactorRuin(ServerPlayer player) {
        ServerLevel level = (ServerLevel) player.level();
        BlockPos center = player.blockPosition().below(8); // Deep underground

        // Clear large underground area
        for (int x = -12; x <= 12; x++) {
            for (int y = -8; y <= 6; y++) {
                for (int z = -12; z <= 12; z++) {
                    level.setBlockAndUpdate(center.offset(x, y, z), Blocks.AIR.defaultBlockState());
                }
            }
        }

        // Reactor chamber floor - nexus_scar_stone (melted/reinforced)
        for (int x = -10; x <= 10; x++) {
            for (int z = -10; z <= 10; z++) {
                level.setBlockAndUpdate(center.offset(x, -8, z), ModBlocks.NEXUS_SCAR_STONE.get().defaultBlockState());
            }
        }

        // Walls - concrete rubble with gaps
        for (int x = -10; x <= 10; x++) {
            for (int z = -10; z <= 10; z++) {
                if (Math.abs(x) == 10 || Math.abs(z) == 10) {
                    for (int y = -8; y <= 5; y++) {
                        if (level.getRandom().nextFloat() > 0.2f) {
                            level.setBlockAndUpdate(center.offset(x, y, z), ModBlocks.CONCRETE_RUBBLE.get().defaultBlockState());
                        }
                    }
                }
            }
        }

        // Central reactor core - heavy radiation
        for (int x = -2; x <= 2; x++) {
            for (int z = -2; z <= 2; z++) {
                for (int y = -7; y <= -4; y++) {
                    level.setBlockAndUpdate(center.offset(x, y, z), ModBlocks.RADIATION_BLOCK.get().defaultBlockState());
                }
            }
        }

        // Scattered debris
        for (int i = 0; i < 30; i++) {
            int x = level.getRandom().nextInt(18) - 9;
            int z = level.getRandom().nextInt(18) - 9;
            int y = level.getRandom().nextInt(10) - 7;
            if (level.getRandom().nextFloat() > 0.5f) {
                level.setBlockAndUpdate(center.offset(x, y, z), ModBlocks.DEBRIS_BLOCK.get().defaultBlockState());
            }
        }

        placeLootCache(level, center.offset(-8, -7, -8), "chests/reactor_ruin_cache");
        placeLootCache(level, center.offset(8, -7, 8), "chests/reactor_ruin_cache");

        player.sendSystemMessage(Component.literal("§a[REACTOR RUIN]§r Generated at " + center.toShortString()));
    }

    private static void placeLootCache(ServerLevel level, BlockPos pos, String lootTablePath) {
        BlockState state = ModBlocks.STRUCTURE_CACHE.get().defaultBlockState();
        level.setBlockAndUpdate(pos, state);
        if (level.getBlockEntity(pos) instanceof RandomizableContainerBlockEntity cache) {
            Identifier lootTableId = Identifier.tryParse(EchoAshfallProtocol.MODID + ":" + lootTablePath);
            if (lootTableId != null) {
                cache.setLootTable(ResourceKey.create(Registries.LOOT_TABLE, lootTableId), level.getRandom().nextLong());
                cache.setChanged();
                level.sendBlockUpdated(pos, state, state, 2);
            }
        }
    }

    @SuppressWarnings("unchecked")
    private static void registerUnchecked(CommandDispatcher<?> dispatcher) {
        register((CommandDispatcher<CommandSourceStack>) dispatcher);
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
