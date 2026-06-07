package com.knoxhack.echoagriculturereclamation.registry;

import com.knoxhack.echo.adaptercore.EchoBackendRegistryBridge;
import com.knoxhack.echo.adaptercore.EchoBackendRegistryEntry;
import com.knoxhack.echoagriculturereclamation.EchoAgricultureReclamation;
import com.knoxhack.echoagriculturereclamation.block.HydroponicTrayBlock;
import com.knoxhack.echoagriculturereclamation.block.ReclamationCropBlock;
import com.knoxhack.echoagriculturereclamation.block.ReclamationMachineBlock;
import com.knoxhack.echoagriculturereclamation.block.ReclamationSoilBlock;
import com.knoxhack.echoagriculturereclamation.content.CropSpec;
import com.knoxhack.echoagriculturereclamation.content.SoilState;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;

public final class ModBlocks {
   public static final Object BLOCKS = EchoBackendRegistryBridge.create(BuiltInRegistries.BLOCK, EchoAgricultureReclamation.MODID);
   private static final Map<String, EchoBackendRegistryEntry<ReclamationCropBlock>> CROPS = new LinkedHashMap<>();
   private static final List<EchoBackendRegistryEntry<Block>> BLOCK_ITEMS = new ArrayList<>();

   public static final EchoBackendRegistryEntry<Block> DEAD_SOIL = soil("dead_soil", SoilState.DEAD, MapColor.COLOR_GRAY);
   public static final EchoBackendRegistryEntry<Block> CONTAMINATED_SOIL = soil("contaminated_soil", SoilState.CONTAMINATED, MapColor.TERRACOTTA_PURPLE);
   public static final EchoBackendRegistryEntry<Block> IRRADIATED_SOIL = soil("irradiated_soil", SoilState.IRRADIATED, MapColor.TERRACOTTA_GREEN);
   public static final EchoBackendRegistryEntry<Block> TOXIC_MUD = soil("toxic_mud", SoilState.TOXIC_MUD, MapColor.TERRACOTTA_LIGHT_GREEN);
   public static final EchoBackendRegistryEntry<Block> PURIFIED_SOIL = soil("purified_soil", SoilState.PURIFIED, MapColor.DIRT);
   public static final EchoBackendRegistryEntry<Block> STABILIZED_SOIL = soil("stabilized_soil", SoilState.STABILIZED, MapColor.COLOR_GREEN);
   public static final EchoBackendRegistryEntry<Block> RESTORED_SOIL = soil("restored_soil", SoilState.RESTORED, MapColor.GRASS);

   public static final EchoBackendRegistryEntry<Block> HYDROPONIC_TRAY = tracked(register("hydroponic_tray",
      id -> new HydroponicTrayBlock(properties(id).mapColor(MapColor.COLOR_CYAN).strength(2.0F, 5.0F).sound(SoundType.COPPER).noOcclusion())));
   public static final EchoBackendRegistryEntry<Block> SEED_VAULT_TERMINAL = machine("seed_vault_terminal", ReclamationMachineBlock.MachineKind.SEED_VAULT_TERMINAL, MapColor.COLOR_LIGHT_BLUE);
   public static final EchoBackendRegistryEntry<Block> SOIL_PURIFIER = machine("soil_purifier", ReclamationMachineBlock.MachineKind.SOIL_PURIFIER, MapColor.COLOR_GREEN);
   public static final EchoBackendRegistryEntry<Block> GENE_STABILIZER = machine("gene_stabilizer", ReclamationMachineBlock.MachineKind.GENE_STABILIZER, MapColor.COLOR_PURPLE);
   public static final EchoBackendRegistryEntry<Block> BIO_REACTOR = machine("bio_reactor", ReclamationMachineBlock.MachineKind.BIO_REACTOR, MapColor.PLANT);
   public static final EchoBackendRegistryEntry<Block> GREENHOUSE_CONTROLLER = machine("greenhouse_controller", ReclamationMachineBlock.MachineKind.GREENHOUSE_CONTROLLER, MapColor.COLOR_CYAN);
   public static final EchoBackendRegistryEntry<Block> POLLINATOR_DRONE_DOCK = machine("pollinator_drone_dock", ReclamationMachineBlock.MachineKind.POLLINATOR_DRONE_DOCK, MapColor.GOLD);
   public static final EchoBackendRegistryEntry<Block> SPORE_FILTER = machine("spore_filter", ReclamationMachineBlock.MachineKind.SPORE_FILTER, MapColor.COLOR_LIGHT_GREEN);
   public static final EchoBackendRegistryEntry<Block> COMPOST_RECYCLER = machine("compost_recycler", ReclamationMachineBlock.MachineKind.COMPOST_RECYCLER, MapColor.DIRT);
   public static final EchoBackendRegistryEntry<Block> ECOLOGY_SCANNER = machine("ecology_scanner", ReclamationMachineBlock.MachineKind.ECOLOGY_SCANNER, MapColor.COLOR_LIGHT_BLUE);
   public static final EchoBackendRegistryEntry<Block> GREENHOUSE_GLASS = tracked(register("greenhouse_glass",
      id -> new Block(properties(id).mapColor(MapColor.COLOR_LIGHT_BLUE).strength(0.8F, 1.5F).sound(SoundType.GLASS).noOcclusion().isValidSpawn((state, level, pos, entityType) -> false))));

   public static final EchoBackendRegistryEntry<ReclamationCropBlock> ASH_WHEAT_CROP = crop(CropSpec.byPath("ash_wheat"));
   public static final EchoBackendRegistryEntry<ReclamationCropBlock> HARDROOT_CROP = crop(CropSpec.byPath("hardroot"));
   public static final EchoBackendRegistryEntry<ReclamationCropBlock> GLOW_BEANS_CROP = crop(CropSpec.byPath("glow_beans"));
   public static final EchoBackendRegistryEntry<ReclamationCropBlock> RADLEAF_CROP = crop(CropSpec.byPath("radleaf"));
   public static final EchoBackendRegistryEntry<ReclamationCropBlock> MUTANT_BERRIES_CROP = crop(CropSpec.byPath("mutant_berries"));
   public static final EchoBackendRegistryEntry<ReclamationCropBlock> CRYO_MOSS_CROP = crop(CropSpec.byPath("cryo_moss"));
   public static final EchoBackendRegistryEntry<ReclamationCropBlock> CLEAN_CORN_CROP = crop(CropSpec.byPath("clean_corn"));
   public static final EchoBackendRegistryEntry<ReclamationCropBlock> MEDICINAL_ALOE_CROP = crop(CropSpec.byPath("medicinal_aloe"));
   public static final EchoBackendRegistryEntry<ReclamationCropBlock> FILTER_REED_CROP = crop(CropSpec.byPath("filter_reed"));
   public static final EchoBackendRegistryEntry<ReclamationCropBlock> NEXUS_ORCHID_CROP = crop(CropSpec.byPath("nexus_orchid"));
   public static final EchoBackendRegistryEntry<ReclamationCropBlock> SIGNAL_FUNGUS_CROP = crop(CropSpec.byPath("signal_fungus"));

   private ModBlocks() {
   }

   public static void register(Object eventBus) {
      EchoBackendRegistryBridge.registerEventBus(BLOCKS, eventBus);
   }

   public static List<EchoBackendRegistryEntry<Block>> blockItems() {
      return List.copyOf(BLOCK_ITEMS);
   }

   public static List<EchoBackendRegistryEntry<ReclamationCropBlock>> cropBlocks() {
      return List.copyOf(CROPS.values());
   }

   public static ReclamationCropBlock cropBlock(CropSpec spec) {
      return CROPS.get(spec.path()).get();
   }

   public static Block blockFor(SoilState state) {
      return switch (state) {
         case DEAD -> DEAD_SOIL.get();
         case CONTAMINATED -> CONTAMINATED_SOIL.get();
         case IRRADIATED -> IRRADIATED_SOIL.get();
         case TOXIC_MUD -> TOXIC_MUD.get();
         case PURIFIED -> PURIFIED_SOIL.get();
         case STABILIZED -> STABILIZED_SOIL.get();
         case RESTORED -> RESTORED_SOIL.get();
      };
   }

   private static EchoBackendRegistryEntry<Block> soil(String name, SoilState state, MapColor color) {
      return tracked(register(name, id -> new ReclamationSoilBlock(state, properties(id).mapColor(color).strength(0.6F).sound(SoundType.GRAVEL).randomTicks())));
   }

   private static EchoBackendRegistryEntry<Block> machine(String name, ReclamationMachineBlock.MachineKind kind, MapColor color) {
      return tracked(register(name, id -> new ReclamationMachineBlock(kind, properties(id).mapColor(color).strength(3.0F, 7.0F).sound(SoundType.METAL))));
   }

   private static EchoBackendRegistryEntry<ReclamationCropBlock> crop(CropSpec spec) {
      EchoBackendRegistryEntry<ReclamationCropBlock> crop = EchoBackendRegistryBridge.registerWithId(BLOCKS, spec.path() + "_crop",
         id -> new ReclamationCropBlock(spec, properties(id).mapColor(MapColor.PLANT).noCollision().randomTicks().instabreak().sound(SoundType.CROP)));
      CROPS.put(spec.path(), crop);
      return crop;
   }

   private static EchoBackendRegistryEntry<Block> tracked(EchoBackendRegistryEntry<Block> block) {
      BLOCK_ITEMS.add(block);
      return block;
   }

   private static EchoBackendRegistryEntry<Block> register(String name, java.util.function.Function<net.minecraft.resources.Identifier, Block> factory) {
      return EchoBackendRegistryBridge.registerWithId(BLOCKS, name, factory);
   }

   private static BlockBehaviour.Properties properties(net.minecraft.resources.Identifier id) {
      return BlockBehaviour.Properties.of().setId(ResourceKey.create(Registries.BLOCK, id));
   }
}
