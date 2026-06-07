package com.knoxhack.echoashfallprotocol.block.entity;

import com.knoxhack.echoashfallprotocol.block.PowerNodeBlock;
import com.knoxhack.echoashfallprotocol.registry.ModBlockEntities;
import com.knoxhack.echoashfallprotocol.registry.ModBlocks;
import com.knoxhack.echoashfallprotocol.world.NexusWorldData;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import com.knoxhack.echo.adaptercore.EchoValueIOSerializable;

/**
 * Nexus Core Block Entity — tracks endgame choice and power grid sufficiency.
 * Scans surrounding world for activated Power Nodes to determine if the
 * Core interface can be unlocked.
 */
public class NexusCoreBlockEntity extends BlockEntity implements EchoValueIOSerializable {

    public enum NexusChoice { NONE, RESTORE, DESTROY, CONTROL }

    private NexusChoice choice = NexusChoice.NONE;
    private boolean discovered = false;

    public NexusCoreBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.NEXUS_CORE.get(), pos, state);
    }

    /**
     * Count activated Power Nodes within 256 blocks of the Nexus Core.
     */
    public int getActivatedNodeCount(Level level, BlockPos corePos) {
        if (level instanceof ServerLevel serverLevel) {
            int savedCount = NexusWorldData.get(serverLevel).countActiveNodes(serverLevel, corePos, 256);
            if (savedCount > 0) {
                return savedCount;
            }
        }

        int count = 0;
        int radius = 256;
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (int x = -radius; x <= radius; x += 16) {
            for (int z = -radius; z <= radius; z += 16) {
                for (int y = -64; y <= 320; y += 8) {
                    cursor.set(corePos.getX() + x, y, corePos.getZ() + z);
                    BlockState state = level.getBlockState(cursor);
                    if (state.is(ModBlocks.POWER_NODE.get()) && state.getValue(PowerNodeBlock.ACTIVE)) {
                        count++;
                    }
                }
            }
        }
        return count;
    }

    public boolean hasChoiceBeenMade() {
        return choice != NexusChoice.NONE;
    }

    public NexusChoice getChoice() {
        return choice;
    }

    public void makeChoice(NexusChoice choice) {
        this.choice = choice;
        setChanged();
    }

    public boolean isDiscovered() {
        return discovered;
    }

    public void setDiscovered() {
        this.discovered = true;
        setChanged();
    }

    @Override
    public void serialize(ValueOutput output) {
        output.putString("choice", choice.name());
        output.putBoolean("discovered", discovered);
    }

    @Override
    public void deserialize(ValueInput input) {
        String choiceName = input.getStringOr("choice", NexusChoice.NONE.name());
        try {
            choice = NexusChoice.valueOf(choiceName);
        } catch (IllegalArgumentException e) {
            choice = NexusChoice.NONE;
        }
        discovered = input.getBooleanOr("discovered", false);
    }
}
