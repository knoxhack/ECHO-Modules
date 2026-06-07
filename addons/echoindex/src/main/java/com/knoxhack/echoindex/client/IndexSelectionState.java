package com.knoxhack.echoindex.client;

import net.minecraft.resources.Identifier;

public final class IndexSelectionState {
    private Identifier itemId;
    private Identifier recipeId;
    private String machineId = "";
    private String modId = "";
    private long revision;

    public Identifier itemId() {
        return itemId;
    }

    public Identifier recipeId() {
        return recipeId;
    }

    public String machineId() {
        return machineId;
    }

    public String modId() {
        return modId;
    }

    public long revision() {
        return revision;
    }

    public void selectItem(Identifier id) {
        itemId = id;
        if (id != null) {
            modId = id.getNamespace();
        }
        revision++;
    }

    public void selectRecipe(Identifier id) {
        recipeId = id;
        revision++;
    }

    public void selectMachine(String id) {
        machineId = id == null ? "" : id.strip();
        revision++;
    }

    public void selectMod(String id) {
        modId = id == null ? "" : id.strip();
        revision++;
    }
}
