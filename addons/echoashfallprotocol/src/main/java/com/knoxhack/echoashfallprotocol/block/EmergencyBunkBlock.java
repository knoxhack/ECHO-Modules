package com.knoxhack.echoashfallprotocol.block;

import com.knoxhack.echoashfallprotocol.event.AshfallAdapterCoreEarlyEventRuntime;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.CollisionGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.storage.LevelData;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

import java.util.Map;
import java.util.Optional;

public class EmergencyBunkBlock extends Block {
    public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final EnumProperty<BedPart> PART = BlockStateProperties.BED_PART;
    public static final BooleanProperty OCCUPIED = BlockStateProperties.OCCUPIED;
    private static final VoxelShape SHAPE = Block.box(0.0D, 0.0D, 0.0D, 16.0D, 9.0D, 16.0D);

    public EmergencyBunkBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any()
                .setValue(FACING, Direction.SOUTH)
                .setValue(PART, BedPart.FOOT)
                .setValue(OCCUPIED, false));
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction facing = context.getHorizontalDirection();
        BlockPos headPos = context.getClickedPos().relative(facing);
        if (!context.getLevel().getWorldBorder().isWithinBounds(headPos)
                || !context.getLevel().getBlockState(headPos).canBeReplaced(context)) {
            return null;
        }
        return defaultBlockState()
                .setValue(FACING, facing)
                .setValue(PART, BedPart.FOOT)
                .setValue(OCCUPIED, false);
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, net.minecraft.world.item.ItemStack stack) {
        if (!level.isClientSide()) {
            BlockPos headPos = pos.relative(state.getValue(FACING));
            level.setBlock(headPos, state.setValue(PART, BedPart.HEAD), 3);
            level.updateNeighborsAt(pos, Blocks.AIR);
            state.updateNeighbourShapes(level, pos, 3);
        }
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        if (player instanceof ServerPlayer serverPlayer) {
            return useEmergencyBunk(state, level, pos, serverPlayer, "emergency_bunk_use");
        }
        return InteractionResult.SUCCESS_SERVER;
    }

    public static InteractionResult useEmergencyBunk(
            BlockState state,
            Level level,
            BlockPos pos,
            ServerPlayer serverPlayer,
            String source) {
        BlockPos headPos = headPos(state, pos);
        BlockState headState = level.getBlockState(headPos);
        if (!isCompleteBunk(level, headPos, headState)) {
            serverPlayer.sendSystemMessage(Component.literal("\u00A7b[ECHO-7]\u00A7r Emergency bunk frame is incomplete."));
            return InteractionResult.CONSUME;
        }

        if (resolveRespawnPosition(headState, EntityType.PLAYER, level, headPos, serverPlayer.getYRot()).isEmpty()) {
            serverPlayer.sendSystemMessage(Component.literal("\u00A7b[ECHO-7]\u00A7r Emergency bunk needs clear stand-up space before it can anchor recovery."));
            return InteractionResult.CONSUME;
        }

        anchorRespawnAndRecordShelter(serverPlayer, level, headPos, source == null || source.isBlank()
                ? "emergency_bunk_use"
                : source);
        markEmergencyBunkRested(level, headPos, headState, serverPlayer);
        serverPlayer.sendSystemMessage(Component.literal("\u00A7b[ECHO-7]\u00A7r Emergency bunk registered as your fallback point."));
        return InteractionResult.SUCCESS_SERVER;
    }

    @Override
    protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos,
                                     Direction directionToNeighbour, BlockPos neighbourPos, BlockState neighbourState,
                                     RandomSource random) {
        if (directionToNeighbour == neighbourDirection(state)) {
            return isMatchingCounterpart(state, neighbourState)
                    ? state.setValue(OCCUPIED, neighbourState.getValue(OCCUPIED))
                    : Blocks.AIR.defaultBlockState();
        }
        return super.updateShape(state, level, ticks, pos, directionToNeighbour, neighbourPos, neighbourState, random);
    }

    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (!level.isClientSide() && player.preventsBlockDrops()) {
            BlockPos otherPos = pos.relative(neighbourDirection(state));
            BlockState otherState = level.getBlockState(otherPos);
            if (isMatchingCounterpart(state, otherState)) {
                level.setBlock(otherPos, Blocks.AIR.defaultBlockState(), 35);
                level.levelEvent(player, 2001, otherPos, Block.getId(otherState));
            }
        }
        return super.playerWillDestroy(level, pos, state, player);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    public boolean isBed(BlockState state, BlockGetter level, BlockPos pos, LivingEntity sleeper) {
        return level instanceof LevelReader levelReader && isCompleteBunk(levelReader, pos, state);
    }

    @Override
    public Optional<ServerPlayer.RespawnPosAngle> getRespawnPosition(BlockState state, EntityType<?> type,
                                                                     LevelReader levelReader, BlockPos pos,
                                                                     float orientation) {
        return resolveRespawnPosition(state, type, levelReader, pos, orientation);
    }

    @Override
    public void setBedOccupied(BlockState state, Level level, BlockPos pos, LivingEntity sleeper, boolean occupied) {
        setOccupiedIfPresent(level, pos, state, occupied);
        BlockPos otherPos = pos.relative(neighbourDirection(state));
        BlockState otherState = level.getBlockState(otherPos);
        if (isMatchingCounterpart(state, otherState)) {
            setOccupiedIfPresent(level, otherPos, otherState, occupied);
        }
    }

    @Override
    public Direction getBedDirection(BlockState state, LevelReader level, BlockPos pos) {
        return state.getValue(FACING);
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, PART, OCCUPIED);
    }

    public static BlockPos headPos(BlockState state, BlockPos pos) {
        return state.getValue(PART) == BedPart.HEAD ? pos : pos.relative(state.getValue(FACING));
    }

    public static BlockPos footPos(BlockState state, BlockPos pos) {
        return state.getValue(PART) == BedPart.FOOT ? pos : pos.relative(state.getValue(FACING).getOpposite());
    }

    public static Optional<ServerPlayer.RespawnPosAngle> resolveRespawnPosition(BlockState state, EntityType<?> type,
                                                                               LevelReader levelReader, BlockPos pos,
                                                                               float orientation) {
        if (!(levelReader instanceof CollisionGetter collisionGetter) || !(state.getBlock() instanceof EmergencyBunkBlock)) {
            return Optional.empty();
        }

        BlockPos headPos = headPos(state, pos);
        BlockState headState = levelReader.getBlockState(headPos);
        if (!isCompleteBunk(levelReader, headPos, headState)) {
            return Optional.empty();
        }

        Direction facing = headState.getValue(FACING);
        return BedBlock.findStandUpPosition(type, collisionGetter, headPos, facing, orientation)
                .map(position -> ServerPlayer.RespawnPosAngle.of(position, headPos, 0.0F));
    }

    public static boolean isCompleteBunk(LevelReader level, BlockPos pos, BlockState state) {
        if (!(state.getBlock() instanceof EmergencyBunkBlock)) {
            return false;
        }
        BlockPos headPos = headPos(state, pos);
        BlockState headState = level.getBlockState(headPos);
        if (!(headState.getBlock() instanceof EmergencyBunkBlock)
                || headState.getValue(PART) != BedPart.HEAD) {
            return false;
        }
        BlockPos footPos = footPos(headState, headPos);
        if (!footPos.equals(footPos(state, pos))) {
            return false;
        }
        BlockState footState = level.getBlockState(footPos);
        return footState.getBlock() instanceof EmergencyBunkBlock
                && footState.getValue(PART) == BedPart.FOOT
                && footState.getValue(FACING) == headState.getValue(FACING);
    }

    private static void setOccupiedIfPresent(Level level, BlockPos pos, BlockState state, boolean occupied) {
        if (state.getBlock() instanceof EmergencyBunkBlock && state.getValue(OCCUPIED) != occupied) {
            level.setBlock(pos, state.setValue(OCCUPIED, occupied), 3);
        }
    }

    private static Direction neighbourDirection(BlockState state) {
        return state.getValue(PART) == BedPart.FOOT ? state.getValue(FACING) : state.getValue(FACING).getOpposite();
    }

    private static boolean isMatchingCounterpart(BlockState state, BlockState otherState) {
        return otherState.getBlock() instanceof EmergencyBunkBlock
                && otherState.getValue(PART) != state.getValue(PART)
                && otherState.getValue(FACING) == state.getValue(FACING);
    }

    private static boolean isRespawnConfigAnchoredAt(ServerPlayer.RespawnConfig config, Level level, BlockPos headPos) {
        return config != null
                && !config.forced()
                && config.respawnData().dimension().equals(level.dimension())
                && config.respawnData().pos().equals(headPos);
    }

    private static void anchorRespawnAndRecordShelter(ServerPlayer player, Level level, BlockPos headPos, String source) {
        if (!isRespawnConfigAnchoredAt(player.getRespawnConfig(), level, headPos)) {
            player.setRespawnPosition(
                    new ServerPlayer.RespawnConfig(
                            LevelData.RespawnData.of(level.dimension(), headPos, player.getYRot(), player.getXRot()),
                            false),
                    false);
        }
        AshfallAdapterCoreEarlyEventRuntime.specialMarker(
                player,
                "shelter:slept",
                Map.of("source", source));
    }

    private static void markEmergencyBunkRested(Level level, BlockPos headPos, BlockState headState, ServerPlayer player) {
        setOccupiedIfPresent(level, headPos, headState, true);
        BlockPos footPos = footPos(headState, headPos);
        BlockState footState = level.getBlockState(footPos);
        if (isMatchingCounterpart(headState, footState)) {
            setOccupiedIfPresent(level, footPos, footState, true);
        }
        player.awardStat(net.minecraft.stats.Stats.SLEEP_IN_BED);
        player.sendOverlayMessage(Component.literal("Emergency bunk anchored recovery point."));
    }
}
