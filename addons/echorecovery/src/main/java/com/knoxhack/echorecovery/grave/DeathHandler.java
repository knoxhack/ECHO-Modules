package com.knoxhack.echorecovery.grave;

import com.knoxhack.echo.adaptercore.EchoBackendLifecycleBridge;
import com.knoxhack.echo.adaptercore.EchoBackendWorldEventBridge;
import com.knoxhack.echorecovery.EchoRecovery;
import com.knoxhack.echorecovery.api.RecoveryItemRuleResult;
import com.knoxhack.echorecovery.api.RecoveryIntegrations;
import com.knoxhack.echorecovery.block.entity.GraveBlockEntity;
import com.knoxhack.echorecovery.config.RecoveryConfig;
import com.knoxhack.echorecovery.content.RecoveryContent;
import com.knoxhack.echorecovery.content.RecoveryGraveType;
import com.knoxhack.echorecovery.data.RecoveryWorldData;
import com.knoxhack.echorecovery.integration.RecoveryDataCoreIntegration;
import com.knoxhack.echorecovery.integration.RecoveryIntegrationDispatcher;
import com.knoxhack.echorecovery.item.GraveKeyItem;
import com.knoxhack.echorecovery.registry.ModBlocks;
import com.knoxhack.echorecovery.registry.ModItems;
import com.knoxhack.echorecovery.registry.ModSounds;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

public final class DeathHandler {
    public static final net.minecraft.resources.Identifier ASHFALL_FIELD_RECOVERY_CACHE =
            net.minecraft.resources.Identifier.fromNamespaceAndPath(EchoRecovery.MODID, "ashfall_field_recovery_cache");
    private static final Map<UUID, List<ItemStack>> PENDING_SOULBOUND = new ConcurrentHashMap<>();

    private DeathHandler() {}

    public static void register() {
        EchoBackendLifecycleBridge.registerGameEventHandler(DeathHandler::onPlayerDeath);
        EchoBackendLifecycleBridge.registerGameEventHandler(DeathHandler::onPlayerClone);
    }

    public static void onPlayerDeath(Object event) {
        Object source = invoke(event, "getSource");
        if (source == null || !(invoke(event, "getEntity") instanceof ServerPlayer player)) {
            return;
        }
        if (!RecoveryConfig.ENABLE_GRAVES.get()) {
            return;
        }
        if (!RecoveryConfig.CREATE_GRAVE_ON_PVP.get() && invoke(source, "getEntity") instanceof ServerPlayer) {
            return;
        }
        createGrave(player, deathMessage(source, player));
    }

    public static void onPlayerClone(Object event) {
        if (!wasDeathClone(event)) {
            return;
        }
        if (EchoBackendWorldEventBridge.cloneOriginalPlayer(event) instanceof ServerPlayer oldPlayer
                && EchoBackendWorldEventBridge.cloneNewPlayer(event) instanceof ServerPlayer newPlayer) {
            List<ItemStack> soulbound = PENDING_SOULBOUND.remove(oldPlayer.getUUID());
            if (soulbound != null) {
                for (ItemStack stack : soulbound) {
                    if (!stack.isEmpty() && !newPlayer.getInventory().add(stack.copy())) {
                        newPlayer.drop(stack.copy(), false);
                    }
                }
            }
            if (RecoveryConfig.GRAVE_KEY_ENABLED.get()) {
                for (int i = 0; i < oldPlayer.getInventory().getContainerSize(); i++) {
                    ItemStack stack = oldPlayer.getInventory().getItem(i);
                    if (stack.is(ModItems.GRAVE_KEY.get())) {
                        newPlayer.getInventory().add(stack.copy());
                    }
                }
            }
        }
    }

    private static boolean wasDeathClone(Object event) {
        Object value = invoke(event, "isWasDeath");
        return Boolean.TRUE.equals(value);
    }

    private static String deathMessage(Object source, ServerPlayer player) {
        try {
            Object component = source.getClass()
                    .getMethod("getLocalizedDeathMessage", net.minecraft.world.entity.LivingEntity.class)
                    .invoke(source, player);
            Object text = invoke(component, "getString");
            return text == null ? player.getScoreboardName() + " died" : String.valueOf(text);
        } catch (ReflectiveOperationException | RuntimeException exception) {
            return player.getScoreboardName() + " died";
        }
    }

    private static Object invoke(Object target, String methodName) {
        if (target == null) {
            return null;
        }
        try {
            return target.getClass().getMethod(methodName).invoke(target);
        } catch (ReflectiveOperationException | RuntimeException exception) {
            return null;
        }
    }

    public static UUID createGrave(ServerPlayer player, String deathCause) {
        ServerLevel level = (ServerLevel) player.level();
        BlockPos deathPos = player.blockPosition();
        BlockPos origin = deathPos;
        boolean voidDeath = player.getY() < level.getMinY();
        boolean lavaDeath = player.isInLava();

        if (voidDeath && !RecoveryConfig.CREATE_GRAVE_ON_VOID_DEATH.get()) {
            return null;
        }
        if (lavaDeath && !RecoveryConfig.CREATE_GRAVE_ON_LAVA_DEATH.get()) {
            return null;
        }
        if (voidDeath && RecoveryConfig.VOID_DEATH_MODE.get() == RecoveryConfig.VoidDeathMode.DISABLED) {
            return null;
        }

        if (voidDeath) {
            origin = GraveManager.resolveVoidDeathPosition(player);
        }

        GraveManager.PlacementResult placement = lavaDeath && !RecoveryConfig.LAVA_DEATH_SAFE_PLACEMENT.get()
                ? new GraveManager.PlacementResult(origin, "lava safe placement disabled",
                        List.of("Lava death safe relocation disabled by config."), false)
                : GraveManager.findPlacement(player, level, origin, deathCause);
        BlockPos gravePos = placement.pos();
        if (gravePos.getY() < level.getMinY() || gravePos.getY() > level.getMinY() + level.getHeight()) {
            gravePos = level.getRespawnData().pos();
        }

        boolean fieldCache = EchoRecovery.isAshfallLoaded();
        RecoveryGraveType graveType = RecoveryContent.graveType(fieldCache
                ? ASHFALL_FIELD_RECOVERY_CACHE
                : RecoveryContent.DEFAULT_GRAVE_TYPE);
        BlockState state = (voidDeath ? ModBlocks.VOID_CACHE.get()
                : fieldCache ? ModBlocks.RECOVERY_CACHE.get() : ModBlocks.GRAVE.get()).defaultBlockState();
        level.setBlock(gravePos, state, 3);

        if (!(level.getBlockEntity(gravePos) instanceof GraveBlockEntity grave)) {
            EchoRecovery.LOGGER.error("Failed to create grave block entity at {}", gravePos);
            return null;
        }

        UUID graveId = UUID.randomUUID();
        grave.setGraveId(graveId);
        grave.setOwner(player.getUUID(), player.getScoreboardName());
        grave.setCreatedAt(System.currentTimeMillis());
        grave.setDeathCause(deathCause);
        grave.setDimension(level.dimension().identifier().toString());
        grave.setSourceDimension(level.dimension().identifier().toString());
        grave.setGraveTypeId(graveType.id().toString());
        grave.setContaminated(graveType.contaminated() || fieldCache);
        grave.setTemporaryPlatform(placement.temporaryPlatformCreated());
        List<String> hazardNotes = new ArrayList<>(graveType.hazardNotes());
        hazardNotes.addAll(placement.hazardNotes());
        grave.setHazardNotes(hazardNotes);
        grave.setDeathMessage(deathCause);

        CaptureContext capture = new CaptureContext(level, player, grave, gravePos, deathCause);
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            if (!shouldCaptureSlot(i)) {
                continue;
            }
            ItemStack stack = player.getInventory().getItem(i);
            if (!stack.isEmpty()) {
                int slotIndex = i;
                capture.capture(stack, slotIndex, () -> player.getInventory().setItem(slotIndex, ItemStack.EMPTY));
            }
        }
        if (RecoveryConfig.STORE_XP.get()) {
            grave.setXpStored(capturableExperience(player));
            player.totalExperience = 0;
            player.experienceLevel = 0;
            player.experienceProgress = 0.0F;
        }
        if (!capture.soulbound().isEmpty()) {
            PENDING_SOULBOUND.put(player.getUUID(), capture.soulbound());
        }

        long expiresAt = 0;
        int expirationMinutes = RecoveryConfig.GRAVE_EXPIRATION_MINUTES.get();
        if (expirationMinutes > 0) {
            expiresAt = grave.createdAt() + (expirationMinutes * 60000L);
        }
        grave.setExpiresAt(expiresAt);

        RecoveryWorldData data = RecoveryWorldData.getOrCreate(level);
        RecoveryDataCoreIntegration.recordDeath(player);
        data.addGrave(player.getUUID(), RecoveryWorldData.GraveEntry.fromBlockEntity(grave, deathPos, placement.fallbackReason()));

        if (RecoveryConfig.ENABLE_DEATH_HISTORY.get()) {
            data.addDeathRecord(player.getUUID(), new RecoveryWorldData.DeathRecord(
                player.getUUID(), grave.createdAt(), deathCause,
                level.dimension().identifier().toString(), gravePos, false, false
            ));
        }

        if (RecoveryConfig.GRAVE_KEY_ENABLED.get()) {
            ItemStack key = new ItemStack(ModItems.GRAVE_KEY.get());
            GraveKeyItem.bindToGrave(key, graveId, gravePos, level.dimension().identifier());
            if (!player.getInventory().add(key)) {
                ItemEntity entity = new ItemEntity(level, player.getX(), player.getY(), player.getZ(), key);
                level.addFreshEntity(entity);
            }
        }

        level.playSound(null, gravePos, ModSounds.GRAVE_CREATE.get(), net.minecraft.sounds.SoundSource.BLOCKS, 1.0f, 1.0f);
        RecoveryDataCoreIntegration.recordGraveCreated(player);
        RecoveryIntegrations.graveCreated(player, grave.snapshot());
        RecoveryIntegrationDispatcher.onGraveCreated(player, gravePos, graveId.toString());

        EchoRecovery.LOGGER.info("Created grave {} for {} at {}", graveId, player.getScoreboardName(), gravePos);
        return graveId;
    }

    private static boolean shouldCaptureSlot(int slot) {
        if (slot >= 36 && slot <= 39) {
            return RecoveryConfig.STORE_ARMOR.get();
        }
        if (slot == 40) {
            return RecoveryConfig.STORE_OFFHAND.get();
        }
        return RecoveryConfig.STORE_ITEMS.get();
    }

    public static int capturableExperience(ServerPlayer player) {
        if (player == null) {
            return 0;
        }
        return Math.max(Math.max(0, player.totalExperience), experienceFromLevelProgress(player));
    }

    private static int experienceFromLevelProgress(ServerPlayer player) {
        int base = totalExperienceForLevel(Math.max(0, player.experienceLevel));
        int toNext = player.getXpNeededForNextLevel();
        int progress = Math.round(Math.max(0.0F, Math.min(1.0F, player.experienceProgress)) * toNext);
        return Math.max(0, base + progress);
    }

    private static int totalExperienceForLevel(int level) {
        if (level <= 16) {
            return level * level + 6 * level;
        }
        if (level <= 31) {
            return (int) (2.5D * level * level - 40.5D * level + 360.0D);
        }
        return (int) (4.5D * level * level - 162.5D * level + 2220.0D);
    }

    private static final class CaptureContext {
        private final ServerLevel level;
        private final ServerPlayer player;
        private final GraveBlockEntity grave;
        private final BlockPos gravePos;
        private final String deathCause;
        private final List<ItemStack> soulbound = new ArrayList<>();
        private int graveSlot;

        private CaptureContext(ServerLevel level, ServerPlayer player, GraveBlockEntity grave, BlockPos gravePos, String deathCause) {
            this.level = level;
            this.player = player;
            this.grave = grave;
            this.gravePos = gravePos;
            this.deathCause = deathCause;
        }

        private void capture(ItemStack stack, int originalSlot, Runnable clearSource) {
            RecoveryItemRuleResult result = RecoveryRuleEngine.evaluate(player, stack, deathCause);
            switch (result) {
                case SOULBOUND -> {
                    soulbound.add(stack.copy());
                    clearSource.run();
                }
                case DROP_ON_DEATH -> {
                    drop(stack.copy());
                    clearSource.run();
                }
                case DESTROY_ON_DEATH -> clearSource.run();
                case NO_GRAVE -> {
                    // Leave the stack for vanilla/default death handling.
                }
                case PROTECTED, ALWAYS_GRAVE -> {
                    if (graveSlot < grave.items().size()) {
                        grave.items().set(graveSlot, stack.copy());
                        grave.setOriginalSlot(graveSlot, originalSlot);
                        graveSlot++;
                        clearSource.run();
                    } else if (RecoveryConfig.DROP_OVERFLOW_ITEMS.get()) {
                        drop(stack.copy());
                        clearSource.run();
                    }
                }
            }
        }

        private void drop(ItemStack stack) {
            ItemEntity entity = new ItemEntity(level, gravePos.getX() + 0.5D, gravePos.getY() + 0.5D, gravePos.getZ() + 0.5D, stack);
            level.addFreshEntity(entity);
        }

        private List<ItemStack> soulbound() {
            return List.copyOf(soulbound);
        }
    }
}
