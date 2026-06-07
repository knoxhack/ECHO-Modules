package com.knoxhack.echoagriculturereclamation.block.entity;

import com.knoxhack.echoagriculturereclamation.content.CropSpec;
import com.knoxhack.echoagriculturereclamation.content.ReclamationContent;
import com.knoxhack.echoagriculturereclamation.content.ReclamationCropLogic;
import com.knoxhack.echoagriculturereclamation.content.SeedProfile;
import com.knoxhack.echoagriculturereclamation.content.SoilState;
import com.knoxhack.echoagriculturereclamation.progress.ReclamationProgress;
import com.knoxhack.echoagriculturereclamation.progress.ReclamationRestoration;
import com.knoxhack.echoagriculturereclamation.menu.HydroponicTrayMenu;
import com.knoxhack.echoagriculturereclamation.registry.ModBlockEntities;
import com.knoxhack.echoagriculturereclamation.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

public class HydroponicTrayBlockEntity extends BaseContainerBlockEntity implements WorldlyContainer {
   public static final int SEED_SLOT = 0;
   public static final int NUTRIENT_SLOT = 1;
   public static final int OUTPUT_SLOT = 2;
   public static final int SLOT_COUNT = 3;
   public static final int DATA_HAS_PROFILE = 0;
   public static final int DATA_AGE = 1;
   public static final int DATA_GROWTH_TICKS = 2;
   public static final int DATA_GROWTH_TICKS_MAX = 3;
   public static final int DATA_NUTRIENT = 4;
   public static final int DATA_NUTRIENT_CAP = 5;
   public static final int DATA_STABILITY = 6;
   public static final int DATA_CONTAMINATION = 7;
   public static final int DATA_GREENHOUSE = 8;
   public static final int DATA_STATUS = 9;
   public static final int DATA_COUNT = 10;
   private static final int[] SLOTS = {SEED_SLOT, NUTRIENT_SLOT, OUTPUT_SLOT};
   private final NonNullList<ItemStack> items = NonNullList.withSize(SLOT_COUNT, ItemStack.EMPTY);
   private SeedProfile profile;
   private int age;
   private int growthTicks;
   private int nutrient;
   private final ContainerData data = new ContainerData() {
      @Override
      public int get(int index) {
         return switch (index) {
            case DATA_HAS_PROFILE -> profile == null ? 0 : 1;
            case DATA_AGE -> age;
            case DATA_GROWTH_TICKS -> growthTicks;
            case DATA_GROWTH_TICKS_MAX -> ReclamationContent.machines().hydroponicGrowthTicks();
            case DATA_NUTRIENT -> nutrient;
            case DATA_NUTRIENT_CAP -> ReclamationContent.machines().hydroponicNutrientCap();
            case DATA_STABILITY -> profile == null ? 0 : profile.stability();
            case DATA_CONTAMINATION -> profile == null ? 0 : profile.contaminationTier();
            case DATA_GREENHOUSE -> level == null ? 0 : ReclamationProgress.greenhouseContext(level, worldPosition).score();
            case DATA_STATUS -> statusCode();
            default -> 0;
         };
      }

      @Override
      public void set(int index, int value) {
      }

      @Override
      public int getCount() {
         return DATA_COUNT;
      }
   };

   public HydroponicTrayBlockEntity(BlockPos pos, BlockState state) {
      super(ModBlockEntities.HYDROPONIC_TRAY.get(), pos, state);
   }

   public static void tick(Level level, BlockPos pos, BlockState state, HydroponicTrayBlockEntity tray) {
      if (level.isClientSide()) {
         return;
      }
      tray.consumeBufferedInputs(null);
      if (tray.profile == null || tray.age >= 7) {
         return;
      }
      tray.growthTicks++;
      if (tray.growthTicks < ReclamationContent.machines().hydroponicGrowthTicks()) {
         return;
      }
      tray.growthTicks = 0;
      var greenhouse = ReclamationProgress.growthGreenhouseContext(level, pos);
      int chance = ReclamationCropLogic.growthChance(tray.profile.spec(), SoilState.STABILIZED, tray.profile, greenhouse, tray.nutrient);
      if (level.getRandom().nextInt(100) < chance) {
         tray.age++;
         if (tray.nutrient > 0) {
            tray.nutrient--;
         }
         tray.sync();
      }
   }

   public boolean insertSeed(Player player, ItemStack stack) {
      if (profile != null) {
         player.sendSystemMessage(Component.literal("ECHO FIELD // Hydroponic tray already holds " + profile.spec().displayName()
            + " at growth " + age + "/7. Harvest or sneak-use to extract the culture."));
         return false;
      }
      SeedProfile next = stack.get(ModItems.seedProfileComponent());
      if (next == null) {
         player.sendSystemMessage(Component.literal("ECHO FIELD // Hydroponic tray needs a recovered seed profile."));
         return false;
      }
      profile = next;
      age = 0;
      growthTicks = 0;
      sync();
      if (!player.getAbilities().instabuild) {
         stack.shrink(1);
      }
      ReclamationProgress.discoverSeed(player, next.spec());
      ReclamationProgress.mark(player, "hydroponics_online");
      ReclamationProgress.mark(player, "first_growth_started");
      player.sendSystemMessage(Component.literal("ECHO FIELD // Hydroponic tray seeded with " + next.spec().displayName()
         + " (stability " + next.stability() + "%, contamination " + next.contaminationTier() + ")."));
      return true;
   }

   public boolean addNutrient(Player player, ItemStack stack) {
      int cap = ReclamationContent.machines().hydroponicNutrientCap();
      if (nutrient >= cap) {
         player.sendSystemMessage(Component.literal("ECHO FIELD // Hydroponic nutrient buffer already full at " + nutrient + "/" + cap + "."));
         return false;
      }
      nutrient = Math.min(cap, nutrient + ReclamationContent.machines().hydroponicNutrientPerMix());
      sync();
      if (!player.getAbilities().instabuild) {
         stack.shrink(1);
      }
      player.sendSystemMessage(Component.literal("ECHO FIELD // Nutrient mix loaded. Tray buffer " + nutrient + "/" + cap
         + "; growth checks spend nutrient only when the crop advances."));
      return true;
   }

   public boolean readyToHarvest() {
      return profile != null && age >= 7;
   }

   public boolean harvest(Player player) {
      if (profile == null) {
         player.sendSystemMessage(Component.literal("ECHO FIELD // Hydroponic tray empty."));
         return true;
      }
      CropSpec spec = profile.spec();
      if (age < 7) {
         player.sendSystemMessage(Component.literal("ECHO FIELD // " + spec.displayName() + " hydroponic growth " + age + "/7. Nutrient " + nutrient + "/" + ReclamationContent.machines().hydroponicNutrientCap() + "."));
         return true;
      }
      boolean stable = ReclamationCropLogic.stable(profile);
      var greenhouse = level == null ? ReclamationProgress.GreenhouseScan.empty().asContext() : ReclamationProgress.greenhouseContext(level, worldPosition);
      int count = ReclamationCropLogic.yield(spec, profile, greenhouse, true);
      give(player, new ItemStack(ModItems.produceFor(spec).get(), Math.max(1, count)));
      ReclamationProgress.recordGrowth(player, spec, stable);
      if (level instanceof ServerLevel serverLevel) {
         ReclamationRestoration.cropMatured(serverLevel, worldPosition, player, spec, profile);
      }
      boolean returnSeed = (!stable && ReclamationProgress.needsStabilizationSeed(player))
         || level != null && ReclamationCropLogic.shouldReturnContaminatedSeed(level.getRandom(), profile, greenhouse);
      if (returnSeed) {
         ItemStack failed = new ItemStack(ModItems.CONTAMINATED_SEED.get());
         failed.set(ModItems.seedProfileComponent(), ReclamationCropLogic.degradedSeed(profile));
         give(player, failed);
         ReclamationProgress.recordStabilizationSeed(player);
      }
      age = 0;
      growthTicks = 0;
      sync();
      player.sendSystemMessage(Component.literal("ECHO FIELD // Hydroponic harvest complete: " + Math.max(1, count) + "x " + spec.displayName()
         + ". " + (returnSeed ? "Seed culture shed a contaminated cutting for stabilization." : "Tray culture reset for regrowth.")));
      return true;
   }

   public boolean serviceFromPollinator(ServerLevel level, int baseGrowthBonus) {
      if (profile == null || age >= 7) {
         return false;
      }
      var greenhouse = ReclamationProgress.growthGreenhouseContext(level, worldPosition);
      int bonus = greenhouse.pollinationBonus(baseGrowthBonus);
      if (bonus <= 0 || !ReclamationCropLogic.canGrow(profile.spec(), SoilState.STABILIZED, profile, greenhouse)) {
         return false;
      }
      int chance = Math.min(95, ReclamationCropLogic.growthChance(profile.spec(), SoilState.STABILIZED, profile, greenhouse, nutrient) + bonus);
      if (level.getRandom().nextInt(100) < chance) {
         age++;
         if (nutrient > 0) {
            nutrient--;
         }
         sync();
      }
      return true;
   }

   public boolean extractSeed(Player player) {
      if (profile == null) {
         player.sendSystemMessage(Component.literal("ECHO FIELD // Hydroponic tray empty."));
         return true;
      }
      ItemStack seed = new ItemStack(ReclamationCropLogic.stable(profile) ? ModItems.STABILIZED_SEED.get() : ModItems.CONTAMINATED_SEED.get());
      seed.set(ModItems.seedProfileComponent(), profile);
      give(player, seed);
      player.sendSystemMessage(Component.literal("ECHO FIELD // Extracted " + (ReclamationCropLogic.stable(profile) ? "stabilized " : "contaminated ")
         + profile.spec().displayName() + " seed culture from tray."));
      profile = null;
      age = 0;
      growthTicks = 0;
      sync();
      return true;
   }

   public String statusLine() {
      if (profile == null) {
         return "ECHO FIELD // Hydroponic tray empty. Insert contaminated or stabilized seed.";
      }
      return "ECHO FIELD // " + profile.spec().displayName() + " age " + age + "/7, stability " + profile.stability()
         + "%, nutrient " + nutrient + "/" + ReclamationContent.machines().hydroponicNutrientCap() + ".";
   }

   public SeedProfile profile() {
      return profile;
   }

   public int age() {
      return age;
   }

   public int nutrient() {
      return nutrient;
   }

   public ContainerData data() {
      return data;
   }

   @Override
   protected Component getDefaultName() {
      return Component.literal("ECHO Hydroponic Tray");
   }

   @Override
   protected NonNullList<ItemStack> getItems() {
      return items;
   }

   @Override
   protected void setItems(NonNullList<ItemStack> replacement) {
      for (int index = 0; index < Math.min(items.size(), replacement.size()); index++) {
         items.set(index, replacement.get(index));
      }
   }

   @Override
   public int getContainerSize() {
      return items.size();
   }

   @Override
   public boolean canPlaceItem(int slot, ItemStack stack) {
      if (stack == null || stack.isEmpty()) {
         return false;
      }
      if (slot == SEED_SLOT) {
         return profile == null && stack.get(ModItems.seedProfileComponent()) != null;
      }
      if (slot == NUTRIENT_SLOT) {
         return stack.is(ModItems.SOIL_NUTRIENT_MIX.get()) && nutrient < ReclamationContent.machines().hydroponicNutrientCap();
      }
      return false;
   }

   @Override
   public int[] getSlotsForFace(Direction side) {
      return SLOTS;
   }

   @Override
   public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction direction) {
      return canPlaceItem(slot, stack);
   }

   @Override
   public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction direction) {
      return slot == OUTPUT_SLOT;
   }

   @Override
   protected @Nullable AbstractContainerMenu createMenu(int containerId, Inventory inventory) {
      return new HydroponicTrayMenu(containerId, inventory, this, data);
   }

   @Override
   public void writeClientSideData(AbstractContainerMenu menu, RegistryFriendlyByteBuf buffer) {
      buffer.writeBlockPos(getBlockPos());
   }

   @Override
   protected void loadAdditional(ValueInput input) {
      super.loadAdditional(input);
      ContainerHelper.loadAllItems(input, items);
      String cropId = input.getStringOr("crop_id", "");
      if (cropId.isBlank()) {
         profile = null;
      } else {
         profile = new SeedProfile(cropId, input.getIntOr("contamination", 0), input.getIntOr("stability", 0));
      }
      age = Math.max(0, Math.min(7, input.getIntOr("age", 0)));
      growthTicks = Math.max(0, input.getIntOr("growth_ticks", 0));
      nutrient = Math.max(0, Math.min(ReclamationContent.machines().hydroponicNutrientCap(), input.getIntOr("nutrient", 0)));
   }

   @Override
   protected void saveAdditional(ValueOutput output) {
      super.saveAdditional(output);
      ContainerHelper.saveAllItems(output, items);
      if (profile != null) {
         output.putString("crop_id", profile.cropId());
         output.putInt("contamination", profile.contaminationTier());
         output.putInt("stability", profile.stability());
      } else {
         output.putString("crop_id", "");
      }
      output.putInt("age", age);
      output.putInt("growth_ticks", growthTicks);
      output.putInt("nutrient", nutrient);
   }

   @Override
   public Packet<ClientGamePacketListener> getUpdatePacket() {
      return ClientboundBlockEntityDataPacket.create(this);
   }

   @Override
   public CompoundTag getUpdateTag(HolderLookup.Provider provider) {
      return saveWithoutMetadata(provider);
   }

   private void sync() {
      setChanged();
      if (level != null && !level.isClientSide()) {
         BlockState state = getBlockState();
         level.sendBlockUpdated(worldPosition, state, state, Block.UPDATE_CLIENTS);
      }
   }

   private static void give(Player player, ItemStack stack) {
      if (!player.getInventory().add(stack)) {
         player.drop(stack, false);
      }
   }

   private void consumeBufferedInputs(@Nullable Player player) {
      if (profile == null) {
         ItemStack seed = items.get(SEED_SLOT);
         SeedProfile buffered = seed.get(ModItems.seedProfileComponent());
         if (buffered != null) {
            profile = buffered;
            age = 0;
            growthTicks = 0;
            seed.shrink(1);
            if (player != null) {
               ReclamationProgress.discoverSeed(player, buffered.spec());
               ReclamationProgress.mark(player, "hydroponics_online");
               ReclamationProgress.mark(player, "first_growth_started");
            }
            sync();
         }
      }
      ItemStack nutrients = items.get(NUTRIENT_SLOT);
      int cap = ReclamationContent.machines().hydroponicNutrientCap();
      if (!nutrients.isEmpty() && nutrients.is(ModItems.SOIL_NUTRIENT_MIX.get()) && nutrient < cap) {
         nutrient = Math.min(cap, nutrient + ReclamationContent.machines().hydroponicNutrientPerMix());
         nutrients.shrink(1);
         sync();
      }
   }

   private int statusCode() {
      if (profile == null) {
         return 0;
      }
      if (age >= 7) {
         return 3;
      }
      if (nutrient <= 0) {
         return 2;
      }
      return 1;
   }
}
