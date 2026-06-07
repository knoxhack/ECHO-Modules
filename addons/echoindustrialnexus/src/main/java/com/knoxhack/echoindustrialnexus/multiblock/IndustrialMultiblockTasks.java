package com.knoxhack.echoindustrialnexus.multiblock;

import com.knoxhack.echoindustrialnexus.EchoIndustrialNexus;
import com.knoxhack.echoindustrialnexus.integration.IndustrialNexusArrayEffectHandler;
import com.knoxhack.echomultiblockcore.api.AutomationEffectHandlers;
import net.minecraft.resources.Identifier;

public final class IndustrialMultiblockTasks {
   public static final Identifier PROCESS_SCRAP_INTO_SCRAP_PLATE = EchoIndustrialNexus.id("process_scrap_into_scrap_plate");
   public static final Identifier PRESS_SCRAP_PLATE_INTO_REFINED_PLATE = EchoIndustrialNexus.id("press_scrap_plate_into_refined_plate");
   public static final Identifier WELD_REINFORCED_MACHINE_FRAME = EchoIndustrialNexus.id("weld_reinforced_machine_frame");
   public static final Identifier ASSEMBLE_PRECISION_CIRCUIT = EchoIndustrialNexus.id("assemble_precision_circuit");
   public static final Identifier ENCODE_RECIPE_MATRIX_SHARD = EchoIndustrialNexus.id("encode_recipe_matrix_shard");
   public static final Identifier STABILIZE_HYBRID_THERMAL_CORE = EchoIndustrialNexus.id("stabilize_hybrid_thermal_core");
   public static final Identifier FORGE_CORE_KEY_ASSEMBLY = EchoIndustrialNexus.id("forge_core_key_assembly");

   private IndustrialMultiblockTasks() {
   }

   public static void register() {
      AutomationEffectHandlers.register(IndustrialNexusArrayEffectHandler.INSTANCE);
      EchoIndustrialNexus.LOGGER.debug("Industrial Nexus multiblock automation tasks are data-driven through MultiblockCore automation recipes.");
   }
}
