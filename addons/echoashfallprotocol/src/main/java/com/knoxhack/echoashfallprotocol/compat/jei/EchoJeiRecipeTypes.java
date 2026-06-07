package com.knoxhack.echoashfallprotocol.compat.jei;

import com.knoxhack.echoashfallprotocol.EchoAshfallProtocol;
import mezz.jei.api.recipe.types.IRecipeType;

import java.util.List;

public final class EchoJeiRecipeTypes {
    public static final IRecipeType<EchoJeiRecipe> HAND_RECYCLER =
            IRecipeType.create(EchoAshfallProtocol.MODID, "hand_recycler", EchoJeiRecipe.class);
    public static final IRecipeType<EchoJeiRecipe> WATER_PURIFIER =
            IRecipeType.create(EchoAshfallProtocol.MODID, "water_purifier", EchoJeiRecipe.class);
    public static final IRecipeType<EchoJeiRecipe> WATER_COLLECTION =
            IRecipeType.create(EchoAshfallProtocol.MODID, "water_collection", EchoJeiRecipe.class);
    public static final IRecipeType<EchoJeiRecipe> THERMAL_BURNER =
            IRecipeType.create(EchoAshfallProtocol.MODID, "thermal_burner", EchoJeiRecipe.class);
    public static final IRecipeType<EchoJeiRecipe> MICRO_GENERATOR =
            IRecipeType.create(EchoAshfallProtocol.MODID, "micro_generator", EchoJeiRecipe.class);
    public static final IRecipeType<EchoJeiRecipe> FILTER_WORKBENCH =
            IRecipeType.create(EchoAshfallProtocol.MODID, "filter_workbench", EchoJeiRecipe.class);
    public static final IRecipeType<EchoJeiRecipe> SCRAP_PRESS =
            IRecipeType.create(EchoAshfallProtocol.MODID, "scrap_press", EchoJeiRecipe.class);
    public static final IRecipeType<EchoJeiRecipe> ORE_GRINDER =
            IRecipeType.create(EchoAshfallProtocol.MODID, "ore_grinder", EchoJeiRecipe.class);
    public static final IRecipeType<EchoJeiRecipe> ISOTOPE_REFINER =
            IRecipeType.create(EchoAshfallProtocol.MODID, "isotope_refiner", EchoJeiRecipe.class);
    public static final IRecipeType<EchoJeiRecipe> RADIATION_CLEANSER =
            IRecipeType.create(EchoAshfallProtocol.MODID, "radiation_cleanser", EchoJeiRecipe.class);
    public static final IRecipeType<EchoJeiRecipe> CRYSTALLINE_SYNTHESIZER =
            IRecipeType.create(EchoAshfallProtocol.MODID, "crystalline_synthesizer", EchoJeiRecipe.class);
    public static final IRecipeType<EchoJeiRecipe> DEEP_CORE_MINER =
            IRecipeType.create(EchoAshfallProtocol.MODID, "deep_core_miner", EchoJeiRecipe.class);

    public static final List<IRecipeType<EchoJeiRecipe>> ALL = List.of(
            HAND_RECYCLER,
            WATER_PURIFIER,
            WATER_COLLECTION,
            THERMAL_BURNER,
            MICRO_GENERATOR,
            FILTER_WORKBENCH,
            SCRAP_PRESS,
            ORE_GRINDER,
            ISOTOPE_REFINER,
            RADIATION_CLEANSER,
            CRYSTALLINE_SYNTHESIZER,
            DEEP_CORE_MINER
    );

    private EchoJeiRecipeTypes() {
    }
}
