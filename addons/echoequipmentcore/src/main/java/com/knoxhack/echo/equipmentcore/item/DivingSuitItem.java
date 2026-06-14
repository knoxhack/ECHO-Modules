package com.knoxhack.echo.equipmentcore.item;

import com.knoxhack.echo.equipmentcore.api.EquipmentStats;
import com.knoxhack.echo.equipmentcore.api.IEquipmentProvider;
import com.knoxhack.echo.equipmentcore.data.InstalledUpgrades;
import com.knoxhack.echo.equipmentcore.registry.ModDataComponents;
import java.util.function.Consumer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;

public class DivingSuitItem extends Item implements IEquipmentProvider {
    private final String suitId;
    private final float pressureResistance;
    private final float oxygenBonus;
    private final float coldResistance;
    private final float heatResistance;
    private final float corruptionResistance;
    private final int maxDurability;

    public DivingSuitItem(String suitId, float pressureResistance, int maxDurability, Properties properties) {
        this(suitId, pressureResistance, 0.0F, 0.0F, 0.0F, 0.0F, maxDurability, properties);
    }

    public DivingSuitItem(String suitId, float pressureResistance, float oxygenBonus, float coldResistance,
                          float heatResistance, float corruptionResistance, int maxDurability, Properties properties) {
        super(properties);
        this.suitId = suitId;
        this.pressureResistance = pressureResistance;
        this.oxygenBonus = oxygenBonus;
        this.coldResistance = coldResistance;
        this.heatResistance = heatResistance;
        this.corruptionResistance = corruptionResistance;
        this.maxDurability = maxDurability;
    }

    @Override
    public EquipmentStats getStats(ItemStack stack) {
        initialize(stack);
        int damage = stack.getDamageValue();
        int current = Math.max(0, maxDurability - damage);
        EquipmentStats base = new EquipmentStats(
                pressureResistance,
                oxygenBonus,
                coldResistance,
                heatResistance,
                corruptionResistance,
                current,
                maxDurability
        );
        return applyUpgrades(stack, base);
    }

    public static void initialize(ItemStack stack) {
        if (stack.isEmpty() || !(stack.getItem() instanceof DivingSuitItem)) {
            return;
        }
        if (stack.get(ModDataComponents.INSTALLED_UPGRADES.get()) == null) {
            stack.set(ModDataComponents.INSTALLED_UPGRADES.get(), InstalledUpgrades.EMPTY);
        }
    }

    public static boolean installUpgrade(ItemStack suitStack, ItemStack upgradeStack) {
        if (suitStack.isEmpty() || !(suitStack.getItem() instanceof DivingSuitItem)
                || upgradeStack.isEmpty() || !(upgradeStack.getItem() instanceof UpgradeModuleItem upgrade)) {
            return false;
        }
        initialize(suitStack);
        InstalledUpgrades installed = suitStack.getOrDefault(ModDataComponents.INSTALLED_UPGRADES.get(), InstalledUpgrades.EMPTY);
        if (installed.contains(upgrade.upgradeId())) {
            return false;
        }
        suitStack.set(ModDataComponents.INSTALLED_UPGRADES.get(), installed.with(upgrade.upgradeId()));
        upgradeStack.shrink(1);
        return true;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display,
                                Consumer<Component> tooltip, TooltipFlag flag) {
        initialize(stack);
        EquipmentStats stats = getStats(stack);
        tooltip.accept(Component.literal("Pressure: " + stats.pressureResistance()));
        tooltip.accept(Component.literal("Durability: " + stats.durability() + "/" + stats.maxDurability()));
        InstalledUpgrades upgrades = stack.getOrDefault(ModDataComponents.INSTALLED_UPGRADES.get(), InstalledUpgrades.EMPTY);
        if (!upgrades.upgrades().isEmpty()) {
            tooltip.accept(Component.literal("Upgrades: " + String.join(", ", upgrades.upgrades())));
        }
    }

    private static EquipmentStats applyUpgrades(ItemStack stack, EquipmentStats base) {
        InstalledUpgrades upgrades = stack.getOrDefault(ModDataComponents.INSTALLED_UPGRADES.get(), InstalledUpgrades.EMPTY);
        float pressure = base.pressureResistance();
        float oxygen = base.oxygenBonus();
        float cold = base.coldResistance();
        float heat = base.heatResistance();
        float corruption = base.corruptionResistance();
        for (String upgrade : upgrades.upgrades()) {
            switch (upgrade) {
                case "echoequipmentcore:reinforced_joints" -> {
                    pressure += 0.1F;
                    corruption += 0.05F;
                }
                case "echoequipmentcore:oxygen_scrubber" -> oxygen += 0.2F;
                case "echoequipmentcore:thermal_regulator" -> {
                    cold += 0.15F;
                    heat += 0.15F;
                }
                case "echoequipmentcore:emergency_buoyancy" -> pressure += 0.05F;
                default -> {
                    // laser_cutter and unknown upgrades provide no hazard stats.
                }
            }
        }
        return new EquipmentStats(pressure, oxygen, cold, heat, corruption, base.durability(), base.maxDurability());
    }
}
