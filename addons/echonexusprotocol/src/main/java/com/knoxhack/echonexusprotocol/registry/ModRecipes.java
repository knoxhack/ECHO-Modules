package com.knoxhack.echonexusprotocol.registry;

import com.knoxhack.echo.adaptercore.EchoBackendRegistryBridge;
import com.knoxhack.echo.adaptercore.EchoBackendRegistryEntry;
import com.knoxhack.echonexusprotocol.EchoNexusProtocol;
import com.knoxhack.echonexusprotocol.recipe.NexusProcessingRecipe;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;

public final class ModRecipes {
   private static final Object SERIALIZERS = EchoBackendRegistryBridge.create(Registries.RECIPE_SERIALIZER, EchoNexusProtocol.MODID);
   private static final Object TYPES = EchoBackendRegistryBridge.create(Registries.RECIPE_TYPE, EchoNexusProtocol.MODID);
   public static final EchoBackendRegistryEntry<RecipeSerializer<NexusProcessingRecipe>> NEXUS_PROCESSING_SERIALIZER = EchoBackendRegistryBridge.register(SERIALIZERS,
      "nexus_processing", () -> new RecipeSerializer(NexusProcessingRecipe.CODEC, NexusProcessingRecipe.STREAM_CODEC)
   );
   public static final EchoBackendRegistryEntry<RecipeType<NexusProcessingRecipe>> NEXUS_PROCESSING_TYPE = EchoBackendRegistryBridge.register(TYPES,
      "nexus_processing", () -> RecipeType.simple(Identifier.fromNamespaceAndPath("echonexusprotocol", "nexus_processing"))
   );

   private ModRecipes() {
   }

   public static void register(Object eventBus) {
      EchoBackendRegistryBridge.registerEventBus(SERIALIZERS, eventBus);
      EchoBackendRegistryBridge.registerEventBus(TYPES, eventBus);
   }
}
