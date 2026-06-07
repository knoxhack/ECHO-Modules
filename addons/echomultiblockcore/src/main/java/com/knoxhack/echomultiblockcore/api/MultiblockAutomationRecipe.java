package com.knoxhack.echomultiblockcore.api;

import java.util.List;
import net.minecraft.resources.Identifier;

public record MultiblockAutomationRecipe(
        Identifier id,
        String displayName,
        Identifier category,
        List<Identifier> allowedMultiblocks,
        WorkcellType requiredWorkcell,
        List<RobotToolType> requiredTools,
        List<AutomationIngredient> inputs,
        List<AutomationOutput> outputs,
        boolean consumeInputsOnStart,
        int durationTicks,
        int heatPerSecond,
        String animation,
        int integrityRepair,
        List<String> notes,
        List<Identifier> effects,
        List<CapabilityRequirement> capabilityCosts,
        List<Identifier> requiredUpgrades,
        int repairPriority,
        Identifier animationProfile,
        boolean autoBuilderEligible) {
    public MultiblockAutomationRecipe(
            Identifier id,
            String displayName,
            Identifier category,
            List<Identifier> allowedMultiblocks,
            WorkcellType requiredWorkcell,
            List<RobotToolType> requiredTools,
            List<AutomationIngredient> inputs,
            List<AutomationOutput> outputs,
            boolean consumeInputsOnStart,
            int durationTicks,
            int heatPerSecond,
            String animation,
            int integrityRepair,
            List<String> notes,
            List<Identifier> effects) {
        this(id, displayName, category, allowedMultiblocks, requiredWorkcell, requiredTools, inputs, outputs,
                consumeInputsOnStart, durationTicks, heatPerSecond, animation, integrityRepair, notes, effects,
                List.of(), List.of(), integrityRepair > 0 ? 50 : 0, null, true);
    }

    public MultiblockAutomationRecipe(
            Identifier id,
            String displayName,
            Identifier category,
            List<Identifier> allowedMultiblocks,
            WorkcellType requiredWorkcell,
            List<RobotToolType> requiredTools,
            List<AutomationIngredient> inputs,
            List<AutomationOutput> outputs,
            boolean consumeInputsOnStart,
            int durationTicks,
            int heatPerSecond,
            String animation,
            int integrityRepair,
            List<String> notes) {
        this(id, displayName, category, allowedMultiblocks, requiredWorkcell, requiredTools, inputs, outputs,
                consumeInputsOnStart, durationTicks, heatPerSecond, animation, integrityRepair, notes, List.of());
    }

    public MultiblockAutomationRecipe {
        if (id == null) {
            throw new IllegalArgumentException("Automation recipe id is required.");
        }
        displayName = displayName == null || displayName.isBlank() ? id.getPath().replace('_', ' ') : displayName.strip();
        category = category == null ? Identifier.fromNamespaceAndPath(id.getNamespace(), "assembly") : category;
        allowedMultiblocks = List.copyOf(allowedMultiblocks == null ? List.of() : allowedMultiblocks);
        requiredWorkcell = requiredWorkcell == null ? WorkcellType.ASSEMBLY : requiredWorkcell;
        requiredTools = List.copyOf(requiredTools == null ? List.of() : requiredTools);
        inputs = List.copyOf(inputs == null ? List.of() : inputs);
        outputs = List.copyOf(outputs == null ? List.of() : outputs);
        durationTicks = Math.max(20, durationTicks);
        heatPerSecond = Math.max(0, heatPerSecond);
        animation = animation == null || animation.isBlank() ? "assemble" : animation.strip();
        integrityRepair = Math.max(0, integrityRepair);
        notes = List.copyOf(notes == null ? List.of() : notes.stream()
                .filter(note -> note != null && !note.isBlank())
                .map(String::strip)
                .toList());
        effects = List.copyOf(effects == null ? List.of() : effects.stream()
                .filter(effect -> effect != null)
                .toList());
        capabilityCosts = List.copyOf(capabilityCosts == null ? List.of() : capabilityCosts);
        requiredUpgrades = List.copyOf(requiredUpgrades == null ? List.of() : requiredUpgrades.stream()
                .filter(upgrade -> upgrade != null)
                .toList());
        repairPriority = Math.max(0, repairPriority);
        animationProfile = animationProfile == null
                ? Identifier.fromNamespaceAndPath(id.getNamespace(), animation)
                : animationProfile;
    }

    public boolean allowsMultiblock(Identifier definitionId) {
        return allowedMultiblocks.isEmpty() || allowedMultiblocks.contains(definitionId);
    }

    public String inputSummary() {
        if (inputs.isEmpty()) {
            return "No inputs";
        }
        return inputs.stream().map(AutomationIngredient::summary).reduce((left, right) -> left + ", " + right).orElse("No inputs");
    }

    public String outputSummary() {
        if (outputs.isEmpty()) {
            return integrityRepair > 0 ? "Repair +" + integrityRepair + "% integrity" : "No outputs";
        }
        return outputs.stream().map(AutomationOutput::summary).reduce((left, right) -> left + ", " + right).orElse("No outputs");
    }
}
