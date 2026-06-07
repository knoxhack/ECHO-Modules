package com.knoxhack.echoashfallprotocol.item;

import com.knoxhack.echoashfallprotocol.Config;
import com.knoxhack.echoashfallprotocol.registry.ModDataComponents;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.Codec;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.Mth;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.TooltipProvider;

import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

/**
 * Data-component backed tooltip templates for Ashfall items.
 */
public record AshfallTooltip(String id, List<Component> templateLines) implements TooltipProvider {
    private static final String NEXUS_UPGRADE_KEY = "nexus_upgrades";

    public static final Codec<AshfallTooltip> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.fieldOf("id").forGetter(AshfallTooltip::id),
            ComponentSerialization.CODEC.listOf().optionalFieldOf("template_lines", List.of())
                    .forGetter(AshfallTooltip::templateLines)
    ).apply(instance, AshfallTooltip::new));
    public static final StreamCodec<RegistryFriendlyByteBuf, AshfallTooltip> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, AshfallTooltip::id,
            ComponentSerialization.STREAM_CODEC.apply(ByteBufCodecs.list()), AshfallTooltip::templateLines,
            AshfallTooltip::new);

    public static AshfallTooltip of(String id) {
        return new AshfallTooltip(id, List.of());
    }

    @Override
    public void addToTooltip(Item.TooltipContext context, Consumer<Component> tooltip, TooltipFlag flag,
                             DataComponentGetter components) {
        templateLines.forEach(tooltip);
        switch (id) {
            case "alloy_blade" -> {
                tooltip.accept(literal("Damage: 8.0", 0xCCCCCC));
                tooltip.accept(literal("Armor Penetration: 20%", 0xCCCCCC));
                tooltip.accept(literal("Forged from pre-war alloy stock.", 0x888888));
            }
            case "alloy_hammer" -> {
                tooltip.accept(literal("Damage: 10.0 (slow)", 0xCCCCCC));
                tooltip.accept(literal("AOE knockback in 1.5-block radius.", 0xCCCCCC));
                tooltip.accept(literal("A weapon of pure devastation.", 0x888888));
            }
            case "bandage" -> {
                tooltip.accept(literal("Stops bleeding and removes poison.", 0xAAFFAA));
                tooltip.accept(literal("Right-click to apply.", 0xAAAAAA));
            }
            case "battery_basic" -> battery(tooltip, components, 2_000);
            case "battery_advanced" -> battery(tooltip, components, 10_000);
            case "battery_elite" -> battery(tooltip, components, 50_000);
            case "bone_knife" -> {
                tooltip.accept(literal("Damage: ~3.0", 0xCCCCCC));
                tooltip.accept(literal("Primitive blade carved from animal bone.", 0xAAAAAA));
                tooltip.accept(literal("Better than bare fists.", 0x888888));
            }
            case "clean_water" -> {
                tooltip.accept(translatable("tooltip.EchoAshfallProtocol.clean_water.desc", 0x55AAFF));
                tooltip.accept(translatable("tooltip.EchoAshfallProtocol.clean_water.hydration", 0x55FF55));
            }
            case "contaminated" -> {
                tooltip.accept(literal("[CONTAMINATED] Toxic material", 0xFF4444));
                tooltip.accept(literal("Purify in Water Purifier or Atmospheric Scrubber", 0xAAAAAA));
            }
            case "crude_spear" -> {
                tooltip.accept(literal("Damage: 5.0", 0xCCCCCC));
                tooltip.accept(literal("Extended reach from scrap and bone.", 0xAAAAAA));
                tooltip.accept(literal("Cobbled together from the ruins.", 0x888888));
            }
            case "dirty_water" -> {
                tooltip.accept(translatable("tooltip.EchoAshfallProtocol.dirty_water.desc", 0x886633));
                tooltip.accept(translatable("tooltip.EchoAshfallProtocol.dirty_water.warning", 0xFF5555));
            }
            case "filter_basic" -> filter(tooltip, "Basic", 0xAAAA00, 300);
            case "filter_advanced" -> filter(tooltip, "Advanced", 0x5555FF, 600);
            case "filter_elite" -> filter(tooltip, "Elite", 0xFF55FF, 1000);
            case "field_manual" -> {
                tooltip.accept(translatable("tooltip.EchoAshfallProtocol.field_manual.desc", 0x55AAFF));
                tooltip.accept(translatable("tooltip.EchoAshfallProtocol.field_manual.use", 0xAAAAAA));
            }
            case "gas_mask" -> {
                tooltip.accept(translatable("tooltip.EchoAshfallProtocol.gas_mask.desc", 0x55AAFF));
                tooltip.accept(translatable("tooltip.EchoAshfallProtocol.gas_mask.filter_required", 0xAAAAAA));
                tooltip.accept(Component.empty());
                if (Config.VERBOSE_TOOLTIPS.get()) {
                    tooltip.accept(literal("Use a Filter Cartridge to refill hazard-zone capacity.", 0x55FF55));
                    tooltip.accept(literal("Check filter charge before entering toxic routes.", 0x55AAFF));
                }
            }
            case "hazmat_head" -> hazmat(tooltip, "tooltip.EchoAshfallProtocol.hazmat_head", true);
            case "hazmat_chest" -> hazmat(tooltip, "tooltip.EchoAshfallProtocol.hazmat_chest", false);
            case "hazmat_legs" -> hazmat(tooltip, "tooltip.EchoAshfallProtocol.hazmat_legs", false);
            case "hazmat_feet" -> hazmat(tooltip, "tooltip.EchoAshfallProtocol.hazmat_feet", false);
            case "hide_wrap" -> {
                tooltip.accept(literal("Improvised face wrap.", 0xAAAAAA));
                tooltip.accept(literal("Does not provide atmospheric protection.", 0x888888));
            }
            case "instability_dampener" -> {
                tooltip.accept(translatable("tooltip.EchoAshfallProtocol.instability_dampener.desc", 0x55AAFF));
                tooltip.accept(translatable("tooltip.EchoAshfallProtocol.instability_dampener.use", 0xAAAAAA));
            }
            case "mutagen" -> {
                tooltip.accept(translatable("tooltip.EchoAshfallProtocol.mutagen.desc", 0xFF5555));
                tooltip.accept(translatable("tooltip.EchoAshfallProtocol.mutagen.warning", 0xAAAAAA));
            }
            case "nexus_annihilator" -> nexusWeapon(tooltip, components, true);
            case "nexus_blade" -> nexusWeapon(tooltip, components, false);
            case "nexus_crystal" -> {
                tooltip.accept(literal("A crystallized shard of Grid energy.", 0xAA00AA));
                tooltip.accept(literal("Required for Nexus-grade equipment.", 0x888888));
            }
            case "prefall_archives_key" -> {
                tooltip.accept(Component.literal("Opens the way to the Pre-Fall Archives").withStyle(ChatFormatting.AQUA));
                tooltip.accept(Component.literal("Requires: Nexus choice made").withStyle(ChatFormatting.GRAY));
                tooltip.accept(Component.literal("The final Warden waits inside.").withStyle(ChatFormatting.GRAY));
            }
            case "portable_signal_scanner" -> {
                tooltip.accept(translatable("tooltip.EchoAshfallProtocol.portable_signal_scanner.desc", 0x55AAFF));
                tooltip.accept(translatable("tooltip.EchoAshfallProtocol.portable_signal_scanner.use", 0xAAAAAA));
                tooltip.accept(translatable("tooltip.EchoAshfallProtocol.portable_signal_scanner.deep_scan", 0x888888));
            }
            case "rad_away" -> {
                tooltip.accept(translatable("tooltip.EchoAshfallProtocol.radaway.desc", 0x55FF55));
                tooltip.accept(translatable("tooltip.EchoAshfallProtocol.radaway.regen", 0xAAAAAA));
            }
            case "rare_tech_schematic" -> {
                tooltip.accept(Component.translatable("tooltip.EchoAshfallProtocol.rare_tech_schematic.desc")
                        .withStyle(ChatFormatting.LIGHT_PURPLE));
                tooltip.accept(Component.translatable("tooltip.EchoAshfallProtocol.rare_tech_schematic.use")
                        .withStyle(ChatFormatting.GRAY));
            }
            case "return_keystone" -> {
                tooltip.accept(Component.literal("Returns you to your Archives entry point").withStyle(ChatFormatting.AQUA));
                tooltip.accept(Component.literal("Only works in the Pre-Fall Archives").withStyle(ChatFormatting.GRAY));
            }
            case "relay_scanner_lens" -> {
                tooltip.accept(translatable("tooltip.EchoAshfallProtocol.relay_scanner_lens.desc", 0x55AAFF));
                tooltip.accept(translatable("tooltip.EchoAshfallProtocol.relay_scanner_lens.use", 0xAAAAAA));
            }
            case "return_beacon" -> {
                tooltip.accept(translatable("tooltip.EchoAshfallProtocol.return_beacon.desc", 0x55AAFF));
                tooltip.accept(translatable("tooltip.EchoAshfallProtocol.return_beacon.cooldown", 0xAAAAAA));
            }
            case "scout_drone" -> {
                tooltip.accept(translatable("tooltip.EchoAshfallProtocol.scout_drone.desc", 0x55AAFF));
                if (Config.VERBOSE_TOOLTIPS.get()) {
                    tooltip.accept(literal("Right-click to deploy. Sneak-use to cycle your deployed drone mode.", 0xAAAAAA));
                    tooltip.accept(literal("Also responds to ECHO terminal Drone commands as a fallback link.", 0x55FF55));
                }
            }
            case "scrap_knife" -> scrapKnife(tooltip, components);
            case "stim_pack" -> {
                tooltip.accept(literal("Injects adrenaline and stimulants.", 0xFFFF55));
                tooltip.accept(literal("Grants brief Regeneration II and Speed.", 0xAAAAAA));
            }
            default -> {
                if (id.startsWith("armor:")) {
                    armor(tooltip, id);
                } else if (id.startsWith("data_log:")) {
                    dataLog(tooltip, id.substring("data_log:".length()));
                } else if (id.startsWith("schematic:")) {
                    schematic(tooltip, id.substring("schematic:".length()));
                }
            }
        }
    }

    private static void battery(Consumer<Component> tooltip, DataComponentGetter components, int capacity) {
        int stored = Mth.clamp(components.getOrDefault(ModDataComponents.STORED_ENERGY.get(), 0), 0, capacity);
        tooltip.accept(Component.translatable("tooltip.EchoAshfallProtocol.battery.energy", stored, capacity)
                .withStyle(ChatFormatting.AQUA));
        tooltip.accept(Component.translatable("tooltip.EchoAshfallProtocol.battery.transfer")
                .withStyle(ChatFormatting.GRAY));
    }

    private static void filter(Consumer<Component> tooltip, String label, int color, int amount) {
        tooltip.accept(literal(label + " Filter", color));
        tooltip.accept(Component.translatable("tooltip.EchoAshfallProtocol.filter_cartridge.capacity", amount)
                .withColor(0xAAAAAA));
        if (Config.VERBOSE_TOOLTIPS.get()) {
            tooltip.accept(literal("Install into a Gas Mask before entering toxic air.", 0x55AAFF));
        }
    }

    private static void hazmat(Consumer<Component> tooltip, String key, boolean helmet) {
        tooltip.accept(translatable(key, 0x55AAFF));
        tooltip.accept(translatable("tooltip.EchoAshfallProtocol.hazmat_rad_protection", 0xAAFFAA));
        if (helmet) {
            tooltip.accept(translatable("tooltip.EchoAshfallProtocol.hazmat_full_set_bonus", 0xFFAA55));
        }
    }

    private static void armor(Consumer<Component> tooltip, String id) {
        String[] parts = id.split(":", 5);
        if (parts.length != 5) {
            return;
        }
        double defense = Double.parseDouble(parts[1]);
        double toughness = Double.parseDouble(parts[2]);
        double knockbackResistance = Double.parseDouble(parts[3]);
        tooltip.accept(literal(String.format(Locale.ROOT, "Defense: +%.0f", defense), 0xCCCCCC));
        tooltip.accept(literal(String.format(Locale.ROOT, "Toughness: +%.1f", toughness), 0xCCCCCC));
        if (knockbackResistance > 0) {
            tooltip.accept(literal(String.format(Locale.ROOT, "Knockback Resist: +%.0f%%", knockbackResistance * 100), 0xCCCCCC));
        }
        tooltip.accept(literal(parts[4], 0x888888));
    }

    private static void dataLog(Consumer<Component> tooltip, String type) {
        String description = switch (type) {
            case "prefall_history" -> "Documents from before the Gridfall";
            case "nexus_archives" -> "AI system logs and diagnostics";
            case "survivor_journal" -> "Personal accounts from other survivors";
            case "technical_manual" -> "Old World technical documentation";
            case "research_data" -> "Scientific findings and experiments";
            default -> "Recovered archive data";
        };
        tooltip.accept(Component.literal(description).withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
        tooltip.accept(literal("Right-click to read and archive", 0x888888));
    }

    private static void schematic(Consumer<Component> tooltip, String type) {
        switch (type) {
            case "weapons" -> schematic(tooltip, "Weapons", "Unlocks advanced weapon recipes", ChatFormatting.RED);
            case "armor" -> schematic(tooltip, "Armor", "Unlocks protective gear recipes", ChatFormatting.BLUE);
            case "machines" -> schematic(tooltip, "Machines", "Unlocks machine crafting recipes", ChatFormatting.GOLD);
            case "medical" -> schematic(tooltip, "Medical", "Unlocks medical item recipes", ChatFormatting.GREEN);
            case "energy" -> schematic(tooltip, "Energy", "Unlocks power system recipes", ChatFormatting.YELLOW);
            default -> {
            }
        }
    }

    private static void schematic(Consumer<Component> tooltip, String name, String description, ChatFormatting color) {
        tooltip.accept(Component.literal(name).withStyle(color, ChatFormatting.BOLD));
        tooltip.accept(Component.literal(description).withStyle(ChatFormatting.GRAY));
        tooltip.accept(Component.empty());
        tooltip.accept(Component.literal("Use at Research Lab to unlock").withStyle(ChatFormatting.YELLOW));
        tooltip.accept(Component.literal("Tier 2+ crafting recipes").withStyle(ChatFormatting.YELLOW));
    }

    private static void scrapKnife(Consumer<Component> tooltip, DataComponentGetter components) {
        int maxDamage = components.getOrDefault(DataComponents.MAX_DAMAGE, 0);
        int damage = components.getOrDefault(DataComponents.DAMAGE, 0);
        float effectiveness = maxDamage > 0 ? Math.max(0.05F, 1.0F - (float) damage / maxDamage) : 1.0F;
        int effectivenessPercent = (int) (effectiveness * 100);
        tooltip.accept(Component.translatable("tooltip.EchoAshfallProtocol.scrap_knife.friction", effectivenessPercent)
                .withColor(effectivenessPercent > 50 ? 0x55FF55 : 0xFF5555));
        tooltip.accept(translatable("tooltip.EchoAshfallProtocol.scrap_knife.desc", 0xAAAAAA));
    }

    private static void nexusWeapon(Consumer<Component> tooltip, DataComponentGetter components, boolean annihilator) {
        int upgradeLevel = nexusUpgradeLevel(components);
        float baseDamage = annihilator ? 15.0F : 11.0F;
        float totalDamage = baseDamage + upgradeLevel;
        String pierce = annihilator ? "50%" : "35%";
        if (upgradeLevel > 0) {
            tooltip.accept(Component.literal("[Nexus Forged +" + upgradeLevel + "]").withStyle(ChatFormatting.AQUA));
            tooltip.accept(literal(String.format(Locale.ROOT, "Damage: %.1f + %s Piercing", totalDamage, pierce),
                    annihilator ? 0xFF5555 : 0xCCCCCC));
        } else {
            tooltip.accept(literal(String.format(Locale.ROOT, "Damage: %.1f + %s Piercing", baseDamage, pierce),
                    annihilator ? 0xFF5555 : 0xCCCCCC));
        }
        if (annihilator) {
            tooltip.accept(literal("Area Effect: 3-block radius", 0xFF5555));
            tooltip.accept(literal("Irradiates targets heavily on hit.", 0xFF5555));
            tooltip.accept(Component.empty());
            tooltip.accept(literal("Forged in the destruction of the Nexus Core.", 0xAAAAAA));
            tooltip.accept(literal("The wasteland trembles before this blade.", 0xAAAAAA));
        } else {
            tooltip.accept(literal("Irradiates targets on hit.", 0xFF5555));
            tooltip.accept(literal("Nexus energy flows through the blade.", 0xAA00AA));
        }
        if (upgradeLevel < 5) {
            tooltip.accept(literal("Upgrade at anvil with Nexus Crystals", 0x888888));
        }
    }

    private static int nexusUpgradeLevel(DataComponentGetter components) {
        CustomData customData = components.get(DataComponents.CUSTOM_DATA);
        if (customData == null) {
            return 0;
        }
        CompoundTag tag = customData.copyTag();
        return Mth.clamp(tag.getIntOr(NEXUS_UPGRADE_KEY, 0), 0, 5);
    }

    private static Component translatable(String key, int color) {
        return Component.translatable(key).withColor(color);
    }

    private static Component literal(String text, int color) {
        return Component.literal(text).withColor(color);
    }
}
