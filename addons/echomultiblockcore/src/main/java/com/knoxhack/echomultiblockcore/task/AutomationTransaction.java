package com.knoxhack.echomultiblockcore.task;

import com.knoxhack.echomultiblockcore.api.AutomationIngredient;
import com.knoxhack.echomultiblockcore.api.AutomationOutput;
import com.knoxhack.echomultiblockcore.block.entity.MultiblockCrateBlockEntity;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.world.item.ItemStack;

public final class AutomationTransaction {
    private final List<AutomationIngredient> inputs;
    private final List<AutomationOutput> outputs;

    public AutomationTransaction(List<AutomationIngredient> inputs, List<AutomationOutput> outputs) {
        this.inputs = List.copyOf(inputs == null ? List.of() : inputs);
        this.outputs = List.copyOf(outputs == null ? List.of() : outputs);
    }

    public Check check(MultiblockCrateBlockEntity input, MultiblockCrateBlockEntity outputCrate) {
        if (!inputs.isEmpty() && input == null) {
            return Check.blocked("Missing input crate.");
        }
        if (!outputs.isEmpty() && outputCrate == null) {
            return Check.blocked("Missing output crate.");
        }
        if (input != null) {
            for (AutomationIngredient ingredient : inputs) {
                if (input.countMatching(ingredient::matches) < ingredient.count()) {
                    return Check.blocked("Missing input: " + ingredient.summary() + ".");
                }
            }
        }
        return checkOutput(outputCrate);
    }

    public Check checkOutput(MultiblockCrateBlockEntity outputCrate) {
        if (!outputs.isEmpty() && outputCrate == null) {
            return Check.blocked("Missing output crate.");
        }
        if (outputCrate != null) {
            for (AutomationOutput output : outputs) {
                ItemStack stack = output.stack();
                if (!stack.isEmpty() && !outputCrate.canInsert(stack)) {
                    return Check.blocked("Output crate is full.");
                }
            }
        }
        return Check.ok();
    }

    public boolean consume(MultiblockCrateBlockEntity input) {
        if (inputs.isEmpty()) {
            return true;
        }
        if (input == null) {
            return false;
        }
        List<ItemStack> consumed = new ArrayList<>();
        for (AutomationIngredient ingredient : inputs) {
            List<ItemStack> consumedForIngredient = consumeIngredient(input, ingredient);
            if (count(consumedForIngredient) < ingredient.count()) {
                rollback(input, consumed);
                rollback(input, consumedForIngredient);
                return false;
            }
            consumed.addAll(consumedForIngredient);
        }
        return true;
    }

    public Commit commit(MultiblockCrateBlockEntity input, MultiblockCrateBlockEntity outputCrate) {
        Check check = check(input, outputCrate);
        if (!check.ready()) {
            return Commit.blocked(check.reason());
        }
        List<ItemStack> consumed = new ArrayList<>();
        for (AutomationIngredient ingredient : inputs) {
            List<ItemStack> consumedForIngredient = consumeIngredient(input, ingredient);
            if (count(consumedForIngredient) < ingredient.count()) {
                rollback(input, consumed);
                rollback(input, consumedForIngredient);
                return Commit.blocked("Inputs changed before task completion.");
            }
            consumed.addAll(consumedForIngredient);
        }
        if (!produce(outputCrate)) {
            rollback(input, consumed);
            return Commit.blocked("Output crate is full.");
        }
        return Commit.done();
    }

    public Commit produceOnly(MultiblockCrateBlockEntity outputCrate) {
        Check check = checkOutput(outputCrate);
        if (!check.ready()) {
            return Commit.blocked(check.reason());
        }
        return produce(outputCrate) ? Commit.done() : Commit.blocked("Output crate is full.");
    }

    public boolean produce(MultiblockCrateBlockEntity outputCrate) {
        if (outputs.isEmpty()) {
            return true;
        }
        if (outputCrate == null) {
            return false;
        }
        List<ItemStack> produced = new ArrayList<>();
        for (AutomationOutput output : outputs) {
            ItemStack stack = output.stack();
            if (stack.isEmpty()) {
                continue;
            }
            int inserted = outputCrate.insertStack(stack.copy());
            if (inserted != stack.getCount()) {
                rollbackOutput(outputCrate, produced);
                return false;
            }
            produced.add(stack.copy());
        }
        return true;
    }

    public List<String> inputSummary() {
        return inputs.stream().map(AutomationIngredient::summary).toList();
    }

    public List<String> outputSummary() {
        return outputs.stream().map(AutomationOutput::summary).toList();
    }

    private static List<ItemStack> consumeIngredient(MultiblockCrateBlockEntity input, AutomationIngredient ingredient) {
        List<ItemStack> consumed = new ArrayList<>();
        int remaining = ingredient.count();
        for (int slot = 0; slot < input.getContainerSize() && remaining > 0; slot++) {
            ItemStack stack = input.getItem(slot);
            if (!stack.isEmpty() && ingredient.matches(stack)) {
                int moved = Math.min(remaining, stack.getCount());
                ItemStack copy = stack.copy();
                copy.setCount(moved);
                consumed.add(copy);
                stack.shrink(moved);
                if (stack.isEmpty()) {
                    input.setItem(slot, ItemStack.EMPTY);
                }
                remaining -= moved;
            }
        }
        input.setChanged();
        return consumed;
    }

    private static int count(List<ItemStack> stacks) {
        return stacks.stream().mapToInt(ItemStack::getCount).sum();
    }

    private static void rollback(MultiblockCrateBlockEntity input, List<ItemStack> consumed) {
        if (input == null) {
            return;
        }
        for (ItemStack stack : consumed) {
            input.insertStack(stack.copy());
        }
    }

    private static void rollbackOutput(MultiblockCrateBlockEntity output, List<ItemStack> produced) {
        if (output == null) {
            return;
        }
        for (ItemStack stack : produced) {
            output.consume(stack.getItem(), stack.getCount());
        }
    }

    public record Check(boolean ready, String reason) {
        public static Check ok() {
            return new Check(true, "");
        }

        public static Check blocked(String reason) {
            return new Check(false, reason == null ? "Task blocked." : reason);
        }
    }

    public record Commit(boolean completed, String reason) {
        public static Commit done() {
            return new Commit(true, "");
        }

        public static Commit blocked(String reason) {
            return new Commit(false, reason == null ? "Task blocked." : reason);
        }
    }
}
