package com.knoxhack.echostationfall.world;

import com.knoxhack.echostationfall.EchoStationfall;
import com.knoxhack.echostationfall.progression.StationSection;
import com.knoxhack.echostationfall.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootTable;

public final class StationfallStationGenerator {
    public static final ResourceKey<LootTable> POWER_CACHE =
            loot("chests/station_salvage_power");
    public static final ResourceKey<LootTable> COMMON_CACHE =
            loot("chests/station_salvage_common");
    public static final ResourceKey<LootTable> COMMAND_CACHE =
            loot("chests/station_salvage_command");

    private static final int Y = 95;
    private static final int HX = 16;
    private static final int HZ = 10;

    private StationfallStationGenerator() {
    }

    public static void generate(ServerLevel level) {
        for (StationSection section : StationSection.values()) {
            section(level, section);
            if (section.next() != null) {
                connector(level, section);
            }
        }
    }

    public static void repairLighting(ServerLevel level) {
        for (StationSection section : StationSection.values()) {
            sectionLights(level, section);
            if (section.next() != null) {
                connectorLights(level, section);
            }
        }
    }

    public static BlockPos sectionLightPos(StationSection section) {
        return new BlockPos(section.centerX(), Y + 6, 0);
    }

    public static BlockPos connectorLightPos(StationSection section) {
        return new BlockPos(section.centerX() + 20, Y + 4, 0);
    }

    public static BlockPos powerCachePos(StationSection section) {
        return new BlockPos(section.centerX() - 4, Y + 1, -6);
    }

    public static BlockPos commonCachePos(StationSection section) {
        return new BlockPos(section.centerX() + 10, Y + 1, 6);
    }

    public static BlockPos commandCachePos() {
        return new BlockPos(StationSection.COMMAND_MODULE.centerX() - 10, Y + 1, 6);
    }

    private static void section(ServerLevel level, StationSection section) {
        BlockPos center = section.center();
        BlockState floor = ModBlocks.STATIONFALL_PLATING.get().defaultBlockState();
        BlockState wall = ModBlocks.STATIONFALL_WALL_PANEL.get().defaultBlockState();

        for (int x = center.getX() - HX; x <= center.getX() + HX; x++) {
            for (int z = -HZ; z <= HZ; z++) {
                level.setBlock(new BlockPos(x, Y, z), floor, 2);
                level.setBlock(new BlockPos(x, Y + 6, z), wall, 2);
                for (int y = Y + 1; y <= Y + 5; y++) {
                    boolean boundary = z == -HZ || z == HZ || x == center.getX() - HX || x == center.getX() + HX;
                    level.setBlock(new BlockPos(x, y, z), boundary ? wall : Blocks.AIR.defaultBlockState(), 2);
                }
            }
        }

        level.setBlock(section.powerNodePos(), ModBlocks.STATION_POWER_NODE.get().defaultBlockState(), 2);
        level.setBlock(section.logTerminalPos(), ModBlocks.CREW_LOG_TERMINAL.get().defaultBlockState(), 2);
        level.setBlock(new BlockPos(center.getX() + 6, Y + 1, -HZ), ModBlocks.HULL_BREACH.get().defaultBlockState(), 2);
        cache(level, powerCachePos(section), POWER_CACHE);
        cache(level, commonCachePos(section), COMMON_CACHE);

        if (section == StationSection.DATA_CORE) {
            level.setBlock(new BlockPos(center.getX(), Y + 1, 6), ModBlocks.DATA_CORE_TERMINAL.get().defaultBlockState(), 2);
        }
        if (section == StationSection.COMMAND_MODULE) {
            level.setBlock(new BlockPos(center.getX(), Y + 1, 0), ModBlocks.COMMAND_CONSOLE.get().defaultBlockState(), 2);
            cache(level, commandCachePos(), COMMAND_CACHE);
        }
        if (section == StationSection.HYDROPONICS_BAY) {
            for (int x = center.getX() - 8; x <= center.getX() + 8; x += 4) {
                level.setBlock(new BlockPos(x, Y + 1, 4), ModBlocks.CORRUPTED_HYDROPONIC_GROWTH.get().defaultBlockState(), 2);
            }
        }
        if (section == StationSection.CONTAINMENT_WING) {
            for (int z = -6; z <= 6; z += 4) {
                level.setBlock(new BlockPos(center.getX() - 6, Y + 1, z), ModBlocks.CONTAINMENT_POD.get().defaultBlockState(), 2);
                level.setBlock(new BlockPos(center.getX() + 6, Y + 1, z), ModBlocks.CONTAINMENT_POD.get().defaultBlockState(), 2);
            }
        }
        if (section == StationSection.OBSERVATION_DECK) {
            for (int x = center.getX() - 8; x <= center.getX() + 8; x++) {
                level.setBlock(new BlockPos(x, Y + 2, HZ), ModBlocks.CRACKED_OBSERVATION_GLASS.get().defaultBlockState(), 2);
                level.setBlock(new BlockPos(x, Y + 3, HZ), ModBlocks.CRACKED_OBSERVATION_GLASS.get().defaultBlockState(), 2);
            }
        }
        dress(level, section);
        sectionCacheMarkers(level, section);
        sectionLights(level, section);
    }

    private static void connector(ServerLevel level, StationSection section) {
        BlockState floor = ModBlocks.STATIONFALL_PLATING.get().defaultBlockState();
        BlockState wall = ModBlocks.STATIONFALL_WALL_PANEL.get().defaultBlockState();
        for (int x = section.centerX() + HX; x <= section.next().centerX() - HX; x++) {
            for (int z = -2; z <= 2; z++) {
                level.setBlock(new BlockPos(x, Y, z), floor, 2);
                for (int y = Y + 1; y <= Y + 4; y++) {
                    boolean boundary = z == -2 || z == 2 || y == Y + 4;
                    level.setBlock(new BlockPos(x, y, z), boundary ? wall : Blocks.AIR.defaultBlockState(), 2);
                }
            }
        }
        level.setBlock(section.doorPos(), ModBlocks.PRESSURE_DOOR.get().defaultBlockState(), 2);
        connectorLights(level, section);
    }

    private static void sectionLights(ServerLevel level, StationSection section) {
        for (int dx = -10; dx <= 10; dx += 10) {
            for (int dz = -6; dz <= 6; dz += 6) {
                placeLight(level, new BlockPos(section.centerX() + dx, Y + 6, dz));
            }
        }
    }

    private static void connectorLights(ServerLevel level, StationSection section) {
        for (int x = section.centerX() + 20; x <= section.next().centerX() - 20; x += 8) {
            placeLight(level, new BlockPos(x, Y + 4, 0));
        }
    }

    private static void placeLight(ServerLevel level, BlockPos pos) {
        level.setBlock(pos, Blocks.SEA_LANTERN.defaultBlockState(), 2);
    }

    private static void dress(ServerLevel level, StationSection section) {
        BlockPos center = section.center();
        for (int x = center.getX() - 12; x <= center.getX() + 12; x += 6) {
            BlockPos marker = new BlockPos(x, Y + 1, 0);
            if (level.getBlockState(marker).isAir()) {
                level.setBlock(marker, Blocks.LIGHT_GRAY_CARPET.defaultBlockState(), 2);
            }
        }
        for (int x = center.getX() - 12; x <= center.getX() + 12; x += 8) {
            level.setBlock(new BlockPos(x, Y + 1, -9), Blocks.IRON_BARS.defaultBlockState(), 2);
        }
        for (int z = -6; z <= 6; z += 6) {
            level.setBlock(new BlockPos(center.getX() + 13, Y + 1, z), Blocks.IRON_BARS.defaultBlockState(), 2);
        }
        switch (section) {
            case HYDROPONICS_BAY -> {
                for (int x = center.getX() - 10; x <= center.getX() + 10; x += 5) {
                    level.setBlock(new BlockPos(x, Y + 1, -4), Blocks.MOSS_BLOCK.defaultBlockState(), 2);
                    level.setBlock(new BlockPos(x, Y + 2, -4), ModBlocks.CORRUPTED_HYDROPONIC_GROWTH.get().defaultBlockState(), 2);
                }
            }
            case MEDICAL_WING -> {
                level.setBlock(new BlockPos(center.getX() + 4, Y + 1, 4), Blocks.WHITE_WOOL.defaultBlockState(), 2);
                level.setBlock(new BlockPos(center.getX() + 5, Y + 1, 4), Blocks.RED_WOOL.defaultBlockState(), 2);
                level.setBlock(new BlockPos(center.getX() - 5, Y + 1, 4), Blocks.WHITE_WOOL.defaultBlockState(), 2);
                level.setBlock(new BlockPos(center.getX() - 4, Y + 1, 4), Blocks.RED_WOOL.defaultBlockState(), 2);
            }
            case ENGINEERING_DECK -> {
                level.setBlock(new BlockPos(center.getX() + 8, Y + 1, -4), Blocks.LEVER.defaultBlockState(), 2);
                level.setBlock(new BlockPos(center.getX() + 9, Y + 1, -4), Blocks.COPPER_BLOCK.defaultBlockState(), 2);
                for (int z = -5; z <= 5; z += 5) {
                    level.setBlock(new BlockPos(center.getX() - 8, Y + 1, z), ModBlocks.STATION_POWER_NODE.get().defaultBlockState(), 2);
                }
            }
            case DATA_CORE -> {
                level.setBlock(new BlockPos(center.getX() - 7, Y + 1, 3), Blocks.PURPLE_STAINED_GLASS.defaultBlockState(), 2);
                level.setBlock(new BlockPos(center.getX() + 7, Y + 1, 3), Blocks.PURPLE_STAINED_GLASS.defaultBlockState(), 2);
                for (int z = -6; z <= 6; z += 4) {
                    level.setBlock(new BlockPos(center.getX(), Y + 1, z), ModBlocks.DATA_CORE_TERMINAL.get().defaultBlockState(), 2);
                }
            }
            case OBSERVATION_DECK -> {
                level.setBlock(new BlockPos(center.getX(), Y + 1, 8), Blocks.LIGHTNING_ROD.defaultBlockState(), 2);
                for (int x = center.getX() - 6; x <= center.getX() + 6; x += 6) {
                    level.setBlock(new BlockPos(x, Y + 1, HZ - 1), ModBlocks.CRACKED_OBSERVATION_GLASS.get().defaultBlockState(), 2);
                }
            }
            case COMMAND_MODULE -> {
                level.setBlock(new BlockPos(center.getX() - 5, Y + 1, -5), Blocks.REDSTONE_LAMP.defaultBlockState(), 2);
                level.setBlock(new BlockPos(center.getX() + 5, Y + 1, -5), Blocks.REDSTONE_LAMP.defaultBlockState(), 2);
                level.setBlock(new BlockPos(center.getX(), Y + 1, -6), ModBlocks.DAMAGED_AIRLOCK.get().defaultBlockState(), 2);
            }
            default -> {
            }
        }
    }

    private static void sectionCacheMarkers(ServerLevel level, StationSection section) {
        BlockPos power = powerCachePos(section);
        BlockPos common = commonCachePos(section);
        level.setBlock(power.north(), Blocks.REDSTONE_TORCH.defaultBlockState(), 2);
        level.setBlock(common.south(), Blocks.LANTERN.defaultBlockState(), 2);
        level.setBlock(power.below(), ModBlocks.STATIONFALL_PLATING.get().defaultBlockState(), 2);
        level.setBlock(common.below(), ModBlocks.STATIONFALL_PLATING.get().defaultBlockState(), 2);
        if (section == StationSection.COMMAND_MODULE) {
            level.setBlock(commandCachePos().north(), Blocks.REDSTONE_LAMP.defaultBlockState(), 2);
        }
    }

    private static void cache(ServerLevel level, BlockPos pos, ResourceKey<LootTable> lootTable) {
        level.setBlock(pos, Blocks.BARREL.defaultBlockState(), 2);
        if (level.getBlockEntity(pos) instanceof RandomizableContainerBlockEntity container) {
            container.setLootTable(lootTable, level.getRandom().nextLong());
        }
    }

    private static ResourceKey<LootTable> loot(String path) {
        return ResourceKey.create(
                Registries.LOOT_TABLE,
                Identifier.fromNamespaceAndPath(EchoStationfall.MODID, path)
        );
    }
}
