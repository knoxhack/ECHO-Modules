package com.knoxhack.echoashfallprotocol.schematic;

import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import com.knoxhack.echo.adaptercore.EchoValueIOSerializable;

import java.util.HashSet;
import java.util.Set;

/**
 * Tracks which recipes a player has permanently unlocked via Schematic Fragments.
 * Introduced in v1.2 "First Light" to gate advanced recipes behind discovery.
 *
 * Recipes for v1.0/v1.1 content remain auto-unlocked on world load so existing
 * saves don't regress — see SchematicFragmentItem.autoUnlockLegacyForUpgrader.
 */
public class UnlockedSchematicsData implements EchoValueIOSerializable {

    private final Set<String> unlocked = new HashSet<>();
    private boolean legacyUpgradeApplied = false;

    public UnlockedSchematicsData() {}

    public boolean isUnlocked(String recipeId) {
        return unlocked.contains(recipeId);
    }

    public boolean unlock(String recipeId) {
        return unlocked.add(recipeId);
    }

    public Set<String> getAll() {
        return java.util.Collections.unmodifiableSet(unlocked);
    }

    public int count() {
        return unlocked.size();
    }

    public boolean isLegacyUpgradeApplied() {
        return legacyUpgradeApplied;
    }

    public void setLegacyUpgradeApplied(boolean value) {
        legacyUpgradeApplied = value;
    }

    @Override
    public void serialize(ValueOutput output) {
        output.putBoolean("legacyUpgradeApplied", legacyUpgradeApplied);
        output.putInt("count", unlocked.size());
        int i = 0;
        for (String id : unlocked) {
            output.putString("s_" + i++, id);
        }
    }

    @Override
    public void deserialize(ValueInput input) {
        unlocked.clear();
        legacyUpgradeApplied = input.getBooleanOr("legacyUpgradeApplied", false);
        int count = input.getIntOr("count", 0);
        for (int i = 0; i < count; i++) {
            String id = input.getStringOr("s_" + i, "");
            if (!id.isEmpty()) unlocked.add(id);
        }
    }
}
