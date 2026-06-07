package com.knoxhack.echoashfallprotocol.event;

import com.knoxhack.echoashfallprotocol.Config;
import com.knoxhack.echoashfallprotocol.entity.ModEntities;
import com.knoxhack.echoashfallprotocol.registry.ModAttachments;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * Smart Events System - events react to player behavior.
 * HIGH_TECH_USAGE -> more AI attacks
 * HOARDING -> raids
 * STAYING_HIDDEN -> fewer threats
 */
public class SmartEventHandler {

    private static final int DECAY_INTERVAL = 1200; // Decay every 60 seconds
    private static final int EVENT_CHECK_INTERVAL = 1200; // Check for events every 60 seconds
    // Raised from 70 to 200 and decay from 5 to 15. Previous values triggered a raid every
    // ~24 crafts which made natural progression feel like an event-spam factory.
    private static final int TECH_USAGE_THRESHOLD = 200;
    private static final int DECAY_AMOUNT = 15;

    public static void onPlayerTick(Object event) {
        if (!(eventValue(event, "getEntity") instanceof ServerPlayer player)) return;
        long gameTime = player.level().getGameTime();

        SmartEventData data = player.getData(ModAttachments.SMART_EVENT_DATA.get());

        if (gameTime - data.getLastDecayTick() > DECAY_INTERVAL) {
            data.decayAll(DECAY_AMOUNT);
            data.setLastDecayTick(gameTime);
            player.setData(ModAttachments.SMART_EVENT_DATA.get(), data);
        }

        if (player.isCrouching() && gameTime % 100 == 0) {
            data.addStealth(1);
            player.setData(ModAttachments.SMART_EVENT_DATA.get(), data);
        }

        if (gameTime % EVENT_CHECK_INTERVAL == 0) {
            triggerSmartEvents(player, data);
        }
    }

    public static void onItemCrafted(Object event) {
        if (eventValue(event, "getEntity") instanceof ServerPlayer player) {
            SmartEventData data = player.getData(ModAttachments.SMART_EVENT_DATA.get());
            data.addTechUsage(3);
            player.setData(ModAttachments.SMART_EVENT_DATA.get(), data);
        }
    }

    public static void onItemPickup(Object event) {
        if (!(eventValue(event, "getPlayer") instanceof ServerPlayer player)) {
            return;
        }

        ItemStack pickedUpStack = itemStackValue(event, "getOriginalStack");
        if (pickedUpStack.isEmpty()) {
            return;
        }

        SmartEventData data = player.getData(ModAttachments.SMART_EVENT_DATA.get());
        data.addHoarding(1);
        player.setData(ModAttachments.SMART_EVENT_DATA.get(), data);

        if (pickedUpStack.is(com.knoxhack.echoashfallprotocol.registry.ModBlocks.NEXUS_CORE.get().asItem())) {
            com.knoxhack.echoashfallprotocol.echo.EchoGuideManager.tick(player);
        }

        com.knoxhack.echoashfallprotocol.echo.QuestData quest =
                player.getData(com.knoxhack.echoashfallprotocol.registry.ModAttachments.QUEST_DATA.get());

        String itemId = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(pickedUpStack.getItem()).toString();
        if (!quest.isAssetDiscovered(itemId)) {
            quest.discoverAsset(itemId);
            player.setData(com.knoxhack.echoashfallprotocol.registry.ModAttachments.QUEST_DATA.get(), quest);
            player.sendSystemMessage(Component.literal("[ECHO-7] ")
                    .withStyle(ChatFormatting.AQUA)
                    .append(Component.literal("New item profile registered: "
                            + pickedUpStack.getItem().getName(pickedUpStack).getString())
                            .withStyle(ChatFormatting.WHITE)));
        }
        if (pickedUpStack.is(com.knoxhack.echoashfallprotocol.registry.ModItems.MUTATED_TISSUE.get())) {
            recordSampleRecovered(player, quest);
        }
    }

    private static void recordSampleRecovered(ServerPlayer player, com.knoxhack.echoashfallprotocol.echo.QuestData quest) {
        com.knoxhack.echoashfallprotocol.world.POIScannerService.ScanHit hit =
                com.knoxhack.echoashfallprotocol.world.POIScannerService.scan(player);
        quest.visitLocation("special", "sample:mutated_tissue");
        if (hit != null && hit.distance() <= com.knoxhack.echoashfallprotocol.world.POIScannerService.DISCOVERY_RADIUS * 1.5D) {
            quest.recordPOIState(hit.id(), com.knoxhack.echoashfallprotocol.echo.QuestData.POIObjectiveState.SAMPLE_RECOVERED);
        }
        com.knoxhack.echoashfallprotocol.echo.QuestData.saveAndSync(player, quest);
    }

    private static void triggerSmartEvents(ServerPlayer player, SmartEventData data) {
        Level level = player.level();
        float reactivity = (float) Config.EVENT_REACTIVITY_MULTIPLIER.get().doubleValue();
        if (reactivity < 0.0f) reactivity = 0.0f;

        if (data.getTechUsageScore() > TECH_USAGE_THRESHOLD
                && level.getRandom().nextFloat() < (0.22f * reactivity)) {
            player.sendSystemMessage(Component.literal(
                    "[ECHO-7] Energy signature detected by hostile AI. Incoming threat!").withColor(0xFF5555));

            spawnThreatWave(player,
                    new EntityType<?>[]{
                            ModEntities.ECHO_DRONE.get(),
                            ModEntities.RAD_ZOMBIE.get()
                    },
                    3,
                    20.0);
        }

        if (data.getHoardingScore() > 85
                && level.getRandom().nextFloat() < (0.14f * reactivity)) {
            player.sendSystemMessage(Component.literal(
                    "[ECHO-7] Resource accumulation detected by scavenger bands. Defend your position!").withColor(0xFF8800));

            spawnThreatWave(player,
                    new EntityType<?>[]{
                            ModEntities.SCAVENGER_BANDIT.get(),
                            ModEntities.SCAVENGER_BANDIT.get(),
                            ModEntities.IRRADIATED_WOLF.get()
                    },
                    3,
                    16.0);

            data.addHoarding(-50);
            player.setData(ModAttachments.SMART_EVENT_DATA.get(), data);
        }

        if (data.getStealthScore() > 40) {
            // Future expansion: reduce nearby mob awareness.
        }
    }

    private static void spawnThreatWave(ServerPlayer player, EntityType<?>[] waveTypes, int count, double radius) {
        Level level = player.level();
        if (waveTypes.length == 0 || count <= 0) {
            return;
        }

        for (int i = 0; i < count; i++) {
            EntityType<?> pick = waveTypes[level.getRandom().nextInt(waveTypes.length)];
            spawnThreat(player, level, pick, radius);
        }
    }

    private static void spawnThreat(ServerPlayer player, Level level, EntityType<?> type, double radius) {
        if (!(type.create(level, EntitySpawnReason.EVENT) instanceof Mob mob)) {
            return;
        }

        double x = player.getX() + (level.getRandom().nextDouble() - 0.5) * radius;
        double z = player.getZ() + (level.getRandom().nextDouble() - 0.5) * radius;
        BlockPos spawnPos = level.getHeightmapPos(
                net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                new BlockPos((int) x, 0, (int) z));

        mob.setPos(x, spawnPos.getY(), z);
        level.addFreshEntity(mob);
    }

    private static ItemStack itemStackValue(Object event, String methodName) {
        Object value = eventValue(event, methodName);
        return value instanceof ItemStack stack ? stack : ItemStack.EMPTY;
    }

    private static Object eventValue(Object event, String methodName) {
        if (event == null) {
            return null;
        }
        try {
            return event.getClass().getMethod(methodName).invoke(event);
        } catch (ReflectiveOperationException | RuntimeException exception) {
            return null;
        }
    }
}
