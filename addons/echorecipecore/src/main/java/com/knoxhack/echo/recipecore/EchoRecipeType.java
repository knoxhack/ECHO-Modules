package com.knoxhack.echo.recipecore;

public enum EchoRecipeType {
    CRAFTING("crafting"),
    SMELTING("smelting"),
    BLASTING("blasting"),
    SMOKING("smoking"),
    CAMPFIRE("campfire"),
    STONECUTTING("stonecutting"),
    SMITHING("smithing"),
    MACHINE("machine"),
    MULTIBLOCK("multiblock"),
    RITUAL("ritual"),
    ARCANE("arcane"),
    SALVAGE("salvage"),
    REPAIR("repair"),
    UPGRADE("upgrade"),
    CUSTOM("custom"),
    UNKNOWN("unknown");

    private final String serializedName;

    EchoRecipeType(String serializedName) {
        this.serializedName = serializedName;
    }

    public String serializedName() {
        return serializedName;
    }

    public boolean vanillaLike() {
        return this == CRAFTING
                || this == SMELTING
                || this == BLASTING
                || this == SMOKING
                || this == CAMPFIRE
                || this == STONECUTTING
                || this == SMITHING;
    }

    public boolean machineLike() {
        return this == MACHINE || this == MULTIBLOCK || this == SALVAGE || this == REPAIR || this == UPGRADE;
    }
}
