package com.knoxhack.echorecovery.block;

import com.knoxhack.echorecovery.block.entity.GraveBlockEntity;
import com.knoxhack.echorecovery.config.RecoveryConfig;
import com.knoxhack.echorecovery.api.RecoveryIntegrations;
import com.knoxhack.echorecovery.grave.GraveAccessResult;
import com.knoxhack.echorecovery.grave.GraveManager;
import com.knoxhack.echorecovery.integration.RecoveryDataCoreIntegration;
import com.knoxhack.echorecovery.registry.ModSounds;
import com.mojang.serialization.MapCodec;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.Permissions;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

public class GraveBlock extends BaseEntityBlock {
    public static final MapCodec<GraveBlock> CODEC = simpleCodec(p -> new GraveBlock(GraveVariant.GRAVE, p));
    private static final VoxelShape SHAPE = Block.box(2.0, 0.0, 2.0, 14.0, 14.0, 14.0);
    private final GraveVariant variant;

    public GraveBlock(GraveVariant variant, Properties properties) {
        super(properties);
        this.variant = variant;
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    public GraveVariant variant() {
        return variant;
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new GraveBlockEntity(pos, state);
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        if (level.getBlockEntity(pos) instanceof GraveBlockEntity grave && placer instanceof Player player) {
            grave.setOwner(player.getUUID(), player.getScoreboardName());
        }
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hitResult) {
        if (!(level.getBlockEntity(pos) instanceof GraveBlockEntity grave)) {
            return InteractionResult.PASS;
        }
        boolean admin = player instanceof ServerPlayer serverPlayer
            && serverPlayer.createCommandSourceStack().permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER)
            && RecoveryConfig.ADMIN_BYPASS.get();
        GraveAccessResult result = GraveManager.accessGrave(grave, player, admin);
        if (result != GraveAccessResult.ALLOWED) {
            if (!level.isClientSide()) {
                UUID keyId = com.knoxhack.echorecovery.item.GraveKeyItem.getGraveId(stack);
                boolean wrongKey = keyId != null && !keyId.equals(grave.graveId());
                player.sendSystemMessage(Component.translatable(wrongKey
                    ? "commands.echorecovery.graves.failure.wrong_key"
                    : "commands.echorecovery.graves.failure.access_denied"));
            }
            return InteractionResult.CONSUME;
        }
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        level.playSound(null, pos, ModSounds.GRAVE_OPEN.get(), net.minecraft.sounds.SoundSource.BLOCKS, 1.0f, 1.0f);
        grave.syncToWorldData();
        MenuProvider menuProvider = new SimpleMenuProvider(
            (containerId, playerInventory, p) -> new com.knoxhack.echorecovery.menu.GraveMenu(
                containerId, playerInventory, grave),
            grave.getDisplayName());
        if (player instanceof ServerPlayer serverPlayer) {
            if (variant == GraveVariant.RECOVERY_CACHE) {
                dispatchAshfallMachineUse(serverPlayer, level, pos, "echorecovery:recovery_cache");
            }
            RecoveryDataCoreIntegration.recordGraveOpened(serverPlayer);
            RecoveryIntegrations.graveOpened(serverPlayer, grave.snapshot());
            serverPlayer.openMenu(menuProvider, buffer -> buffer.writeBlockPos(pos));
        }
        return InteractionResult.CONSUME;
    }

    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (!level.isClientSide() && RecoveryConfig.ALLOW_GRAVE_BREAKING.get()) {
            if (level.getBlockEntity(pos) instanceof GraveBlockEntity grave) {
                UUID playerId = player.getUUID();
                boolean admin = player instanceof ServerPlayer serverPlayer
                    && serverPlayer.createCommandSourceStack().permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER)
                    && RecoveryConfig.ADMIN_BYPASS.get();
                if (GraveManager.canBreak(grave, playerId, admin)) {
                    GraveManager.dropGraveContents(grave, level, pos);
                }
            }
        }
        return super.playerWillDestroy(level, pos, state, player);
    }

    public void onExploded(BlockState state, Level level, BlockPos pos, Explosion explosion) {
        if (RecoveryConfig.EXPLOSION_PROOF.get()) {
            return;
        }
        if (level.getBlockEntity(pos) instanceof GraveBlockEntity grave) {
            GraveManager.dropGraveContents(grave, level, pos);
        }
    }

    public void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
        if (level.isClientSide()) {
            return;
        }
        if (entity instanceof ItemEntity) {
            entity.setDeltaMovement(0, 0, 0);
        }
    }

    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (!state.is(newState.getBlock()) && level.getBlockEntity(pos) instanceof GraveBlockEntity grave) {
            if (grave.isRecovered() || RecoveryConfig.DELETE_EMPTY_GRAVES.get() && grave.isCompletelyEmpty()) {
                // no drops
            } else {
                GraveManager.dropGraveContents(grave, level, pos);
            }
        }
    }

    @Override
    public boolean dropFromExplosion(Explosion explosion) {
        return !RecoveryConfig.EXPLOSION_PROOF.get();
    }

    private static void dispatchAshfallMachineUse(ServerPlayer player, Level level, BlockPos pos, String machineId) {
        try {
            Class<?> runtime = Class.forName(
                    "com.knoxhack.echoashfallprotocol.nativebridge.AshfallAdapterCoreMachineRuntimeHost");
            runtime.getMethod("dispatchUseBlock", ServerPlayer.class, Level.class, BlockPos.class, String.class)
                    .invoke(null, player, level, pos, machineId);
        } catch (ReflectiveOperationException | LinkageError ignored) {
            // Recovery remains standalone when Ashfall's AdapterCore machine host is absent.
        }
    }

    public enum GraveVariant {
        GRAVE, DEATH_CACHE, RECOVERY_CACHE, SOUL_URN, VOID_CACHE
    }
}
