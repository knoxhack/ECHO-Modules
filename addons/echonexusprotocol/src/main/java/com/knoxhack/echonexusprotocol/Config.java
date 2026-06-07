package com.knoxhack.echonexusprotocol;

import com.knoxhack.echocore.api.config.EchoConfigCategory;
import com.knoxhack.echocore.api.config.EchoConfigEntry;
import com.knoxhack.echocore.api.config.EchoConfigModule;
import com.knoxhack.echocore.api.config.EchoConfigProvider;
import com.knoxhack.echocore.api.config.EchoConfigRegistry;
import com.knoxhack.echocore.api.config.EchoConfigSide;
import com.knoxhack.echocore.api.config.EchoNativeConfigSpec;
import java.util.List;

public final class Config {
   public static final EchoNativeConfigSpec SPEC;
   public static final EchoNativeConfigSpec.BooleanValue FORCE_NEXUS_UNLOCK;
   public static final EchoNativeConfigSpec.StringValue BALANCE_PRESET;
   public static final EchoNativeConfigSpec.IntValue MACHINE_CAPACITY;
   public static final EchoNativeConfigSpec.IntValue MACHINE_TRANSFER;
   public static final EchoNativeConfigSpec.IntValue MACHINE_DURATION_PERCENT;
   public static final EchoNativeConfigSpec.IntValue MACHINE_CHARGE_COST_PERCENT;
   public static final EchoNativeConfigSpec.IntValue MACHINE_CHARGE_OUTPUT_PERCENT;
   public static final EchoNativeConfigSpec.IntValue FILTER_CORRUPTION_REDUCTION;
   public static final EchoNativeConfigSpec.IntValue FILTER_LEAK_PRESSURE;
   public static final EchoNativeConfigSpec.IntValue STABILIZER_FIELD_GAIN;
   public static final EchoNativeConfigSpec.IntValue STABILIZER_CORRUPTION_REDUCTION;
   public static final EchoNativeConfigSpec.IntValue REACTOR_OUTPUT_PERCENT;
   public static final EchoNativeConfigSpec.IntValue FIELD_TICK_INTERVAL;
   public static final EchoNativeConfigSpec.BooleanValue CHUNK_CORRUPTION_TICK_ENABLED;
   public static final EchoNativeConfigSpec.IntValue FIELD_DECAY_LOW_PRESSURE;
   public static final EchoNativeConfigSpec.IntValue FIELD_DECAY_HIGH_PRESSURE;
   public static final EchoNativeConfigSpec.IntValue CORRUPTION_SPREAD_FIELD_THRESHOLD;
   public static final EchoNativeConfigSpec.IntValue CORRUPTION_SPREAD_PRESSURE_THRESHOLD;
   public static final EchoNativeConfigSpec.IntValue STORM_WINDOW_MULTIPLIER;
   public static final EchoNativeConfigSpec.IntValue REALITY_TEAR_FIELD_THRESHOLD;
   public static final EchoNativeConfigSpec.IntValue FIELD_MAP_RADIUS;
   public static final EchoNativeConfigSpec.IntValue SEAL_TICK_INTERVAL;
   public static final EchoNativeConfigSpec.IntValue SEAL_RADIUS;
   public static final EchoNativeConfigSpec.IntValue SEAL_RELAY_AMOUNT;
   public static final EchoNativeConfigSpec.IntValue SEAL_DEFENSE_DAMAGE;
   public static final EchoNativeConfigSpec.IntValue SEAL_PURIFY_PRESSURE_REDUCTION;
   public static final EchoNativeConfigSpec.IntValue SEAL_COLLAPSE_FIELD_LOSS;
   public static final EchoNativeConfigSpec.IntValue ARMOR_LOCK_COOLDOWN;
   public static final EchoNativeConfigSpec.IntValue ARMOR_LOCK_FIELD_GAIN;
   public static final EchoNativeConfigSpec.IntValue ARMOR_LOCK_CORRUPTION_REDUCTION;
   public static final EchoNativeConfigSpec.IntValue STABILIZED_PURITY_FIELD_GAIN;
   public static final EchoNativeConfigSpec.IntValue STABILIZED_PURITY_CORRUPTION_REDUCTION;
   public static final EchoNativeConfigSpec.IntValue FIELD_ANCHOR_TICKS;
   public static final EchoNativeConfigSpec.IntValue FIELD_ANCHOR_FIELD_GAIN;
   public static final EchoNativeConfigSpec.IntValue FIELD_ANCHOR_CORRUPTION_REDUCTION;
   public static final EchoNativeConfigSpec.IntValue BOSS_HEALTH_PERCENT;
   public static final EchoNativeConfigSpec.IntValue BOSS_DAMAGE_PERCENT;
   public static final EchoNativeConfigSpec.IntValue DUNGEON_LOOT_SCALE_PERCENT;

   private Config() {
   }

   public static void registerEchoConfig() {
      EchoConfigRegistry.register(EchoConfigProvider.of(EchoNexusProtocol.MODID, () -> new EchoConfigModule(
         EchoNexusProtocol.MODID,
         "Nexus Protocol",
         List.of(
            new EchoConfigCategory("progression", "Progression", List.of(
               EchoConfigEntry.stringSpec("balance_preset", "Balance Preset",
                  "Admin label for pack tuning; numeric values remain authoritative.",
                  EchoConfigSide.COMMON, BALANCE_PRESET, true, false, false))),
            new EchoConfigCategory("machines", "Machines", List.of(
               EchoConfigEntry.intSpec("machine_capacity", "Machine Capacity", "",
                  EchoConfigSide.COMMON, MACHINE_CAPACITY, 1000, 1000000, true, false, false),
               EchoConfigEntry.intSpec("machine_transfer", "Machine Transfer", "",
                  EchoConfigSide.COMMON, MACHINE_TRANSFER, 1, 100000, true, false, false),
               EchoConfigEntry.intSpec("duration_percent", "Duration Percent",
                  "Percent multiplier for Nexus processing recipe duration.",
                  EchoConfigSide.COMMON, MACHINE_DURATION_PERCENT, 10, 500, true, false, false),
               EchoConfigEntry.intSpec("charge_cost_percent", "Charge Cost Percent",
                  "Percent multiplier for machine Nexus Charge costs.",
                  EchoConfigSide.COMMON, MACHINE_CHARGE_COST_PERCENT, 0, 500, true, false, false),
               EchoConfigEntry.intSpec("charge_output_percent", "Charge Output Percent",
                  "Percent multiplier for machine Nexus Charge output.",
                  EchoConfigSide.COMMON, MACHINE_CHARGE_OUTPUT_PERCENT, 0, 500, true, false, false),
               EchoConfigEntry.intSpec("reactor_output_percent", "Reactor Output Percent",
                  "Extra percent multiplier applied to passive Corruption Reactor generation.",
                  EchoConfigSide.COMMON, REACTOR_OUTPUT_PERCENT, 0, 500, true, false, false))),
            new EchoConfigCategory("field", "Reality Field", List.of(
               EchoConfigEntry.intSpec("field_tick_interval", "Field Tick Interval", "",
                  EchoConfigSide.COMMON, FIELD_TICK_INTERVAL, 20, 1200, true, false, false),
               EchoConfigEntry.booleanSpec("chunk_corruption_tick", "Chunk Corruption Tick",
                  "Enable chunk-level field and pressure ticks.",
                  EchoConfigSide.COMMON, CHUNK_CORRUPTION_TICK_ENABLED, true, false, false),
               EchoConfigEntry.intSpec("low_pressure_decay", "Low Pressure Decay", "",
                  EchoConfigSide.COMMON, FIELD_DECAY_LOW_PRESSURE, 0, 20, true, false, false),
               EchoConfigEntry.intSpec("high_pressure_decay", "High Pressure Decay", "",
                  EchoConfigSide.COMMON, FIELD_DECAY_HIGH_PRESSURE, 0, 20, true, false, false),
               EchoConfigEntry.intSpec("storm_window_multiplier", "Storm Window Multiplier", "",
                  EchoConfigSide.COMMON, STORM_WINDOW_MULTIPLIER, 1, 20, true, false, false),
               EchoConfigEntry.intSpec("field_map_radius", "Field Map Radius",
                  "Fixed terminal field-map display radius in chunks.",
                  EchoConfigSide.COMMON, FIELD_MAP_RADIUS, 1, 2, true, false, false))),
            new EchoConfigCategory("seals", "Protocol Seals", List.of(
               EchoConfigEntry.intSpec("seal_tick_interval", "Seal Tick Interval", "",
                  EchoConfigSide.COMMON, SEAL_TICK_INTERVAL, 10, 1200, true, false, false),
               EchoConfigEntry.intSpec("seal_radius", "Seal Radius", "",
                  EchoConfigSide.COMMON, SEAL_RADIUS, 1, 16, true, false, false),
               EchoConfigEntry.intSpec("seal_relay_amount", "Seal Relay Amount", "",
                  EchoConfigSide.COMMON, SEAL_RELAY_AMOUNT, 1, 10000, true, false, false),
               EchoConfigEntry.intSpec("seal_defense_damage", "Seal Defense Damage", "",
                  EchoConfigSide.COMMON, SEAL_DEFENSE_DAMAGE, 1, 100, true, false, false),
               EchoConfigEntry.intSpec("purify_pressure_reduction", "Purify Pressure Reduction", "",
                  EchoConfigSide.COMMON, SEAL_PURIFY_PRESSURE_REDUCTION, 0, 100, true, false, false))),
            new EchoConfigCategory("bosses", "Bosses", List.of(
               EchoConfigEntry.intSpec("boss_health_percent", "Boss Health Percent", "",
                  EchoConfigSide.COMMON, BOSS_HEALTH_PERCENT, 10, 1000, true, false, false),
               EchoConfigEntry.intSpec("boss_damage_percent", "Boss Damage Percent", "",
                  EchoConfigSide.COMMON, BOSS_DAMAGE_PERCENT, 10, 1000, true, false, false))),
            new EchoConfigCategory("dungeons", "Dungeons", List.of(
               EchoConfigEntry.intSpec("loot_scale_percent", "Loot Scale Percent",
                  "Loot intensity hint used by dungeon templates and tests.",
                  EchoConfigSide.COMMON, DUNGEON_LOOT_SCALE_PERCENT, 0, 500, true, false, false)))))));
   }

   static {
      EchoNativeConfigSpec.Builder builder = new EchoNativeConfigSpec.Builder();
      builder.push("progression");
      FORCE_NEXUS_UNLOCK = builder.comment("Development override. When true, Nexus progression ignores the canonical Stationfall Blackbox handoff.")
         .define("forceNexusUnlock", false);
      BALANCE_PRESET = builder.comment("Documentation preset label for pack/admin tuning. Valid labels: casual, standard, hardcore. Numeric values below remain authoritative.")
         .define("balancePreset", "standard");
      builder.pop();
      builder.push("machines");
      MACHINE_CAPACITY = builder.defineInRange("machineCapacity", 16000, 1000, 1000000);
      MACHINE_TRANSFER = builder.defineInRange("machineTransfer", 512, 1, 100000);
      MACHINE_DURATION_PERCENT = builder.comment("Percent multiplier for Nexus processing recipe duration. Beta default keeps machines responsive without making charge free.")
         .defineInRange("machineDurationPercent", 90, 10, 500);
      MACHINE_CHARGE_COST_PERCENT = builder.comment("Percent multiplier for machine Nexus Charge costs.")
         .defineInRange("machineChargeCostPercent", 90, 0, 500);
      MACHINE_CHARGE_OUTPUT_PERCENT = builder.comment("Percent multiplier for machine Nexus Charge output.")
         .defineInRange("machineChargeOutputPercent", 100, 0, 500);
      FILTER_CORRUPTION_REDUCTION = builder.defineInRange("filterCorruptionReduction", 4, 0, 100);
      FILTER_LEAK_PRESSURE = builder.defineInRange("filterLeakPressure", 2, 0, 100);
      STABILIZER_FIELD_GAIN = builder.defineInRange("stabilizerFieldGain", 6, 0, 100);
      STABILIZER_CORRUPTION_REDUCTION = builder.defineInRange("stabilizerCorruptionReduction", 4, 0, 100);
      REACTOR_OUTPUT_PERCENT = builder.comment("Extra percent multiplier applied to passive Corruption Reactor generation.")
         .defineInRange("reactorOutputPercent", 100, 0, 500);
      builder.pop();
      builder.push("field");
      FIELD_TICK_INTERVAL = builder.defineInRange("fieldTickInterval", 80, 20, 1200);
      CHUNK_CORRUPTION_TICK_ENABLED = builder.define("chunkCorruptionTickEnabled", true);
      FIELD_DECAY_LOW_PRESSURE = builder.defineInRange("fieldDecayLowPressure", 1, 0, 20);
      FIELD_DECAY_HIGH_PRESSURE = builder.defineInRange("fieldDecayHighPressure", 2, 0, 20);
      CORRUPTION_SPREAD_FIELD_THRESHOLD = builder.defineInRange("corruptionSpreadFieldThreshold", 40, 0, 100);
      CORRUPTION_SPREAD_PRESSURE_THRESHOLD = builder.defineInRange("corruptionSpreadPressureThreshold", 15, 0, 100);
      STORM_WINDOW_MULTIPLIER = builder.defineInRange("stormWindowMultiplier", 4, 1, 20);
      REALITY_TEAR_FIELD_THRESHOLD = builder.defineInRange("realityTearFieldThreshold", 20, 0, 100);
      FIELD_MAP_RADIUS = builder.comment("Fixed Terminal field-map display radius in chunks. Nexus sync stores a stable 5x5 grid for this release, so valid values are intentionally capped at 2.")
         .defineInRange("fieldMapRadius", 2, 1, 2);
      builder.pop();
      builder.push("seals");
      SEAL_TICK_INTERVAL = builder.defineInRange("sealTickInterval", 35, 10, 1200);
      SEAL_RADIUS = builder.defineInRange("sealRadius", 5, 1, 16);
      SEAL_RELAY_AMOUNT = builder.defineInRange("sealRelayAmount", 96, 1, 10000);
      SEAL_DEFENSE_DAMAGE = builder.defineInRange("sealDefenseDamage", 6, 1, 100);
      SEAL_PURIFY_PRESSURE_REDUCTION = builder.defineInRange("sealPurifyPressureReduction", 5, 0, 100);
      SEAL_COLLAPSE_FIELD_LOSS = builder.defineInRange("sealCollapseFieldLoss", 6, 0, 100);
      builder.pop();
      builder.push("gear");
      ARMOR_LOCK_COOLDOWN = builder.defineInRange("armorLockCooldown", 2400, 0, 72000);
      ARMOR_LOCK_FIELD_GAIN = builder.defineInRange("armorLockFieldGain", 8, 0, 100);
      ARMOR_LOCK_CORRUPTION_REDUCTION = builder.defineInRange("armorLockCorruptionReduction", 8, 0, 100);
      STABILIZED_PURITY_FIELD_GAIN = builder.comment("Field recovery from the stronger collapsed-chunk purity charge.")
         .defineInRange("stabilizedPurityFieldGain", 16, 0, 100);
      STABILIZED_PURITY_CORRUPTION_REDUCTION = builder.defineInRange("stabilizedPurityCorruptionReduction", 32, 0, 100);
      FIELD_ANCHOR_TICKS = builder.comment("Quarantine duration applied by Field Anchor recovery tool.")
         .defineInRange("fieldAnchorTicks", 2400, 0, 72000);
      FIELD_ANCHOR_FIELD_GAIN = builder.defineInRange("fieldAnchorFieldGain", 12, 0, 100);
      FIELD_ANCHOR_CORRUPTION_REDUCTION = builder.defineInRange("fieldAnchorCorruptionReduction", 24, 0, 100);
      builder.pop();
      builder.push("bosses");
      BOSS_HEALTH_PERCENT = builder.defineInRange("bossHealthPercent", 115, 10, 1000);
      BOSS_DAMAGE_PERCENT = builder.defineInRange("bossDamagePercent", 105, 10, 1000);
      builder.pop();
      builder.push("dungeons");
      DUNGEON_LOOT_SCALE_PERCENT = builder.comment("Loot intensity hint used by dungeon templates and tests. Datapacks can tune loot tables separately.")
         .defineInRange("dungeonLootScalePercent", 110, 0, 500);
      builder.pop();
      SPEC = builder.build();
   }
}
