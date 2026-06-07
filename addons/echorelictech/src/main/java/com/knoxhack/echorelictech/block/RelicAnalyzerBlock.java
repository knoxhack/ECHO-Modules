package com.knoxhack.echorelictech.block;

import com.knoxhack.echorelictech.api.RelicTechApi;
import com.knoxhack.echorelictech.api.event.RelicTechEvents;
import com.knoxhack.echorelictech.api.relic.UnidentifiedRelicData;
import com.knoxhack.echorelictech.block.entity.RelicAnalyzerBlockEntity;
import com.knoxhack.echorelictech.data.RelicDefinitionLoader;
import com.knoxhack.echorelictech.registry.ModDataComponents;
import com.knoxhack.echorelictech.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

import java.util.ArrayList;
import java.util.List;

public class RelicAnalyzerBlock extends Block implements EntityBlock {
    public RelicAnalyzerBlock(Properties props) {
        super(props);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level.isClientSide()) return InteractionResult.SUCCESS;
        if (level.getBlockEntity(pos) instanceof RelicAnalyzerBlockEntity be) {
            if (be.hasOutput()) {
                ItemStack out = be.takeOutput();
                if (!player.getInventory().add(out)) {
                    player.drop(out, false);
                }
                player.sendSystemMessage(Component.translatable("block.echorelictech.relic_analyzer.take_output"));
                return InteractionResult.SUCCESS;
            }
            if (!be.getInput().isEmpty()) {
                ItemStack in = be.getInput();
                be.setInput(ItemStack.EMPTY);
                if (!player.getInventory().add(in)) {
                    player.drop(in, false);
                }
                player.sendSystemMessage(Component.translatable("block.echorelictech.relic_analyzer.take_input"));
                return InteractionResult.SUCCESS;
            }
        }
        player.sendSystemMessage(Component.translatable("block.echorelictech.relic_analyzer.insert_relic"));
        return InteractionResult.SUCCESS;
    }

    @Override
    public InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, net.minecraft.world.InteractionHand hand, BlockHitResult hit) {
        if (level.isClientSide()) return InteractionResult.SUCCESS;
        if (!(player instanceof net.minecraft.server.level.ServerPlayer serverPlayer)) return InteractionResult.SUCCESS;
        if (level.getBlockEntity(pos) instanceof RelicAnalyzerBlockEntity be) {
            if (be.hasOutput()) {
                player.sendSystemMessage(Component.translatable("block.echorelictech.relic_analyzer.has_output"));
                return InteractionResult.SUCCESS;
            }
            if (stack.is(ModItems.UNIDENTIFIED_RELIC.get())) {
                ItemStack input = stack.split(1);
                be.setInput(input);
                // Process immediately for beta (could be timed)
                ItemStack result = resolveUnidentified(input);
                if (result != null) {
                    be.setInput(ItemStack.EMPTY);
                    be.setOutput(result);
                    RelicTechEvents.fireAnalyze(serverPlayer, input, result);
                    player.sendSystemMessage(Component.translatable("block.echorelictech.relic_analyzer.analysis_complete"));
                } else {
                    be.setInput(input); // give it back
                    player.sendSystemMessage(Component.translatable("block.echorelictech.relic_analyzer.analysis_failed"));
                }
                return InteractionResult.SUCCESS;
            }
            if (!be.getInput().isEmpty()) {
                player.sendSystemMessage(Component.translatable("block.echorelictech.relic_analyzer.busy"));
                return InteractionResult.SUCCESS;
            }
        }
        return InteractionResult.PASS;
    }

    private ItemStack resolveUnidentified(ItemStack unidentified) {
        var hidden = unidentified.get(ModDataComponents.UNIDENTIFIED_RELIC_DATA.get());
        Identifier targetId;
        if (hidden != null && hidden.targetRelicId() != null) {
            targetId = hidden.targetRelicId();
        } else {
            targetId = pickRandomRelicId();
        }
        if (targetId == null) return null;
        ItemStack result = RelicTechApi.createRelicStack(targetId);
        if (result.isEmpty()) return null;
        var data = result.get(ModDataComponents.RELIC_DATA.get());
        if (data != null) {
            result.set(ModDataComponents.RELIC_DATA.get(), data.makeIdentified());
        }
        return result;
    }

    private Identifier pickRandomRelicId() {
        var defs = RelicDefinitionLoader.all();
        List<Identifier> enabled = new ArrayList<>();
        for (var entry : defs.entrySet()) {
            if (entry.getValue().enabled()) {
                enabled.add(entry.getValue().id());
            }
        }
        if (enabled.isEmpty()) return Identifier.fromNamespaceAndPath("echorelictech", "phase_anchor");
        return enabled.get((int)(Math.random() * enabled.size()));
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new RelicAnalyzerBlockEntity(pos, state);
    }
}
