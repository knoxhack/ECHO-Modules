package com.knoxhack.echoashfallprotocol.worldgen;

import com.knoxhack.echoashfallprotocol.EchoAshfallProtocol;
import com.knoxhack.echoashfallprotocol.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.storage.loot.LootTable;

/**
 * Builds vanilla-style village houses procedurally within faction villages.
 * Each faction gets themed variants using their specific block palettes.
 */
public class VanillaVillageHouseBuilder {
    private static final ResourceKey<LootTable> SURVIVOR_CACHE = ResourceKey.create(Registries.LOOT_TABLE,
        Identifier.fromNamespaceAndPath(EchoAshfallProtocol.MODID, "chests/survivor_cache"));

    /**
     * Faction-specific block palettes for vanilla house theming.
     */
    public record FactionPalette(
        Block wallBlock,      // Replaces planks/cobblestone walls
        Block floorBlock,     // Replaces floor planks
        Block roofBlock,      // Replaces roof stairs
        Block windowBlock,    // Replaces glass panes
        Block doorBlock,      // Replaces doors
        Block supportBlock,   // Replaces logs/fence posts
        Block lightBlock,     // Replaces torches
        Block detailBlock     // Accent blocks, crafting tables, etc.
    ) {
        /**
         * Returns true if a block should be replaced with this palette's wall block.
         */
        public Block transformBlock(Block original) {
            if (original == Blocks.OAK_PLANKS || 
                original == Blocks.SPRUCE_PLANKS ||
                original == Blocks.BIRCH_PLANKS ||
                original == Blocks.JUNGLE_PLANKS ||
                original == Blocks.ACACIA_PLANKS ||
                original == Blocks.DARK_OAK_PLANKS ||
                original == Blocks.MANGROVE_PLANKS ||
                original == Blocks.CHERRY_PLANKS ||
                original == Blocks.BAMBOO_PLANKS ||
                original == Blocks.COBBLESTONE ||
                original == Blocks.MOSSY_COBBLESTONE ||
                original == Blocks.COBBLED_DEEPSLATE) {
                return wallBlock;
            }
            if (original == Blocks.OAK_LOG || 
                original == Blocks.SPRUCE_LOG ||
                original == Blocks.BIRCH_LOG ||
                original == Blocks.JUNGLE_LOG ||
                original == Blocks.ACACIA_LOG ||
                original == Blocks.DARK_OAK_LOG ||
                original == Blocks.MANGROVE_LOG ||
                original == Blocks.CHERRY_LOG ||
                original == Blocks.STRIPPED_OAK_LOG ||
                original == Blocks.STRIPPED_SPRUCE_LOG) {
                return supportBlock;
            }
            if (original == Blocks.OAK_STAIRS || 
                original == Blocks.SPRUCE_STAIRS ||
                original == Blocks.BIRCH_STAIRS ||
                original == Blocks.JUNGLE_STAIRS ||
                original == Blocks.ACACIA_STAIRS ||
                original == Blocks.DARK_OAK_STAIRS ||
                original == Blocks.COBBLESTONE_STAIRS ||
                original == Blocks.MOSSY_COBBLESTONE_STAIRS) {
                return roofBlock;
            }
            if (original == Blocks.GLASS_PANE ||
                original == Blocks.WHITE_STAINED_GLASS_PANE ||
                original == Blocks.LIGHT_GRAY_STAINED_GLASS_PANE ||
                original == Blocks.BROWN_STAINED_GLASS_PANE) {
                return windowBlock;
            }
            if (original == Blocks.OAK_DOOR ||
                original == Blocks.SPRUCE_DOOR ||
                original == Blocks.BIRCH_DOOR ||
                original == Blocks.JUNGLE_DOOR ||
                original == Blocks.ACACIA_DOOR ||
                original == Blocks.DARK_OAK_DOOR ||
                original == Blocks.MANGROVE_DOOR ||
                original == Blocks.CHERRY_DOOR ||
                original == Blocks.BAMBOO_DOOR) {
                return doorBlock;
            }
            if (original == Blocks.OAK_FENCE ||
                original == Blocks.SPRUCE_FENCE ||
                original == Blocks.BIRCH_FENCE ||
                original == Blocks.JUNGLE_FENCE ||
                original == Blocks.ACACIA_FENCE ||
                original == Blocks.DARK_OAK_FENCE) {
                return supportBlock;
            }
            if (original == Blocks.TORCH || 
                original == Blocks.WALL_TORCH ||
                original == Blocks.SOUL_TORCH ||
                original == Blocks.SOUL_WALL_TORCH) {
                return lightBlock;
            }
            if (original == Blocks.CHEST || original == Blocks.BARREL) {
                return ModBlocks.STRUCTURE_CACHE.get();
            }
            if (original == Blocks.CRAFTING_TABLE ||
                original == Blocks.FURNACE) {
                return detailBlock;
            }
            return original;
        }
    }

    // === FACTION PALETTES ===
    
    /** Radwarden: Military/harsh - Stone bricks, iron, dark materials */
    public static final FactionPalette RADWARDEN_PALETTE = new FactionPalette(
        ModBlocks.CONCRETE_RUBBLE.get(),
        ModBlocks.ASH_STONE.get(),
        ModBlocks.RUSTED_METAL_SHEET.get(),
        ModBlocks.REBAR_BLOCK.get(),
        ModBlocks.RUSTED_METAL_SHEET.get(),
        ModBlocks.DROP_POD_HULL.get(),
        ModBlocks.POWER_NODE.get(),
        ModBlocks.WORKSHOP_BLOCK.get()
    );

    /** Crashbreak: Neutral/trade - Oak wood, maintained, classic village */
    public static final FactionPalette CRASHBREAK_PALETTE = new FactionPalette(
        ModBlocks.CHARRED_WOOD_LOG.get(),
        ModBlocks.DEAD_WOOD_LOG.get(),
        ModBlocks.RUSTED_METAL_SHEET.get(),
        ModBlocks.SHATTERED_GLASS.get(),
        ModBlocks.RUSTED_METAL_SHEET.get(),
        ModBlocks.DEAD_WOOD_LOG.get(),
        ModBlocks.POWER_NODE.get(),
        ModBlocks.STRUCTURE_CACHE.get()
    );

    /** Sporebound: Overgrown/bio-adapted - Mossy, spruce, tinted glass, organic */
    public static final FactionPalette SPOREBOUND_PALETTE = new FactionPalette(
        ModBlocks.TOXIC_MOSS.get(),
        ModBlocks.CONTAMINATED_SOIL.get(),
        ModBlocks.CHARRED_WOOD_LOG.get(),
        ModBlocks.SHATTERED_GLASS.get(),
        ModBlocks.CORRODED_PIPE.get(),
        ModBlocks.CHARRED_WOOD_LOG.get(),
        ModBlocks.OOZE_CRYSTAL.get(),
        ModBlocks.BIO_PROCESSING_STATION.get()
    );

    /**
     * Place a vanilla-style house in the given room footprint.
     * The house type is determined by room dimensions and faction.
     */
    public static void placeVanillaHouse(ServerLevel level, BlockPos origin, 
                                          Room room, StructureType type, RandomSource random) {
        // Determine faction palette
        FactionPalette palette = switch (type) {
            case RADWARDEN_OUTPOST -> RADWARDEN_PALETTE;
            case CRASHBREAK_SALVAGE_YARD -> CRASHBREAK_PALETTE;
            case SPOREBOUND_SANCTUM -> SPOREBOUND_PALETTE;
            default -> CRASHBREAK_PALETTE;
        };

        // Determine house size from room type
        int houseStyle = switch (room.getType()) {
            case VANILLA_HOUSE_SMALL -> 1;
            case VANILLA_HOUSE_MEDIUM -> 2;
            case VANILLA_HOUSE_LARGE -> 3;
            default -> 1;
        };

        // Place house based on style
        switch (houseStyle) {
            case 1 -> placeSmallHouse(level, origin, room, palette, random);
            case 2 -> placeMediumHouse(level, origin, room, palette, random);
            case 3 -> placeLargeHouse(level, origin, room, palette, random);
        }
    }

    /**
     * Small house: 5-6 wide, 5-6 deep, 4 tall.
     * Simple box with gable roof, single door, 1-2 windows.
     */
    private static void placeSmallHouse(ServerLevel level, BlockPos origin, Room room,
                                         FactionPalette palette, RandomSource random) {
        int rx = room.getX();
        int rz = room.getZ();
        int y = room.getY();
        int width = Math.min(room.getWidth(), 6);
        int depth = Math.min(room.getDepth(), 6);
        int height = Math.min(room.getHeight(), 4);

        // Center the house in the room
        int offsetX = (room.getWidth() - width) / 2;
        int offsetZ = (room.getDepth() - depth) / 2;
        rx += offsetX;
        rz += offsetZ;

        // Floor
        for (int x = 0; x < width; x++) {
            for (int z = 0; z < depth; z++) {
                BlockPos pos = origin.offset(rx + x, y - 1, rz + z);
                level.setBlock(pos, palette.floorBlock.defaultBlockState(), 2);
            }
        }

        // Walls (with door and windows)
        int doorX = width / 2;
        Direction doorFacing = Direction.SOUTH; // Door on south face
        
        for (int x = 0; x < width; x++) {
            for (int z = 0; z < depth; z++) {
                boolean isEdge = x == 0 || x == width - 1 || z == 0 || z == depth - 1;
                if (!isEdge) continue;

                for (int h = 0; h < height; h++) {
                    BlockPos pos = origin.offset(rx + x, y + h, rz + z);
                    
                    // Door placement
                    if (z == depth - 1 && x == doorX && h < 2) {
                        if (h == 1) continue; // Upper part handled by lower placement
                        placeDoor(level, pos, palette.doorBlock, doorFacing);
                        continue;
                    }
                    
                    // Window placement (skip corners)
                    boolean isCorner = (x == 0 || x == width - 1) && (z == 0 || z == depth - 1);
                    boolean isWindowHeight = h == 1 || h == 2;
                    boolean isWindowPos = (x == 0 || x == width - 1) && (z == depth / 2) ||
                                          (z == 0) && (x == width / 2);
                    
                    if (!isCorner && isWindowHeight && isWindowPos && random.nextBoolean()) {
                        level.setBlock(pos, palette.windowBlock.defaultBlockState(), 2);
                    } else {
                        level.setBlock(pos, palette.wallBlock.defaultBlockState(), 2);
                    }
                }
            }
        }

        // Gable roof
        buildGableRoof(level, origin, rx, rz, y + height, width, depth, palette.roofBlock, random);

        // Interior furnishings
        placeInterior(level, origin, rx, rz, y, width, depth, height, palette, random, true);
    }

    /**
     * Medium house: 7-8 wide, 7-8 deep, 5 tall.
     * Two-room layout or larger single room with chimney.
     */
    private static void placeMediumHouse(ServerLevel level, BlockPos origin, Room room,
                                          FactionPalette palette, RandomSource random) {
        int rx = room.getX();
        int rz = room.getZ();
        int y = room.getY();
        int width = Math.min(room.getWidth(), 8);
        int depth = Math.min(room.getDepth(), 8);
        int height = Math.min(room.getHeight(), 5);

        // Center in room
        int offsetX = (room.getWidth() - width) / 2;
        int offsetZ = (room.getDepth() - depth) / 2;
        rx += offsetX;
        rz += offsetZ;

        // Floor
        for (int x = 0; x < width; x++) {
            for (int z = 0; z < depth; z++) {
                BlockPos pos = origin.offset(rx + x, y - 1, rz + z);
                // Checker pattern floor for medium houses
                if ((x + z) % 2 == 1 && random.nextFloat() < 0.3f) {
                    level.setBlock(pos, palette.supportBlock.defaultBlockState(), 2);
                } else {
                    level.setBlock(pos, palette.floorBlock.defaultBlockState(), 2);
                }
            }
        }

        // Walls with multiple windows
        int doorX = width / 3;
        Direction doorFacing = Direction.SOUTH;
        
        for (int x = 0; x < width; x++) {
            for (int z = 0; z < depth; z++) {
                boolean isEdge = x == 0 || x == width - 1 || z == 0 || z == depth - 1;
                if (!isEdge) continue;

                for (int h = 0; h < height; h++) {
                    BlockPos pos = origin.offset(rx + x, y + h, rz + z);
                    
                    // Door
                    if (z == depth - 1 && x == doorX && h < 2) {
                        if (h == 1) continue;
                        placeDoor(level, pos, palette.doorBlock, doorFacing);
                        continue;
                    }
                    
                    // Multiple windows
                    boolean isCorner = (x == 0 || x == width - 1) && (z == 0 || z == depth - 1);
                    boolean isWindowHeight = h == 1 || h == 2;
                    boolean isWindowPos = (x == 1 || x == width - 2) && (z == 0 || z == depth - 1) ||
                                          (x == 0 || x == width - 1) && (z == depth / 3 || z == 2 * depth / 3);
                    
                    if (!isCorner && isWindowHeight && isWindowPos) {
                        level.setBlock(pos, palette.windowBlock.defaultBlockState(), 2);
                    } else {
                        level.setBlock(pos, palette.wallBlock.defaultBlockState(), 2);
                    }
                }
            }
        }

        // Higher gable roof
        buildGableRoof(level, origin, rx, rz, y + height, width, depth, palette.roofBlock, random);

        // Chimney (for larger houses)
        int chimneyX = width - 2;
        int chimneyZ = 2;
        for (int h = -1; h < 3; h++) {
            BlockPos pos = origin.offset(rx + chimneyX, y + height + h, rz + chimneyZ);
            level.setBlock(pos, palette.supportBlock.defaultBlockState(), 2);
        }

        // Interior
        placeInterior(level, origin, rx, rz, y, width, depth, height, palette, random, false);
    }

    /**
     * Large house: 9-10 wide, 9-10 deep, 6 tall.
     * Multi-room with interior walls, multiple doors, fireplace.
     */
    private static void placeLargeHouse(ServerLevel level, BlockPos origin, Room room,
                                         FactionPalette palette, RandomSource random) {
        int rx = room.getX();
        int rz = room.getZ();
        int y = room.getY();
        int width = Math.min(room.getWidth(), 10);
        int depth = Math.min(room.getDepth(), 10);
        int height = Math.min(room.getHeight(), 6);

        // Center in room
        int offsetX = (room.getWidth() - width) / 2;
        int offsetZ = (room.getDepth() - depth) / 2;
        rx += offsetX;
        rz += offsetZ;

        // Floor with pattern
        for (int x = 0; x < width; x++) {
            for (int z = 0; z < depth; z++) {
                BlockPos pos = origin.offset(rx + x, y - 1, rz + z);
                // Border pattern
                boolean isBorder = x == 0 || x == width - 1 || z == 0 || z == depth - 1;
                if (isBorder) {
                    level.setBlock(pos, palette.supportBlock.defaultBlockState(), 2);
                } else {
                    level.setBlock(pos, palette.floorBlock.defaultBlockState(), 2);
                }
            }
        }

        // Outer walls
        int doorX = width / 2;
        Direction doorFacing = Direction.SOUTH;
        
        for (int x = 0; x < width; x++) {
            for (int z = 0; z < depth; z++) {
                boolean isEdge = x == 0 || x == width - 1 || z == 0 || z == depth - 1;
                if (!isEdge) continue;

                for (int h = 0; h < height; h++) {
                    BlockPos pos = origin.offset(rx + x, y + h, rz + z);
                    
                    // Main door
                    if (z == depth - 1 && x == doorX && h < 2) {
                        if (h == 1) continue;
                        placeDoor(level, pos, palette.doorBlock, doorFacing);
                        continue;
                    }
                    
                    // Many windows on large house
                    boolean isCorner = (x == 0 || x == width - 1) && (z == 0 || z == depth - 1);
                    boolean isWindowHeight = h >= 1 && h <= 3;
                    boolean isWindowPos = (x % 3 == 1) && (z == 0 || z == depth - 1) ||
                                          (z % 3 == 1) && (x == 0 || x == width - 1);
                    
                    if (!isCorner && isWindowHeight && isWindowPos) {
                        level.setBlock(pos, palette.windowBlock.defaultBlockState(), 2);
                    } else {
                        level.setBlock(pos, palette.wallBlock.defaultBlockState(), 2);
                    }
                }
            }
        }

        // Interior dividing wall (creates two rooms)
        int wallX = width / 2;
        for (int z = 2; z < depth - 2; z++) {
            for (int h = 0; h < height - 1; h++) {
                BlockPos pos = origin.offset(rx + wallX, y + h, rz + z);
                // Doorway in middle
                if (z == depth / 2 && h < 2) {
                    if (h == 1) continue;
                    placeDoor(level, pos, palette.doorBlock, Direction.EAST);
                    continue;
                }
                level.setBlock(pos, palette.wallBlock.defaultBlockState(), 2);
            }
        }

        // Complex roof - stepped gable
        buildSteppedGableRoof(level, origin, rx, rz, y + height, width, depth, palette.roofBlock, random);

        // Central fireplace/chimney
        int fireX = width / 2;
        int fireZ = depth / 2;
        for (int h = -1; h < 5; h++) {
            BlockPos pos = origin.offset(rx + fireX, y + h, rz + fireZ);
            if (h < 0) {
                level.setBlock(pos, ModBlocks.POWER_NODE.get().defaultBlockState(), 2);
            } else {
                level.setBlock(pos, palette.supportBlock.defaultBlockState(), 2);
            }
        }

        // Interior for two rooms
        placeInterior(level, origin, rx, rz, y, wallX, depth, height, palette, random, false);
        placeInterior(level, origin, rx + wallX + 1, rz, y, width - wallX - 1, depth, height, palette, random, false);
    }

    /**
     * Build a simple gable roof (triangular) over the house.
     */
    private static void buildGableRoof(ServerLevel level, BlockPos origin, int rx, int rz, int y,
                                       int width, int depth, Block roofBlock, RandomSource random) {
        int roofHeight = Math.min(width, depth) / 2 + 1;
        
        for (int h = 0; h < roofHeight; h++) {
            int inset = h;
            int roofWidth = width - 2 * inset;
            int roofDepth = depth - 2 * inset;
            
            if (roofWidth <= 0 || roofDepth <= 0) break;
            
            for (int x = 0; x < roofWidth; x++) {
                for (int z = 0; z < roofDepth; z++) {
                    // Only place on the perimeter of this level
                    boolean isPerimeter = x == 0 || x == roofWidth - 1 || z == 0 || z == roofDepth - 1;
                    if (!isPerimeter && h < roofHeight - 1) continue;
                    
                    BlockPos pos = origin.offset(rx + inset + x, y + h, rz + inset + z);
                    BlockState stairState = getStairState(roofBlock, x, z, roofWidth, roofDepth, h);
                    level.setBlock(pos, stairState, 2);
                }
            }
        }
    }

    /**
     * Build a stepped gable roof for larger houses.
     */
    private static void buildSteppedGableRoof(ServerLevel level, BlockPos origin, int rx, int rz, int y,
                                               int width, int depth, Block roofBlock, RandomSource random) {
        int roofHeight = Math.min(width, depth) / 2 + 2;
        
        for (int h = 0; h < roofHeight; h++) {
            int inset = h / 2; // Step every other layer for larger overhang
            int roofWidth = width - 2 * inset;
            int roofDepth = depth - 2 * inset;
            
            if (roofWidth <= 0 || roofDepth <= 0) break;
            
            for (int x = 0; x < roofWidth; x++) {
                for (int z = 0; z < roofDepth; z++) {
                    boolean isPerimeter = x == 0 || x == roofWidth - 1 || z == 0 || z == roofDepth - 1;
                    if (!isPerimeter && h < roofHeight - 1) continue;
                    
                    BlockPos pos = origin.offset(rx + inset + x, y + h, rz + inset + z);
                    BlockState stairState = getStairState(roofBlock, x, z, roofWidth, roofDepth, h);
                    level.setBlock(pos, stairState, 2);
                }
            }
        }
    }

    /**
     * Get the stair block state with proper facing for roof slopes.
     */
    private static BlockState getStairState(Block roofBlock, int x, int z, int width, int depth, int height) {
        // Default to just the block if it's not stairs
        if (!roofBlock.defaultBlockState().hasProperty(BlockStateProperties.STAIRS_SHAPE)) {
            return roofBlock.defaultBlockState();
        }
        
        // Simple approach: alternate directions based on position
        Direction facing;
        if (x == 0) facing = Direction.EAST;
        else if (x == width - 1) facing = Direction.WEST;
        else if (z == 0) facing = Direction.SOUTH;
        else if (z == depth - 1) facing = Direction.NORTH;
        else facing = Direction.NORTH;
        
        return roofBlock.defaultBlockState()
            .setValue(BlockStateProperties.HORIZONTAL_FACING, facing)
            .setValue(BlockStateProperties.HALF, height % 2 == 0 ? 
                net.minecraft.world.level.block.state.properties.Half.TOP : 
                net.minecraft.world.level.block.state.properties.Half.BOTTOM);
    }

    /**
     * Place a door with upper and lower halves.
     */
    private static void placeDoor(ServerLevel level, BlockPos pos, Block doorBlock, Direction facing) {
        BlockState doorState = doorBlock.defaultBlockState();
        if (doorState.hasProperty(BlockStateProperties.HORIZONTAL_FACING)) {
            doorState = doorState.setValue(BlockStateProperties.HORIZONTAL_FACING, facing);
        }
        level.setBlock(pos, doorState, 2);
    }

    /**
     * Place interior furnishings based on house size.
     */
    private static void placeInterior(ServerLevel level, BlockPos origin, int rx, int rz, int y,
                                       int width, int depth, int height, FactionPalette palette,
                                       RandomSource random, boolean isSmall) {
        // Bed/cot
        int bedX = 1 + random.nextInt(Math.max(1, width - 3));
        int bedZ = 1 + random.nextInt(Math.max(1, depth - 3));
        BlockPos bedPos = origin.offset(rx + bedX, y, rz + bedZ);
        Direction bedDir = Direction.Plane.HORIZONTAL.getRandomDirection(random);
        BlockState bunkFoot = ModBlocks.EMERGENCY_BUNK.get().defaultBlockState()
                .setValue(com.knoxhack.echoashfallprotocol.block.EmergencyBunkBlock.FACING, bedDir)
                .setValue(com.knoxhack.echoashfallprotocol.block.EmergencyBunkBlock.PART,
                        net.minecraft.world.level.block.state.properties.BedPart.FOOT);
        level.setBlock(bedPos, bunkFoot, 2);
        level.setBlock(bedPos.relative(bedDir),
                bunkFoot.setValue(com.knoxhack.echoashfallprotocol.block.EmergencyBunkBlock.PART,
                        net.minecraft.world.level.block.state.properties.BedPart.HEAD),
                2);

        // Work/Detail station
        int workX = width - 2;
        int workZ = 1 + random.nextInt(Math.max(1, depth - 3));
        BlockPos workPos = origin.offset(rx + workX, y, rz + workZ);
        level.setBlock(workPos, palette.detailBlock.defaultBlockState(), 2);

        // Light source
        int lightX = width / 2;
        int lightZ = depth / 2;
        BlockPos lightPos = origin.offset(rx + lightX, y + height - 1, rz + lightZ);
        if (level.getBlockState(lightPos).isAir()) {
            level.setBlock(lightPos, palette.lightBlock.defaultBlockState(), 2);
        }

        // Additional details for larger houses
        if (!isSmall) {
            // Chest
            int chestX = 1 + random.nextInt(Math.max(1, width - 3));
            int chestZ = depth - 2;
            BlockPos chestPos = origin.offset(rx + chestX, y, rz + chestZ);
            if (random.nextBoolean()) {
                placeLootChest(level, chestPos, random);
            }

            // Chair/stool
            int stoolX = width / 2;
            int stoolZ = depth / 2 + 1;
            BlockPos stoolPos = origin.offset(rx + stoolX, y, rz + stoolZ);
            if (random.nextFloat() < 0.5f) {
                level.setBlock(stoolPos, ModBlocks.SUPPLY_CRATE.get().defaultBlockState(), 2);
            }
        }
    }

    private static void placeLootChest(ServerLevel level, BlockPos pos, RandomSource random) {
        BlockState state = ModBlocks.STRUCTURE_CACHE.get().defaultBlockState();
        level.setBlock(pos, state, 2);

        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity instanceof RandomizableContainerBlockEntity container) {
            container.setLootTable(SURVIVOR_CACHE, random.nextLong());
            container.setChanged();
            level.sendBlockUpdated(pos, state, state, 2);
        } else {
            EchoAshfallProtocol.LOGGER.warn("Placed village loot chest at {} without a loot-capable block entity", pos);
        }
    }
}
