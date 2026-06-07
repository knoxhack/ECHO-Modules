package com.knoxhack.echomultiblockcore.block.entity;

import com.knoxhack.echomultiblockcore.api.EchoRoboticComponent;
import com.knoxhack.echomultiblockcore.api.MultiblockAutomationRecipe;
import com.knoxhack.echomultiblockcore.api.RobotState;
import com.knoxhack.echomultiblockcore.api.RobotToolType;
import com.knoxhack.echomultiblockcore.integration.MultiblockMissionHooks;
import com.knoxhack.echomultiblockcore.item.ToolHeadItem;
import com.knoxhack.echomultiblockcore.registry.ModBlockEntities;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

public class RoboticArmBlockEntity extends BlockEntity implements EchoRoboticComponent {
    private ItemStack installedTool = ItemStack.EMPTY;
    private RobotState state = RobotState.IDLE;
    private Identifier currentTaskId;
    private int heat;
    private int cooldown;
    private BlockPos targetPos = BlockPos.ZERO;
    private long lastAnimationTick;

    public RoboticArmBlockEntity(BlockPos pos, BlockState blockState) {
        this(ModBlockEntities.ROBOTIC_ARM.get(), pos, blockState);
    }

    protected RoboticArmBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState blockState) {
        super(type, pos, blockState);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, RoboticArmBlockEntity arm) {
        if (level.isClientSide()) {
            return;
        }
        if (arm.cooldown > 0) {
            arm.cooldown--;
        }
        if (arm.heat > 0 && (level.getGameTime() % 10L == 0L || arm.state == RobotState.COOLING)) {
            arm.heat = Math.max(0, arm.heat - (arm.state == RobotState.COOLING ? 4 : 1));
        }
        if (arm.state == RobotState.COOLING && arm.heat < 40) {
            arm.state = RobotState.IDLE;
        }
        arm.setChanged();
    }

    public ItemStack installedTool() {
        return installedTool.copy();
    }

    public boolean installTool(ItemStack stack, Player player) {
        if (stack == null || stack.isEmpty() || !(stack.getItem() instanceof ToolHeadItem)) {
            return false;
        }
        if (!installedTool.isEmpty() || state == RobotState.WORKING || state == RobotState.MOVING) {
            if (player != null) {
                player.sendSystemMessage(Component.translatable("message.echomultiblockcore.robot.tool_busy"));
            }
            return false;
        }
        installedTool = stack.copyWithCount(1);
        setChanged();
        if (player != null) {
            player.sendSystemMessage(Component.translatable("message.echomultiblockcore.robot.tool_installed", toolLabel()));
            MultiblockMissionHooks.recordRobotToolInstalled(player, toolLabel());
        }
        return true;
    }

    public ItemStack removeTool(Player player) {
        if (installedTool.isEmpty() || state == RobotState.WORKING || state == RobotState.MOVING) {
            if (player != null) {
                player.sendSystemMessage(Component.translatable("message.echomultiblockcore.robot.no_tool_removed"));
            }
            return ItemStack.EMPTY;
        }
        ItemStack removed = installedTool.copy();
        installedTool = ItemStack.EMPTY;
        setChanged();
        if (player != null) {
            player.sendSystemMessage(Component.translatable("message.echomultiblockcore.robot.tool_removed"));
        }
        return removed;
    }

    public void addHeat(int value) {
        heat = Math.min(getMaxHeat(), heat + Math.max(0, value));
        if (heat >= getMaxHeat()) {
            state = RobotState.COOLING;
            cooldown = 80;
        }
        setChanged();
    }

    public boolean isAvailable() {
        return !installedTool.isEmpty() && state != RobotState.COOLING && state != RobotState.JAMMED
                && state != RobotState.DAMAGED && state != RobotState.OFFLINE;
    }

    public Component statusComponent() {
        return Component.translatable("message.echomultiblockcore.robot.status",
                state.name(), toolLabel(), getReach(), Math.round((heat / (float) getMaxHeat()) * 100.0F),
                currentTaskId == null ? "Idle" : currentTaskId.toString());
    }

    private String toolLabel() {
        if (installedTool.getItem() instanceof ToolHeadItem head) {
            return head.toolType().name();
        }
        return "None";
    }

    @Override
    public Identifier getRobotId() {
        return Identifier.fromNamespaceAndPath("echomultiblockcore", "robotic_arm/" + Long.toUnsignedString(getBlockPos().asLong()));
    }

    @Override
    public Identifier getRobotRuntimeId(BlockPos controllerPos) {
        String controller = controllerPos == null ? "unlinked" : Long.toUnsignedString(controllerPos.asLong());
        return Identifier.fromNamespaceAndPath("echomultiblockcore",
                "controller/" + controller + "/robotic_arm/" + Long.toUnsignedString(getBlockPos().asLong()));
    }

    @Override
    public RobotState getRobotState() {
        return state;
    }

    @Override
    public List<RobotToolType> getInstalledTools() {
        return installedTool.getItem() instanceof ToolHeadItem head ? List.of(head.toolType()) : List.of();
    }

    @Override
    public boolean canPerform(MultiblockAutomationRecipe recipe) {
        return recipe != null && isAvailable()
                && (recipe.requiredTools().isEmpty() || getInstalledTools().stream().anyMatch(recipe.requiredTools()::contains));
    }

    @Override
    public void assignTask(MultiblockAutomationRecipe recipe) {
        currentTaskId = recipe == null ? null : recipe.id();
        state = recipe == null ? RobotState.IDLE : RobotState.WORKING;
        setChanged();
    }

    @Override
    public void clearTask() {
        currentTaskId = null;
        if (state == RobotState.WORKING || state == RobotState.MOVING) {
            state = heat >= 80 ? RobotState.COOLING : RobotState.IDLE;
        }
        setChanged();
    }

    @Override
    public int getReach() {
        return 5;
    }

    @Override
    public int getPrecision() {
        return getInstalledTools().contains(RobotToolType.SCANNER) ? 3 : 1;
    }

    @Override
    public int getStrength() {
        return getInstalledTools().contains(RobotToolType.WELDER) || getInstalledTools().contains(RobotToolType.ASSEMBLER) ? 3 : 1;
    }

    @Override
    public int getHeat() {
        return heat;
    }

    @Override
    public int getMaxHeat() {
        return 100;
    }

    public BlockPos targetPos() {
        return targetPos;
    }

    public void setTargetPos(BlockPos targetPos) {
        this.targetPos = targetPos == null ? BlockPos.ZERO : targetPos.immutable();
        this.lastAnimationTick = level == null ? 0L : level.getGameTime();
        setChanged();
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        installedTool = input.read("installed_tool", ItemStack.CODEC).orElse(ItemStack.EMPTY);
        state = RobotState.values()[Math.max(0, Math.min(RobotState.values().length - 1, input.getIntOr("robot_state", RobotState.IDLE.ordinal())))];
        currentTaskId = Identifier.tryParse(input.getStringOr("current_task", ""));
        heat = input.getIntOr("heat", 0);
        cooldown = input.getIntOr("cooldown", 0);
        targetPos = BlockPos.of(input.getLongOr("target_pos", 0L));
        lastAnimationTick = input.getLongOr("last_animation_tick", 0L);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        if (!installedTool.isEmpty()) {
            output.store("installed_tool", ItemStack.CODEC, installedTool);
        }
        output.putInt("robot_state", state.ordinal());
        output.putString("current_task", currentTaskId == null ? "" : currentTaskId.toString());
        output.putInt("heat", heat);
        output.putInt("cooldown", cooldown);
        output.putLong("target_pos", targetPos.asLong());
        output.putLong("last_animation_tick", lastAnimationTick);
    }
}
