package com.knoxhack.echoashfallprotocol.registry;

import com.knoxhack.echo.adaptercore.EchoBackendRegistryBridge;
import com.knoxhack.echo.adaptercore.EchoBackendRegistryEntry;
import com.knoxhack.echoashfallprotocol.EchoAshfallProtocol;
import com.knoxhack.echoashfallprotocol.block.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;

import java.util.function.Function;
import java.util.function.Supplier;

public class ModBlocks {
    public static final Object BLOCKS = EchoBackendRegistryBridge.create(BuiltInRegistries.BLOCK, EchoAshfallProtocol.MODID);
    // We register BlockItems alongside blocks using the items register from ModItems
    public static final Object BLOCK_ITEMS = EchoBackendRegistryBridge.create(BuiltInRegistries.ITEM, EchoAshfallProtocol.MODID);

    // === ENVIRONMENTAL BLOCKS ===
    public static final EchoBackendRegistryEntry<Block> DEBRIS_BLOCK = registerCustomBlock("debris_block",
            DebrisBlock::new, BlockBehaviour.Properties.of()
                    .mapColor(MapColor.STONE)
                    .strength(1.0f, 2.0f)
                    .sound(SoundType.GRAVEL)
                    .requiresCorrectToolForDrops());
    public static final EchoBackendRegistryEntry<BlockItem> DEBRIS_BLOCK_ITEM = registerSimpleBlockItem("debris_block", DEBRIS_BLOCK);

    public static final EchoBackendRegistryEntry<Block> TOXIC_PUDDLE = registerCustomBlock("toxic_puddle",
            ToxicPuddleBlock::new, BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_GREEN)
                    .strength(0.5f)
                    .sound(SoundType.SLIME_BLOCK)
                    .noOcclusion()
                    .speedFactor(0.6f));
    public static final EchoBackendRegistryEntry<BlockItem> TOXIC_PUDDLE_ITEM = registerSimpleBlockItem("toxic_puddle", TOXIC_PUDDLE);

    public static final EchoBackendRegistryEntry<Block> RADIATION_BLOCK = registerSimpleBlock("radiation_block",
            p -> p.mapColor(MapColor.COLOR_YELLOW)
                    .strength(2.0f)
                    .sound(SoundType.METAL)
                    .lightLevel(s -> 7));
    public static final EchoBackendRegistryEntry<BlockItem> RADIATION_BLOCK_ITEM = registerSimpleBlockItem("radiation_block", RADIATION_BLOCK);

    // === WASTELAND HAZARD BLOCKS ===
    public static final EchoBackendRegistryEntry<Block> ACIDIC_SLUDGE = registerCustomBlock("acidic_sludge",
            AcidicSludgeBlock::new, BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_LIGHT_GREEN)
                    .strength(0.3f)
                    .sound(SoundType.HONEY_BLOCK)
                    .noOcclusion()
                    .speedFactor(0.4f));
    public static final EchoBackendRegistryEntry<BlockItem> ACIDIC_SLUDGE_ITEM = registerSimpleBlockItem("acidic_sludge", ACIDIC_SLUDGE);

    public static final EchoBackendRegistryEntry<Block> FALLOUT_DUST = registerCustomBlock("fallout_dust",
            FalloutDustBlock::new, BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_YELLOW)
                    .strength(0.2f)
                    .sound(SoundType.SAND)
                    .noOcclusion());
    public static final EchoBackendRegistryEntry<BlockItem> FALLOUT_DUST_ITEM = registerSimpleBlockItem("fallout_dust", FALLOUT_DUST);

    public static final EchoBackendRegistryEntry<Block> CONTAMINATED_SOIL = registerCustomBlock("contaminated_soil",
            ContaminatedSoilBlock::new, BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_GRAY)
                    .strength(0.5f)
                    .sound(SoundType.GRAVEL));
    public static final EchoBackendRegistryEntry<BlockItem> CONTAMINATED_SOIL_ITEM = registerSimpleBlockItem("contaminated_soil", CONTAMINATED_SOIL);

    // === WASTELAND TERRAIN SURFACES ===
    public static final EchoBackendRegistryEntry<Block> WASTELAND_DIRT = registerSimpleBlock("wasteland_dirt",
            p -> p.mapColor(MapColor.DIRT)
                    .strength(0.55f)
                    .sound(SoundType.GRAVEL));
    public static final EchoBackendRegistryEntry<BlockItem> WASTELAND_DIRT_ITEM = registerSimpleBlockItem("wasteland_dirt", WASTELAND_DIRT);

    public static final EchoBackendRegistryEntry<Block> WASTELAND_GRASS_BLOCK = registerSimpleBlock("wasteland_grass_block",
            p -> p.mapColor(MapColor.COLOR_BROWN)
                    .strength(0.6f)
                    .sound(SoundType.GRASS));
    public static final EchoBackendRegistryEntry<BlockItem> WASTELAND_GRASS_BLOCK_ITEM = registerSimpleBlockItem("wasteland_grass_block", WASTELAND_GRASS_BLOCK);

    public static final EchoBackendRegistryEntry<Block> ASHEN_WASTELAND_DIRT = registerSimpleBlock("ashen_wasteland_dirt",
            p -> p.mapColor(MapColor.COLOR_GRAY)
                    .strength(0.5f)
                    .sound(SoundType.GRAVEL));
    public static final EchoBackendRegistryEntry<BlockItem> ASHEN_WASTELAND_DIRT_ITEM = registerSimpleBlockItem("ashen_wasteland_dirt", ASHEN_WASTELAND_DIRT);

    public static final EchoBackendRegistryEntry<Block> BURNT_WASTELAND_SOIL = registerSimpleBlock("burnt_wasteland_soil",
            p -> p.mapColor(MapColor.COLOR_BLACK)
                    .strength(0.65f)
                    .sound(SoundType.GRAVEL));
    public static final EchoBackendRegistryEntry<BlockItem> BURNT_WASTELAND_SOIL_ITEM = registerSimpleBlockItem("burnt_wasteland_soil", BURNT_WASTELAND_SOIL);

    public static final EchoBackendRegistryEntry<Block> TOXIC_WASTELAND_GRASS_BLOCK = registerSimpleBlock("toxic_wasteland_grass_block",
            p -> p.mapColor(MapColor.COLOR_GREEN)
                    .strength(0.6f)
                    .sound(SoundType.GRASS)
                    .speedFactor(0.92f));
    public static final EchoBackendRegistryEntry<BlockItem> TOXIC_WASTELAND_GRASS_BLOCK_ITEM = registerSimpleBlockItem("toxic_wasteland_grass_block", TOXIC_WASTELAND_GRASS_BLOCK);

    public static final EchoBackendRegistryEntry<Block> MUTATED_WASTELAND_GRASS_BLOCK = registerSimpleBlock("mutated_wasteland_grass_block",
            p -> p.mapColor(MapColor.COLOR_PURPLE)
                    .strength(0.6f)
                    .sound(SoundType.GRASS)
                    .lightLevel(s -> 1));
    public static final EchoBackendRegistryEntry<BlockItem> MUTATED_WASTELAND_GRASS_BLOCK_ITEM = registerSimpleBlockItem("mutated_wasteland_grass_block", MUTATED_WASTELAND_GRASS_BLOCK);

    public static final EchoBackendRegistryEntry<Block> IRRADIATED_CRUST = registerSimpleBlock("irradiated_crust",
            p -> p.mapColor(MapColor.COLOR_LIGHT_GREEN)
                    .strength(0.8f)
                    .sound(SoundType.GRAVEL)
                    .lightLevel(s -> 2));
    public static final EchoBackendRegistryEntry<BlockItem> IRRADIATED_CRUST_ITEM = registerSimpleBlockItem("irradiated_crust", IRRADIATED_CRUST);

    public static final EchoBackendRegistryEntry<Block> NEXUS_CRACKED_SOIL = registerSimpleBlock("nexus_cracked_soil",
            p -> p.mapColor(MapColor.COLOR_PURPLE)
                    .strength(0.85f)
                    .sound(SoundType.GRAVEL)
                    .lightLevel(s -> 3));
    public static final EchoBackendRegistryEntry<BlockItem> NEXUS_CRACKED_SOIL_ITEM = registerSimpleBlockItem("nexus_cracked_soil", NEXUS_CRACKED_SOIL);

    public static final EchoBackendRegistryEntry<Block> OIL_STAINED_CONCRETE = registerCustomBlock("oil_stained_concrete",
            OilStainedConcreteBlock::new, BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_BLACK)
                    .strength(1.5f)
                    .sound(SoundType.STONE)
                    .speedFactor(0.7f));
    public static final EchoBackendRegistryEntry<BlockItem> OIL_STAINED_CONCRETE_ITEM = registerSimpleBlockItem("oil_stained_concrete", OIL_STAINED_CONCRETE);

    public static final EchoBackendRegistryEntry<Block> CRACKED_ASPHALT = registerSimpleBlock("cracked_asphalt",
            p -> p.mapColor(MapColor.COLOR_GRAY)
                    .strength(1.25f)
                    .sound(SoundType.STONE)
                    .requiresCorrectToolForDrops());
    public static final EchoBackendRegistryEntry<BlockItem> CRACKED_ASPHALT_ITEM = registerSimpleBlockItem("cracked_asphalt", CRACKED_ASPHALT);

    public static final EchoBackendRegistryEntry<Block> CONCRETE_RUBBLE = registerCustomBlock("concrete_rubble",
            ConcreteRubbleBlock::new, BlockBehaviour.Properties.of()
                    .mapColor(MapColor.STONE)
                    .strength(0.8f)
                    .sound(SoundType.GRAVEL));
    public static final EchoBackendRegistryEntry<BlockItem> CONCRETE_RUBBLE_ITEM = registerSimpleBlockItem("concrete_rubble", CONCRETE_RUBBLE);

    public static final EchoBackendRegistryEntry<Block> RUSTED_METAL_SHEET = registerCustomBlock("rusted_metal_sheet",
            RustedMetalSheetBlock::new, BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_BROWN)
                    .strength(2.0f)
                    .sound(SoundType.METAL)
                    .requiresCorrectToolForDrops());
    public static final EchoBackendRegistryEntry<BlockItem> RUSTED_METAL_SHEET_ITEM = registerSimpleBlockItem("rusted_metal_sheet", RUSTED_METAL_SHEET);

    public static final EchoBackendRegistryEntry<Block> TOXIC_WASTE_BARREL = registerCustomBlock("toxic_waste_barrel",
            ToxicWasteBarrelBlock::new, BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_GREEN)
                    .strength(2.0f)
                    .sound(SoundType.METAL));
    public static final EchoBackendRegistryEntry<BlockItem> TOXIC_WASTE_BARREL_ITEM = registerSimpleBlockItem("toxic_waste_barrel", TOXIC_WASTE_BARREL);

    public static final EchoBackendRegistryEntry<Block> MUTATED_BUSH = registerCustomBlock("mutated_bush",
            HazardousBushBlock::new, BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_PURPLE)
                    .strength(0.2f)
                    .sound(SoundType.GRASS)
                    .noOcclusion());
    public static final EchoBackendRegistryEntry<BlockItem> MUTATED_BUSH_ITEM = registerSimpleBlockItem("mutated_bush", MUTATED_BUSH);

    // === WASTELAND VEGETATION ===
    public static final EchoBackendRegistryEntry<Block> DEAD_WOOD_LOG = registerCustomBlock("dead_wood_log",
            p -> new net.minecraft.world.level.block.RotatedPillarBlock(p), BlockBehaviour.Properties.of()
                    .mapColor(MapColor.WOOD)
                    .strength(2.0f)
                    .sound(SoundType.WOOD));
    public static final EchoBackendRegistryEntry<BlockItem> DEAD_WOOD_LOG_ITEM = registerSimpleBlockItem("dead_wood_log", DEAD_WOOD_LOG);

    public static final EchoBackendRegistryEntry<Block> CHARRED_WOOD_LOG = registerCustomBlock("charred_wood_log",
            p -> new net.minecraft.world.level.block.RotatedPillarBlock(p), BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_BLACK)
                    .strength(2.0f)
                    .sound(SoundType.WOOD));
    public static final EchoBackendRegistryEntry<BlockItem> CHARRED_WOOD_LOG_ITEM = registerSimpleBlockItem("charred_wood_log", CHARRED_WOOD_LOG);

    public static final EchoBackendRegistryEntry<Block> DRY_GRASS = registerCustomBlock("dry_grass",
            HazardousBushBlock::new, BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_BROWN)
                    .strength(0.0f)
                    .sound(SoundType.GRASS)
                    .noCollision()
                    .noOcclusion());
    public static final EchoBackendRegistryEntry<BlockItem> DRY_GRASS_ITEM = registerSimpleBlockItem("dry_grass", DRY_GRASS);

    public static final EchoBackendRegistryEntry<Block> DRY_TALL_GRASS = registerCustomBlock("dry_tall_grass",
            HazardousDoublePlantBlock::new, BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_BROWN)
                    .strength(0.0f)
                    .sound(SoundType.GRASS)
                    .noCollision()
                    .noOcclusion());
    public static final EchoBackendRegistryEntry<BlockItem> DRY_TALL_GRASS_ITEM = registerSimpleBlockItem("dry_tall_grass", DRY_TALL_GRASS);

    // === BIOME-TINTED GRASS OVERHAUL ===
    public static final EchoBackendRegistryEntry<Block> WASTELAND_GRASS = registerCustomBlock("wasteland_grass",
            BiomeTintedGrassBlock::new, BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_BROWN)
                    .strength(0.0f)
                    .sound(SoundType.GRASS)
                    .noCollision()
                    .noOcclusion());
    public static final EchoBackendRegistryEntry<BlockItem> WASTELAND_GRASS_ITEM = registerSimpleBlockItem("wasteland_grass", WASTELAND_GRASS);

    public static final EchoBackendRegistryEntry<Block> WASTELAND_TALL_GRASS = registerCustomBlock("wasteland_tall_grass",
            HazardousDoublePlantBlock::new, BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_BROWN)
                    .strength(0.0f)
                    .sound(SoundType.GRASS)
                    .noCollision()
                    .noOcclusion());
    public static final EchoBackendRegistryEntry<BlockItem> WASTELAND_TALL_GRASS_ITEM = registerSimpleBlockItem("wasteland_tall_grass", WASTELAND_TALL_GRASS);

    public static final EchoBackendRegistryEntry<Block> TOXIC_GRASS = registerCustomBlock("toxic_grass",
            BiomeTintedGrassBlock::new, BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_GREEN)
                    .strength(0.0f)
                    .sound(SoundType.GRASS)
                    .noCollision()
                    .noOcclusion());
    public static final EchoBackendRegistryEntry<BlockItem> TOXIC_GRASS_ITEM = registerSimpleBlockItem("toxic_grass", TOXIC_GRASS);

    public static final EchoBackendRegistryEntry<Block> TOXIC_TALL_GRASS = registerCustomBlock("toxic_tall_grass",
            HazardousDoublePlantBlock::new, BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_GREEN)
                    .strength(0.0f)
                    .sound(SoundType.GRASS)
                    .noCollision()
                    .noOcclusion());
    public static final EchoBackendRegistryEntry<BlockItem> TOXIC_TALL_GRASS_ITEM = registerSimpleBlockItem("toxic_tall_grass", TOXIC_TALL_GRASS);

    public static final EchoBackendRegistryEntry<Block> NUCLEAR_GRASS = registerCustomBlock("nuclear_grass",
            BiomeTintedGrassBlock::new, BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_LIGHT_GREEN)
                    .strength(0.0f)
                    .sound(SoundType.GRASS)
                    .lightLevel(s -> 3)
                    .randomTicks()
                    .noCollision()
                    .noOcclusion());
    public static final EchoBackendRegistryEntry<BlockItem> NUCLEAR_GRASS_ITEM = registerSimpleBlockItem("nuclear_grass", NUCLEAR_GRASS);

    public static final EchoBackendRegistryEntry<Block> NUCLEAR_TALL_GRASS = registerCustomBlock("nuclear_tall_grass",
            HazardousDoublePlantBlock::new, BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_LIGHT_GREEN)
                    .strength(0.0f)
                    .sound(SoundType.GRASS)
                    .lightLevel(s -> 4)
                    .noCollision()
                    .noOcclusion());
    public static final EchoBackendRegistryEntry<BlockItem> NUCLEAR_TALL_GRASS_ITEM = registerSimpleBlockItem("nuclear_tall_grass", NUCLEAR_TALL_GRASS);

    public static final EchoBackendRegistryEntry<Block> BURNT_GRASS = registerCustomBlock("burnt_grass",
            BiomeTintedGrassBlock::new, BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_BLACK)
                    .strength(0.0f)
                    .sound(SoundType.GRASS)
                    .noCollision()
                    .noOcclusion());
    public static final EchoBackendRegistryEntry<BlockItem> BURNT_GRASS_ITEM = registerSimpleBlockItem("burnt_grass", BURNT_GRASS);

    public static final EchoBackendRegistryEntry<Block> BURNT_TALL_GRASS = registerCustomBlock("burnt_tall_grass",
            HazardousDoublePlantBlock::new, BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_BLACK)
                    .strength(0.0f)
                    .sound(SoundType.GRASS)
                    .noCollision()
                    .noOcclusion());
    public static final EchoBackendRegistryEntry<BlockItem> BURNT_TALL_GRASS_ITEM = registerSimpleBlockItem("burnt_tall_grass", BURNT_TALL_GRASS);

    public static final EchoBackendRegistryEntry<Block> MUTATED_LEAVES_PURPLE = registerCustomBlock("mutated_leaves_purple",
            MutatedLeavesBlock::new, BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_PURPLE)
                    .strength(0.2f)
                    .randomTicks()
                    .sound(SoundType.GRASS)
                    .noOcclusion());
    public static final EchoBackendRegistryEntry<BlockItem> MUTATED_LEAVES_PURPLE_ITEM = registerSimpleBlockItem("mutated_leaves_purple", MUTATED_LEAVES_PURPLE);

    public static final EchoBackendRegistryEntry<Block> MUTATED_LEAVES_GRAY = registerCustomBlock("mutated_leaves_gray",
            MutatedLeavesBlock::new, BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_GRAY)
                    .strength(0.2f)
                    .randomTicks()
                    .sound(SoundType.GRASS)
                    .noOcclusion());
    public static final EchoBackendRegistryEntry<BlockItem> MUTATED_LEAVES_GRAY_ITEM = registerSimpleBlockItem("mutated_leaves_gray", MUTATED_LEAVES_GRAY);

    public static final EchoBackendRegistryEntry<Block> ASH_LAYER = registerCustomBlock("ash_layer",
            net.minecraft.world.level.block.SnowLayerBlock::new, BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_GRAY)
                    .strength(0.1f)
                    .sound(SoundType.SNOW)
                    .noOcclusion());
    public static final EchoBackendRegistryEntry<BlockItem> ASH_LAYER_ITEM = registerSimpleBlockItem("ash_layer", ASH_LAYER);

    // === NEW WASTELAND VEGETATION (Biome Overhaul) ===
    public static final EchoBackendRegistryEntry<Block> IRRADIATED_CACTUS = registerCustomBlock("irradiated_cactus",
            IrradiatedCactusBlock::new, BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_GREEN)
                    .strength(0.4f)
                    .sound(SoundType.WOOL)
                    .lightLevel(s -> 8)
                    .noOcclusion());
    public static final EchoBackendRegistryEntry<BlockItem> IRRADIATED_CACTUS_ITEM = registerSimpleBlockItem("irradiated_cactus", IRRADIATED_CACTUS);

    public static final EchoBackendRegistryEntry<Block> WASTELAND_REED = registerCustomBlock("wasteland_reed",
            WastelandReedBlock::new, BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_BROWN)
                    .strength(0.0f)
                    .sound(SoundType.GRASS)
                    .noCollision()
                    .noOcclusion());
    public static final EchoBackendRegistryEntry<BlockItem> WASTELAND_REED_ITEM = registerSimpleBlockItem("wasteland_reed", WASTELAND_REED);

    public static final EchoBackendRegistryEntry<Block> ASH_BUSH = registerCustomBlock("ash_bush",
            AshBushBlock::new, BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_GRAY)
                    .strength(0.2f)
                    .sound(SoundType.GRASS)
                    .noCollision()
                    .noOcclusion());
    public static final EchoBackendRegistryEntry<BlockItem> ASH_BUSH_ITEM = registerSimpleBlockItem("ash_bush", ASH_BUSH);

    // === FIRST LIGHT WILDERNESS BLOCKS ===
    public static final EchoBackendRegistryEntry<Block> WILD_BERRY_BUSH = registerCustomBlock("wild_berry_bush",
            WildBerryBushBlock::new, BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_GREEN)
                    .strength(0.1f)
                    .sound(SoundType.SWEET_BERRY_BUSH)
                    .randomTicks()
                    .noCollision()
                    .noOcclusion());
    public static final EchoBackendRegistryEntry<BlockItem> WILD_BERRY_BUSH_ITEM = registerSimpleBlockItem("wild_berry_bush", WILD_BERRY_BUSH);

    public static final EchoBackendRegistryEntry<Block> RAIN_COLLECTOR = registerCustomBlock("rain_collector",
            RainCollectorBlock::new, BlockBehaviour.Properties.of()
                    .mapColor(MapColor.WOOD)
                    .strength(1.8f)
                    .sound(SoundType.WOOD));
    public static final EchoBackendRegistryEntry<BlockItem> RAIN_COLLECTOR_ITEM = registerSimpleBlockItem("rain_collector", RAIN_COLLECTOR);

    public static final EchoBackendRegistryEntry<Block> ASH_CAMPFIRE = registerCustomBlock("ash_campfire",
            AshCampfireBlock::new, BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_GRAY)
                    .strength(2.0f)
                    .sound(SoundType.WOOD)
                    .lightLevel(s -> s.getValue(CampfireBlock.LIT) ? 13 : 0)
                    .noOcclusion());
    public static final EchoBackendRegistryEntry<BlockItem> ASH_CAMPFIRE_ITEM = registerSimpleBlockItem("ash_campfire", ASH_CAMPFIRE);

    public static final EchoBackendRegistryEntry<Block> NUCLEAR_FUNGUS = registerCustomBlock("nuclear_fungus",
            NuclearFungusBlock::new, BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_CYAN)
                    .strength(0.1f)
                    .sound(SoundType.FUNGUS)
                    .lightLevel(s -> 6)
                    .noCollision()
                    .noOcclusion());
    public static final EchoBackendRegistryEntry<BlockItem> NUCLEAR_FUNGUS_ITEM = registerSimpleBlockItem("nuclear_fungus", NUCLEAR_FUNGUS);

    public static final EchoBackendRegistryEntry<Block> RUSTY_WHEAT = registerCustomBlock("rusty_wheat",
            RustyWheatBlock::new, BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_RED)
                    .strength(0.0f)
                    .sound(SoundType.CROP)
                    .noCollision()
                    .noOcclusion());
    public static final EchoBackendRegistryEntry<BlockItem> RUSTY_WHEAT_ITEM = registerSimpleBlockItem("rusty_wheat", RUSTY_WHEAT);

    public static final EchoBackendRegistryEntry<Block> TOXIC_MOSS = registerCustomBlock("toxic_moss",
            ToxicMossBlock::new, BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_GREEN)
                    .strength(0.1f)
                    .sound(SoundType.MOSS)
                    .noCollision()
                    .noOcclusion());
    public static final EchoBackendRegistryEntry<BlockItem> TOXIC_MOSS_ITEM = registerSimpleBlockItem("toxic_moss", TOXIC_MOSS);

    public static final EchoBackendRegistryEntry<Block> BURNT_FERN = registerCustomBlock("burnt_fern",
            BurntFernBlock::new, BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_BLACK)
                    .strength(0.0f)
                    .sound(SoundType.GRASS)
                    .noCollision()
                    .noOcclusion());
    public static final EchoBackendRegistryEntry<BlockItem> BURNT_FERN_ITEM = registerSimpleBlockItem("burnt_fern", BURNT_FERN);

    public static final EchoBackendRegistryEntry<Block> MUTATED_SAPLING = registerCustomBlock("mutated_sapling",
            MutatedSaplingBlock::new, BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_PURPLE)
                    .strength(0.0f)
                    .sound(SoundType.GRASS)
                    .lightLevel(s -> 3)
                    .randomTicks()
                    .noCollision()
                    .noOcclusion());
    public static final EchoBackendRegistryEntry<BlockItem> MUTATED_SAPLING_ITEM = registerSimpleBlockItem("mutated_sapling", MUTATED_SAPLING);

    // === GROUND DEBRIS BLOCKS (Biome Overhaul) ===
    public static final EchoBackendRegistryEntry<Block> RUBBLE = registerCustomBlock("rubble",
            RubbleBlock::new, BlockBehaviour.Properties.of()
                    .mapColor(MapColor.STONE)
                    .strength(0.5f)
                    .sound(SoundType.GRAVEL)
                    .noOcclusion());
    public static final EchoBackendRegistryEntry<BlockItem> RUBBLE_ITEM = registerSimpleBlockItem("rubble", RUBBLE);

    public static final EchoBackendRegistryEntry<Block> CONCRETE_CHUNK = registerCustomBlock("concrete_chunk",
            ConcreteChunkBlock::new, BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_GRAY)
                    .strength(1.0f)
                    .sound(SoundType.STONE)
                    .requiresCorrectToolForDrops());
    public static final EchoBackendRegistryEntry<BlockItem> CONCRETE_CHUNK_ITEM = registerSimpleBlockItem("concrete_chunk", CONCRETE_CHUNK);

    public static final EchoBackendRegistryEntry<Block> RUSTED_METAL_DEBRIS = registerCustomBlock("rusted_metal_debris",
            RustedMetalDebrisBlock::new, BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_BROWN)
                    .strength(1.5f)
                    .sound(SoundType.METAL)
                    .requiresCorrectToolForDrops());
    public static final EchoBackendRegistryEntry<BlockItem> RUSTED_METAL_DEBRIS_ITEM = registerSimpleBlockItem("rusted_metal_debris", RUSTED_METAL_DEBRIS);

    public static final EchoBackendRegistryEntry<Block> SCATTERED_BONES = registerCustomBlock("scattered_bones",
            ScatteredBonesBlock::new, BlockBehaviour.Properties.of()
                    .mapColor(MapColor.SNOW)
                    .strength(0.1f)
                    .sound(SoundType.BONE_BLOCK)
                    .noOcclusion());
    public static final EchoBackendRegistryEntry<BlockItem> SCATTERED_BONES_ITEM = registerSimpleBlockItem("scattered_bones", SCATTERED_BONES);

    public static final EchoBackendRegistryEntry<Block> DEEP_ASH = registerCustomBlock("deep_ash",
            DeepAshBlock::new, BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_GRAY)
                    .strength(0.2f)
                    .sound(SoundType.SAND)
                    .noOcclusion());
    public static final EchoBackendRegistryEntry<BlockItem> DEEP_ASH_ITEM = registerSimpleBlockItem("deep_ash", DEEP_ASH);

    // === BIOME RESOURCE SUBSTRATES ===
    public static final EchoBackendRegistryEntry<Block> WASTELAND_STONE = registerSimpleBlock("wasteland_stone",
            p -> p.mapColor(MapColor.COLOR_GRAY)
                    .strength(1.5f, 6.0f)
                    .sound(SoundType.STONE)
                    .requiresCorrectToolForDrops());
    public static final EchoBackendRegistryEntry<BlockItem> WASTELAND_STONE_ITEM = registerSimpleBlockItem("wasteland_stone", WASTELAND_STONE);

    public static final EchoBackendRegistryEntry<Block> WASTELAND_TRACE_RUBBLE = registerSimpleBlock("wasteland_trace_rubble",
            p -> p.mapColor(MapColor.COLOR_GRAY)
                    .strength(0.9f)
                    .sound(SoundType.GRAVEL)
                    .requiresCorrectToolForDrops());
    public static final EchoBackendRegistryEntry<BlockItem> WASTELAND_TRACE_RUBBLE_ITEM = registerSimpleBlockItem("wasteland_trace_rubble", WASTELAND_TRACE_RUBBLE);

    public static final EchoBackendRegistryEntry<Block> INDUSTRIAL_AGGREGATE = registerSimpleBlock("industrial_aggregate",
            p -> p.mapColor(MapColor.COLOR_GRAY)
                    .strength(1.2f)
                    .sound(SoundType.STONE)
                    .requiresCorrectToolForDrops());
    public static final EchoBackendRegistryEntry<BlockItem> INDUSTRIAL_AGGREGATE_ITEM = registerSimpleBlockItem("industrial_aggregate", INDUSTRIAL_AGGREGATE);

    public static final EchoBackendRegistryEntry<Block> TOXIC_SLAGSTONE = registerSimpleBlock("toxic_slagstone",
            p -> p.mapColor(MapColor.COLOR_GREEN)
                    .strength(1.0f)
                    .sound(SoundType.STONE)
                    .requiresCorrectToolForDrops()
                    .lightLevel(s -> 1));
    public static final EchoBackendRegistryEntry<BlockItem> TOXIC_SLAGSTONE_ITEM = registerSimpleBlockItem("toxic_slagstone", TOXIC_SLAGSTONE);

    public static final EchoBackendRegistryEntry<Block> IRRADIATED_SHALE = registerSimpleBlock("irradiated_shale",
            p -> p.mapColor(MapColor.COLOR_LIGHT_GREEN)
                    .strength(1.1f)
                    .sound(SoundType.STONE)
                    .requiresCorrectToolForDrops()
                    .lightLevel(s -> 3));
    public static final EchoBackendRegistryEntry<BlockItem> IRRADIATED_SHALE_ITEM = registerSimpleBlockItem("irradiated_shale", IRRADIATED_SHALE);

    public static final EchoBackendRegistryEntry<Block> CRYOGENIC_FRACTURED_STONE = registerSimpleBlock("cryogenic_fractured_stone",
            p -> p.mapColor(MapColor.ICE)
                    .strength(1.1f)
                    .sound(SoundType.STONE)
                    .requiresCorrectToolForDrops());
    public static final EchoBackendRegistryEntry<BlockItem> CRYOGENIC_FRACTURED_STONE_ITEM = registerSimpleBlockItem("cryogenic_fractured_stone", CRYOGENIC_FRACTURED_STONE);

    public static final EchoBackendRegistryEntry<Block> CRASH_SLAG = registerSimpleBlock("crash_slag",
            p -> p.mapColor(MapColor.COLOR_BLACK)
                    .strength(1.2f)
                    .sound(SoundType.BASALT)
                    .requiresCorrectToolForDrops()
                    .lightLevel(s -> 1));
    public static final EchoBackendRegistryEntry<BlockItem> CRASH_SLAG_ITEM = registerSimpleBlockItem("crash_slag", CRASH_SLAG);

    // === BIOME OVERHAUL VISUAL BLOCKS ===
    public static final EchoBackendRegistryEntry<Block> NEXUS_SCAR_STONE = registerSimpleBlock("nexus_scar_stone",
            p -> p.mapColor(MapColor.COLOR_PURPLE)
                    .strength(1.6f, 6.0f)
                    .sound(SoundType.DEEPSLATE)
                    .requiresCorrectToolForDrops()
                    .lightLevel(s -> 1));
    public static final EchoBackendRegistryEntry<BlockItem> NEXUS_SCAR_STONE_ITEM = registerSimpleBlockItem("nexus_scar_stone", NEXUS_SCAR_STONE);

    public static final EchoBackendRegistryEntry<Block> ECHO_CRYSTAL = registerSimpleBlock("echo_crystal",
            p -> p.mapColor(MapColor.COLOR_PURPLE)
                    .strength(1.0f)
                    .sound(SoundType.AMETHYST)
                    .requiresCorrectToolForDrops()
                    .noOcclusion()
                    .lightLevel(s -> 12));
    public static final EchoBackendRegistryEntry<BlockItem> ECHO_CRYSTAL_ITEM = registerSimpleBlockItem("echo_crystal", ECHO_CRYSTAL);

    public static final EchoBackendRegistryEntry<Block> ENERGIZED_FISSURE = registerCustomBlock("energized_fissure",
            EnergizedFissureBlock::new, BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_PURPLE)
                    .strength(0.7f)
                    .sound(SoundType.AMETHYST)
                    .requiresCorrectToolForDrops()
                    .lightLevel(s -> 10));
    public static final EchoBackendRegistryEntry<BlockItem> ENERGIZED_FISSURE_ITEM = registerSimpleBlockItem("energized_fissure", ENERGIZED_FISSURE);

    public static final EchoBackendRegistryEntry<Block> SCORCHED_ASH = registerSimpleBlock("scorched_ash",
            p -> p.mapColor(MapColor.COLOR_BLACK)
                    .strength(0.4f)
                    .sound(SoundType.SAND));
    public static final EchoBackendRegistryEntry<BlockItem> SCORCHED_ASH_ITEM = registerSimpleBlockItem("scorched_ash", SCORCHED_ASH);

    public static final EchoBackendRegistryEntry<Block> TWISTED_METAL = registerSimpleBlock("twisted_metal",
            p -> p.mapColor(MapColor.METAL)
                    .strength(2.0f, 6.0f)
                    .sound(SoundType.METAL)
                    .requiresCorrectToolForDrops());
    public static final EchoBackendRegistryEntry<BlockItem> TWISTED_METAL_ITEM = registerSimpleBlockItem("twisted_metal", TWISTED_METAL);

    public static final EchoBackendRegistryEntry<Block> CABLE_BUNDLE = registerSimpleBlock("cable_bundle",
            p -> p.mapColor(MapColor.COLOR_BLACK)
                    .strength(0.8f)
                    .sound(SoundType.WOOL)
                    .noOcclusion());
    public static final EchoBackendRegistryEntry<BlockItem> CABLE_BUNDLE_ITEM = registerSimpleBlockItem("cable_bundle", CABLE_BUNDLE);

    public static final EchoBackendRegistryEntry<Block> CRACKED_EARTH = registerSimpleBlock("cracked_earth",
            p -> p.mapColor(MapColor.DIRT)
                    .strength(0.7f)
                    .sound(SoundType.GRAVEL));
    public static final EchoBackendRegistryEntry<BlockItem> CRACKED_EARTH_ITEM = registerSimpleBlockItem("cracked_earth", CRACKED_EARTH);

    public static final EchoBackendRegistryEntry<Block> ASH_STONE = registerSimpleBlock("ash_stone",
            p -> p.mapColor(MapColor.COLOR_GRAY)
                    .strength(1.4f, 6.0f)
                    .sound(SoundType.STONE)
                    .requiresCorrectToolForDrops());
    public static final EchoBackendRegistryEntry<BlockItem> ASH_STONE_ITEM = registerSimpleBlockItem("ash_stone", ASH_STONE);

    public static final EchoBackendRegistryEntry<Block> SCRAP_ORE = registerSimpleBlock("scrap_ore",
            p -> p.mapColor(MapColor.METAL)
                    .strength(2.4f, 6.0f)
                    .sound(SoundType.STONE)
                    .requiresCorrectToolForDrops());
    public static final EchoBackendRegistryEntry<BlockItem> SCRAP_ORE_ITEM = registerSimpleBlockItem("scrap_ore", SCRAP_ORE);

    public static final EchoBackendRegistryEntry<Block> THORN_SCRUB = registerCustomBlock("thorn_scrub",
            HazardousBushBlock::new, BlockBehaviour.Properties.of()
                    .mapColor(MapColor.PLANT)
                    .strength(0.1f)
                    .sound(SoundType.GRASS)
                    .noCollision()
                    .noOcclusion());
    public static final EchoBackendRegistryEntry<BlockItem> THORN_SCRUB_ITEM = registerSimpleBlockItem("thorn_scrub", THORN_SCRUB);

    public static final EchoBackendRegistryEntry<Block> ACID_MUD = registerCustomBlock("acid_mud",
            AcidicSludgeBlock::new, BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_LIGHT_GREEN)
                    .strength(0.5f)
                    .sound(SoundType.HONEY_BLOCK)
                    .lightLevel(s -> 2));
    public static final EchoBackendRegistryEntry<BlockItem> ACID_MUD_ITEM = registerSimpleBlockItem("acid_mud", ACID_MUD);

    public static final EchoBackendRegistryEntry<Block> OOZE_CRYSTAL = registerSimpleBlock("ooze_crystal",
            p -> p.mapColor(MapColor.COLOR_LIGHT_GREEN)
                    .strength(0.8f)
                    .sound(SoundType.AMETHYST)
                    .requiresCorrectToolForDrops()
                    .noOcclusion()
                    .lightLevel(s -> 11));
    public static final EchoBackendRegistryEntry<BlockItem> OOZE_CRYSTAL_ITEM = registerSimpleBlockItem("ooze_crystal", OOZE_CRYSTAL);

    public static final EchoBackendRegistryEntry<Block> CORRODED_PIPE = registerSimpleBlock("corroded_pipe",
            p -> p.mapColor(MapColor.METAL)
                    .strength(1.4f)
                    .sound(SoundType.METAL)
                    .requiresCorrectToolForDrops()
                    .noOcclusion());
    public static final EchoBackendRegistryEntry<BlockItem> CORRODED_PIPE_ITEM = registerSimpleBlockItem("corroded_pipe", CORRODED_PIPE);

    public static final EchoBackendRegistryEntry<Block> REBAR_BLOCK = registerSimpleBlock("rebar_block",
            p -> p.mapColor(MapColor.METAL)
                    .strength(1.6f, 5.0f)
                    .sound(SoundType.METAL)
                    .requiresCorrectToolForDrops()
                    .noOcclusion());
    public static final EchoBackendRegistryEntry<BlockItem> REBAR_BLOCK_ITEM = registerSimpleBlockItem("rebar_block", REBAR_BLOCK);

    public static final EchoBackendRegistryEntry<Block> SHATTERED_GLASS = registerSimpleBlock("shattered_glass",
            p -> p.mapColor(MapColor.ICE)
                    .strength(0.4f)
                    .sound(SoundType.GLASS)
                    .noOcclusion());
    public static final EchoBackendRegistryEntry<BlockItem> SHATTERED_GLASS_ITEM = registerSimpleBlockItem("shattered_glass", SHATTERED_GLASS);

    public static final EchoBackendRegistryEntry<Block> URANIUM_CRYSTAL = registerSimpleBlock("uranium_crystal",
            p -> p.mapColor(MapColor.COLOR_LIGHT_GREEN)
                    .strength(1.2f)
                    .sound(SoundType.AMETHYST)
                    .requiresCorrectToolForDrops()
                    .noOcclusion()
                    .lightLevel(s -> 13));
    public static final EchoBackendRegistryEntry<BlockItem> URANIUM_CRYSTAL_ITEM = registerSimpleBlockItem("uranium_crystal", URANIUM_CRYSTAL);

    public static final EchoBackendRegistryEntry<Block> RADIOACTIVE_SLUDGE = registerCustomBlock("radioactive_sludge",
            RadioactiveSludgeBlock::new, BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_LIGHT_GREEN)
                    .strength(0.5f)
                    .sound(SoundType.HONEY_BLOCK)
                    .lightLevel(s -> 3));
    public static final EchoBackendRegistryEntry<BlockItem> RADIOACTIVE_SLUDGE_ITEM = registerSimpleBlockItem("radioactive_sludge", RADIOACTIVE_SLUDGE);

    public static final EchoBackendRegistryEntry<Block> PERMAFROST = registerSimpleBlock("permafrost",
            p -> p.mapColor(MapColor.ICE)
                    .strength(1.1f)
                    .sound(SoundType.STONE)
                    .requiresCorrectToolForDrops());
    public static final EchoBackendRegistryEntry<BlockItem> PERMAFROST_ITEM = registerSimpleBlockItem("permafrost", PERMAFROST);

    public static final EchoBackendRegistryEntry<Block> BLUE_ICE_CRYSTAL = registerSimpleBlock("blue_ice_crystal",
            p -> p.mapColor(MapColor.ICE)
                    .strength(0.9f)
                    .sound(SoundType.AMETHYST)
                    .requiresCorrectToolForDrops()
                    .noOcclusion()
                    .lightLevel(s -> 10));
    public static final EchoBackendRegistryEntry<BlockItem> BLUE_ICE_CRYSTAL_ITEM = registerSimpleBlockItem("blue_ice_crystal", BLUE_ICE_CRYSTAL);

    public static final EchoBackendRegistryEntry<Block> FROZEN_CONDUIT = registerSimpleBlock("frozen_conduit",
            p -> p.mapColor(MapColor.METAL)
                    .strength(1.5f)
                    .sound(SoundType.METAL)
                    .requiresCorrectToolForDrops()
                    .noOcclusion());
    public static final EchoBackendRegistryEntry<BlockItem> FROZEN_CONDUIT_ITEM = registerSimpleBlockItem("frozen_conduit", FROZEN_CONDUIT);

    // === STRUCTURE BLOCKS ===
    public static final EchoBackendRegistryEntry<Block> DROP_POD_HULL = registerSimpleBlock("drop_pod_hull",
            p -> p.mapColor(MapColor.COLOR_GRAY)
                    .strength(50.0f, 1200.0f)
                    .sound(SoundType.NETHERITE_BLOCK)
                    .requiresCorrectToolForDrops());
    public static final EchoBackendRegistryEntry<BlockItem> DROP_POD_HULL_ITEM = registerSimpleBlockItem("drop_pod_hull", DROP_POD_HULL);

    public static final EchoBackendRegistryEntry<Block> DROP_POD_GLASS = registerSimpleBlock("drop_pod_glass",
            p -> p.mapColor(MapColor.COLOR_LIGHT_BLUE)
                    .strength(10.0f, 600.0f)
                    .sound(SoundType.GLASS)
                    .noOcclusion()
                    .lightLevel(s -> 4));
    public static final EchoBackendRegistryEntry<BlockItem> DROP_POD_GLASS_ITEM = registerSimpleBlockItem("drop_pod_glass", DROP_POD_GLASS);

    public static final EchoBackendRegistryEntry<Block> EMERGENCY_BUNK = registerCustomBlock("emergency_bunk",
            EmergencyBunkBlock::new, BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_GRAY)
                    .strength(1.4f)
                    .sound(SoundType.WOOL)
                    .noOcclusion());
    public static final EchoBackendRegistryEntry<BlockItem> EMERGENCY_BUNK_ITEM = registerSimpleBlockItem("emergency_bunk", EMERGENCY_BUNK);

    public static final EchoBackendRegistryEntry<Block> STRUCTURE_CACHE = registerCustomBlock("structure_cache",
            StructureCacheBlock::new, BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_BROWN)
                    .strength(2.0f, 3.0f)
                    .sound(SoundType.WOOD)
                    .noOcclusion());
    public static final EchoBackendRegistryEntry<BlockItem> STRUCTURE_CACHE_ITEM = registerSimpleBlockItem("structure_cache", STRUCTURE_CACHE);

    public static final EchoBackendRegistryEntry<Block> ECHO_CACHE = registerCustomBlock("echo_cache",
            EchoContainerBlock::new, BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_GRAY)
                    .strength(2.0f, 4.0f)
                    .sound(SoundType.METAL)
                    .noOcclusion());
    public static final EchoBackendRegistryEntry<BlockItem> ECHO_CACHE_ITEM = registerSimpleBlockItem("echo_cache", ECHO_CACHE);

    public static final EchoBackendRegistryEntry<Block> ECHO_CRATE = registerCustomBlock("echo_crate",
            EchoContainerBlock::new, BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_GRAY)
                    .strength(2.0f, 4.0f)
                    .sound(SoundType.METAL)
                    .noOcclusion());
    public static final EchoBackendRegistryEntry<BlockItem> ECHO_CRATE_ITEM = registerSimpleBlockItem("echo_crate", ECHO_CRATE);

    // === MACHINES ===
    public static final EchoBackendRegistryEntry<Block> HAND_RECYCLER = registerCustomBlock("hand_recycler",
            HandRecyclerBlock::new, BlockBehaviour.Properties.of()
                    .mapColor(MapColor.METAL)
                    .strength(3.5f)
                    .sound(SoundType.METAL)
                    .requiresCorrectToolForDrops()
                    .lightLevel(s -> s.getValue(HandRecyclerBlock.ACTIVE) ? 8 : 0));
    public static final EchoBackendRegistryEntry<BlockItem> HAND_RECYCLER_ITEM = registerSimpleBlockItem("hand_recycler", HAND_RECYCLER);

    public static final EchoBackendRegistryEntry<Block> THERMAL_BURNER = registerCustomBlock("thermal_burner",
            ThermalBurnerBlock::new, BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_ORANGE)
                    .strength(3.5f)
                    .sound(SoundType.METAL)
                    .requiresCorrectToolForDrops()
                    .lightLevel(s -> s.getValue(ThermalBurnerBlock.ACTIVE) ? 13 : 0));
    public static final EchoBackendRegistryEntry<BlockItem> THERMAL_BURNER_ITEM = registerSimpleBlockItem("thermal_burner", THERMAL_BURNER);

    public static final EchoBackendRegistryEntry<Block> WATER_PURIFIER = registerCustomBlock("water_purifier",
            WaterPurifierBlock::new, BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_CYAN)
                    .strength(3.5f)
                    .sound(SoundType.METAL)
                    .requiresCorrectToolForDrops()
                    .lightLevel(s -> s.getValue(WaterPurifierBlock.ACTIVE) ? 6 : 0));
    public static final EchoBackendRegistryEntry<BlockItem> WATER_PURIFIER_ITEM = registerSimpleBlockItem("water_purifier", WATER_PURIFIER);

    public static final EchoBackendRegistryEntry<Block> MICRO_GENERATOR = registerCustomBlock("micro_generator",
            MicroGeneratorBlock::new, BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_RED)
                    .strength(3.5f)
                    .sound(SoundType.METAL)
                    .requiresCorrectToolForDrops()
                    .lightLevel(s -> s.getValue(MicroGeneratorBlock.ACTIVE) ? 10 : 0));
    public static final EchoBackendRegistryEntry<BlockItem> MICRO_GENERATOR_ITEM = registerSimpleBlockItem("micro_generator", MICRO_GENERATOR);

    // === TIER 2.5 POWER GENERATION (Machinery Expansion) ===
    public static final EchoBackendRegistryEntry<Block> THERMAL_ARRAY = registerCustomBlock("thermal_array",
            com.knoxhack.echoashfallprotocol.block.ThermalArrayBlock::new, BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_ORANGE)
                    .strength(4.0f)
                    .sound(SoundType.METAL)
                    .requiresCorrectToolForDrops()
                    .lightLevel(s -> s.getValue(com.knoxhack.echoashfallprotocol.block.ThermalArrayBlock.ACTIVE) ? 13 : 0));
    public static final EchoBackendRegistryEntry<BlockItem> THERMAL_ARRAY_ITEM = registerSimpleBlockItem("thermal_array", THERMAL_ARRAY);

    public static final EchoBackendRegistryEntry<Block> FILTER_WORKBENCH = registerCustomBlock("filter_workbench",
            FilterWorkbenchBlock::new, BlockBehaviour.Properties.of()
                    .mapColor(MapColor.METAL)
                    .strength(3.0f)
                    .sound(SoundType.METAL)
                    .requiresCorrectToolForDrops());
    public static final EchoBackendRegistryEntry<BlockItem> FILTER_WORKBENCH_ITEM = registerSimpleBlockItem("filter_workbench", FILTER_WORKBENCH);

    public static final EchoBackendRegistryEntry<Block> BATTERY_BANK = registerCustomBlock("battery_bank",
            BatteryBankBlock::new, BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_BLUE)
                    .strength(3.5f)
                    .sound(SoundType.METAL)
                    .requiresCorrectToolForDrops()
                    .lightLevel(s -> 4));
    public static final EchoBackendRegistryEntry<BlockItem> BATTERY_BANK_ITEM = registerSimpleBlockItem("battery_bank", BATTERY_BANK);

    public static final EchoBackendRegistryEntry<Block> SCRAP_DYNAMO = registerCustomBlock("scrap_dynamo",
            ScrapDynamoBlock::new, BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_ORANGE)
                    .strength(4.0f)
                    .sound(SoundType.METAL)
                    .requiresCorrectToolForDrops()
                    .lightLevel(s -> s.getValue(ScrapDynamoBlock.ACTIVE) ? 12 : 0));
    public static final EchoBackendRegistryEntry<BlockItem> SCRAP_DYNAMO_ITEM = registerSimpleBlockItem("scrap_dynamo", SCRAP_DYNAMO);

    public static final EchoBackendRegistryEntry<Block> SCRAP_PRESS = registerCustomBlock("scrap_press",
            ScrapPressBlock::new, BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_GRAY)
                    .strength(3.5f)
                    .sound(SoundType.METAL)
                    .requiresCorrectToolForDrops()
                    .lightLevel(s -> 6));
    public static final EchoBackendRegistryEntry<BlockItem> SCRAP_PRESS_ITEM = registerSimpleBlockItem("scrap_press", SCRAP_PRESS);

    public static final EchoBackendRegistryEntry<Block> SIGNAL_SCANNER = registerCustomBlock("signal_scanner",
            SignalScannerBlock::new, BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_GREEN)
                    .strength(3.0f)
                    .sound(SoundType.METAL)
                    .requiresCorrectToolForDrops()
                    .lightLevel(s -> 8));
    public static final EchoBackendRegistryEntry<BlockItem> SIGNAL_SCANNER_ITEM = registerSimpleBlockItem("signal_scanner", SIGNAL_SCANNER);

    public static final EchoBackendRegistryEntry<Block> FIELD_MED_BAY = registerCustomBlock("field_med_bay",
            FieldMedBayBlock::new, BlockBehaviour.Properties.of()
                    .mapColor(MapColor.SNOW)
                    .strength(3.0f)
                    .sound(SoundType.METAL)
                    .requiresCorrectToolForDrops()
                    .lightLevel(s -> 5));
    public static final EchoBackendRegistryEntry<BlockItem> FIELD_MED_BAY_ITEM = registerSimpleBlockItem("field_med_bay", FIELD_MED_BAY);

    public static final EchoBackendRegistryEntry<Block> ATMOSPHERIC_SCRUBBER = registerCustomBlock("atmospheric_scrubber",
            AtmosphericScrubberBlock::new, BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_CYAN)
                    .strength(3.5f)
                    .sound(SoundType.METAL)
                    .requiresCorrectToolForDrops()
                    .lightLevel(s -> 10));
    public static final EchoBackendRegistryEntry<BlockItem> ATMOSPHERIC_SCRUBBER_ITEM = registerSimpleBlockItem("atmospheric_scrubber", ATMOSPHERIC_SCRUBBER);

    public static final EchoBackendRegistryEntry<Block> AUTOFEED_HOPPER = registerCustomBlock("autofeed_hopper",
            AutofeedHopperBlock::new, BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_ORANGE)
                    .strength(2.5f)
                    .sound(SoundType.METAL)
                    .requiresCorrectToolForDrops()
                    .lightLevel(s -> 3));
    public static final EchoBackendRegistryEntry<BlockItem> AUTOFEED_HOPPER_ITEM = registerSimpleBlockItem("autofeed_hopper", AUTOFEED_HOPPER);

    public static final EchoBackendRegistryEntry<Block> CONTAMINANT_CONDENSER = registerCustomBlock("contaminant_condenser",
            ContaminantCondenserBlock::new, BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_PURPLE)
                    .strength(4.0f)
                    .sound(SoundType.METAL)
                    .requiresCorrectToolForDrops()
                    .lightLevel(s -> 7));
    public static final EchoBackendRegistryEntry<BlockItem> CONTAMINANT_CONDENSER_ITEM = registerSimpleBlockItem("contaminant_condenser", CONTAMINANT_CONDENSER);

    // === GEO-EXTRACTOR MACHINES ===
    public static final EchoBackendRegistryEntry<Block> ORE_GRINDER = registerCustomBlock("ore_grinder",
            com.knoxhack.echoashfallprotocol.block.OreGrinderBlock::new, BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_BROWN)
                    .strength(3.5f)
                    .sound(SoundType.METAL)
                    .requiresCorrectToolForDrops()
                    .lightLevel(s -> s.getValue(com.knoxhack.echoashfallprotocol.block.OreGrinderBlock.ACTIVE) ? 6 : 0));
    public static final EchoBackendRegistryEntry<BlockItem> ORE_GRINDER_ITEM = registerSimpleBlockItem("ore_grinder", ORE_GRINDER);

    public static final EchoBackendRegistryEntry<Block> ISOTOPE_REFINER = registerCustomBlock("isotope_refiner",
            com.knoxhack.echoashfallprotocol.block.IsotopeRefinerBlock::new, BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_CYAN)
                    .strength(4.0f)
                    .sound(SoundType.METAL)
                    .requiresCorrectToolForDrops()
                    .lightLevel(s -> s.getValue(com.knoxhack.echoashfallprotocol.block.IsotopeRefinerBlock.ACTIVE) ? 12 : 0));
    public static final EchoBackendRegistryEntry<BlockItem> ISOTOPE_REFINER_ITEM = registerSimpleBlockItem("isotope_refiner", ISOTOPE_REFINER);

    public static final EchoBackendRegistryEntry<Block> CRYSTALLINE_SYNTHESIZER = registerCustomBlock("crystalline_synthesizer",
            com.knoxhack.echoashfallprotocol.block.CrystallineSynthesizerBlock::new, BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_PURPLE)
                    .strength(5.0f)
                    .sound(SoundType.NETHERITE_BLOCK)
                    .requiresCorrectToolForDrops()
                    .lightLevel(s -> switch (s.getValue(com.knoxhack.echoashfallprotocol.block.CrystallineSynthesizerBlock.PHASE)) {
                        case 1 -> 5; case 2 -> 10; case 3 -> 15; case 4 -> 8; default -> 0;
                    }));
    public static final EchoBackendRegistryEntry<BlockItem> CRYSTALLINE_SYNTHESIZER_ITEM = registerSimpleBlockItem("crystalline_synthesizer", CRYSTALLINE_SYNTHESIZER);

    // === ENDGAME / POWER GRID ===
    public static final EchoBackendRegistryEntry<Block> POWER_NODE = registerCustomBlock("power_node",
            com.knoxhack.echoashfallprotocol.block.PowerNodeBlock::new, BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_CYAN)
                    .strength(8.0f, 1200.0f)
                    .sound(SoundType.METAL)
                    .requiresCorrectToolForDrops()
                    .lightLevel(s -> s.getValue(com.knoxhack.echoashfallprotocol.block.PowerNodeBlock.ACTIVE) ? 12 : 3));
    public static final EchoBackendRegistryEntry<BlockItem> POWER_NODE_ITEM = registerSimpleBlockItem("power_node", POWER_NODE);

    public static final EchoBackendRegistryEntry<Block> NEXUS_CORE = registerCustomBlock("nexus_core",
            com.knoxhack.echoashfallprotocol.block.NexusCoreBlock::new, BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_PURPLE)
                    .strength(2000.0f, 3600000.0f)
                    .sound(SoundType.NETHERITE_BLOCK)
                    .requiresCorrectToolForDrops()
                    .lightLevel(s -> 15));
    public static final EchoBackendRegistryEntry<BlockItem> NEXUS_CORE_ITEM = registerSimpleBlockItem("nexus_core", NEXUS_CORE);

    // === ENDGAME MACHINES ===
    public static final EchoBackendRegistryEntry<Block> DEEP_CORE_MINER = registerCustomBlock("deep_core_miner",
            com.knoxhack.echoashfallprotocol.block.DeepCoreMinerBlock::new, BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_BLACK)
                    .strength(5.0f)
                    .sound(SoundType.METAL)
                    .requiresCorrectToolForDrops()
                    .lightLevel(s -> 5));
    public static final EchoBackendRegistryEntry<BlockItem> DEEP_CORE_MINER_ITEM = registerSimpleBlockItem("deep_core_miner", DEEP_CORE_MINER);

    public static final EchoBackendRegistryEntry<Block> RADIATION_CLEANSER = registerCustomBlock("radiation_cleanser",
            com.knoxhack.echoashfallprotocol.block.RadiationCleanserBlock::new, BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_GREEN)
                    .strength(3.5f)
                    .sound(SoundType.METAL)
                    .requiresCorrectToolForDrops()
                    .lightLevel(s -> 4));
    public static final EchoBackendRegistryEntry<BlockItem> RADIATION_CLEANSER_ITEM = registerSimpleBlockItem("radiation_cleanser", RADIATION_CLEANSER);

    // === EXPLORATION 1.1 - RESEARCH SYSTEM ===
    public static final EchoBackendRegistryEntry<Block> RESEARCH_LAB = registerCustomBlock("research_lab",
            com.knoxhack.echoashfallprotocol.block.ResearchLabBlock::new, BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_CYAN)
                    .strength(4.0f)
                    .sound(SoundType.METAL)
                    .requiresCorrectToolForDrops()
                    .lightLevel(s -> 8));
    public static final EchoBackendRegistryEntry<BlockItem> RESEARCH_LAB_ITEM = registerSimpleBlockItem("research_lab", RESEARCH_LAB);

    public static final EchoBackendRegistryEntry<Block> RELAY_STATION = registerCustomBlock("relay_station",
            com.knoxhack.echoashfallprotocol.block.RelayStationBlock::new, BlockBehaviour.Properties.of()
                    .mapColor(MapColor.METAL)
                    .strength(3.5f)
                    .sound(SoundType.METAL)
                    .requiresCorrectToolForDrops()
                    .lightLevel(s -> s.getValue(com.knoxhack.echoashfallprotocol.block.RelayStationBlock.ACTIVE) ? 12 : 0));
    public static final EchoBackendRegistryEntry<BlockItem> RELAY_STATION_ITEM = registerSimpleBlockItem("relay_station", RELAY_STATION);

    public static final EchoBackendRegistryEntry<Block> WORKSHOP_BLOCK = registerCustomBlock("workshop_block",
            com.knoxhack.echoashfallprotocol.block.WorkshopBlock::new, BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_ORANGE)
                    .strength(3.0f)
                    .sound(SoundType.METAL)
                    .requiresCorrectToolForDrops()
                    .lightLevel(s -> 6));
    public static final EchoBackendRegistryEntry<BlockItem> WORKSHOP_BLOCK_ITEM = registerSimpleBlockItem("workshop_block", WORKSHOP_BLOCK);

    // === MACHINE INTEGRATION ===
    public static final EchoBackendRegistryEntry<Block> ITEM_PIPE = registerCustomBlock("item_pipe",
            com.knoxhack.echoashfallprotocol.block.ItemPipeBlock::new, BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_GRAY)
                    .strength(2.0f)
                    .sound(SoundType.METAL)
                    .requiresCorrectToolForDrops()
                    .noOcclusion());
    public static final EchoBackendRegistryEntry<BlockItem> ITEM_PIPE_ITEM = registerSimpleBlockItem("item_pipe", ITEM_PIPE);

    public static final EchoBackendRegistryEntry<Block> POWER_CABLE = registerCustomBlock("power_cable",
            com.knoxhack.echoashfallprotocol.block.PowerCableBlock::new, BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_RED)
                    .strength(1.5f)
                    .sound(SoundType.METAL)
                    .requiresCorrectToolForDrops()
                    .noOcclusion()
                    .lightLevel(s -> s.getValue(com.knoxhack.echoashfallprotocol.block.PowerCableBlock.ACTIVE) ? 4 : 0));
    public static final EchoBackendRegistryEntry<BlockItem> POWER_CABLE_ITEM = registerSimpleBlockItem("power_cable", POWER_CABLE);

    public static final EchoBackendRegistryEntry<Block> REINFORCED_POWER_CABLE = registerCustomBlock("reinforced_power_cable",
            props -> new com.knoxhack.echoashfallprotocol.block.PowerCableBlock(props, 2000, 256), BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_BLUE)
                    .strength(2.0f)
                    .sound(SoundType.METAL)
                    .requiresCorrectToolForDrops()
                    .noOcclusion()
                    .lightLevel(s -> s.getValue(com.knoxhack.echoashfallprotocol.block.PowerCableBlock.ACTIVE) ? 5 : 0));
    public static final EchoBackendRegistryEntry<BlockItem> REINFORCED_POWER_CABLE_ITEM = registerSimpleBlockItem("reinforced_power_cable", REINFORCED_POWER_CABLE);

    public static final EchoBackendRegistryEntry<Block> HIGH_VOLTAGE_POWER_CABLE = registerCustomBlock("high_voltage_power_cable",
            props -> new com.knoxhack.echoashfallprotocol.block.PowerCableBlock(props, 4000, 1024), BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_PURPLE)
                    .strength(2.5f)
                    .sound(SoundType.METAL)
                    .requiresCorrectToolForDrops()
                    .noOcclusion()
                    .lightLevel(s -> s.getValue(com.knoxhack.echoashfallprotocol.block.PowerCableBlock.ACTIVE) ? 7 : 1));
    public static final EchoBackendRegistryEntry<BlockItem> HIGH_VOLTAGE_POWER_CABLE_ITEM = registerSimpleBlockItem("high_voltage_power_cable", HIGH_VOLTAGE_POWER_CABLE);

    public static final EchoBackendRegistryEntry<Block> ENERGY_METER = registerCustomBlock("energy_meter",
            EnergyMeterBlock::new, BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_CYAN)
                    .strength(2.5f)
                    .sound(SoundType.METAL)
                    .requiresCorrectToolForDrops()
                    .lightLevel(s -> 4));
    public static final EchoBackendRegistryEntry<BlockItem> ENERGY_METER_ITEM = registerSimpleBlockItem("energy_meter", ENERGY_METER);

    public static final EchoBackendRegistryEntry<Block> LOAD_DISTRIBUTOR = registerCustomBlock("load_distributor",
            LoadDistributorBlock::new, BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_LIGHT_BLUE)
                    .strength(3.0f)
                    .sound(SoundType.METAL)
                    .requiresCorrectToolForDrops()
                    .lightLevel(s -> 5));
    public static final EchoBackendRegistryEntry<BlockItem> LOAD_DISTRIBUTOR_ITEM = registerSimpleBlockItem("load_distributor", LOAD_DISTRIBUTOR);

    public static final EchoBackendRegistryEntry<Block> NEXUS_CAPACITOR = registerCustomBlock("nexus_capacitor",
            NexusCapacitorBlock::new, BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_PURPLE)
                    .strength(6.0f)
                    .sound(SoundType.NETHERITE_BLOCK)
                    .requiresCorrectToolForDrops()
                    .lightLevel(s -> 8));
    public static final EchoBackendRegistryEntry<BlockItem> NEXUS_CAPACITOR_ITEM = registerSimpleBlockItem("nexus_capacitor", NEXUS_CAPACITOR);

    public static final EchoBackendRegistryEntry<Block> FACTORY_CONTROLLER = registerCustomBlock("factory_controller",
            com.knoxhack.echoashfallprotocol.block.FactoryControllerBlock::new, BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_BLUE)
                    .strength(3.0f)
                    .sound(SoundType.METAL)
                    .requiresCorrectToolForDrops()
                    .lightLevel(s -> s.getValue(com.knoxhack.echoashfallprotocol.block.FactoryControllerBlock.ACTIVE) ? 8 : 4));
    public static final EchoBackendRegistryEntry<BlockItem> FACTORY_CONTROLLER_ITEM = registerSimpleBlockItem("factory_controller", FACTORY_CONTROLLER);

    // === FACTION VILLAGER PROFESSION BLOCKS ===
    // Radwarden workstations
    public static final EchoBackendRegistryEntry<Block> WEAPON_RACK = registerSimpleProfessionBlock("weapon_rack",
            MapColor.COLOR_BLUE, SoundType.METAL);
    public static final EchoBackendRegistryEntry<BlockItem> WEAPON_RACK_ITEM = registerSimpleBlockItem("weapon_rack", WEAPON_RACK);

    public static final EchoBackendRegistryEntry<Block> SUPPLY_CRATE = registerCustomBlock("supply_crate",
            EchoContainerBlock::new, BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_LIGHT_BLUE)
                    .strength(2.0f, 4.0f)
                    .sound(SoundType.WOOD)
                    .noOcclusion());
    public static final EchoBackendRegistryEntry<BlockItem> SUPPLY_CRATE_ITEM = registerSimpleBlockItem("supply_crate", SUPPLY_CRATE);

    // Crashbreak workstations
    public static final EchoBackendRegistryEntry<Block> TRADE_COUNTER = registerSimpleProfessionBlock("trade_counter",
            MapColor.COLOR_YELLOW, SoundType.WOOD);
    public static final EchoBackendRegistryEntry<BlockItem> TRADE_COUNTER_ITEM = registerSimpleBlockItem("trade_counter", TRADE_COUNTER);

    public static final EchoBackendRegistryEntry<Block> SURVEY_TABLE = registerSimpleProfessionBlock("survey_table",
            MapColor.COLOR_BROWN, SoundType.WOOD);
    public static final EchoBackendRegistryEntry<BlockItem> SURVEY_TABLE_ITEM = registerSimpleBlockItem("survey_table", SURVEY_TABLE);

    // Sporebound workstations
    public static final EchoBackendRegistryEntry<Block> BIO_PROCESSING_STATION = registerSimpleProfessionBlock("bio_processing_station",
            MapColor.COLOR_GREEN, SoundType.SLIME_BLOCK);
    public static final EchoBackendRegistryEntry<BlockItem> BIO_PROCESSING_STATION_ITEM = registerSimpleBlockItem("bio_processing_station", BIO_PROCESSING_STATION);

    public static final EchoBackendRegistryEntry<Block> SPORE_GARDEN = registerSimpleProfessionBlock("spore_garden",
            MapColor.COLOR_LIGHT_GREEN, SoundType.GRASS);
    public static final EchoBackendRegistryEntry<BlockItem> SPORE_GARDEN_ITEM = registerSimpleBlockItem("spore_garden", SPORE_GARDEN);

    private static EchoBackendRegistryEntry<Block> registerSimpleProfessionBlock(String name, MapColor color, SoundType sound) {
        return EchoBackendRegistryBridge.registerWithId(BLOCKS, name, id -> new ProfessionBlock(withId(BlockBehaviour.Properties.of()
                .mapColor(color)
                .strength(2.5f)
                .sound(sound)
                .noOcclusion(), id)));
    }

    private static EchoBackendRegistryEntry<Block> registerSimpleBlock(String name,
            Function<BlockBehaviour.Properties, BlockBehaviour.Properties> propertiesFactory) {
        return EchoBackendRegistryBridge.registerWithId(BLOCKS, name,
                id -> new Block(withId(propertiesFactory.apply(BlockBehaviour.Properties.of()), id)));
    }

    private static EchoBackendRegistryEntry<BlockItem> registerSimpleBlockItem(String name,
            Supplier<? extends Block> block) {
        return EchoBackendRegistryBridge.registerWithId(BLOCK_ITEMS, name,
                id -> new BlockItem(block.get(), new Item.Properties()
                        .setId(ResourceKey.create(Registries.ITEM, id))));
    }

    public static void register(Object eventBus) {
        EchoBackendRegistryBridge.registerEventBus(BLOCKS, eventBus);
        EchoBackendRegistryBridge.registerEventBus(BLOCK_ITEMS, eventBus);
    }

    private static <T extends Block> EchoBackendRegistryEntry<T> registerCustomBlock(String name, Function<BlockBehaviour.Properties, T> factory, BlockBehaviour.Properties properties) {
        return EchoBackendRegistryBridge.registerWithId(BLOCKS, name, id -> factory.apply(withId(properties, id)));
    }

    private static BlockBehaviour.Properties withId(BlockBehaviour.Properties properties, Identifier id) {
        return properties.setId(ResourceKey.create(Registries.BLOCK, id));
    }
}
