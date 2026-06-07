package com.knoxhack.echoashfallprotocol.guardian;

import com.knoxhack.echoashfallprotocol.entity.ModEntities;
import com.knoxhack.echoashfallprotocol.entity.boss.BiomeBossEntity;
import com.knoxhack.echoashfallprotocol.faction.AshfallBiomeFactions;
import com.knoxhack.echoashfallprotocol.registry.ModItems;
import com.knoxhack.echoashfallprotocol.worldgen.StructureType;
import net.minecraft.world.BossEvent;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.ItemLike;

import java.util.Collection;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;

public final class BiomeGuardianProfiles {
    private static final Map<String, BiomeGuardianProfile> BY_BIOME = new LinkedHashMap<>();
    private static final Map<String, BiomeGuardianProfile> BY_BOSS_PATH = new LinkedHashMap<>();
    private static final Map<String, BiomeGuardianProfile> BY_MISSION = new LinkedHashMap<>();

    static {
        register(profile(
                "ruined_plains",
                "neutralize_plains_warlord",
                "plains_warlord",
                "Plains Warlord",
                AshfallBiomeFactions.RADWARDEN_COMPACT,
                StructureType.RADWARDEN_OUTPOST,
                Set.of(StructureType.BIO_LAB, StructureType.RADIO_TOWER, StructureType.CRASHBREAK_SALVAGE_YARD, StructureType.DROP_POD),
                "reinforced bunker door",
                "Underground Radwarden Command Bunker",
                "A survivor commander turned emergency order into territory law below the plains and never stood down.",
                ModEntities.PLAINS_WARLORD::get,
                ModEntities.SCAVENGER_BANDIT::get,
                0xFFFFA94D,
                175.0f, 10.0f, 9.0f, 0.255f,
                6.0f, 6.5f, 105, 255, 7,
                BiomeGuardianProfile.GuardianAbility.COMMAND_RALLY,
                BiomeGuardianProfile.ArenaHazard.COMMAND_BUNKER,
                BiomeGuardianProfile.ParticleCue.ELECTRIC_SPARK,
                BiomeGuardianProfile.PulseSound.ANVIL,
                true,
                List.of(status(MobEffects.MINING_FATIGUE, 70, 0)),
                List.of(status(MobEffects.MINING_FATIGUE, 70, 0)),
                rewards(
                        loot(ModItems.NEXUS_CRYSTAL::get, 1, 1),
                        loot(ModItems.DENSE_ALLOY_CHUNK::get, 3, 2),
                        loot(ModItems.ENERGY_CELL::get, 3, 2),
                        loot(ModItems.SCHEMATIC_FRAGMENT_WEAPONS::get, 1, 0)
                ),
                visual("textures/entity/plains_warlord.png", 0xFFFFB35A, 0xFFFFB35A, 1.02f, 0.88f,
                        BossEvent.BossBarColor.YELLOW, false, BiomeGuardianProfile.VisualVariant.WARLORD),
                "Command rally marks the bunker and calls reserves through any side lane left unsecured.",
                "Clear side rooms first and carry spare weapon durability; this fight punishes tunnel vision.",
                "Bunker channel active. Warlord calling reserves from dead doctrine.",
                "Command chain collapsing. All guns to the breach.",
                "Ruined Plains command bunker neutralized. Local patrol signal quiet."
        ));
        register(profile(
                "ruined_cityscape",
                "neutralize_city_ruin_stalker",
                "city_ruin_stalker",
                "City Ruin Stalker",
                AshfallBiomeFactions.CRASHBREAK_SALVAGE,
                StructureType.DATA_CENTER,
                Set.of(StructureType.SUBWAY_STATION, StructureType.SEWER_JUNCTION, StructureType.TRAIN_YARD, StructureType.ABANDONED_MINE),
                "subway service stair",
                "Subway Data-Vault Ambush Site",
                "A dead-city predator nests where transit cameras and data vaults still share power and blind spots breed hunger.",
                ModEntities.CITY_RUIN_STALKER::get,
                ModEntities.CITY_STALKER::get,
                0xFFC8A4FF,
                150.0f, 11.0f, 7.0f, 0.285f,
                5.5f, 5.0f, 90, 275, 5,
                BiomeGuardianProfile.GuardianAbility.SHADOW_AMBUSH,
                BiomeGuardianProfile.ArenaHazard.SHADOW_CORRIDORS,
                BiomeGuardianProfile.ParticleCue.ASH,
                BiomeGuardianProfile.PulseSound.SHRIEK,
                false,
                List.of(status(MobEffects.BLINDNESS, 55, 0)),
                List.of(status(MobEffects.BLINDNESS, 50, 0)),
                rewards(
                        loot(ModItems.NEXUS_CRYSTAL::get, 1, 1),
                        loot(ModItems.DENSE_ALLOY_CHUNK::get, 2, 2),
                        loot(ModItems.SCHEMATIC_FRAGMENT_MACHINES::get, 1, 0),
                        loot(ModItems.POWER_CELL::get, 1, 1)
                ),
                visual("textures/entity/city_ruin_stalker.png", 0xFFD0A8FF, 0xFFE0B6FF, 0.92f, 0.68f,
                        BossEvent.BossBarColor.PURPLE, false, BiomeGuardianProfile.VisualVariant.STALKER),
                "Shadow ambush blinds players and repositions the stalker through broken sightlines.",
                "Bring blocks or lights, and never let the arena choose what is behind you.",
                "Urban blackout routine active. Sightlines compromised.",
                "Vault shadows moving. Do not trust open corridors.",
                "City ambush signal collapsed. Data-vault lanes readable again."
        ));
        register(profile(
                "industrial_ruins",
                "neutralize_industrial_juggernaut",
                "industrial_juggernaut",
                "Industrial Juggernaut",
                AshfallBiomeFactions.CRASHBREAK_SALVAGE,
                StructureType.DATA_CENTER,
                Set.of(StructureType.TRAIN_YARD, StructureType.ABANDONED_MINE, StructureType.DERELICT_WORKSHOP, StructureType.SUBWAY_STATION),
                "ruined freight lift",
                "Factory Sublevel Maintenance Core",
                "An industrial exoshell still enforces production quotas in a sealed machine sublevel with no workers left to spare.",
                ModEntities.INDUSTRIAL_JUGGERNAUT::get,
                ModEntities.RUST_WALKER::get,
                0xFFFF8C42,
                215.0f, 12.0f, 13.0f, 0.215f,
                7.0f, 7.2f, 118, 275, 6,
                BiomeGuardianProfile.GuardianAbility.OVERHEAT_SLAM,
                BiomeGuardianProfile.ArenaHazard.HEAT_VENTS,
                BiomeGuardianProfile.ParticleCue.SMOKE,
                BiomeGuardianProfile.PulseSound.ANVIL,
                true,
                List.of(status(MobEffects.MINING_FATIGUE, 100, 0)),
                List.of(status(MobEffects.MINING_FATIGUE, 90, 0)),
                rewards(
                        loot(ModItems.NEXUS_CRYSTAL::get, 1, 2),
                        loot(ModItems.DENSE_ALLOY_CHUNK::get, 5, 3),
                        loot(ModItems.SCHEMATIC_FRAGMENT_WEAPONS::get, 1, 0),
                        loot(ModItems.MACHINE_UPGRADE_OVERCLOCK::get, 1, 0)
                ),
                visual("textures/entity/industrial_juggernaut.png", 0xFFFF8E45, 0xFFFF9A3D, 1.16f, 1.04f,
                        BossEvent.BossBarColor.YELLOW, false, BiomeGuardianProfile.VisualVariant.JUGGERNAUT),
                "Overheat slam punishes close-range tunneling but leaves readable windups around vent cycles.",
                "Bring sustained healing and keep a clear path around machinery lanes before the floor becomes instructions.",
                "Maintenance core pressurizing. Armor plating engaged.",
                "Factory failsafe active. Heavy machinery entering kill cycle.",
                "Industrial maintenance core disabled. Production order void."
        ));
        register(profile(
                "toxic_swamp",
                "neutralize_toxic_hive_matriarch",
                "toxic_hive_matriarch",
                "Toxic Hive Matriarch",
                AshfallBiomeFactions.SPOREBOUND_SANCTUM,
                StructureType.SPOREBOUND_SANCTUM,
                Set.of(StructureType.BIO_LAB, StructureType.SEWER_JUNCTION, StructureType.REACTOR_RUIN),
                "toxic sinkhole",
                "Sunken Bio-Hive",
                "A bio-command growth learned to route swamp toxins through living carriers and call it adaptation.",
                ModEntities.TOXIC_HIVE_MATRIARCH::get,
                ModEntities.MUTATED_CRAWLER::get,
                0xFF42D67E,
                185.0f, 9.0f, 8.0f, 0.235f,
                6.5f, 6.6f, 100, 235, 8,
                BiomeGuardianProfile.GuardianAbility.TOXIC_BROOD,
                BiomeGuardianProfile.ArenaHazard.HIVE_PODS,
                BiomeGuardianProfile.ParticleCue.SPORE,
                BiomeGuardianProfile.PulseSound.SLIME,
                false,
                List.of(status(MobEffects.WEAKNESS, 120, 0)),
                List.of(status(MobEffects.POISON, 85, 0), status(MobEffects.WEAKNESS, 90, 0)),
                rewards(
                        loot(ModItems.NEXUS_CRYSTAL::get, 1, 2),
                        loot(ModItems.DENSE_ALLOY_CHUNK::get, 2, 2),
                        loot(ModItems.FILTER_CARTRIDGE_ELITE::get, 1, 0),
                        loot(ModItems.MUTATED_TISSUE::get, 4, 3)
                ),
                visual("textures/entity/toxic_hive_matriarch.png", 0xFF5DDE7C, 0xFF7DFF9B, 1.05f, 0.92f,
                        BossEvent.BossBarColor.GREEN, false, BiomeGuardianProfile.VisualVariant.MATRIARCH),
                "Toxic brood fills the arena with crawlers and poison pressure if pods are ignored.",
                "Carry elite filtration, clean water, and a fast add-clear plan.",
                "Hive sacs rupturing. Filter load rising.",
                "Matriarch broadcasting brood panic. Keep moving.",
                "Toxic bio-hive command severed. Swamp pressure losing coordination."
        ));
        register(profile(
                "crash_zone_wasteland",
                "neutralize_crash_zone_colossus",
                "crash_zone_colossus",
                "Crash Zone Colossus",
                AshfallBiomeFactions.CRASHBREAK_SALVAGE,
                StructureType.MILITARY_VAULT,
                Set.of(StructureType.DROP_POD, StructureType.TRAIN_YARD, StructureType.SATELLITE_ARRAY),
                "impact vault breach",
                "Buried Military Crash Vault",
                "A fused wreckage mass guards the vault where crash telemetry, weapons stockpiles, and failed rescue orders crossed.",
                ModEntities.CRASH_ZONE_COLOSSUS::get,
                ModEntities.SCAVENGER_BANDIT::get,
                0xFFE25959,
                225.0f, 13.0f, 13.0f, 0.205f,
                8.0f, 7.8f, 120, 285, 7,
                BiomeGuardianProfile.GuardianAbility.DEBRIS_ARTILLERY,
                BiomeGuardianProfile.ArenaHazard.DEBRIS_FIELD,
                BiomeGuardianProfile.ParticleCue.SMOKE,
                BiomeGuardianProfile.PulseSound.EXPLOSION,
                true,
                List.of(status(MobEffects.MINING_FATIGUE, 100, 0)),
                List.of(status(MobEffects.MINING_FATIGUE, 80, 0)),
                rewards(
                        loot(ModItems.NEXUS_CRYSTAL::get, 2, 2),
                        loot(ModItems.DENSE_ALLOY_CHUNK::get, 5, 4),
                        loot(ModItems.SCHEMATIC_FRAGMENT_ARMOR::get, 1, 0),
                        loot(ModItems.POWER_CELL::get, 1, 1)
                ),
                visual("textures/entity/crash_zone_colossus.png", 0xFFE65B5B, 0xFFFF6F4C, 1.24f, 1.12f,
                        BossEvent.BossBarColor.RED, false, BiomeGuardianProfile.VisualVariant.COLOSSUS),
                "Debris artillery marks shock lanes before a heavy wreckage pulse.",
                "Repair armor first and keep escape space open around the vault core.",
                "Impact servos cycling. Brace for debris shock.",
                "Colossus core unstable. Wreckage field entering overload.",
                "Crash-zone vault control broken. Rescue telemetry released."
        ));
        register(profile(
                "radiation_zone",
                "neutralize_radiation_behemoth",
                "radiation_behemoth",
                "Radiation Behemoth",
                AshfallBiomeFactions.RADWARDEN_COMPACT,
                StructureType.REACTOR_RUIN,
                Set.of(StructureType.MILITARY_VAULT, StructureType.RADIO_TOWER, StructureType.SATELLITE_ARRAY),
                "reactor access hatch",
                "Reactor Containment Basement",
                "A reactor containment failure condensed into a moving radiation sink with a heartbeat of bad light.",
                ModEntities.RADIATION_BEHEMOTH::get,
                ModEntities.GLOWING_GHOUL::get,
                0xFFFF3333,
                205.0f, 11.0f, 12.0f, 0.225f,
                7.5f, 7.4f, 105, 245, 8,
                BiomeGuardianProfile.GuardianAbility.RADIATION_MELTDOWN,
                BiomeGuardianProfile.ArenaHazard.REACTOR_HOT_ZONE,
                BiomeGuardianProfile.ParticleCue.GLOW,
                BiomeGuardianProfile.PulseSound.ANCHOR,
                false,
                List.of(status(MobEffects.POISON, 90, 0)),
                List.of(status(MobEffects.POISON, 95, 0)),
                rewards(
                        loot(ModItems.NEXUS_CRYSTAL::get, 2, 2),
                        loot(ModItems.DENSE_ALLOY_CHUNK::get, 3, 3),
                        loot(ModItems.RAD_AWAY::get, 2, 1),
                        loot(ModItems.SCHEMATIC_FRAGMENT_ENERGY::get, 1, 0)
                ),
                visual("textures/entity/radiation_behemoth.png", 0xFFFF4A4A, 0xFFFF3030, 1.12f, 1.0f,
                        BossEvent.BossBarColor.RED, false, BiomeGuardianProfile.VisualVariant.BEHEMOTH),
                "Radiation meltdown stacks poison-style pressure and demands cleanup between pulses.",
                "Carry RadAway, hazmat gear, and a scrubber fallback if available.",
                "Containment rods exposed. Radiation climbing.",
                "Behemoth heat bloom detected. Treat exposure after the fight.",
                "Reactor containment behemoth neutralized. Hot-zone signal dropping."
        ));
        register(profile(
                "cryogenic_ruins",
                "neutralize_cryogenic_overseer",
                "cryogenic_overseer",
                "Cryogenic Overseer",
                AshfallBiomeFactions.RADWARDEN_COMPACT,
                StructureType.CRYOGENIC_RUINS,
                Set.of(StructureType.OBSERVATION_POST, StructureType.RELAY_STATION),
                "frozen service shaft",
                "Frozen Cryo-Storage Facility",
                "A preserved control intelligence still believes thawing survivors is a breach and cold is consent.",
                ModEntities.CRYOGENIC_OVERSEER::get,
                ModEntities.STEAM_WRAITH::get,
                0xFF8CD7FF,
                190.0f, 10.0f, 9.0f, 0.245f,
                6.5f, 6.2f, 96, 260, 6,
                BiomeGuardianProfile.GuardianAbility.CRYO_LOCK,
                BiomeGuardianProfile.ArenaHazard.CRYO_VENTS,
                BiomeGuardianProfile.ParticleCue.SNOW,
                BiomeGuardianProfile.PulseSound.GLASS,
                false,
                List.of(status(MobEffects.SLOWNESS, 110, 1)),
                List.of(status(MobEffects.SLOWNESS, 135, 1)),
                rewards(
                        loot(ModItems.NEXUS_CRYSTAL::get, 2, 2),
                        loot(ModItems.DENSE_ALLOY_CHUNK::get, 2, 2),
                        loot(ModItems.THERMAL_LINER::get, 1, 0),
                        loot(ModItems.HAND_WARMER::get, 3, 2)
                ),
                visual("textures/entity/cryogenic_overseer.png", 0xFF8FDBFF, 0xFF8FE6FF, 1.04f, 0.9f,
                        BossEvent.BossBarColor.BLUE, false, BiomeGuardianProfile.VisualVariant.OVERSEER),
                "Cryo lock slows the arena and punishes players who let freeze windows overlap.",
                "Bring thermal liners, hand warmers, food, and a fast weapon.",
                "Cryo-lock disengaged. Temperature dropping fast.",
                "Overseer venting storage lines. Warmth is now ammunition.",
                "Cryogenic storage command shut down. Thaw authority returned to the living."
        ));
        register(profile(
                "nexus_scar",
                "neutralize_nexus_scar_avatar",
                "nexus_scar_avatar",
                "Nexus Scar Avatar",
                AshfallBiomeFactions.SPOREBOUND_SANCTUM,
                StructureType.REACTOR_RUIN,
                Set.of(StructureType.BIO_LAB, StructureType.MILITARY_VAULT, StructureType.SATELLITE_ARRAY),
                "folded Nexus breach",
                "Folded Anomaly Chamber",
                "The Scar shaped a buried command echo into an avatar of the Core's unresolved will.",
                ModEntities.NEXUS_SCAR_AVATAR::get,
                ModEntities.ECHO_DRONE::get,
                0xFFFF4DFF,
                245.0f, 13.0f, 14.0f, 0.27f,
                8.5f, 8.2f, 82, 215, 9,
                BiomeGuardianProfile.GuardianAbility.NEXUS_RECURSION,
                BiomeGuardianProfile.ArenaHazard.ANOMALY_RIFTS,
                BiomeGuardianProfile.ParticleCue.PORTAL,
                BiomeGuardianProfile.PulseSound.ANCHOR,
                true,
                List.of(status(MobEffects.POISON, 85, 0)),
                List.of(status(MobEffects.POISON, 90, 0), status(MobEffects.WEAKNESS, 115, 0)),
                rewards(
                        loot(ModItems.NEXUS_CRYSTAL::get, 4, 2),
                        loot(ModItems.DENSE_ALLOY_CHUNK::get, 4, 3),
                        loot(ModItems.POWER_CELL::get, 3, 1),
                        loot(ModItems.SCHEMATIC_FRAGMENT_ENERGY::get, 1, 0)
                ),
                visual("textures/entity/nexus_scar_avatar.png", 0xFFFF67F8, 0xFFFF67F8, 1.18f, 1.08f,
                        BossEvent.BossBarColor.PURPLE, true, BiomeGuardianProfile.VisualVariant.NEXUS),
                "Nexus recursion teleports, clones pressure through drones, and stacks weakness near the end.",
                "Bring your best filtration, medicine, alloy or Nexus-tier weapons, and an exit route you trust more than the floor.",
                "Nexus recursion detected. Reality anchors slipping.",
                "Avatar entering final loop. The Core is listening.",
                "Nexus Scar avatar dissolved. Core route exposed."
        ));
    }

    private BiomeGuardianProfiles() {
    }

    private static BiomeGuardianProfile profile(String biomePath,
                                                String missionId,
                                                String bossPath,
                                                String title,
                                                net.minecraft.resources.Identifier ownerFaction,
                                                StructureType mainStructure,
                                                Set<StructureType> supportStructures,
                                                String surfaceEntrance,
                                                String undergroundSite,
                                                String lore,
                                                Supplier<EntityType<? extends BiomeBossEntity>> bossType,
                                                Supplier<EntityType<? extends Entity>> defenderType,
                                                int color,
                                                float maxHealth,
                                                float attackDamage,
                                                float armor,
                                                float movementSpeed,
                                                float pulseDamage,
                                                float pulseRadius,
                                                int pulseCooldownBase,
                                                int summonCooldownBase,
                                                int maxDefenders,
                                                BiomeGuardianProfile.GuardianAbility ability,
                                                BiomeGuardianProfile.ArenaHazard arenaHazard,
                                                BiomeGuardianProfile.ParticleCue particleCue,
                                                BiomeGuardianProfile.PulseSound pulseSound,
                                                boolean pulseKnockback,
                                                List<BiomeGuardianProfile.StatusEffect> strikeEffects,
                                                List<BiomeGuardianProfile.StatusEffect> pulseEffects,
                                                BiomeGuardianProfile.RewardBundle rewardBundle,
                                                BiomeGuardianProfile.VisualProfile visual,
                                                String mechanicHint,
                                                String prepHint,
                                                String phaseTwoLine,
                                                String phaseThreeLine,
                                                String defeatLine) {
        return new BiomeGuardianProfile(
                biomePath,
                missionId,
                bossPath,
                title,
                ownerFaction,
                mainStructure,
                supportStructures,
                surfaceEntrance,
                undergroundSite,
                lore,
                bossType,
                defenderType,
                color,
                maxHealth,
                attackDamage,
                armor,
                movementSpeed,
                pulseDamage,
                pulseRadius,
                pulseCooldownBase,
                summonCooldownBase,
                maxDefenders,
                ability,
                arenaHazard,
                particleCue,
                pulseSound,
                pulseKnockback,
                strikeEffects,
                pulseEffects,
                guardianRewardBundle(rewardBundle),
                visual,
                mechanicHint,
                prepHint,
                phaseTwoLine,
                phaseThreeLine,
                defeatLine,
                polishFor(ability, arenaHazard, title)
        );
    }

    private static void register(BiomeGuardianProfile profile) {
        BY_BIOME.put(profile.biomePath(), profile);
        BY_BOSS_PATH.put(profile.bossPath(), profile);
        BY_MISSION.put(profile.missionId(), profile);
    }

    private static BiomeGuardianProfile.StatusEffect status(net.minecraft.core.Holder<net.minecraft.world.effect.MobEffect> effect,
                                                           int duration,
                                                           int amplifier) {
        return new BiomeGuardianProfile.StatusEffect(effect, duration, amplifier);
    }

    private static BiomeGuardianProfile.LootEntry loot(Supplier<? extends ItemLike> item, int minCount, int randomBonus) {
        return new BiomeGuardianProfile.LootEntry(item, minCount, randomBonus);
    }

    private static BiomeGuardianProfile.RewardBundle rewards(BiomeGuardianProfile.LootEntry... entries) {
        return new BiomeGuardianProfile.RewardBundle(List.of(entries));
    }

    private static BiomeGuardianProfile.RewardBundle guardianRewardBundle(BiomeGuardianProfile.RewardBundle base) {
        List<BiomeGuardianProfile.LootEntry> entries = new ArrayList<>(base.entries());
        entries.add(loot(ModItems.GUARDIAN_DATACORE::get, 1, 0));
        return new BiomeGuardianProfile.RewardBundle(List.copyOf(entries));
    }

    private static BiomeGuardianProfile.PolishData polishFor(BiomeGuardianProfile.GuardianAbility ability,
                                                            BiomeGuardianProfile.ArenaHazard hazard,
                                                            String title) {
        return switch (ability) {
            case SHIELD_PULSE -> new BiomeGuardianProfile.PolishData(
                    "Recovery beacon ring", "Shield pulse opens a recovery window after each forced retreat.",
                    "Lit recovery beacons", "Rescue drone defender waves",
                    "Intro guardian datacore", "A readable first guardian built around safe pockets, failed rescue logic, and obvious shield pulses.",
                    title + " Entrance");
            case COMMAND_RALLY -> new BiomeGuardianProfile.PolishData(
                    "Command bunker lanes", "Command rally routes reserves through side lanes.",
                    "Rally banners and bunker cover", "Reserve squad pressure",
                    "Combat logistics datacore", "A command fight that rewards lane control before old authority floods the bunker with squads.",
                    title + " Bunker");
            case SHADOW_AMBUSH -> new BiomeGuardianProfile.PolishData(
                    "Broken sightline corridors", "Blackout ambush breaks line of sight.",
                    "Lit sightline pockets", "Shadow reposition pressure",
                    "Ambush telemetry datacore", "A city ambush that turns lighting, corner checks, and corridor discipline into counterplay.",
                    title + " Vault");
            case OVERHEAT_SLAM -> new BiomeGuardianProfile.PolishData(
                    "Overheated machinery lanes", "Armor slam leaves vent windows.",
                    "Vents and maintenance pockets", "Heavy slam pressure",
                    "Alloy foundry datacore", "A plated machine fight where baiting slams matters more than obeying the old factory floor.",
                    title + " Core");
            case TOXIC_BROOD -> new BiomeGuardianProfile.PolishData(
                    "Hive pod cluster", "Hive bloom punishes ignored pods.",
                    "Clearable hive pods", "Brood add pressure",
                    "Filtration datacore", "An attrition fight about filters, poison cleanup, add control, and staying ahead of the air.",
                    title + " Hive");
            case DEBRIS_ARTILLERY -> new BiomeGuardianProfile.PolishData(
                    "Marked wreckage lanes", "Debris impact marks shock lanes.",
                    "Wreckage cover and salvage shield", "Artillery lane pressure",
                    "Crash armor datacore", "A wreckage fight that teaches players to read impact lanes before rescue metal turns hostile.",
                    title + " Breach");
            case RADIATION_MELTDOWN -> new BiomeGuardianProfile.PolishData(
                    "Reactor scrubber pockets", "Meltdown pulse spikes exposure.",
                    "Scrubber pockets", "Exposure cleanup pressure",
                    "Reactor cleanup datacore", "An exposure-control fight that rewards planned RadAway, scrubber windows, and disciplined retreat.",
                    title + " Reactor");
            case CRYO_LOCK -> new BiomeGuardianProfile.PolishData(
                    "Warm-pocket thermal arrays", "Cryo lockdown stacks freeze pressure.",
                    "Warm pockets", "Freeze-control pressure",
                    "Thermal datacore", "A movement-control fight where warm pockets and thaw timing keep the arena readable.",
                    title + " Shaft");
            case NEXUS_RECURSION -> new BiomeGuardianProfile.PolishData(
                    "Anomaly rift anchors", "Nexus recursion escalates clones and weakness loops.",
                    "Rift anchors", "Anomaly clone pressure",
                    "Nexus-route datacore", "A capstone guardian with escalating rifts, teleport pressure, and a clean Core-route handoff.",
                    title + " Rift");
        };
    }

    private static BiomeGuardianProfile.VisualProfile visual(String texturePath,
                                                            int tint,
                                                            int glowColor,
                                                            float scale,
                                                            float shadow,
                                                            BossEvent.BossBarColor bossBarColor,
                                                            boolean darkenScreen,
                                                            BiomeGuardianProfile.VisualVariant variant) {
        String glowTexturePath = texturePath.replace(".png", "_glow.png");
        return new BiomeGuardianProfile.VisualProfile(
                texturePath,
                glowTexturePath,
                tint,
                glowColor,
                scale,
                shadow,
                bossBarColor,
                darkenScreen,
                variant
        );
    }

    public static Optional<BiomeGuardianProfile> byBiome(String biomePath) {
        return Optional.ofNullable(BY_BIOME.get(normalizePath(biomePath)));
    }

    public static Optional<BiomeGuardianProfile> byBossPath(String bossPath) {
        return Optional.ofNullable(BY_BOSS_PATH.get(normalizePath(bossPath)));
    }

    public static Optional<BiomeGuardianProfile> byEntityId(String entityId) {
        if (entityId == null) {
            return Optional.empty();
        }
        int separator = entityId.indexOf(':');
        return byBossPath(separator >= 0 ? entityId.substring(separator + 1) : entityId);
    }

    public static Optional<BiomeGuardianProfile> byMissionId(String missionId) {
        return Optional.ofNullable(BY_MISSION.get(missionId));
    }

    public static Optional<BiomeGuardianProfile> byBossType(EntityType<?> type) {
        if (type == null) {
            return Optional.empty();
        }
        return all().stream()
                .filter(profile -> profile.bossType().get() == type)
                .findFirst();
    }

    public static Collection<BiomeGuardianProfile> all() {
        return Collections.unmodifiableCollection(BY_BIOME.values());
    }

    public static Set<String> biomePaths() {
        return Collections.unmodifiableSet(BY_BIOME.keySet());
    }

    public static StructureType getMainStructureForBiome(String biomePath) {
        return byBiome(biomePath)
                .map(BiomeGuardianProfile::mainStructure)
                .orElse(null);
    }

    public static boolean isProfileStructureForBiome(String biomePath, StructureType type) {
        return byBiome(biomePath)
                .map(profile -> profile.supportsStructure(type))
                .orElse(false);
    }

    public static boolean isMainStructureForBiome(String biomePath, StructureType type) {
        return byBiome(biomePath)
                .map(profile -> profile.mainStructure() == type)
                .orElse(false);
    }

    private static String normalizePath(String id) {
        if (id == null) {
            return "";
        }
        String normalized = id.trim();
        int slash = normalized.lastIndexOf('/');
        int bracket = normalized.lastIndexOf(']');
        if (slash >= 0 && bracket > slash) {
            normalized = normalized.substring(slash + 1, bracket);
        }
        int separator = normalized.indexOf(':');
        if (separator >= 0 && separator + 1 < normalized.length()) {
            normalized = normalized.substring(separator + 1);
        }
        return normalized;
    }
}
