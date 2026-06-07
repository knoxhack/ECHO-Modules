package com.knoxhack.echoorbitalremnants.registry;

import com.knoxhack.echo.adaptercore.EchoBackendRegistryBridge;
import com.knoxhack.echo.adaptercore.EchoBackendRegistryEntry;
import com.knoxhack.echoorbitalremnants.EchoOrbitalRemnants;
import com.knoxhack.echoorbitalremnants.recipe.OrbitalProcessingRecipe;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;

public final class ModRecipes {
    private static final Object RECIPE_TYPES =
            EchoBackendRegistryBridge.create(Registries.RECIPE_TYPE, EchoOrbitalRemnants.MODID);
    private static final Object RECIPE_SERIALIZERS =
            EchoBackendRegistryBridge.create(Registries.RECIPE_SERIALIZER, EchoOrbitalRemnants.MODID);

    public static final EchoBackendRegistryEntry<RecipeType<OrbitalProcessingRecipe>> ORBITAL_PROCESSING_TYPE =
            EchoBackendRegistryBridge.register(RECIPE_TYPES, "orbital_processing",
                    () -> RecipeType.simple(Identifier.fromNamespaceAndPath(EchoOrbitalRemnants.MODID, "orbital_processing")));

    public static final EchoBackendRegistryEntry<RecipeSerializer<OrbitalProcessingRecipe>> ORBITAL_PROCESSING_SERIALIZER =
            EchoBackendRegistryBridge.register(RECIPE_SERIALIZERS, "orbital_processing",
                    () -> new RecipeSerializer<>(OrbitalProcessingRecipe.CODEC, OrbitalProcessingRecipe.STREAM_CODEC));

    private ModRecipes() {
    }

    public static void register(Object eventBus) {
        EchoBackendRegistryBridge.registerEventBus(RECIPE_TYPES, eventBus);
        EchoBackendRegistryBridge.registerEventBus(RECIPE_SERIALIZERS, eventBus);
    }
}
