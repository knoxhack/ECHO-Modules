package com.knoxhack.echoindustrialnexus.registry;

import com.knoxhack.echo.adaptercore.EchoBackendFluidBridge;
import com.knoxhack.echo.adaptercore.EchoBackendRegistryEntry;
import com.knoxhack.echoindustrialnexus.EchoIndustrialNexus;
import com.knoxhack.echoindustrialnexus.block.entity.IndustrialMachineBlockEntity;
import java.util.Locale;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.material.Fluid;

public final class ModFluids {
   private static final Object FLUID_TYPES = EchoBackendFluidBridge.createFluidTypeRegistry(EchoIndustrialNexus.MODID);
   private static final Object FLUIDS = EchoBackendFluidBridge.createFluidRegistry(EchoIndustrialNexus.MODID);
   public static final IndustrialFluid DIRTY_WATER = register("dirty_water", IndustrialMachineBlockEntity.FLUID_DIRTY_WATER, 285, 1200, Rarity.COMMON);
   public static final IndustrialFluid CLEAN_WATER = register("clean_water", IndustrialMachineBlockEntity.FLUID_CLEAN_WATER, 285, 1000, Rarity.COMMON);
   public static final IndustrialFluid TOXIC_SLUDGE = register("toxic_sludge", IndustrialMachineBlockEntity.FLUID_TOXIC_SLUDGE, 330, 3600, Rarity.UNCOMMON);
   public static final IndustrialFluid STATIC_FLUID = register("static_fluid", IndustrialMachineBlockEntity.FLUID_STATIC, 220, 1800, Rarity.RARE);
   public static final IndustrialFluid CRYO_GEL = register("cryo_gel", IndustrialMachineBlockEntity.FLUID_CRYO_GEL, 140, 2400, Rarity.UNCOMMON);
   public static final IndustrialFluid COOLANT = register("coolant", IndustrialMachineBlockEntity.FLUID_COOLANT, 175, 900, Rarity.UNCOMMON);
   public static final IndustrialFluid CHEMICAL_SOLVENT = register("chemical_solvent", IndustrialMachineBlockEntity.FLUID_SOLVENT, 315, 1300, Rarity.UNCOMMON);
   public static final IndustrialFluid NEXUS_GEL = register("nexus_gel", IndustrialMachineBlockEntity.FLUID_NEXUS_GEL, 260, 2100, Rarity.RARE);
   public static final IndustrialFluid OIL_RESIDUE = register("oil_residue", IndustrialMachineBlockEntity.FLUID_OIL_RESIDUE, 310, 4200, Rarity.COMMON);
   public static final List<IndustrialFluid> ALL = List.of(
      DIRTY_WATER,
      CLEAN_WATER,
      TOXIC_SLUDGE,
      STATIC_FLUID,
      CRYO_GEL,
      COOLANT,
      CHEMICAL_SOLVENT,
      NEXUS_GEL,
      OIL_RESIDUE
   );

   private ModFluids() {
   }

   public static void register(Object eventBus) {
      EchoBackendFluidBridge.registerEventBus(FLUID_TYPES, eventBus);
      EchoBackendFluidBridge.registerEventBus(FLUIDS, eventBus);
   }

   public static Object resourceFor(int fluidId) {
      IndustrialFluid fluid = byId(fluidId);
      return fluid == null ? EchoBackendFluidBridge.emptyFluidResource() : EchoBackendFluidBridge.fluidResourceOf(fluid.source().get());
   }

   public static int idFor(Object resource) {
      if (EchoBackendFluidBridge.isEmptyFluidResource(resource)) {
         return IndustrialMachineBlockEntity.FLUID_NONE;
      }
      Fluid fluid = EchoBackendFluidBridge.fluidResourceFluid(resource);
      for (IndustrialFluid industrialFluid : ALL) {
         if (industrialFluid.source().get() == fluid || industrialFluid.flowing().get() == fluid) {
            return industrialFluid.id();
         }
      }
      return IndustrialMachineBlockEntity.FLUID_NONE;
   }

   public static IndustrialFluid byId(int id) {
      for (IndustrialFluid fluid : ALL) {
         if (fluid.id() == id) {
            return fluid;
         }
      }
      return null;
   }

   public static int idForName(String raw) {
      if (raw == null || raw.isBlank()) {
         return IndustrialMachineBlockEntity.FLUID_NONE;
      }
      String normalized = raw.strip().toLowerCase(Locale.ROOT);
      Identifier identifier = Identifier.tryParse(normalized);
      String path = identifier == null ? normalized : identifier.getPath();
      for (IndustrialFluid fluid : ALL) {
         if (fluid.name().equals(path)) {
            return fluid.id();
         }
      }
      return IndustrialMachineBlockEntity.FLUID_NONE;
   }

   public static String identifierFor(int id) {
      IndustrialFluid fluid = byId(id);
      return fluid == null ? "" : EchoIndustrialNexus.MODID + ":" + fluid.name();
   }

   public static String displayNameFor(int id) {
      IndustrialFluid fluid = byId(id);
      if (fluid == null) {
         return "Empty";
      }
      StringBuilder label = new StringBuilder();
      for (String word : fluid.name().split("_")) {
         if (word.isBlank()) {
            continue;
         }
         if (label.length() > 0) {
            label.append(' ');
         }
         label.append(Character.toUpperCase(word.charAt(0)));
         if (word.length() > 1) {
            label.append(word.substring(1));
         }
      }
      return label.isEmpty() ? fluid.name() : label.toString();
   }

   public static boolean isNexusFluid(int id) {
      return id == IndustrialMachineBlockEntity.FLUID_STATIC || id == IndustrialMachineBlockEntity.FLUID_NEXUS_GEL;
   }

   public static boolean isHazardousFluid(int id) {
      return id == IndustrialMachineBlockEntity.FLUID_TOXIC_SLUDGE
         || id == IndustrialMachineBlockEntity.FLUID_STATIC
         || id == IndustrialMachineBlockEntity.FLUID_NEXUS_GEL
         || id == IndustrialMachineBlockEntity.FLUID_OIL_RESIDUE;
   }

   public static boolean isPressurizedSafe(int id) {
      return id == IndustrialMachineBlockEntity.FLUID_CLEAN_WATER
         || id == IndustrialMachineBlockEntity.FLUID_COOLANT
         || id == IndustrialMachineBlockEntity.FLUID_CRYO_GEL;
   }

   private static IndustrialFluid register(String name, int id, int temperature, int viscosity, Rarity rarity) {
      EchoBackendRegistryEntry<?> type = EchoBackendFluidBridge.registerFluidType(
         FLUID_TYPES,
         name,
         EchoIndustrialNexus.MODID,
         temperature,
         viscosity,
         rarity
      );
      AtomicReference<EchoBackendRegistryEntry<Fluid>> sourceRef = new AtomicReference<>();
      AtomicReference<EchoBackendRegistryEntry<Fluid>> flowingRef = new AtomicReference<>();
      EchoBackendRegistryEntry<Fluid> source = EchoBackendFluidBridge.registerSourceFluid(
         FLUIDS,
         name,
         type,
         () -> sourceRef.get().get(),
         () -> flowingRef.get().get()
      );
      EchoBackendRegistryEntry<Fluid> flowing = EchoBackendFluidBridge.registerFlowingFluid(
         FLUIDS,
         "flowing_" + name,
         type,
         () -> sourceRef.get().get(),
         () -> flowingRef.get().get()
      );
      sourceRef.set(source);
      flowingRef.set(flowing);
      return new IndustrialFluid(id, name, type, source, flowing);
   }

   public record IndustrialFluid(
      int id,
      String name,
      EchoBackendRegistryEntry<?> type,
      EchoBackendRegistryEntry<Fluid> source,
      EchoBackendRegistryEntry<Fluid> flowing
   ) {
   }
}
