package com.knoxhack.echoashfallprotocol.survival;

import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import com.knoxhack.echo.adaptercore.EchoValueIOSerializable;

import java.util.ArrayList;
import java.util.List;

/**
 * Tracks active mutations and their side effects.
 */
public class MutationData implements EchoValueIOSerializable {

    public enum MutationType {
        NIGHT_VISION("night_vision", "Enhanced Optics", "Grants night vision"),
        FAST_SCAVENGE("fast_scavenge", "Accelerated Reflexes", "Faster block breaking"),
        RAD_RESISTANCE("rad_resistance", "Rad Adaptation", "Reduced radiation accumulation"),
        REGENERATION("regeneration", "Bio-Regen", "Slow passive regeneration"),
        THICK_SKIN("thick_skin", "Dermal Plating", "Reduced damage taken");

        private final String id;
        private final String displayName;
        private final String description;

        MutationType(String id, String displayName, String description) {
            this.id = id;
            this.displayName = displayName;
            this.description = description;
        }

        public String getId() { return id; }
        public String getDisplayName() { return displayName; }
        public String getDescription() { return description; }
    }

    public enum SideEffect {
        VISUAL_GLITCH,
        DAMAGE_SPIKE,
        AI_AGGRO,
        HUNGER_DRAIN,
        WEAKNESS
    }

    private final List<String> activeMutations = new ArrayList<>();
    private final List<String> activeSideEffects = new ArrayList<>();
    private int mutationCount = 0;

    public MutationData() {}

    public boolean hasMutation(MutationType type) {
        return activeMutations.contains(type.getId());
    }

    public void addMutation(MutationType type) {
        if (!hasMutation(type)) {
            activeMutations.add(type.getId());
            mutationCount++;
            if (mutationCount >= 2 && !activeSideEffects.contains(SideEffect.VISUAL_GLITCH.name())) {
                activeSideEffects.add(SideEffect.VISUAL_GLITCH.name());
            }
            if (mutationCount >= 3 && !activeSideEffects.contains(SideEffect.DAMAGE_SPIKE.name())) {
                activeSideEffects.add(SideEffect.DAMAGE_SPIKE.name());
            }
            if (mutationCount >= 4 && !activeSideEffects.contains(SideEffect.AI_AGGRO.name())) {
                activeSideEffects.add(SideEffect.AI_AGGRO.name());
            }
        }
    }

    public List<String> getActiveMutations() { return activeMutations; }
    public List<String> getActiveSideEffects() { return activeSideEffects; }
    public int getMutationCount() { return mutationCount; }

    public boolean hasSideEffect(SideEffect effect) {
        return activeSideEffects.contains(effect.name());
    }

    public void removeSideEffect(SideEffect effect) {
        activeSideEffects.remove(effect.name());
    }

    public void clearSideEffects() {
        activeSideEffects.clear();
    }

    @Override
    public void serialize(ValueOutput output) {
        output.putInt("mutationCount", mutationCount);
        output.putInt("mutationListSize", activeMutations.size());
        for (int i = 0; i < activeMutations.size(); i++) {
            output.putString("mutation_" + i, activeMutations.get(i));
        }
        output.putInt("sideEffectListSize", activeSideEffects.size());
        for (int i = 0; i < activeSideEffects.size(); i++) {
            output.putString("sideEffect_" + i, activeSideEffects.get(i));
        }
    }

    @Override
    public void deserialize(ValueInput input) {
        mutationCount = input.getIntOr("mutationCount", 0);
        int mutSize = input.getIntOr("mutationListSize", 0);
        activeMutations.clear();
        for (int i = 0; i < mutSize; i++) {
            input.getString("mutation_" + i).ifPresent(activeMutations::add);
        }
        int seSize = input.getIntOr("sideEffectListSize", 0);
        activeSideEffects.clear();
        for (int i = 0; i < seSize; i++) {
            input.getString("sideEffect_" + i).ifPresent(activeSideEffects::add);
        }
    }
}
