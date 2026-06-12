package com.knoxhack.echoashfallprotocol.echo;

import com.echoplatform.echocore.api.EchoRuntimeModules;
import com.echoplatform.echocore.api.EchoCoreServices;
import com.knoxhack.echoashfallprotocol.Config;
import com.knoxhack.echoashfallprotocol.event.EnvironmentalEventHandler;
import com.knoxhack.echoashfallprotocol.event.EnvironmentalEventType;
import com.knoxhack.echoashfallprotocol.integration.AshfallDiscoveryProvider;
import com.knoxhack.echoashfallprotocol.item.BatteryItem;
import com.knoxhack.echoashfallprotocol.registry.ModBlocks;
import com.knoxhack.echoashfallprotocol.registry.ModAttachments;
import com.knoxhack.echoashfallprotocol.registry.ModItems;
import com.knoxhack.echoashfallprotocol.registry.ModSounds;
import com.knoxhack.echoashfallprotocol.entity.ModEntities;
import com.knoxhack.echoashfallprotocol.entity.EchoCompanionDrone;
import com.knoxhack.echoashfallprotocol.entity.faction.FactionNpcEntity;
import com.knoxhack.echoashfallprotocol.survival.SurvivalData;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.phys.AABB;
import net.minecraft.core.Holder;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.biome.Biome;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages the ECHO-7 AI guide system.
 * Handles mission assignment, completion checking, and contextual messages.
 */
public class EchoGuideManager {
    private static final net.minecraft.resources.Identifier FIND_DROP_POD_ADVANCEMENT =
            net.minecraft.resources.Identifier.fromNamespaceAndPath(
                    com.knoxhack.echoashfallprotocol.EchoAshfallProtocol.MODID,
                    "find_drop_pod");
    private static final String FIND_DROP_POD_CRITERION = "found_drop_pod";
    private static final int STARTER_RECYCLER_BATTERY_ENERGY = 1_000;

    // Cache for entity encounter checks to reduce allocations
    private static final Map<java.util.UUID, CachedEncounterCheck> encounterCache = new ConcurrentHashMap<>();
    private static final Map<java.util.UUID, EchoMessages.Context> lastSurvivalWarningContext = new ConcurrentHashMap<>();
    private static final Map<java.util.UUID, Long> lastSurvivalWarningTick = new ConcurrentHashMap<>();
    private static final double ENCOUNTER_CHECK_DISTANCE_SQ = 16.0; // Only recheck if moved 4 blocks

    private static record CachedEncounterCheck(BlockPos pos, long tick, AABB aabb) {}

    private static final long MISSION_CHECK_INTERVAL = 40L; // Check every 2 seconds
    private static final long INTRO_STEP_DELAY = 60L; // 3 seconds between intro messages
    private static final long OBJECTIVE_HINT_INTERVAL = 2400L; // 2 minutes between objective reminders
    private static final long EARLY_OBJECTIVE_HINT_INTERVAL = 1200L; // 1 minute during early onboarding
    private static final long CONTEXT_CHECK_INTERVAL = 20L; // Check context every 1 second
    private static final long SURVIVAL_WARNING_COOLDOWN = 1200L; // 1 minute between same-state HUD warnings

    /**
     * Called every server tick per player.
     */
    public static void tick(ServerPlayer player) {
        QuestData quest = player.getData(ModAttachments.QUEST_DATA.get());
        long currentTick = player.level().getGameTime();

        if (quest.repairMissionState(player)) {
            QuestData.saveAndSync(player, quest);
        }

        // Play intro sequence on first join (staged for pacing)
        if (!quest.isEchoIntroPlayed()) {
            playIntroSequence(player, quest, currentTick);
            return;
        }

        // Check mission completion periodically
        if (currentTick % MISSION_CHECK_INTERVAL == 0) {
            checkFactionHubDiscovery(player, quest);
            checkMissionCompletion(player, quest);
        }

        // Send contextual messages periodically (priority-based cooldown in sendContextualMessage)
        if (currentTick % CONTEXT_CHECK_INTERVAL == 0) {
            sendContextualMessage(player, quest, currentTick);
        }

        if (quest.getNextObjectiveHintTick() > 0 && currentTick >= quest.getNextObjectiveHintTick()) {
            Mission current = MissionRegistry.getMission(quest.getCurrentPhase(), quest.getCurrentMissionIndex());
            if (current != null) {
                // Send objective reminder to Action Bar instead of Chat
                MissionUxSummary summary = MissionUxSummary.of(player, quest, current);
                String objectiveHint = "\u00A7b[GOAL]\u00A7r " + summary.shortTitle()
                        + " | " + summary.nextStep();
                sendMessage(player, quest, objectiveHint, true, false);
                quest.setNextObjectiveHintTick(currentTick + getObjectiveHintInterval(quest));
                player.setData(ModAttachments.QUEST_DATA.get(), quest);
            }
        }

        // Biome detection
        checkBiomeTransition(player, quest);

        // Entity encounter detection
        if (currentTick % 100 == 0) { // Check every 5 seconds for performance
            checkEntityEncounters(player, quest);
        }

        // Drone idle chatter (sparse contextual hints via hologram)
        if (currentTick % 40 == 0) {
            boolean chatterScheduleChanged =
                    com.knoxhack.echoashfallprotocol.echo.chat.EchoDroneVoice.tickIdleChatter(player, quest);
            // Periodically sync drone mood with ECHO-7's current personality state
            com.knoxhack.echoashfallprotocol.echo.chat.EchoDroneVoice.syncMood(player);
            if (chatterScheduleChanged) {
                player.setData(ModAttachments.QUEST_DATA.get(), quest);
            }
        }
    }

    private static void checkBiomeTransition(ServerPlayer player, QuestData quest) {
        Holder<Biome> biomeHolder = player.level().getBiome(player.blockPosition());
        String currentId = biomeHolder.getRegisteredName();

        if (!currentId.equals(quest.getLastBiomeId())) {
            quest.setLastBiomeId(currentId);
            quest.visitLocation("biome", currentId);
            if (currentId.contains(":")) {
                quest.visitLocation("biome", currentId.substring(currentId.indexOf(':') + 1));
            }
            if (currentId.startsWith("echoashfallprotocol:")) {
                EchoCoreServices.discoverFeature(player,
                        AshfallDiscoveryProvider.biomeId(currentId.substring(currentId.indexOf(':') + 1)));
            }
            
            EchoMessages.Context context = switch (currentId) {
                case "echoashfallprotocol:the_wasteland" -> EchoMessages.Context.BIOME_WASTELAND;
                case "echoashfallprotocol:toxic_swamp" -> EchoMessages.Context.BIOME_TOXIC_SWAMP;
                case "echoashfallprotocol:ruined_cityscape" -> EchoMessages.Context.BIOME_RUINED_CITY;
                case "echoashfallprotocol:cryogenic_ruins" -> EchoMessages.Context.BIOME_CRYOGENIC;
                case "echoashfallprotocol:crash_zone_wasteland" -> EchoMessages.Context.BIOME_CRASH_ZONE;
                case "echoashfallprotocol:industrial_ruins" -> EchoMessages.Context.BIOME_INDUSTRIAL_RUINS;
                case "echoashfallprotocol:radiation_zone" -> EchoMessages.Context.BIOME_RADIATION_ZONE;
                case "echoashfallprotocol:nexus_scar" -> EchoMessages.Context.BIOME_NEXUS_SCAR;
                default -> null;
            };

            if (context != null) {
                sendMessage(player, quest, EchoMessages.getMessage(context), false, false);
            }

            player.setData(ModAttachments.QUEST_DATA.get(), quest);
        }
    }

    private static void checkFactionHubDiscovery(ServerPlayer player, QuestData quest) {
        Mission current = MissionRegistry.getMission(quest.getCurrentPhase(), quest.getCurrentMissionIndex());
        if (current == null || !"first_faction_contact".equals(current.id())
                || quest.hasVisitedLocation("poi", "faction_hub")) {
            return;
        }

        String discoveredId = detectNearbyFactionHub(player);
        if (discoveredId == null) {
            return;
        }

        quest.discoverPOI(discoveredId);
        quest.discoverPOI("faction_hub");
        quest.visitLocation("poi", "faction_hub");
        quest.addToArchive("[DISCOVERY] Faction Hub located.");
        sendMessage(player, quest, "[ECHO-7] Faction hub located. Return to the terminal to complete contact protocol.", true, false);
        QuestData.saveAndSync(player, quest);
    }

    private static String detectNearbyFactionHub(ServerPlayer player) {
        AABB area = new AABB(player.blockPosition()).inflate(64.0, 24.0, 64.0);
        List<LivingEntity> entities = player.level().getEntitiesOfClass(LivingEntity.class, area);

        for (LivingEntity entity : entities) {
            if (entity instanceof FactionNpcEntity npc) {
                EchoCoreServices.markFactionContacted(player, npc.factionId());
                return npc.factionId().getPath() + "_contact";
            }
        }

        boolean hasVillageNpc = entities.stream().anyMatch(entity -> {
            String typeId = EntityType.getKey(entity.getType()).toString();
            return "minecraft:villager".equals(typeId) || "minecraft:iron_golem".equals(typeId);
        });
        if (hasVillageNpc || hasVillageBlockSignature((ServerLevel) player.level(), player.blockPosition())) {
            return "survivor_network_contact";
        }

        return null;
    }

    private static boolean hasVillageBlockSignature(ServerLevel level, BlockPos center) {
        int paths = 0;
        int hay = 0;
        int lights = 0;
        int bells = 0;

        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (int dx = -48; dx <= 48; dx += 2) {
            for (int dz = -48; dz <= 48; dz += 2) {
                for (int dy = -12; dy <= 16; dy += 2) {
                    cursor.set(center.getX() + dx, center.getY() + dy, center.getZ() + dz);
                    Block block = level.getBlockState(cursor).getBlock();
                    if (block == Blocks.DIRT_PATH) paths++;
                    else if (block == Blocks.HAY_BLOCK) hay++;
                    else if (block == Blocks.LANTERN || block == Blocks.SOUL_LANTERN) lights++;
                    else if (block == Blocks.BELL) bells++;

                    if (bells > 0 || (paths >= 16 && (hay >= 2 || lights >= 4))) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    private static void checkEntityEncounters(ServerPlayer player, QuestData quest) {
        long currentTick = player.level().getGameTime();
        BlockPos playerPos = player.blockPosition();
        
        // Check if we can use cached AABB (player hasn't moved far)
        CachedEncounterCheck cache = encounterCache.get(player.getUUID());
        AABB area;
        if (cache != null && playerPos.distSqr(cache.pos) < ENCOUNTER_CHECK_DISTANCE_SQ && (currentTick - cache.tick) < 100) {
            area = cache.aabb;
        } else {
            area = new AABB(playerPos).inflate(12.0);
            encounterCache.put(player.getUUID(), new CachedEncounterCheck(playerPos, currentTick, area));
        }
        
        List<LivingEntity> entities = player.level().getEntitiesOfClass(LivingEntity.class, area);

        for (LivingEntity entity : entities) {
            if (entity == player) continue;

            String typeId = EntityType.getKey(entity.getType()).toString();
            if (!quest.hasSeenEntity(typeId)) {
                quest.addSeenEntity(typeId);

                EchoMessages.Context context = switch (typeId) {
                    case "echoashfallprotocol:echo_drone" -> EchoMessages.Context.ENTITY_DRONE;
                    case "echoashfallprotocol:scavenger_bandit" -> EchoMessages.Context.ENTITY_SCAVENGER;
                    case "echoashfallprotocol:rad_zombie", "echoashfallprotocol:irradiated_wolf" -> EchoMessages.Context.ENTITY_MUTANT;
                    case "echoashfallprotocol:glowing_ghoul" -> EchoMessages.Context.ENTITY_GLOWING_GHOUL;
                    case "echoashfallprotocol:city_stalker" -> EchoMessages.Context.ENTITY_CITY_STALKER;
                    case "echoashfallprotocol:toxic_slime" -> EchoMessages.Context.ENTITY_TOXIC_SLIME;
                    case "echoashfallprotocol:rust_walker" -> EchoMessages.Context.ENTITY_RUST_WALKER;
                    case "echoashfallprotocol:ash_wraith" -> EchoMessages.Context.ENTITY_ASH_WRAITH;
                    case "echoashfallprotocol:steam_wraith" -> EchoMessages.Context.ENTITY_STEAM_WRAITH;
                    case "echoashfallprotocol:mutated_crawler" -> EchoMessages.Context.ENTITY_MUTATED_CRAWLER;
                    default -> null;
                };

                if (context != null) {
                    sendMessage(player, quest, EchoMessages.getMessage(context), false, false);
                    player.setData(ModAttachments.QUEST_DATA.get(), quest);
                    break; // Only one reaction per check to avoid spam
                }
            }
        }
    }

    private static void playIntroSequence(ServerPlayer player, QuestData quest, long currentTick) {
        initializeDropPod(player, quest);

        List<String> intro = EchoMessages.getIntroSequence();
        int step = quest.getIntroStep();
        if (step < intro.size()) {
            if (currentTick - quest.getLastMessageTick() >= INTRO_STEP_DELAY) {
                player.sendSystemMessage(Component.literal(intro.get(step)));
                quest.setIntroStep(step + 1);
                quest.setLastMessageTick(currentTick);
                player.setData(ModAttachments.QUEST_DATA.get(), quest);
            }
            return;
        }

        quest.setEchoIntroPlayed(true);
        quest.setLastMessageTick(currentTick);
        quest.setNextObjectiveHintTick(currentTick + getObjectiveHintInterval(quest));
        player.setData(ModAttachments.QUEST_DATA.get(), quest);

        // Assign first mission
        Mission firstMission = MissionRegistry.getMission(0, 0);
        if (firstMission != null) {
            sendMessage(player, quest, firstMission.echoMessage(), false, false);
            sendMessage(player, quest, "\u00A7b[ECHO-7]\u00A7r First move: anchor the pod outpost, craft a tool, confirm clean water, then expand.", false, false);
            sendMessage(player, quest, "\u00A76[ECHO-7]\u00A7r Stay near the crash site. Claim each mission reward before crafting the next bridge step.", false, false);
            sendMessage(player, quest, "\u00A7a[ECHO-7]\u00A7r ECHO rewards bridge the purifier route. Use them with pod salvage before relying on random ruins.", false, false);
            if (firstMission.isTurnInMission()) {
                MissionUxSummary summary = MissionUxSummary.of(player, quest, firstMission);
                sendMessage(player, quest,
                        "\u00A7b[GOAL]\u00A7r " + summary.shortTitle() + " | " + summary.nextStep(),
                        true,
                        false);
            }
        }
    }

    // Impact direction: 0=front, 1=right, 2=back, 3=left
    private static int impactDirection;
    
    private static void initializeDropPod(ServerPlayer player, QuestData quest) {
        if (quest.isDropPodInitialized()) {
            spawnDamagedCompanionDrone(player, player.blockPosition(), quest);
            player.setData(ModAttachments.QUEST_DATA.get(), quest);
            return;
        }
        if (player.getPersistentData().getBoolean("ashes_of_tomorrow.received_kit").orElse(false)) {
            quest.setDropPodInitialized(true);
            spawnDamagedCompanionDrone(player, player.blockPosition(), quest);
            player.setData(ModAttachments.QUEST_DATA.get(), quest);
            grantFindDropPodAdvancement(player);
            return;
        }

        if (!(player.level() instanceof ServerLevel level)) return;

        BlockPos center = player.blockPosition();
        var random = level.getRandom();

        // Use NBT-based drop pod placement (unified with world-generated echo pods)
        BlockPos spawnPos = com.knoxhack.echoashfallprotocol.worldgen.ProceduralStructureGenerator.placeStartingDropPod(level, center, random);

        if (spawnPos == null) {
            // Fallback if NBT template not found - log error and skip
            com.knoxhack.echoashfallprotocol.EchoAshfallProtocol.LOGGER.error("Failed to place NBT drop pod in EchoGuideManager");
            return;
        }

        // The NBT template already includes the pod structure, crater, and interior
        // Add additional gameplay elements on top of the NBT placement

        // Play spawn effects (explosion sound, particles, fire)
        playSpawnEffects(player, center, random.nextInt(3) + 1);

        // Teleport player to interior position returned by NBT placement
        player.teleportTo(spawnPos.getX() + 0.5, spawnPos.getY(), spawnPos.getZ() + 0.5);

        // Give personal items directly to player
        givePersonalSupplies(player);

        // Spawn damaged companion drone near player
        spawnDamagedCompanionDrone(player, spawnPos, quest);

        // Add emergency cache chest at front of pod
        placeEmergencyCache(player, center, 1);

        quest.setDropPodInitialized(true);
        player.setData(ModAttachments.QUEST_DATA.get(), quest);
        grantFindDropPodAdvancement(player);
    }

    private static void grantFindDropPodAdvancement(ServerPlayer player) {
        var holder = player.level().getServer().getAdvancements().get(FIND_DROP_POD_ADVANCEMENT);
        if (holder != null) {
            player.getAdvancements().award(holder, FIND_DROP_POD_CRITERION);
        }
    }
    
    private static BlockPos getSafeSpawnPosition(ServerPlayer player, BlockPos center, int crashLevel) {
        // Try multiple positions inside the larger pod body to find a safe spawn
        BlockPos[] possibleSpawns = {
            center.offset(0, 1, 0),   // Center
            center.offset(0, 1, -1),  // Slightly back
            center.offset(0, 1, 1),   // Slightly forward
            center.offset(-1, 1, 0),  // Left
            center.offset(1, 1, 0),   // Right
            center.offset(0, 1, -2),  // Further back (bigger pod)
            center.offset(0, 1, 2),   // Further forward (bigger pod)
            center.offset(-2, 1, 0),  // Further left (bigger pod)
            center.offset(2, 1, 0),   // Further right (bigger pod)
        };
        
        for (BlockPos pos : possibleSpawns) {
            // Check if position is safe (air at feet and head level)
            if (player.level().getBlockState(pos).isAir() && 
                player.level().getBlockState(pos.above()).isAir()) {
                return pos;
            }
        }
        
        // If no safe position found, force clear center and return it
        player.level().setBlockAndUpdate(center.offset(0, 1, 0), Blocks.AIR.defaultBlockState());
        player.level().setBlockAndUpdate(center.offset(0, 2, 0), Blocks.AIR.defaultBlockState());
        return center.offset(0, 1, 0);
    }
    
    private static void playSpawnEffects(ServerPlayer player, BlockPos center, int crashLevel) {
        // Play explosion sound
        player.level().playSound(player, center, 
            net.minecraft.sounds.SoundEvents.GENERIC_EXPLODE.value(), 
            SoundSource.BLOCKS, 1.0f, 0.8f);
        
        // Spawn smoke particles
        for (int i = 0; i < 20; i++) {
            double px = center.getX() + (player.level().getRandom().nextDouble() - 0.5) * 6;
            double py = center.getY() + 1 + player.level().getRandom().nextDouble() * 3;
            double pz = center.getZ() + (player.level().getRandom().nextDouble() - 0.5) * 6;
            ((net.minecraft.server.level.ServerLevel) player.level()).sendParticles(
                net.minecraft.core.particles.ParticleTypes.LARGE_SMOKE, px, py, pz, 1, 0.1, 0.1, 0.1, 0.01);
        }
        
        // Fire particles for destroyed pods
        if (crashLevel == 3) {
            for (int i = 0; i < 10; i++) {
                double px = center.getX() + (player.level().getRandom().nextDouble() - 0.5) * 4;
                double py = center.getY() + 1;
                double pz = center.getZ() + (player.level().getRandom().nextDouble() - 0.5) * 4;
                player.level().setBlockAndUpdate(BlockPos.containing(px, py, pz), ModBlocks.CRASH_SLAG.get().defaultBlockState());
            }
        }
    }
    
    private static void generateFuturisticPod(ServerPlayer player, BlockPos center, int crashLevel, int burialDepth) {
        Block hull = ModBlocks.DROP_POD_HULL.get();
        Block glass = ModBlocks.DROP_POD_GLASS.get();
        Block debris = ModBlocks.DEBRIS_BLOCK.get();
        Block rustedMetal = ModBlocks.RUSTED_METAL_SHEET.get();
        Block charredWood = ModBlocks.CHARRED_WOOD_LOG.get();
        var random = player.level().getRandom();
        
        // Enhanced pod dimensions — BIGGER POD
        int mainLength = 9;    // Main body length (z) — increased from 5
        int mainWidth = 5;     // Main body width (x) — increased from 3
        int mainHeight = 7;    // Main body height (y) — increased from 5
        int noseLength = 3;    // Tapered nose cone — increased from 2
        int thrusterLength = 3; // Flared thruster section — increased from 2
        
        // Clear a larger area for the pod
        clearPodArea(player, center, mainWidth + 2, mainHeight + 2, mainLength + noseLength + thrusterLength + 2, burialDepth);
        
        // Generate main body with directional damage
        for (int dy = -burialDepth; dy < mainHeight; dy++) {
            for (int dx = -mainWidth/2; dx <= mainWidth/2; dx++) {
                for (int dz = -mainLength/2; dz <= mainLength/2; dz++) {
                    BlockPos pos = center.offset(dx, dy, dz);
                    
                    boolean isEdge = Math.abs(dx) == mainWidth/2 || Math.abs(dz) == mainLength/2;
                    boolean isTop = dy == mainHeight - 1;
                    boolean isBottom = dy == -burialDepth;
                    
                    if (isEdge || isTop || isBottom) {
                        double damageChance = calculateDirectionalDamage(dx, dz, mainWidth, mainLength, crashLevel);
                        
                        if (random.nextDouble() < damageChance && dy > 0) {
                            // Progressive damage based on crash level
                            if (crashLevel == 3 && random.nextDouble() < 0.3) {
                                player.level().setBlockAndUpdate(pos, charredWood.defaultBlockState());
                            } else {
                                player.level().setBlockAndUpdate(pos, random.nextBoolean() ? debris.defaultBlockState() : rustedMetal.defaultBlockState());
                            }
                        } else if (isViewportPosition(dx, dy, dz, mainWidth, mainHeight, mainLength)) {
                            // Viewports with directional breaking
                            double breakChance = calculateViewportBreakChance(dx, dz, crashLevel);
                            if (random.nextDouble() < breakChance) {
                                player.level().setBlockAndUpdate(pos, debris.defaultBlockState());
                            } else {
                                player.level().setBlockAndUpdate(pos, glass.defaultBlockState());
                            }
                        } else {
                            player.level().setBlockAndUpdate(pos, hull.defaultBlockState());
                        }
                    } else if (dy >= 0) {
                        player.level().setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
                    }
                }
            }
        }
        
        // Generate nose cone (tapered front)
        generateNoseCone(player, center, mainWidth, mainLength, noseLength, burialDepth, hull, debris, crashLevel);
        
        // Generate thruster section (flared rear)
        generateThrusterSection(player, center, mainWidth, mainLength, thrusterLength, burialDepth, hull, rustedMetal, charredWood, crashLevel);
        
        // Generate fins/stabilizers
        generateFins(player, center, mainHeight, mainLength, hull, debris, crashLevel);
        
        // Place Echo Terminal on back wall of larger pod
        BlockPos terminalPos = center.offset(0, 3, -3); // Back wall, raised position for bigger pod
        if (burialDepth < 2 && crashLevel < 3) {
            EchoCoreServices.placeTerminal(player.level(), terminalPos, player);
        }
    }
    
    private static void clearPodArea(ServerPlayer player, BlockPos center, int width, int height, int length, int burialDepth) {
        // First pass: clear everything in the pod interior area
        for (int dy = -burialDepth - 2; dy < height + 2; dy++) {
            for (int dx = -width/2; dx <= width/2; dx++) {
                for (int dz = -length/2; dz <= length/2; dz++) {
                    BlockPos pos = center.offset(dx, dy, dz);
                    // Clear all blocks except unbreakable bedrock
                    if (!player.level().getBlockState(pos).is(net.minecraft.world.level.block.Blocks.BEDROCK)) {
                        player.level().setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
                    }
                }
            }
        }
    }
    
    private static double calculateDirectionalDamage(int dx, int dz, int width, int length, int crashLevel) {
        // Calculate which side faces the impact direction
        double baseChance = crashLevel * 0.12;
        double directionalMultiplier = 1.0;
        
        switch (impactDirection) {
            case 0: // Front impact (toward +Z)
                if (dz > 0) directionalMultiplier = 2.5;
                else if (dz < 0) directionalMultiplier = 0.3;
                break;
            case 1: // Right impact (toward +X)
                if (dx > 0) directionalMultiplier = 2.5;
                else if (dx < 0) directionalMultiplier = 0.3;
                break;
            case 2: // Back impact (toward -Z)
                if (dz < 0) directionalMultiplier = 2.5;
                else if (dz > 0) directionalMultiplier = 0.3;
                break;
            case 3: // Left impact (toward -X)
                if (dx < 0) directionalMultiplier = 2.5;
                else if (dx > 0) directionalMultiplier = 0.3;
                break;
        }
        
        return Math.min(baseChance * directionalMultiplier, 0.9);
    }
    
    private static boolean isViewportPosition(int dx, int dy, int dz, int width, int height, int length) {
        // Viewports on sides at upper positions
        return Math.abs(dx) == width/2 && dy > 1 && dy < height - 1 && Math.abs(dz) < length/2 - 1;
    }
    
    private static double calculateViewportBreakChance(int dx, int dz, int crashLevel) {
        double baseChance = crashLevel == 1 ? 0.1 : (crashLevel == 2 ? 0.35 : 0.75);
        
        // Viewports facing impact direction break more easily
        boolean facesImpact = false;
        switch (impactDirection) {
            case 0: facesImpact = dz > 0; break; // Front
            case 1: facesImpact = dx > 0; break; // Right  
            case 2: facesImpact = dz < 0; break; // Back
            case 3: facesImpact = dx < 0; break; // Left
        }
        
        return facesImpact ? baseChance * 1.5 : baseChance;
    }
    
    private static void generateNoseCone(ServerPlayer player, BlockPos center, int mainWidth, int mainLength, 
                                         int noseLength, int burialDepth, Block hull, Block debris, int crashLevel) {
        var random = player.level().getRandom();
        int startZ = mainLength/2 + 1;
        
        for (int dz = 0; dz < noseLength; dz++) {
            // Taper down: 3x3 -> 1x1
            int taper = dz; 
            for (int dy = -burialDepth; dy < 3 - taper; dy++) {
                for (int dx = -1 + taper/2; dx <= 1 - taper/2; dx++) {
                    BlockPos pos = center.offset(dx, dy, startZ + dz);
                    
                    // Nose cone takes heavy damage on impact
                    double damageChance = (crashLevel * 0.25) + (dz * 0.15);
                    if (random.nextDouble() < damageChance && dz > 0) {
                        player.level().setBlockAndUpdate(pos, debris.defaultBlockState());
                    } else {
                        player.level().setBlockAndUpdate(pos, hull.defaultBlockState());
                    }
                }
            }
        }
    }
    
    private static void generateThrusterSection(ServerPlayer player, BlockPos center, int mainWidth, int mainLength,
                                                int thrusterLength, int burialDepth, Block hull, Block rustedMetal, 
                                                Block charredWood, int crashLevel) {
        var random = player.level().getRandom();
        int startZ = -mainLength/2 - thrusterLength;
        
        for (int dz = 0; dz < thrusterLength; dz++) {
            // Flare out slightly
            int flare = dz == thrusterLength - 1 ? 1 : 0;
            for (int dy = -burialDepth; dy < 4; dy++) {
                for (int dx = -mainWidth/2 - flare; dx <= mainWidth/2 + flare; dx++) {
                    BlockPos pos = center.offset(dx, dy, startZ + dz);
                    
                    // Thrusters are always damaged/missing
                    double damageChance = 0.4 + (crashLevel * 0.2);
                    if (random.nextDouble() < damageChance) {
                        // Destroyed thrusters use charred wood for scorch marks
                        if (crashLevel == 3 && dy < 2) {
                            player.level().setBlockAndUpdate(pos, charredWood.defaultBlockState());
                        } else {
                            player.level().setBlockAndUpdate(pos, rustedMetal.defaultBlockState());
                        }
                    } else {
                        player.level().setBlockAndUpdate(pos, hull.defaultBlockState());
                    }
                }
            }
        }
    }
    
    private static void generateFins(ServerPlayer player, BlockPos center, int mainHeight, int mainLength,
                                     Block hull, Block debris, int crashLevel) {
        var random = player.level().getRandom();
        // Two stabilizer fins on sides — positioned outside main body for bigger pod
        int[] finX = {-3, 3};
        
        for (int fx : finX) {
            for (int dy = 0; dy < mainHeight - 2; dy++) {
                for (int dz = -mainLength/2 + 2; dz < mainLength/2 - 2; dz++) {
                    BlockPos pos = center.offset(fx, dy, dz);
                    
                    // Fins often break off in crashes
                    double breakChance = crashLevel * 0.25;
                    if (random.nextDouble() < breakChance) {
                        player.level().setBlockAndUpdate(pos, debris.defaultBlockState());
                    } else {
                        player.level().setBlockAndUpdate(pos, hull.defaultBlockState());
                    }
                }
            }
        }
    }
    
    private static void generateEnhancedImpactSite(ServerPlayer player, BlockPos center, int crashLevel) {
        Block debris = ModBlocks.DEBRIS_BLOCK.get();
        Block contaminatedSoil = ModBlocks.CONTAMINATED_SOIL.get();
        var random = player.level().getRandom();
        
        int craterRadius = 6 + crashLevel; // Larger crater for bigger pod
        
        // Scatter debris and contaminated soil
        for (int dx = -craterRadius; dx <= craterRadius; dx++) {
            for (int dz = -craterRadius; dz <= craterRadius; dz++) {
                double distance = Math.sqrt(dx*dx + dz*dz);
                if (distance > craterRadius) continue;
                
                BlockPos surfacePos = center.offset(dx, 0, dz);
                // Find surface
                while (surfacePos.getY() > center.getY() - 3 && player.level().getBlockState(surfacePos).isAir()) {
                    surfacePos = surfacePos.below();
                }
                
                double scatterChance = 1.0 - (distance / craterRadius);
                scatterChance *= crashLevel * 0.3;
                
                if (random.nextDouble() < scatterChance) {
                    Block blockToPlace = random.nextBoolean() ? debris : contaminatedSoil;
                    player.level().setBlockAndUpdate(surfacePos.above(), blockToPlace.defaultBlockState());
                }
                
                // Create gentle crater depression near center
                if (distance < 3 && crashLevel > 1) {
                    BlockPos below = surfacePos.below();
                    if (random.nextBoolean() && !player.level().getBlockState(below).isAir()) {
                        player.level().setBlockAndUpdate(surfacePos, contaminatedSoil.defaultBlockState());
                    }
                }
            }
        }
        
        // Add scattered rusted metal sheets for high damage
        if (crashLevel >= 2) {
            int shrapnelCount = crashLevel * 3;
            for (int i = 0; i < shrapnelCount; i++) {
                int dx = random.nextInt(craterRadius * 2 + 1) - craterRadius;
                int dz = random.nextInt(craterRadius * 2 + 1) - craterRadius;
                BlockPos shrapnelPos = center.offset(dx, 1, dz);
                
                if (player.level().getBlockState(shrapnelPos).isAir()) {
                    player.level().setBlockAndUpdate(shrapnelPos, ModBlocks.RUSTED_METAL_SHEET.get().defaultBlockState());
                }
            }
        }
    }
    
    private static void placeEmergencyCache(ServerPlayer player, BlockPos center, int burialDepth) {
        // Place an Echo cache at the front of the larger pod (storage alcove area)
        BlockPos chestPos = center.offset(0, 1, 3);

        player.level().setBlockAndUpdate(chestPos, ModBlocks.STRUCTURE_CACHE.get().defaultBlockState());

        if (player.level().getBlockEntity(chestPos) instanceof net.minecraft.world.Container cache) {
            populateEmergencyCache(cache, player.level().getRandom());
        }
    }

    private static void populateEmergencyCache(net.minecraft.world.Container chest, net.minecraft.util.RandomSource random) {
        // General emergency supplies
        chest.setItem(0, new ItemStack(ModItems.SCRAP_METAL.get(), 24));
        chest.setItem(1, new ItemStack(ModItems.SCRAP_WIRE.get(), 12));
        chest.setItem(2, new ItemStack(ModItems.SCRAP_PLASTIC.get(), 12));
        chest.setItem(3, new ItemStack(ModItems.DIRTY_WATER_BOTTLE.get(), 4));
        chest.setItem(4, new ItemStack(ModItems.CLEAN_WATER_BOTTLE.get(), 4));
        chest.setItem(5, new ItemStack(ModItems.EMERGENCY_RATION.get(), 6));
        chest.setItem(6, new ItemStack(net.minecraft.world.item.Items.TORCH, 16));
        chest.setItem(7, new ItemStack(net.minecraft.world.item.Items.COOKED_BEEF, 4));
        chest.setItem(8, new ItemStack(ModItems.FILTER_CARTRIDGE_BASIC.get(), 3));
        chest.setItem(9, new ItemStack(ModItems.BANDAGE.get(), 3));
        chest.setItem(10, new ItemStack(ModItems.STIM_PACK.get(), 1));
        chest.setItem(11, new ItemStack(ModItems.RAD_AWAY.get(), 1));
        chest.setItem(12, new ItemStack(net.minecraft.world.item.Items.STICK, 8));
        
        // Random bonus item
        if (random.nextBoolean()) {
            chest.setItem(13, new ItemStack(ModItems.FILTER_CARTRIDGE_BASIC.get(), 1));
        }
    }
    
    private static void addInteriorDetails(ServerPlayer player, BlockPos center, int crashLevel, int burialDepth) {
        var random = player.level().getRandom();
        
        // Emergency lighting - positioned for larger pod interior
        if (crashLevel == 1) {
            // Intact - integrated drop pod glass lighting on walls at multiple positions
            player.level().setBlockAndUpdate(center.offset(-2, 3, 0), ModBlocks.DROP_POD_GLASS.get().defaultBlockState());
            player.level().setBlockAndUpdate(center.offset(2, 3, 0), ModBlocks.DROP_POD_GLASS.get().defaultBlockState());
            player.level().setBlockAndUpdate(center.offset(0, 3, 2), ModBlocks.DROP_POD_GLASS.get().defaultBlockState());
        } else if (crashLevel == 2) {
            // Damaged - exposed power conduits, some broken
            player.level().setBlockAndUpdate(center.offset(-2, 3, 0), 
                random.nextBoolean() ? ModBlocks.POWER_CABLE.get().defaultBlockState() : Blocks.AIR.defaultBlockState());
            if (random.nextBoolean()) {
                player.level().setBlockAndUpdate(center.offset(2, 3, 0), ModBlocks.DROP_POD_GLASS.get().defaultBlockState());
            }
        }
        // Level 3 (destroyed) has no working lights
        
        // Seating/Control station - pilot seats for larger pod
        if (crashLevel < 3 && burialDepth < 2) {
            // Front seats
            BlockPos seat1Pos = center.offset(-1, 0, 2);
            BlockPos seat2Pos = center.offset(1, 0, 2);
            if (player.level().getBlockState(seat1Pos).isAir()) {
                player.level().setBlockAndUpdate(seat1Pos, ModBlocks.EMERGENCY_BUNK.get().defaultBlockState());
            }
            if (player.level().getBlockState(seat2Pos).isAir()) {
                player.level().setBlockAndUpdate(seat2Pos, ModBlocks.EMERGENCY_BUNK.get().defaultBlockState());
            }
        }
        
        // Emergency ceiling lights for bigger pod (glass blocks as light sources)
        if (crashLevel < 3) {
            BlockPos ceilingLight1 = center.offset(0, 6, 0);
            BlockPos ceilingLight2 = center.offset(-2, 6, 2);
            BlockPos ceilingLight3 = center.offset(2, 6, -2);
            player.level().setBlockAndUpdate(ceilingLight1, ModBlocks.DROP_POD_GLASS.get().defaultBlockState());
            if (crashLevel == 1) {
                player.level().setBlockAndUpdate(ceilingLight2, ModBlocks.DROP_POD_GLASS.get().defaultBlockState());
                player.level().setBlockAndUpdate(ceilingLight3, ModBlocks.DROP_POD_GLASS.get().defaultBlockState());
            }
        }
        
        // Scattered interior debris for atmosphere (salvageable scraps)
        if (crashLevel >= 2) {
            BlockPos[] debrisSpots = {
                center.offset(-1, 0, -1),
                center.offset(1, 0, 1),
                center.offset(-2, 0, 2),
                center.offset(2, 0, -2),
            };
            for (BlockPos debrisPos : debrisSpots) {
                if (random.nextDouble() < 0.4 && player.level().getBlockState(debrisPos).isAir()) {
                    // Place a decorative debris block
                    player.level().setBlockAndUpdate(debrisPos, ModBlocks.DEBRIS_BLOCK.get().defaultBlockState());
                }
            }
        }
    }
    
    private static void givePersonalSupplies(ServerPlayer player) {
        // Items that should be with the player personally
        giveStarterGasMaskIfMissing(player);
        giveIfBelow(player, ModItems.FILTER_CARTRIDGE_BASIC.get(), 4);
        giveIfBelow(player, ModItems.EMERGENCY_RATION.get(), 6);
        giveIfBelow(player, ModItems.BANDAGE.get(), 4);
        giveIfBelow(player, ModItems.CLEAN_WATER_BOTTLE.get(), 3);
        giveIfBelow(player, net.minecraft.world.item.Items.WOODEN_SWORD, 1);
    }
    
    private static void spawnDamagedCompanionDrone(ServerPlayer player, BlockPos spawnPos, QuestData quest) {
        if (quest.isDroneDeployed()) return; // Already has a drone
        
        // Spawn drone near the player's safe position inside the pod
        BlockPos dronePos = spawnPos.above(); 
        
        EchoCompanionDrone drone = ModEntities.ECHO_COMPANION_DRONE.get().create((ServerLevel)player.level(), null, dronePos, 
            net.minecraft.world.entity.EntitySpawnReason.EVENT, false, false);
        if (drone != null) {
            drone.setOwnerUUID(player.getUUID());
            drone.setCurrentMode(EchoCompanionDrone.DroneMode.FOLLOW);
            drone.setNoGravity(true);
            
            // Drone starts damaged at 15% "health" (repair progression, not entity health)
            // The actual entity is at full health but capabilities are limited
            player.level().addFreshEntity(drone);
            
            quest.setDroneDeployed(true);
            
            // Send ECHO message about damaged drone
            sendMessage(player, quest, "\u00A76[ECHO-7]\u00A7r Companion drone deployed. Unit is damaged from impact and requires repairs to restore full functionality.", false, true);
            sendMessage(player, quest, "\u00A77->\u00A7r Access the DRONE tab in this terminal to view repair requirements.", false, false);
        }
    }

    private static void giveStarterGasMaskIfMissing(ServerPlayer player) {
        if (countItem(player, ModItems.GAS_MASK.get()) > 0) {
            return;
        }

        ItemStack gasMask = new ItemStack(ModItems.GAS_MASK.get());
        gasMask.setDamageValue(gasMask.getMaxDamage() / 10);
        player.getInventory().add(gasMask);
    }

    private static void giveIfBelow(ServerPlayer player, Item item, int targetCount) {
        int currentCount = countItem(player, item);
        if (currentCount < targetCount) {
            player.getInventory().add(new ItemStack(item, targetCount - currentCount));
        }
    }

    private static int countItem(ServerPlayer player, Item item) {
        int count = 0;
        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
            ItemStack stack = player.getInventory().getItem(slot);
            if (stack.is(item)) {
                count += stack.getCount();
            }
        }
        return count;
    }

    private static void checkMissionCompletion(ServerPlayer player, QuestData quest) {
        int phase = quest.getCurrentPhase();
        int missionIdx = quest.getCurrentMissionIndex();
        Mission currentMission = MissionRegistry.getMission(phase, missionIdx);

        if (currentMission == null) return;
        
        if (currentMission.isTurnInMission()
                && hasAllRequirements(player, currentMission)
                && !isTerminalInstalled()) {
            sendMessage(player, quest,
                    "\u00A7b[ECHO-7]\u00A7r Requirements verified. Completing protocol through server fallback.",
                    true,
                    false);
            completeMission(player, quest, currentMission);
            return;
        }

        // Skip auto-completion for turn-in missions - they require manual turn-in
        if (currentMission.isTurnInMission()) {
            // Remind only when every active requirement is satisfied.
            if (hasAllRequirements(player, currentMission) && !quest.hasTurnInReminder(currentMission.id())) {
                sendMessage(player, quest, "§b[ECHO-7]§r Items ready. Open terminal and press [TURN IN].", true, false);
                quest.setTurnInReminder(currentMission.id());
                player.setData(ModAttachments.QUEST_DATA.get(), quest);
            }
            return;
        }

        // Auto-complete non-turn-in missions
        if (currentMission.isComplete(player)) {
            completeMission(player, quest, currentMission);
        }
    }

    private static boolean isTerminalInstalled() {
        return EchoRuntimeModules.isLoaded("echoterminal");
    }
    
    /**
     * Manually turn in a mission at the terminal.
     * Called when player clicks "Turn In" button in the terminal UI.
     * Validates all requirement types: items, blocks, entity kills, and locations.
     * 
     * @return true if turn-in was successful
     */
    public static boolean turnInMission(ServerPlayer player, QuestData quest, Mission mission) {
        if (!mission.isTurnInMission()) {
            return false; // Not a turn-in mission
        }
        
        // Check all requirement types
        if (!hasAllRequirements(player, mission)) {
            sendMessage(player, quest, "\u00A7c[ECHO-7]\u00A7r Mission requirements not yet satisfied. Check the active mission checklist.", true, false);
            return false;
        }
        
        // Validate item delivery requirements without consuming inventory.
        if (!consumeRequiredItems(player, mission)) {
            sendMessage(player, quest, "§c[ECHO-7]§r Failed to validate item requirements. Contact administrator.", true, false);
            return false;
        }
        
        // Complete the mission
        completeMission(player, quest, mission);
        return true;
    }
    
    /**
     * Check if all mission requirements are satisfied.
     * Validates items, blocks, entity kills, and location visits.
     */
    public static boolean hasAllRequirements(ServerPlayer player, Mission mission) {
        QuestData quest = QuestData.get(player);

        if (!mission.isComplete(player)) {
            return false;
        }

        // Check consumable item delivery requirements.
        if (mission.validatesRequiredItems() && !mission.hasRequiredItems(player)) {
            return false;
        }

        // Check block placement requirements
        for (Mission.BlockRequirement blockReq : mission.requiredBlocks()) {
            if (!MissionRegistry.hasBlockNearPlayer(player, blockReq.blockId())) {
                return false;
            }
        }

        // Check entity kill requirements
        for (Mission.EntityKillRequirement killReq : mission.requiredEntityKills()) {
            int killed = quest.getEntityKills(killReq.entityType());
            if (killed < killReq.count()) {
                return false;
            }
        }

        // Check location visit requirements
        for (Mission.LocationRequirement locReq : mission.requiredLocations()) {
            if (!quest.hasVisitedLocation(locReq.locationType(), locReq.locationId())) {
                return false;
            }
        }

        if (!mission.hasRequiredEquipment(player)) {
            return false;
        }

        return true;
    }
    
    /**
     * Complete a mission and handle rewards/advancement.
     */
    private static void completeMission(ServerPlayer player, QuestData quest, Mission mission) {
        applyMissionCompletionSideEffects(player, mission);
        boolean missionCoreHandled = EchoCoreServices.completeMission(
                player,
                net.minecraft.resources.Identifier.fromNamespaceAndPath(
                        com.knoxhack.echoashfallprotocol.EchoAshfallProtocol.MODID,
                        mission.id()));

        List<ItemStack> rewardStacks = missionCoreHandled
                ? java.util.Collections.emptyList()
                : buildRewardStacks(player, mission);

        // Mark mission as completed in quest data. MissionCore owns its own
        // claimable rewards; legacy Ashfall rewards stay mission-scoped here.
        quest.completeMission(player, mission.id(), rewardStacks);
        quest.clearTurnInReminder(mission.id());

        // Green celebratory burst on the drone, if present
        com.knoxhack.echoashfallprotocol.echo.chat.EchoDroneVoice.triggerEvent(player,
                com.knoxhack.echoashfallprotocol.echo.chat.EchoDroneVoice.EventType.MISSION_COMPLETE);

        // Send completion message
        sendMessage(player, quest, mission.completionMessage(), false, true);
        
        if (!missionCoreHandled && !rewardStacks.isEmpty()) {
            player.sendSystemMessage(Component.literal("\u00A76[ECHO-7]\u00A7r Support cache ready in the mission channel. Claim it before building the next route step."));
        }
        
        // Advance to next mission
        int previousPhase = quest.getCurrentPhase();
        int phase = previousPhase;
        
        quest.repairMissionState(player);
        if (quest.getCurrentPhase() > previousPhase) {
            // Phase complete — advance phase
                // Phase was advanced by repairMissionState().
                sendMessage(player, quest, "§6[ECHO-7]§r Phase " + (phase + 1) + " complete. Advancing to next operational phase.", false, true);
        }

        // Unlocks are recalculated by repairMissionState() using phase gates.

        // Assign next mission
        Mission nextMission = MissionRegistry.getMission(quest.getCurrentPhase(), quest.getCurrentMissionIndex());
        if (nextMission != null && quest.isMissionUnlocked(nextMission.id())) {
            sendMessage(player, quest, nextMission.echoMessage(), false, false);
            quest.setNextObjectiveHintTick(player.level().getGameTime() + getObjectiveHintInterval(quest));
        }

        quest.setLastMessageTick(player.level().getGameTime());
        QuestData.saveAndSync(player, quest);
    }

    private static void applyMissionCompletionSideEffects(ServerPlayer player, Mission mission) {
        if ("build_hand_recycler".equals(mission.id())) {
            ensureStarterRecyclerBattery(player);
        }

        if (mission.id().endsWith("_epilogue")) {
            com.knoxhack.echoashfallprotocol.endgame.PostNexusData post =
                    com.knoxhack.echoashfallprotocol.endgame.PostNexusData.get(player);
            post.setEpilogueComplete(true);
            com.knoxhack.echoashfallprotocol.endgame.PostNexusData.saveAndSync(player, post);
            return;
        }

        if (mission.id().endsWith("_epilogue")) {
            com.knoxhack.echoashfallprotocol.endgame.PostNexusData post =
                    com.knoxhack.echoashfallprotocol.endgame.PostNexusData.get(player);
            post.setEpilogueComplete(true);
            com.knoxhack.echoashfallprotocol.endgame.PostNexusData.saveAndSync(player, post);
        }
    }

    private static void ensureStarterRecyclerBattery(ServerPlayer player) {
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (stack.is(ModItems.BASIC_BATTERY.get())) {
                int stored = BatteryItem.getStoredEnergy(stack);
                if (stored >= STARTER_RECYCLER_BATTERY_ENERGY) {
                    return;
                }
                BatteryItem.setStoredEnergy(stack, STARTER_RECYCLER_BATTERY_ENERGY);
                player.sendSystemMessage(Component.literal(
                        "\u00A7b[ECHO-7]\u00A7r Starter battery charged to 1000 FE for the first recycler run."));
                return;
            }
        }

        ItemStack starterBattery = BatteryItem.withEnergy(
                ModItems.BASIC_BATTERY.get(),
                STARTER_RECYCLER_BATTERY_ENERGY);
        if (!player.getInventory().add(starterBattery)) {
            player.drop(starterBattery, false);
        }
        player.sendSystemMessage(Component.literal(
                "\u00A7b[ECHO-7]\u00A7r Starter battery issued with 1000 FE for the first recycler run."));
    }
    
    /**
     * Unlock any missions that had the completed mission as a prerequisite.
     */
    private static void unlockDependentMissions(ServerPlayer player, QuestData quest, String completedMissionId) {
        boolean anyUnlocked = false;
        
        for (int p = 0; p < MissionRegistry.getPhaseCount(); p++) {
            for (int m = 0; m < MissionRegistry.getMissionCount(p); m++) {
                Mission mission = MissionRegistry.getMission(p, m);
                if (mission != null && !quest.isMissionUnlocked(mission.id())) {
                    // Check if this mission has the completed mission as a prerequisite
                    if (mission.getPrerequisites().contains(completedMissionId)) {
                        // Check if all prerequisites are now complete
                        // Check if all prerequisites are now complete
                        boolean allPrereqsComplete = true;
                        for (String prereqId : mission.getPrerequisites()) {
                            if (!quest.isMissionCompleted(prereqId)) {
                                allPrereqsComplete = false;
                                break;
                            }
                        }
                        if (allPrereqsComplete) {
                            quest.unlockMission(mission.id());
                            anyUnlocked = true;
                        }
                    }
                }
            }
        }
        
        if (anyUnlocked) {
            player.setData(ModAttachments.QUEST_DATA.get(), quest);
        }
    }
    
    private static List<ItemStack> buildRewardStacks(ServerPlayer player, Mission mission) {
        List<ItemStack> rewards = new java.util.ArrayList<>();
        for (ItemStack stack : mission.rewards()) {
            ItemStack copy = stack.copy();
            if (mission.category() == Mission.MissionCategory.EXPLORATION
                    || mission.category() == Mission.MissionCategory.COMBAT
                    || mission.category() == Mission.MissionCategory.STORY) {
                com.knoxhack.echoashfallprotocol.research.PerkEffectHandler.applyLootBonus(player, copy);
            }
            rewards.add(copy);
        }
        return rewards;
    }
    
    /**
     * Validate required items for a turn-in mission without consuming them.
     */
    private static boolean consumeRequiredItems(ServerPlayer player, Mission mission) {
        // Mission turn-in verifies required items but no longer consumes them.
        return true;
    }
    
    /**
     * Claim all pending rewards from the terminal.
     * Called when player clicks "Claim" button or uses claim command.
     */
    public static void claimRewards(ServerPlayer player) {
        QuestData quest = QuestData.get(player);
        Mission current = AshfallMissionActions.currentMission(quest);
        if (current != null && claimRewardsExact(player, quest, current.id(), false)) {
            return;
        }
        for (String missionId : quest.getCompletedMissionIds()) {
            if (claimRewardsExact(player, quest, missionId, false)) {
                return;
            }
        }
        if (!EchoCoreServices.claimTerminalRewards(player)) {
            player.sendSystemMessage(Component.literal("\u00A77[ECHO-7]\u00A7r No sealed terminal caches are waiting."));
        }
    }

    public static void claimRewards(ServerPlayer player, String missionId) {
        String cleanMissionId = AshfallMissionActions.cleanMissionPayload(missionId);
        if (cleanMissionId.isBlank()) {
            claimRewards(player);
            return;
        }
        QuestData quest = QuestData.get(player);
        if (!claimRewardsExact(player, quest, cleanMissionId, true)) {
            player.sendSystemMessage(Component.literal("\u00A77[ECHO-7]\u00A7r No sealed support cache is waiting for this protocol."));
            QuestData.syncToClient(player);
        }
    }

    private static boolean claimRewardsExact(ServerPlayer player, QuestData quest, String missionId, boolean reportRepeat) {
        String cleanMissionId = AshfallMissionActions.cleanMissionPayload(missionId);
        if (cleanMissionId.isBlank()) {
            return false;
        }
        if (claimMissionCoreReward(player, cleanMissionId)) {
            if (quest.hasPendingRewards(cleanMissionId)) {
                quest.claimRewards(cleanMissionId);
                QuestData.saveAndSync(player, quest);
            } else {
                QuestData.syncToClient(player);
            }
            return true;
        }
        List<ItemStack> rewards = quest.claimRewards(cleanMissionId);
        if (!rewards.isEmpty()) {
            awardRewards(player, rewards);
            QuestData.saveAndSync(player, quest);
            return true;
        }
        if (reportRepeat) {
            Mission mission = MissionRegistry.getMissionById(cleanMissionId);
            if (mission != null && quest.isMissionCompleted(cleanMissionId) && mission.rewards().isEmpty()) {
                player.sendSystemMessage(Component.literal("\u00A77[ECHO-7]\u00A7r This protocol is already complete."));
                QuestData.syncToClient(player);
                return true;
            }
        }
        return false;
    }

    private static boolean claimMissionCoreReward(ServerPlayer player, String missionId) {
        return com.knoxhack.echoashfallprotocol.integration.AshfallMissionCoreIntegration.claimReward(player, missionId);
    }

    private static void awardRewards(ServerPlayer player, List<ItemStack> rewards) {
        if (rewards.isEmpty()) return;

        player.sendSystemMessage(Component.literal("\u00A76[SUPPORT CACHE CLAIMED]\u00A7r"));
        for (ItemStack stack : rewards) {
            ItemStack copy = stack.copy();
            player.sendSystemMessage(Component.literal("  + " + copy.getCount() + "x ").append(copy.getHoverName()));
            if (!player.getInventory().add(copy)) {
                player.drop(copy, false);
            }
        }
    }

    private static void sendContextualMessage(ServerPlayer player, QuestData quest, long currentTick) {
        SurvivalData survival = player.getData(ModAttachments.SURVIVAL_DATA.get());
        EchoMessages.Context context = determineContext(player, survival);

        if (context != null) {
            if (isSurvivalWarningContext(context)) {
                java.util.UUID playerId = player.getUUID();
                EchoMessages.Context lastContext = lastSurvivalWarningContext.get(playerId);
                long lastTick = lastSurvivalWarningTick.getOrDefault(playerId, Long.MIN_VALUE / 2);
                long cooldown = Math.max(SURVIVAL_WARNING_COOLDOWN, Config.ECHO_MESSAGE_COOLDOWN.get());

                if (context != lastContext || currentTick - lastTick >= cooldown) {
                    sendMessage(player, quest, EchoMessages.getMessage(context), true, false, false);
                    lastSurvivalWarningContext.put(playerId, context);
                    lastSurvivalWarningTick.put(playerId, currentTick);
                }
                return;
            }

            EchoMessages.Priority priority = EchoMessages.getPriority(context);
            long cooldown = priority.getCooldownTicks();
            
            // Check if enough time has passed based on priority
            if (currentTick - quest.getLastMessageTick() >= cooldown) {
                String msg = EchoMessages.getMessage(context);
                boolean isCritical = (priority == EchoMessages.Priority.CRITICAL);
                sendMessage(player, quest, msg, false, isCritical);
                quest.setLastMessageTick(currentTick);
                player.setData(ModAttachments.QUEST_DATA.get(), quest);
            }
        }
    }

    /**
     * Centralized message system with audio and visual feedback.
     */
    private static void sendMessage(ServerPlayer player, QuestData quest, String message, boolean actionBar, boolean isCritical) {
        sendMessage(player, quest, message, actionBar, isCritical, true);
    }

    private static void sendMessage(ServerPlayer player, QuestData quest, String message, boolean actionBar, boolean isCritical, boolean archiveMessage) {
        SurvivalData survival = player.getData(ModAttachments.SURVIVAL_DATA.get());
        String finalMessage = message;

        // Apply Stability Glitch if radiation is high
        if (survival.getRadiationLevel() > 60.0f && player.level().getRandom().nextFloat() < 0.25f) {
            finalMessage = corruptMessage(message, player.level().getRandom());
        }

        // Add to Archive (Modernization)
        if (archiveMessage) {
            quest.addToArchive(message);
            player.setData(ModAttachments.QUEST_DATA.get(), quest);
        }

        // Route through the drone if it's deployed and nearby: hologram + sound from drone,
        // and tag chat with "DRONE" origin so the player sees ECHO-7 speaking through it.
        com.knoxhack.echoashfallprotocol.echo.chat.EchoDroneVoice.EventType eventType = isCritical
                ? com.knoxhack.echoashfallprotocol.echo.chat.EchoDroneVoice.EventType.CRITICAL_ALERT
                : com.knoxhack.echoashfallprotocol.echo.chat.EchoDroneVoice.EventType.INFO;
        boolean droneRouted = com.knoxhack.echoashfallprotocol.echo.chat.EchoDroneVoice.relayMessage(player, finalMessage, eventType);
        String chatMessage = droneRouted
                ? finalMessage.replaceFirst("^§b\\[ECHO-7\\]§r", "§b[ECHO-7 // DRONE]§r")
                : finalMessage;

        if (actionBar) {
            player.sendSystemMessage(Component.literal(chatMessage), true);
        } else if (isCritical) {
            // Screen Titles for critical alerts
            player.connection.send(new net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket(Component.literal("\u00A7c[ECHO ALERT]")));
            player.connection.send(new net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket(Component.literal(chatMessage)));
            player.sendSystemMessage(Component.literal(chatMessage));
        } else {
            player.sendSystemMessage(Component.literal(chatMessage));
        }

        // If the drone didn't take over the spatial audio, play Echo sound from player position
        if (!droneRouted) {
            player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                    ModSounds.ECHO_MESSAGE.get(),
                    SoundSource.PLAYERS, 1.0f, 1.0f);
        }
    }

    private static boolean isSurvivalWarningContext(EchoMessages.Context context) {
        return switch (context) {
            case NO_FILTER, LOW_HEALTH, LOW_FILTER, HIGH_RADIATION, LOW_HYDRATION -> true;
            default -> false;
        };
    }

    private static String corruptMessage(String message, net.minecraft.util.RandomSource random) {
        StringBuilder sb = new StringBuilder();
        for (char c : message.toCharArray()) {
            if (c != '§' && random.nextFloat() < 0.15f) {
                sb.append("§k").append(c).append("§r");
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    private static EchoMessages.Context determineContext(ServerPlayer player, SurvivalData survival) {
        // Priority-ordered context detection
        if (survival.isToxicAirActive() && (!survival.hasMask() || survival.isFilterDepleted())) {
            return EchoMessages.Context.NO_FILTER;
        }
        if (player.getHealth() < 6.0f) {
            return EchoMessages.Context.LOW_HEALTH;
        }
        if (survival.hasMask() && survival.getFilterPercent() < 0.15f) {
            return EchoMessages.Context.LOW_FILTER;
        }
        if (survival.getRadiationLevel() > 60.0f) {
            return EchoMessages.Context.HIGH_RADIATION;
        }
        if (survival.getHydration() <= Config.HYDRATION_PENALTY_LEVEL.get()) {
            return EchoMessages.Context.LOW_HYDRATION;
        }
        if (isActiveStorm(player)) {
            return EchoMessages.Context.STORM;
        }
        long time = player.level().getOverworldClockTime() % 24000L;
        if (time >= 13000L && time < 23000L) {
            return EchoMessages.Context.NIGHT;
        }

        // Random idle message (low chance)
        if (player.level().getRandom().nextFloat() < 0.1f) {
            return EchoMessages.Context.IDLE;
        }
        return null;
    }

    private static boolean isActiveStorm(ServerPlayer player) {
        return player.level().isThundering()
                || EnvironmentalEventHandler.isEventActive(player.level(), EnvironmentalEventType.RADIATION_STORM)
                || EnvironmentalEventHandler.isEventActive(player.level(), EnvironmentalEventType.TOXIC_STORM)
                || EnvironmentalEventHandler.isEventActive(player.level(), EnvironmentalEventType.ASH_STORM)
                || EnvironmentalEventHandler.isEventActive(player.level(), EnvironmentalEventType.CRYO_FRONT)
                || EnvironmentalEventHandler.isEventActive(player.level(), EnvironmentalEventType.NEXUS_SURGE);
    }

    private static long getObjectiveHintInterval(QuestData quest) {
        return quest.getCurrentPhase() <= 1 ? EARLY_OBJECTIVE_HINT_INTERVAL : OBJECTIVE_HINT_INTERVAL;
    }
}
