package com.knoxhack.echoagriculturereclamation.block;

import com.knoxhack.echoagriculturereclamation.content.CropSpec;
import com.knoxhack.echoagriculturereclamation.content.ReclamationContent;
import com.knoxhack.echoagriculturereclamation.content.ReclamationMetrics;
import com.knoxhack.echoagriculturereclamation.content.SeedProfile;
import com.knoxhack.echoagriculturereclamation.block.entity.ReclamationMachineBlockEntity;
import com.knoxhack.echoagriculturereclamation.entity.PollinatorDroneEntity;
import com.knoxhack.echoagriculturereclamation.integration.ReclamationCrossAddonIntegration;
import com.knoxhack.echoagriculturereclamation.progress.ReclamationProgress;
import com.knoxhack.echoagriculturereclamation.progress.ReclamationRestoration;
import com.knoxhack.echoagriculturereclamation.registry.ModItems;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jspecify.annotations.Nullable;

public class ReclamationMachineBlock extends Block implements EntityBlock {
   private final MachineKind kind;

   public ReclamationMachineBlock(MachineKind kind, Properties properties) {
      super(properties);
      this.kind = kind;
   }

   public MachineKind kind() {
      return kind;
   }

   @Override
   public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
      return new ReclamationMachineBlockEntity(pos, state);
   }

   @Override
   public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
      return (tickLevel, pos, blockState, blockEntity) -> {
         if (blockEntity instanceof ReclamationMachineBlockEntity machine) {
            ReclamationMachineBlockEntity.tick(tickLevel, pos, blockState, machine);
         }
      };
   }

   @Override
   protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
      if (level.isClientSide()) {
         return InteractionResult.SUCCESS;
      }
      if (level.getBlockEntity(pos) instanceof ReclamationMachineBlockEntity machine) {
         machine.quickUse(player, stack);
         return InteractionResult.SUCCESS_SERVER;
      }
      return InteractionResult.CONSUME;
   }

   @Override
   protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
      if (level.isClientSide()) {
         return InteractionResult.SUCCESS;
      }
      if (level.getBlockEntity(pos) instanceof ReclamationMachineBlockEntity machine) {
         if (player.isShiftKeyDown()) {
            machine.quickUse(player, ItemStack.EMPTY);
         } else if (machine instanceof MenuProvider provider) {
            player.openMenu(provider);
         }
         return InteractionResult.SUCCESS_SERVER;
      }
      return InteractionResult.CONSUME;
   }

   private InteractionResult analyzeSeedCapsule(ItemStack stack, Player player) {
      if (stack.isEmpty() || !stack.is(ModItems.RECOVERED_SEED_CAPSULE.get())) {
         player.sendSystemMessage(Component.literal("ECHO FIELD // Seed Vault Terminal expects Recovered Seed Capsule."));
         return InteractionResult.CONSUME;
      }
      CropSpec spec = ReclamationCrossAddonIntegration.recoveredCrop(player, player.getRandom());
      SeedProfile profile = ReclamationCrossAddonIntegration.recoveredProfile(player, spec, player.getRandom());
      ItemStack seed = new ItemStack(ModItems.CONTAMINATED_SEED.get());
      seed.set(ModItems.seedProfileComponent(), profile);
      give(player, seed);
      if (!player.getAbilities().instabuild) {
         stack.shrink(1);
      }
      ReclamationProgress.discoverSeed(player, spec);
      player.sendSystemMessage(Component.literal("ECHO FIELD // Seed identified: " + spec.displayName() + " (" + spec.category().displayName()
         + "), contamination " + profile.contaminationTier() + ", stability " + profile.stability() + "%. Known seeds "
         + ReclamationProgress.knownSeeds(player).size() + "/" + CropSpec.ALL.size() + "."));
      return InteractionResult.SUCCESS_SERVER;
   }

   private InteractionResult purifySoil(ItemStack stack, Level level, BlockPos pos, Player player) {
      if (stack.isEmpty() || !stack.is(ModItems.PURIFICATION_ENZYME.get()) && !stack.is(ModItems.SOIL_NUTRIENT_MIX.get())) {
         player.sendSystemMessage(Component.literal("ECHO FIELD // Load Purification Enzyme or Soil Nutrient Mix."));
         return InteractionResult.CONSUME;
      }
      boolean enzyme = stack.is(ModItems.PURIFICATION_ENZYME.get());
      int changed = ReclamationRestoration.purifyArea(
         level,
         pos,
         ReclamationContent.machines().soilPurifierRadius(),
         enzyme
            ? ReclamationContent.machines().soilPurifierEnzymeBlocks()
            : ReclamationContent.machines().soilPurifierNutrientBlocks()
      );
      if (changed == 0) {
         player.sendSystemMessage(Component.literal("ECHO FIELD // Soil purification found no dead, contaminated, irradiated, or toxic reclamation soil in range."));
         return InteractionResult.CONSUME;
      }
      if (changed > 0 && !player.getAbilities().instabuild) {
         stack.shrink(1);
      }
      ReclamationProgress.mark(player, "soil_analyzed");
      ReclamationProgress.add(player, "soil_purified", changed);
      player.sendSystemMessage(Component.literal("ECHO FIELD // Soil purification pass converted " + changed + " blocks using "
         + (enzyme ? "Purification Enzyme" : "Soil Nutrient Mix") + ". Use recovered seeds on purified or stabilized soil."));
      return InteractionResult.SUCCESS_SERVER;
   }

   private InteractionResult stabilizeSeed(ItemStack stack, Player player) {
      SeedProfile profile = stack.get(ModItems.seedProfileComponent());
      if (stack.isEmpty() || profile == null || !stack.is(ModItems.CONTAMINATED_SEED.get())) {
         player.sendSystemMessage(Component.literal("ECHO FIELD // Insert a contaminated seed profile."));
         return InteractionResult.CONSUME;
      }
      int catalystSlot = catalystSlot(player);
      if (catalystSlot < 0 && !player.getAbilities().instabuild) {
         player.sendSystemMessage(Component.literal("ECHO FIELD // Gene Stabilizer needs Gene Sample or Bio-Gel in inventory before it can process the seed."));
         return InteractionResult.CONSUME;
      }
      ItemStack stable = new ItemStack(ModItems.STABILIZED_SEED.get());
      stable.set(ModItems.seedProfileComponent(), profile.stabilized());
      give(player, stable);
      if (!player.getAbilities().instabuild) {
         player.getInventory().getItem(catalystSlot).shrink(1);
         stack.shrink(1);
      }
      ReclamationProgress.recordStabilization(player);
      player.sendSystemMessage(Component.literal("ECHO FIELD // " + profile.spec().displayName() + " seed stabilized: contamination 0, stability 100%. Plant it for safer yield and restoration pressure."));
      return InteractionResult.SUCCESS_SERVER;
   }

   private InteractionResult runBioReactor(ItemStack stack, Player player) {
      if (stack.isEmpty() || !isOrganic(stack) && !stack.is(ModItems.GENE_SAMPLE.get())) {
         player.sendSystemMessage(Component.literal("ECHO FIELD // Bio-Reactor needs crop matter, contaminated seed, or Gene Sample."));
         return InteractionResult.CONSUME;
      }
      MachineOutput output = bioReactorOutput(stack);
      if (!player.getAbilities().instabuild) {
         stack.shrink(1);
      }
      giveAll(player, output.stacks());
      if (output.bioGel() > 0) {
         ReclamationProgress.add(player, "bio_gel_created", output.bioGel());
      }
      ReclamationProgress.mark(player, "bio_reactor_online");
      player.sendSystemMessage(Component.literal("ECHO FIELD // Bio-Reactor processed " + output.inputName()
         + " into " + describe(output.stacks()) + ". Feed catalysts to the Gene Stabilizer or purifier routes."));
      return InteractionResult.SUCCESS_SERVER;
   }

   private InteractionResult runCompostRecycler(ItemStack stack, Player player) {
      if (stack.isEmpty() || !isOrganic(stack)) {
         player.sendSystemMessage(Component.literal("ECHO FIELD // Compost Recycler needs crop matter or rejected seed mass."));
         return InteractionResult.CONSUME;
      }
      MachineOutput output = compostRecyclerOutput(stack);
      if (!player.getAbilities().instabuild) {
         stack.shrink(1);
      }
      giveAll(player, output.stacks());
      if (output.nutrientMix() > 0) {
         ReclamationProgress.add(player, "nutrient_mix_created", output.nutrientMix());
      }
      ReclamationProgress.mark(player, "compost_recycler_online");
      player.sendSystemMessage(Component.literal("ECHO FIELD // Compost Recycler processed " + output.inputName()
         + " into " + describe(output.stacks()) + ". Use nutrient outputs in Hydroponic Trays or Soil Purifier passes."));
      return InteractionResult.SUCCESS_SERVER;
   }

   private void scanGreenhouse(Level level, BlockPos pos, Player player) {
      ReclamationProgress.GreenhouseScan scan = ReclamationProgress.scanGreenhouse(level, pos);
      ReclamationProgress.GreenhouseContext context = scan.asContext();
      if (level instanceof ServerLevel serverLevel) {
         ReclamationProgress.recordGreenhouseZone(serverLevel, pos, scan);
         context = ReclamationProgress.greenhouseContext(serverLevel, pos);
      }
      int score = context.score();
      ReclamationProgress.max(player, "greenhouse_safety", score);
      if (score >= ReclamationContent.progression().greenhouseSafeThreshold()) {
         ReclamationProgress.mark(player, "greenhouse_online");
      }
      player.sendSystemMessage(Component.literal("ECHO FIELD // Greenhouse safety " + score + "/100 ("
         + context.summaryLabel() + ", " + scan.enclosureLabel() + " enclosure, " + scan.glass() + " glass, "
         + countLabel(scan.filters(), "filter") + ", " + countLabel(scan.activeDocks(), "active dock") + ", "
         + countLabel(scan.idleDocks(), "idle dock") + ", " + countLabel(scan.deployedDrones(), "drone") + ", "
         + countLabel(scan.serviceTargets(), "service target") + "). " + greenhouseAdvice(context)));
   }

   private void scanPollinatorDock(Level level, BlockPos pos, Player player) {
      if (!(level instanceof ServerLevel serverLevel)) {
         return;
      }
      if (player.isShiftKeyDown()) {
         int recalled = PollinatorDroneEntity.recallDrones(serverLevel, pos);
         player.sendSystemMessage(Component.literal("ECHO FIELD // Pollinator dock recall: " + recalled
            + " drone" + (recalled == 1 ? "" : "s") + " recalled."));
         return;
      }
      PollinatorDroneEntity drone = PollinatorDroneEntity.deployOrFind(serverLevel, pos);
      ReclamationProgress.GreenhouseContext context = ReclamationProgress.greenhouseContext(level, pos);
      int targets = ReclamationProgress.pollinationTargets(level, pos);
      int serviceTargets = ReclamationProgress.pollinationServiceTargets(level, pos);
      String activity = targets > 0 ? "active" : "idle";
      player.sendSystemMessage(Component.literal("ECHO FIELD // Pollinator dock " + activity + ": " + targets
         + " crop/tray target" + (targets == 1 ? "" : "s") + ", " + countLabel(serviceTargets, "service target")
         + ". Drone " + drone.statusLine()
         + ". Greenhouse " + context.summaryLabel() + ", safety " + context.score() + "/100. " + context.nextAction()));
   }

   private void scanEcology(Level level, BlockPos pos, Player player) {
      ReclamationProgress.GreenhouseContext greenhouse = ReclamationProgress.greenhouseContext(level, pos);
      ReclamationProgress.mark(player, "soil_analyzed");
      if (level instanceof ServerLevel serverLevel) {
         ReclamationRestoration.scanPulse(serverLevel, pos, player, greenhouse);
      }
      ReclamationMetrics metrics = ReclamationProgress.metrics(player);
      player.sendSystemMessage(Component.literal("ECHO FIELD // Soil " + metrics.soilLabel() + ", greenhouse " + metrics.greenhouseSafety()
         + "% (" + greenhouse.summaryLabel() + ", " + countLabel(greenhouse.scan().deployedDrones(), "drone") + ", "
         + countLabel(greenhouse.scan().serviceTargets(), "service target")
         + "), seed stability " + metrics.cropStability() + "%, food security "
         + metrics.foodSecurity() + "%, restoration " + metrics.restorationScore() + "%. " + restorationAdvice(metrics.restorationScore())));
   }

   private static String greenhouseAdvice(ReclamationProgress.GreenhouseContext context) {
      return context.nextAction();
   }

   private static String countLabel(int count, String noun) {
      return count + " " + noun + (count == 1 ? "" : "s");
   }

   private static String restorationAdvice(int score) {
      if (score < ReclamationContent.progression().purifyThreshold()) {
         return "Next threshold: purification pressure at " + ReclamationContent.progression().purifyThreshold() + "%.";
      }
      if (score < ReclamationContent.progression().stabilizeThreshold()) {
         return "Next threshold: stabilization pressure at " + ReclamationContent.progression().stabilizeThreshold() + "%.";
      }
      if (score < ReclamationContent.progression().restoreThreshold()) {
         return "Next threshold: restored soil pressure at " + ReclamationContent.progression().restoreThreshold() + "%.";
      }
      return "Chunk restoration threshold reached.";
   }

   private static boolean isOrganic(ItemStack stack) {
      return stack.is(ModItems.CONTAMINATED_SEED.get())
         || stack.is(ModItems.STABILIZED_SEED.get())
         || produceSpec(stack) != null;
   }

   private static MachineOutput bioReactorOutput(ItemStack stack) {
      int organic = ReclamationContent.machines().bioReactorOrganicOutput();
      if (stack.is(ModItems.GENE_SAMPLE.get())) {
         int amount = ReclamationContent.machines().bioReactorGeneSampleOutput();
         return MachineOutput.bio("Gene Sample", new ItemStack(ModItems.BIO_GEL.get(), amount), amount);
      }
      CropSpec spec = produceSpec(stack);
      if (spec == null) {
         return MachineOutput.bio("seed mass", new ItemStack(ModItems.BIO_GEL.get(), organic), organic);
      }
      return switch (spec.path()) {
         case "medicinal_aloe" -> MachineOutput.bio(
            spec.displayName(),
            listOf(
               new ItemStack(ModItems.BIO_GEL.get(), organic),
               optionalBridge("echoashfallprotocol", "bandage", 1)
            ),
            organic
         );
         case "signal_fungus" -> {
            int amount = Math.max(organic + 1, 2);
            yield MachineOutput.bio(spec.displayName(), new ItemStack(ModItems.BIO_GEL.get(), amount), amount);
         }
         case "cryo_moss" -> MachineOutput.bio(
            spec.displayName(),
            listOf(
               new ItemStack(ModItems.BIO_GEL.get(), organic),
               new ItemStack(ModItems.PURIFICATION_ENZYME.get())
            ),
            organic
         );
         case "nexus_orchid" -> MachineOutput.other(
            spec.displayName(),
            listOf(
               new ItemStack(ModItems.BIO_GEL.get(), organic),
               new ItemStack(ModItems.GENE_SAMPLE.get()),
               optionalBridge("echonexusprotocol", "nexus_gel", 1)
            )
         ).withBioGel(organic);
         default -> MachineOutput.bio(spec.displayName(), new ItemStack(ModItems.BIO_GEL.get(), organic), organic);
      };
   }

   private static MachineOutput compostRecyclerOutput(ItemStack stack) {
      int compost = ReclamationContent.machines().compostRecyclerOutput();
      CropSpec spec = produceSpec(stack);
      if (spec == null) {
         return MachineOutput.nutrient("seed mass", new ItemStack(ModItems.SOIL_NUTRIENT_MIX.get(), compost), compost);
      }
      return switch (spec.path()) {
         case "filter_reed" -> {
            int amount = Math.max(compost + 2, 3);
            yield MachineOutput.nutrient(
               spec.displayName(),
               listOf(
                  new ItemStack(ModItems.SOIL_NUTRIENT_MIX.get(), amount),
                  optionalBridge("echoashfallprotocol", "plant_fiber", 1)
               ),
               amount
            );
         }
         case "cryo_moss", "signal_fungus" -> {
            int amount = Math.max(compost + 1, 2);
            yield MachineOutput.nutrient(spec.displayName(), new ItemStack(ModItems.SOIL_NUTRIENT_MIX.get(), amount), amount);
         }
         default -> MachineOutput.nutrient(spec.displayName(), new ItemStack(ModItems.SOIL_NUTRIENT_MIX.get(), compost), compost);
      };
   }

   private static CropSpec produceSpec(ItemStack stack) {
      for (CropSpec spec : CropSpec.ALL) {
         if (stack.is(ModItems.produceFor(spec).get())) {
            return spec;
         }
      }
      return null;
   }

   private static ItemStack optionalBridge(String namespace, String path, int count) {
      if (count <= 0) {
         return ItemStack.EMPTY;
      }
      Item item = BuiltInRegistries.ITEM.getOptional(Identifier.fromNamespaceAndPath(namespace, path)).orElse(Items.AIR);
      return item == Items.AIR ? ItemStack.EMPTY : new ItemStack(item, count);
   }

   private static List<ItemStack> listOf(ItemStack... stacks) {
      List<ItemStack> result = new ArrayList<>();
      for (ItemStack stack : stacks) {
         if (!stack.isEmpty()) {
            result.add(stack);
         }
      }
      return result;
   }

   private static String describe(List<ItemStack> stacks) {
      if (stacks.isEmpty()) {
         return "no usable output";
      }
      List<String> parts = new ArrayList<>();
      for (ItemStack stack : stacks) {
         if (!stack.isEmpty()) {
            parts.add(stack.getCount() + "x " + stack.getHoverName().getString());
         }
      }
      return String.join(", ", parts);
   }

   private static int catalystSlot(Player player) {
      int geneSample = slotFor(player, ModItems.GENE_SAMPLE.get());
      return geneSample >= 0 ? geneSample : slotFor(player, ModItems.BIO_GEL.get());
   }

   private static int slotFor(Player player, Item item) {
      for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
         ItemStack stack = player.getInventory().getItem(slot);
         if (!stack.isEmpty() && stack.is(item)) {
            return slot;
         }
      }
      return -1;
   }

   private static void give(Player player, ItemStack stack) {
      if (!player.getInventory().add(stack)) {
         player.drop(stack, false);
      }
   }

   private static void giveAll(Player player, List<ItemStack> stacks) {
      for (ItemStack stack : stacks) {
         if (!stack.isEmpty()) {
            give(player, stack.copy());
         }
      }
   }

   private record MachineOutput(String inputName, List<ItemStack> stacks, int bioGel, int nutrientMix) {
      private static MachineOutput bio(String inputName, ItemStack stack, int bioGel) {
         return bio(inputName, listOf(stack), bioGel);
      }

      private static MachineOutput bio(String inputName, List<ItemStack> stacks, int bioGel) {
         return new MachineOutput(inputName, stacks, bioGel, 0);
      }

      private static MachineOutput nutrient(String inputName, ItemStack stack, int nutrientMix) {
         return nutrient(inputName, listOf(stack), nutrientMix);
      }

      private static MachineOutput nutrient(String inputName, List<ItemStack> stacks, int nutrientMix) {
         return new MachineOutput(inputName, stacks, 0, nutrientMix);
      }

      private static MachineOutput other(String inputName, ItemStack stack) {
         return other(inputName, listOf(stack));
      }

      private static MachineOutput other(String inputName, List<ItemStack> stacks) {
         return new MachineOutput(inputName, stacks, 0, 0);
      }

      private MachineOutput withBioGel(int amount) {
         return new MachineOutput(inputName, stacks, amount, nutrientMix);
      }
   }

   public enum MachineKind implements StringRepresentable {
      SEED_VAULT_TERMINAL("seed_vault_terminal", "Seed Vault Terminal"),
      SOIL_PURIFIER("soil_purifier", "Soil Purifier"),
      GENE_STABILIZER("gene_stabilizer", "Gene Stabilizer"),
      BIO_REACTOR("bio_reactor", "Bio-Reactor"),
      GREENHOUSE_CONTROLLER("greenhouse_controller", "Greenhouse Controller"),
      POLLINATOR_DRONE_DOCK("pollinator_drone_dock", "Pollinator Drone Dock"),
      SPORE_FILTER("spore_filter", "Spore Filter"),
      COMPOST_RECYCLER("compost_recycler", "Compost Recycler"),
      ECOLOGY_SCANNER("ecology_scanner", "Ecology Scanner");

      private final String serializedName;
      private final String displayName;

      MachineKind(String serializedName, String displayName) {
         this.serializedName = serializedName;
         this.displayName = displayName;
      }

      public String displayName() {
         return displayName;
      }

      @Override
      public String getSerializedName() {
         return serializedName.toLowerCase(Locale.ROOT);
      }
   }
}
