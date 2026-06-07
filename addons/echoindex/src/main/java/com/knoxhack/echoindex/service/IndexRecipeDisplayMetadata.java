package com.knoxhack.echoindex.service;

import com.knoxhack.echocore.api.index.IndexRecipeSlot;
import com.knoxhack.echocore.api.index.IndexSlotRole;
import com.knoxhack.echocore.api.index.IndexMachineLayout;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

public record IndexRecipeDisplayMetadata(
        Identifier recipeId,
        IndexRecipeLayoutType type,
        int width,
        int height,
        List<List<ItemStack>> cells,
        ItemStack machine,
        ItemStack output,
        IndexMachineLayout machineLayout) {
    public IndexRecipeDisplayMetadata {
        type = type == null ? IndexRecipeLayoutType.GENERIC : type;
        width = Math.max(0, width);
        height = Math.max(0, height);
        cells = cells == null ? List.of() : cells.stream()
                .map(cell -> cell == null ? List.<ItemStack>of() : cell.stream()
                        .map(stack -> stack == null ? ItemStack.EMPTY : stack.copy())
                        .filter(stack -> !stack.isEmpty())
                        .toList())
                .toList();
        machine = machine == null ? ItemStack.EMPTY : machine.copy();
        output = output == null ? ItemStack.EMPTY : output.copy();
    }

    public IndexRecipeDisplayMetadata(Identifier recipeId, IndexRecipeLayoutType type, int width, int height,
            List<List<ItemStack>> cells, ItemStack machine, ItemStack output) {
        this(recipeId, type, width, height, cells, machine, output, null);
    }

    public static IndexRecipeDisplayMetadata generic(Identifier recipeId) {
        return new IndexRecipeDisplayMetadata(recipeId, IndexRecipeLayoutType.GENERIC, 0, 0,
                List.of(), ItemStack.EMPTY, ItemStack.EMPTY, null);
    }

    public boolean vanillaLayout() {
        return type != IndexRecipeLayoutType.GENERIC;
    }

    public boolean hasMachineLayout() {
        return machineLayout != null && !machineLayout.empty();
    }

    public IndexRecipeDisplayMetadata withMachineLayout(IndexMachineLayout layout) {
        return new IndexRecipeDisplayMetadata(recipeId, type, width, height, cells, machine, output, layout);
    }

    public boolean hasRenderableInputCells() {
        for (List<ItemStack> cell : cells) {
            for (ItemStack stack : cell) {
                if (stack != null && !stack.isEmpty()) {
                    return true;
                }
            }
        }
        return false;
    }

    public IndexRecipeDisplayMetadata withFallbackInputCellsFromSlots(List<IndexRecipeSlot> slots) {
        if (hasRenderableInputCells()) {
            return this;
        }
        List<List<ItemStack>> fallbackCells = inputCellsFromSlots(slots);
        if (fallbackCells.isEmpty()) {
            return this;
        }
        int fallbackWidth = fallbackWidth();
        int fallbackHeight = fallbackHeight(fallbackWidth, fallbackCells.size());
        int targetCells = Math.max(1, fallbackWidth * fallbackHeight);
        List<List<ItemStack>> normalizedCells = new ArrayList<>(targetCells);
        for (int i = 0; i < targetCells; i++) {
            normalizedCells.add(i < fallbackCells.size() ? fallbackCells.get(i) : List.of());
        }
        return new IndexRecipeDisplayMetadata(recipeId, type, fallbackWidth, fallbackHeight,
                normalizedCells, machine, output, machineLayout);
    }

    private int fallbackWidth() {
        if (width > 0) {
            return width;
        }
        return switch (type) {
            case CRAFTING_SHAPED, CRAFTING_SHAPELESS, SMITHING -> 3;
            default -> 1;
        };
    }

    private int fallbackHeight(int fallbackWidth, int cellCount) {
        if (height > 0) {
            return height;
        }
        return switch (type) {
            case CRAFTING_SHAPED, CRAFTING_SHAPELESS, SMITHING ->
                    Math.max(1, Math.min(3, (cellCount + Math.max(1, fallbackWidth) - 1) / Math.max(1, fallbackWidth)));
            default -> 1;
        };
    }

    private static List<List<ItemStack>> inputCellsFromSlots(List<IndexRecipeSlot> slots) {
        if (slots == null || slots.isEmpty()) {
            return List.of();
        }
        List<List<ItemStack>> fallbackCells = new ArrayList<>();
        for (IndexRecipeSlot slot : slots) {
            if (slot == null || slot.role() != IndexSlotRole.INPUT) {
                continue;
            }
            List<ItemStack> stacks = slot.stacks().stream()
                    .filter(stack -> stack != null && !stack.isEmpty())
                    .map(ItemStack::copy)
                    .toList();
            if (!stacks.isEmpty()) {
                fallbackCells.add(stacks);
            }
        }
        return List.copyOf(fallbackCells);
    }
}
