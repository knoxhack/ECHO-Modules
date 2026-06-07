package com.knoxhack.echomultiblockcore.integration;

import com.knoxhack.echocore.api.index.IIndexContentProvider;
import com.knoxhack.echocore.api.index.IndexBuildContext;
import com.knoxhack.echocore.api.index.IndexContentSnapshot;
import com.knoxhack.echocore.api.index.IndexMachineLayoutTemplates;
import com.knoxhack.echocore.api.index.IndexRecipeCategory;
import com.knoxhack.echocore.api.index.IndexRecipeSlot;
import com.knoxhack.echocore.api.index.IndexRecipeView;
import com.knoxhack.echocore.api.index.IndexSlotRole;
import com.knoxhack.echomultiblockcore.EchoMultiblockCore;
import com.knoxhack.echomultiblockcore.api.AutomationIngredient;
import com.knoxhack.echomultiblockcore.api.AutomationOutput;
import com.knoxhack.echomultiblockcore.api.AutomationRecipeRegistry;
import com.knoxhack.echomultiblockcore.api.CapabilityRequirement;
import com.knoxhack.echomultiblockcore.api.MultiblockAutomationRecipe;
import com.knoxhack.echomultiblockcore.api.MultiblockDefinition;
import com.knoxhack.echomultiblockcore.api.RobotToolType;
import com.knoxhack.echomultiblockcore.content.MultiblockContent;
import com.knoxhack.echomultiblockcore.registry.ModBlocks;
import com.knoxhack.echomultiblockcore.registry.ModItems;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public enum MultiblockIndexProvider implements IIndexContentProvider {
    INSTANCE;

    private static final int ACCENT = 0xFF66E8FF;

    @Override
    public Identifier id() {
        return EchoMultiblockCore.id("provider/index_recipes");
    }

    @Override
    public IndexContentSnapshot snapshot(IndexBuildContext context) {
        Player player = context == null ? null : context.player();
        List<IndexRecipeView> recipes = recipes(player);
        return new IndexContentSnapshot(id(), List.of(), List.of(), recipeCategories(player), recipes,
                IndexMachineLayoutTemplates.representativeAll(recipes, "multiblock_controller"),
                List.of(), List.of(), List.of());
    }

    public List<IndexRecipeCategory> recipeCategories(Player player) {
        Map<Identifier, IndexRecipeCategory> categories = new LinkedHashMap<>();
        for (MultiblockAutomationRecipe recipe : AutomationRecipeRegistry.all()) {
            categories.putIfAbsent(recipe.category(), new IndexRecipeCategory(
                    recipe.category(),
                    title(recipe.category()),
                    new ItemStack(ModBlocks.MULTIBLOCK_CONTROLLER.asItem()),
                    ACCENT,
                    560 + categories.size() * 10));
        }
        return List.copyOf(categories.values());
    }

    public List<IndexRecipeView> recipes(Player player) {
        List<IndexRecipeView> views = new ArrayList<>();
        for (MultiblockAutomationRecipe recipe : AutomationRecipeRegistry.all()) {
            views.add(view(recipe));
        }
        return List.copyOf(views);
    }

    private static IndexRecipeView view(MultiblockAutomationRecipe recipe) {
        ItemStack machine = new ItemStack(ModBlocks.MULTIBLOCK_CONTROLLER.asItem());
        List<IndexRecipeSlot> slots = new ArrayList<>();
        for (AutomationIngredient ingredient : recipe.inputs()) {
            List<ItemStack> examples = ingredient.exampleStacks();
            slots.add(examples.isEmpty()
                    ? new IndexRecipeSlot(IndexSlotRole.INPUT, List.of(), ingredient.summary())
                    : new IndexRecipeSlot(IndexSlotRole.INPUT, examples, ingredient.summary()));
        }
        for (AutomationOutput output : recipe.outputs()) {
            ItemStack stack = output.stack();
            slots.add(stack.isEmpty()
                    ? new IndexRecipeSlot(IndexSlotRole.OUTPUT, List.of(), output.summary())
                    : IndexRecipeSlot.output(stack));
        }
        if (recipe.outputs().isEmpty() && recipe.integrityRepair() > 0) {
            slots.add(new IndexRecipeSlot(IndexSlotRole.OUTPUT, List.of(),
                    "Integrity repair +" + recipe.integrityRepair() + "%"));
        }
        slots.add(IndexRecipeSlot.machine(machine));
        slots.add(new IndexRecipeSlot(IndexSlotRole.CATALYST, List.of(),
                "Workcell: " + readable(recipe.requiredWorkcell().name())));
        for (RobotToolType tool : recipe.requiredTools()) {
            ItemStack stack = toolStack(tool);
            if (stack.isEmpty()) {
                slots.add(new IndexRecipeSlot(IndexSlotRole.CATALYST, List.of(), "Tool: " + readable(tool.name())));
            } else {
                slots.add(IndexRecipeSlot.catalyst(stack, "Tool"));
            }
        }
        for (Identifier upgrade : recipe.requiredUpgrades()) {
            ItemStack stack = itemStack(upgrade, 1);
            if (stack.isEmpty()) {
                slots.add(new IndexRecipeSlot(IndexSlotRole.CATALYST, List.of(), "Upgrade: " + readable(upgrade)));
            } else {
                slots.add(IndexRecipeSlot.catalyst(stack, "Upgrade"));
            }
        }
        for (Identifier multiblock : recipe.allowedMultiblocks()) {
            slots.add(new IndexRecipeSlot(IndexSlotRole.INFO, List.of(), "Multiblock: " + readable(multiblock)));
        }

        List<String> notes = new ArrayList<>();
        notes.add("Workcell: " + readable(recipe.requiredWorkcell().name()));
        notes.add("Tools: " + (recipe.requiredTools().isEmpty() ? "Any"
                : recipe.requiredTools().stream().map(tool -> readable(tool.name()))
                .reduce((left, right) -> left + ", " + right).orElse("Any")));
        notes.add("Inputs consumed on start: " + yesNo(recipe.consumeInputsOnStart()));
        if (!recipe.allowedMultiblocks().isEmpty()) {
            notes.add("Allowed multiblocks: " + joinIds(recipe.allowedMultiblocks()));
        }
        for (Identifier multiblock : recipe.allowedMultiblocks()) {
            MultiblockContent.definition(multiblock).ifPresent(definition -> facilityNotes(definition, notes));
        }
        for (CapabilityRequirement requirement : recipe.capabilityCosts()) {
            notes.add("Capability " + requirement.capabilityId() + ": " + requirement.amount() + " "
                    + requirement.unit() + (requirement.throughput() > 0 ? " @ " + requirement.throughput() + "/t" : "")
                    + (requirement.required() ? "" : " optional"));
        }
        if (!recipe.effects().isEmpty()) {
            notes.add("Effects: " + joinIds(recipe.effects()));
        }
        if (!recipe.requiredUpgrades().isEmpty()) {
            notes.add("Required upgrades: " + joinIds(recipe.requiredUpgrades()));
        }
        if (recipe.integrityRepair() > 0) {
            notes.add("Integrity repair: +" + recipe.integrityRepair() + "%, priority " + recipe.repairPriority());
        }
        notes.add("Animation: " + recipe.animationProfile());
        notes.addAll(recipe.notes());

        return new IndexRecipeView(
                recipe.id(),
                recipe.category(),
                recipe.displayName(),
                machine,
                slots,
                notes,
                recipe.durationTicks(),
                false,
                recipe.id().getNamespace());
    }

    private static ItemStack toolStack(RobotToolType tool) {
        return switch (tool) {
            case GRIPPER -> new ItemStack(ModItems.GRIPPER_HEAD.get());
            case WELDER -> new ItemStack(ModItems.WELDER_HEAD.get());
            case CUTTER -> new ItemStack(ModItems.CUTTER_HEAD.get());
            case SCANNER -> new ItemStack(ModItems.SCANNER_HEAD.get());
            case INJECTOR -> new ItemStack(ModItems.INJECTOR_HEAD.get());
            case ASSEMBLER -> new ItemStack(ModItems.ASSEMBLER_HEAD.get());
            case CLAMP -> new ItemStack(ModItems.CLAMP_HEAD.get());
            case DRILL -> new ItemStack(ModItems.DRILL_HEAD.get());
            case CALIBRATOR -> new ItemStack(ModItems.CALIBRATOR_HEAD.get());
            case STABILIZER -> new ItemStack(ModItems.STABILIZER_HEAD.get());
        };
    }

    private static void facilityNotes(MultiblockDefinition definition, List<String> notes) {
        if (!definition.facilityRoute().isBlank()) {
            notes.add("Facility route: " + definition.facilityRoute());
        }
        if (!definition.facilityStage().isBlank()) {
            notes.add("Facility stage: " + definition.facilityStage());
        }
        if (!definition.primaryUse().isBlank()) {
            notes.add("Use: " + definition.primaryUse());
        }
    }

    private static ItemStack itemStack(Identifier itemId, int count) {
        if (itemId == null) {
            return ItemStack.EMPTY;
        }
        Item item = BuiltInRegistries.ITEM.getOptional(itemId).orElse(Items.AIR);
        return item == Items.AIR ? ItemStack.EMPTY : new ItemStack(item, Math.max(1, count));
    }

    private static String joinIds(List<Identifier> ids) {
        return ids.stream().map(Identifier::toString).reduce((left, right) -> left + ", " + right).orElse("");
    }

    private static String yesNo(boolean value) {
        return value ? "yes" : "no";
    }

    private static String title(Identifier id) {
        return readable(id == null ? "automation" : id.getPath());
    }

    private static String readable(Identifier id) {
        return id == null ? "Unknown" : readable(id.getPath());
    }

    private static String readable(String value) {
        String[] parts = (value == null ? "unknown" : value.toLowerCase(Locale.ROOT))
                .replace('/', '_')
                .split("_+");
        StringBuilder builder = new StringBuilder();
        for (String part : parts) {
            if (part.isBlank()) {
                continue;
            }
            if (!builder.isEmpty()) {
                builder.append(' ');
            }
            builder.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1));
        }
        return builder.isEmpty() ? "Unknown" : builder.toString();
    }
}
