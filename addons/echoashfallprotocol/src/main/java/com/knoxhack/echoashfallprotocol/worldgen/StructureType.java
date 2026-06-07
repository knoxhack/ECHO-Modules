package com.knoxhack.echoashfallprotocol.worldgen;

import com.knoxhack.echoashfallprotocol.registry.ModBlocks;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;

import java.util.List;
import java.util.function.Supplier;

/**
 * Defines different types of procedural structures with their characteristics.
 */
public enum StructureType {
    
    BIO_LAB("bio_lab", 3, 8, 16, 32, 
            new BlockPalette(ModBlocks.CONTAMINATED_SOIL::get, ModBlocks.CONCRETE_RUBBLE::get, ModBlocks.SHATTERED_GLASS::get,
                    ModBlocks.TOXIC_MOSS::get, ModBlocks.CORRODED_PIPE::get)),
    
    DATA_CENTER("data_center", 4, 10, 20, 40,
            new BlockPalette(ModBlocks.ASH_STONE::get, ModBlocks.REBAR_BLOCK::get, ModBlocks.POWER_CABLE::get,
                    ModBlocks.RESEARCH_LAB::get, ModBlocks.SIGNAL_SCANNER::get)),
    
    MILITARY_VAULT("military_vault", 5, 12, 24, 48,
            new BlockPalette(ModBlocks.DROP_POD_HULL::get, ModBlocks.RUSTED_METAL_SHEET::get, ModBlocks.REBAR_BLOCK::get,
                    ModBlocks.STRUCTURE_CACHE::get, ModBlocks.WEAPON_RACK::get)),
    
    REACTOR_RUIN("reactor_ruin", 4, 10, 20, 36,
            new BlockPalette(ModBlocks.IRRADIATED_SHALE::get, ModBlocks.RADIATION_BLOCK::get, ModBlocks.CORRODED_PIPE::get,
                    ModBlocks.CONCRETE_RUBBLE::get, ModBlocks.ENERGIZED_FISSURE::get)),
    
    DROP_POD("drop_pod", 1, 3, 7, 14,
            new BlockPalette(ModBlocks.DROP_POD_HULL::get, ModBlocks.DROP_POD_GLASS::get, ModBlocks.RUSTED_METAL_SHEET::get,
                    ModBlocks.WORKSHOP_BLOCK::get, ModBlocks.STRUCTURE_CACHE::get)),
    
    SUBWAY_STATION("subway_station", 3, 6, 18, 30,
            new BlockPalette(ModBlocks.CRACKED_ASPHALT::get, ModBlocks.CONCRETE_RUBBLE::get, ModBlocks.REBAR_BLOCK::get,
                    ModBlocks.RUSTED_METAL_DEBRIS::get, ModBlocks.CORRODED_PIPE::get)),
    
    SATELLITE_ARRAY("satellite_array", 2, 4, 12, 24,
            new BlockPalette(ModBlocks.DROP_POD_HULL::get, ModBlocks.SHATTERED_GLASS::get, ModBlocks.DROP_POD_GLASS::get,
                    ModBlocks.RUSTED_METAL_SHEET::get, ModBlocks.POWER_NODE::get)),
    
    RADIO_TOWER("radio_tower", 2, 3, 10, 20,
            new BlockPalette(ModBlocks.REBAR_BLOCK::get, ModBlocks.DROP_POD_HULL::get, ModBlocks.RELAY_STATION::get,
                    ModBlocks.RUSTED_METAL_SHEET::get, ModBlocks.POWER_NODE::get)),
    
    SEWER_JUNCTION("sewer_junction", 3, 5, 15, 25,
            new BlockPalette(ModBlocks.TOXIC_SLAGSTONE::get, ModBlocks.CONCRETE_RUBBLE::get, ModBlocks.ACIDIC_SLUDGE::get,
                    ModBlocks.TOXIC_PUDDLE::get, ModBlocks.CORRODED_PIPE::get)),
    
    TRAIN_YARD("train_yard", 4, 8, 20, 40,
            new BlockPalette(ModBlocks.CRACKED_ASPHALT::get, ModBlocks.RUSTED_METAL_SHEET::get, ModBlocks.REBAR_BLOCK::get,
                    ModBlocks.DEAD_WOOD_LOG::get, ModBlocks.STRUCTURE_CACHE::get)),
    
    // === EXPLORATION 1.1: FACTION HUBS ===
    RADWARDEN_OUTPOST("radwarden_outpost", 4, 8, 20, 36,
            new BlockPalette(ModBlocks.CONCRETE_RUBBLE::get, ModBlocks.RUSTED_METAL_SHEET::get, ModBlocks.POWER_NODE::get,
                    ModBlocks.REBAR_BLOCK::get, ModBlocks.WEAPON_RACK::get)),
    
    CRASHBREAK_SALVAGE_YARD("crashbreak_salvage_yard", 3, 6, 15, 28,
            new BlockPalette(ModBlocks.DEAD_WOOD_LOG::get, ModBlocks.RUBBLE::get, ModBlocks.STRUCTURE_CACHE::get,
                    ModBlocks.CONCRETE_CHUNK::get, ModBlocks.TRADE_COUNTER::get)),
    
    SPOREBOUND_SANCTUM("sporebound_sanctum", 4, 10, 18, 32,
            new BlockPalette(ModBlocks.TOXIC_MOSS::get, ModBlocks.OOZE_CRYSTAL::get, ModBlocks.ACIDIC_SLUDGE::get,
                    ModBlocks.CONTAMINATED_SOIL::get, ModBlocks.BIO_PROCESSING_STATION::get)),
    
    // === EXPLORATION 1.1: WORLD POIs ===
    CRYOGENIC_RUINS("cryogenic_ruins", 3, 7, 15, 30,
            new BlockPalette(ModBlocks.PERMAFROST::get, ModBlocks.DROP_POD_HULL::get, ModBlocks.SHATTERED_GLASS::get,
                    ModBlocks.BLUE_ICE_CRYSTAL::get, ModBlocks.STRUCTURE_CACHE::get)),
    
    RELAY_STATION("relay_station", 2, 4, 10, 20,
            new BlockPalette(ModBlocks.DROP_POD_HULL::get, ModBlocks.CONCRETE_RUBBLE::get, ModBlocks.POWER_NODE::get,
                    ModBlocks.RUSTED_METAL_SHEET::get, ModBlocks.RELAY_STATION::get)),
    
    DERELICT_WORKSHOP("derelict_workshop", 3, 5, 14, 24,
            new BlockPalette(ModBlocks.CONCRETE_RUBBLE::get, ModBlocks.REBAR_BLOCK::get, ModBlocks.WORKSHOP_BLOCK::get,
                    ModBlocks.RUBBLE::get, ModBlocks.HAND_RECYCLER::get)),
    
    ABANDONED_MINE("abandoned_mine", 5, 12, 20, 40,
            new BlockPalette(ModBlocks.WASTELAND_STONE::get, ModBlocks.SCRAP_ORE::get, ModBlocks.REBAR_BLOCK::get,
                    ModBlocks.RUBBLE::get, ModBlocks.STRUCTURE_CACHE::get)),
    
    OBSERVATION_POST("observation_post", 2, 3, 8, 16,
            new BlockPalette(ModBlocks.DROP_POD_HULL::get, ModBlocks.DROP_POD_GLASS::get, ModBlocks.SIGNAL_SCANNER::get,
                    ModBlocks.RUSTED_METAL_SHEET::get, ModBlocks.ENERGY_METER::get));
    
    private final String name;
    private final int minRooms;
    private final int maxRooms;
    private final int minSize;
    private final int maxSize;
    private final BlockPalette palette;
    
    StructureType(String name, int minRooms, int maxRooms, int minSize, int maxSize, BlockPalette palette) {
        this.name = name;
        this.minRooms = minRooms;
        this.maxRooms = maxRooms;
        this.minSize = minSize;
        this.maxSize = maxSize;
        this.palette = palette;
    }
    
    public String getName() { return name; }
    public int getMinRooms() { return minRooms; }
    public int getMaxRooms() { return maxRooms; }
    public int getMinSize() { return minSize; }
    public int getMaxSize() { return maxSize; }
    public BlockPalette getPalette() { return palette; }
    
    public int getRandomRoomCount(RandomSource random) {
        return minRooms + random.nextInt(maxRooms - minRooms + 1);
    }
    
    public int getRandomSize(RandomSource random) {
        return minSize + random.nextInt(maxSize - minSize + 1);
    }
    
    public static StructureType getRandomType(RandomSource random) {
        return values()[random.nextInt(values().length)];
    }
    
    public static StructureType byName(String name) {
        for (StructureType type : values()) {
            if (type.name.equals(name)) return type;
        }
        return null;
    }
    
    /**
     * Block palette for a structure type
     */
    public static final class BlockPalette {
        private final Supplier<? extends Block> primary;
        private final Supplier<? extends Block> secondary;
        private final Supplier<? extends Block> accent;
        private final Supplier<? extends Block> decayed;
        private final Supplier<? extends Block> special;

        public BlockPalette(Block primary, Block secondary, Block accent, Block decayed, Block special) {
            this(() -> primary, () -> secondary, () -> accent, () -> decayed, () -> special);
        }

        public BlockPalette(
                Supplier<? extends Block> primary,
                Supplier<? extends Block> secondary,
                Supplier<? extends Block> accent,
                Supplier<? extends Block> decayed,
                Supplier<? extends Block> special
        ) {
            this.primary = primary;
            this.secondary = secondary;
            this.accent = accent;
            this.decayed = decayed;
            this.special = special;
        }

        public Block primary() { return primary.get(); }
        public Block secondary() { return secondary.get(); }
        public Block accent() { return accent.get(); }
        public Block decayed() { return decayed.get(); }
        public Block special() { return special.get(); }

        public Block getRandomBlock(RandomSource random, float decayChance) {
            float roll = random.nextFloat();
            if (roll < decayChance * 0.3f) return decayed();
            if (roll < 0.5f) return primary();
            if (roll < 0.8f) return secondary();
            if (roll < 0.95f) return accent();
            return special();
        }
    }

    /**
     * Architectural styles (Pass 5).
     *
     * Each style carries an alternate palette so two structures of the same
     * StructureType placed side-by-side look visibly different. The style is
     * picked per-structure (seeded by origin position) and its palette is used
     * for walls/floors/ceilings in place of {@link #getPalette()}.
     *
     * Each StructureType opts in to a subset via {@link #getValidStyles()}.
     */
    public enum ArchStyle {
        BRUTALIST(new BlockPalette(
                ModBlocks.CONCRETE_RUBBLE::get, ModBlocks.ASH_STONE::get, ModBlocks.REBAR_BLOCK::get,
                ModBlocks.RUBBLE::get, ModBlocks.OIL_STAINED_CONCRETE::get)),
        MODULAR(new BlockPalette(
                ModBlocks.DROP_POD_HULL::get, ModBlocks.DROP_POD_GLASS::get, ModBlocks.SHATTERED_GLASS::get,
                ModBlocks.CONCRETE_CHUNK::get, ModBlocks.POWER_NODE::get)),
        INDUSTRIAL_DECAY(new BlockPalette(
                ModBlocks.RUSTED_METAL_SHEET::get, ModBlocks.RUSTED_METAL_DEBRIS::get, ModBlocks.CORRODED_PIPE::get,
                ModBlocks.CONCRETE_RUBBLE::get, ModBlocks.INDUSTRIAL_AGGREGATE::get)),
        RETROFIT_TIMBER(new BlockPalette(
                ModBlocks.DEAD_WOOD_LOG::get, ModBlocks.CHARRED_WOOD_LOG::get, ModBlocks.ACID_MUD::get,
                ModBlocks.RUBBLE::get, ModBlocks.RAIN_COLLECTOR::get)),
        BUNKER(new BlockPalette(
                ModBlocks.DROP_POD_HULL::get, ModBlocks.RUSTED_METAL_SHEET::get, ModBlocks.REBAR_BLOCK::get,
                ModBlocks.CONCRETE_RUBBLE::get, ModBlocks.POWER_CABLE::get));

        private final BlockPalette palette;

        ArchStyle(BlockPalette palette) {
            this.palette = palette;
        }

        public BlockPalette getPalette() { return palette; }
    }

    /**
     * Returns the list of architectural styles this StructureType is allowed
     * to use. The first call returns null only if the type has no overrides
     * and should fall back to {@link #getPalette()}.
     */
    public List<ArchStyle> getValidStyles() {
        return switch (this) {
            case BIO_LAB -> List.of(ArchStyle.BRUTALIST, ArchStyle.MODULAR);
            case DATA_CENTER -> List.of(ArchStyle.BRUTALIST, ArchStyle.MODULAR, ArchStyle.INDUSTRIAL_DECAY);
            case MILITARY_VAULT -> List.of(ArchStyle.BUNKER);
            case REACTOR_RUIN -> List.of(ArchStyle.INDUSTRIAL_DECAY, ArchStyle.BRUTALIST);
            case SUBWAY_STATION -> List.of(ArchStyle.INDUSTRIAL_DECAY, ArchStyle.BRUTALIST);
            case SATELLITE_ARRAY, RELAY_STATION, OBSERVATION_POST -> List.of(ArchStyle.MODULAR, ArchStyle.BUNKER);
            case RADIO_TOWER -> List.of(ArchStyle.INDUSTRIAL_DECAY);
            case SEWER_JUNCTION -> List.of(ArchStyle.INDUSTRIAL_DECAY);
            case TRAIN_YARD -> List.of(ArchStyle.INDUSTRIAL_DECAY, ArchStyle.RETROFIT_TIMBER);
            case RADWARDEN_OUTPOST -> List.of(ArchStyle.BUNKER, ArchStyle.BRUTALIST);
            case CRASHBREAK_SALVAGE_YARD -> List.of(ArchStyle.RETROFIT_TIMBER, ArchStyle.INDUSTRIAL_DECAY);
            case SPOREBOUND_SANCTUM -> List.of(ArchStyle.RETROFIT_TIMBER, ArchStyle.INDUSTRIAL_DECAY);
            case CRYOGENIC_RUINS -> List.of(ArchStyle.MODULAR, ArchStyle.BUNKER);
            case DERELICT_WORKSHOP -> List.of(ArchStyle.RETROFIT_TIMBER, ArchStyle.INDUSTRIAL_DECAY);
            case ABANDONED_MINE -> List.of(ArchStyle.RETROFIT_TIMBER, ArchStyle.INDUSTRIAL_DECAY);
            default -> List.of();
        };
    }

    /**
     * Pick one ArchStyle for a structure instance, seeded by the origin so the
     * same chunk regenerates identically. Returns null if the type has no
     * style overrides; callers fall back to {@link #getPalette()}.
     */
    /**
     * Maximum floors a structure of this type may generate (Pass 1).
     * Default is 1 (single-story); types with multi-floor support opt in here.
     * Gated rollout; start with DATA_CENTER and MILITARY_VAULT only.
     */
    public int maxFloors() {
        return switch (this) {
            case DATA_CENTER, MILITARY_VAULT -> 2;
            default -> 1;
        };
    }

    public ArchStyle pickStyle(long seed) {
        List<ArchStyle> styles = getValidStyles();
        if (styles.isEmpty()) return null;
        // Deterministic mixing of the seed so neighbouring origins don't
        // collapse onto the same style.
        long mixed = (seed ^ (seed >>> 32)) * 2862933555777941757L + 3037000493L;
        int idx = (int) Math.floorMod(mixed, styles.size());
        return styles.get(idx);
    }
}

