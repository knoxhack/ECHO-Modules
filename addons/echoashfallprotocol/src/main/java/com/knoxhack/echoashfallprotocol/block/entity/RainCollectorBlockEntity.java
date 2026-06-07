package com.knoxhack.echoashfallprotocol.block.entity;

import com.knoxhack.echoashfallprotocol.echo.QuestData;
import com.knoxhack.echoashfallprotocol.event.AshfallAdapterCoreEarlyEventRuntime;
import com.knoxhack.echoashfallprotocol.event.EnvironmentalEventHandler;
import com.knoxhack.echoashfallprotocol.registry.ModBlockEntities;
import com.knoxhack.echoashfallprotocol.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

public class RainCollectorBlockEntity extends BlockEntity {
    public static final int CAPACITY_BOTTLES = 4;
    private static final int TICKS_PER_BOTTLE = 600;

    private int storedBottles;
    private int collectionProgress;

    public RainCollectorBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.RAIN_COLLECTOR.get(), pos, state);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, RainCollectorBlockEntity entity) {
        if (entity.storedBottles >= CAPACITY_BOTTLES) {
            entity.collectionProgress = 0;
            return;
        }

        if (!canCollect(level, pos)) {
            return;
        }

        entity.collectionProgress++;
        if (entity.collectionProgress >= TICKS_PER_BOTTLE) {
            entity.collectionProgress = 0;
            entity.storedBottles++;
            entity.setChanged();
            level.sendBlockUpdated(pos, state, state, 3);
        } else if (entity.collectionProgress % 100 == 0) {
            entity.setChanged();
        }
    }

    public static boolean canCollect(Level level, BlockPos pos) {
        BlockPos skyPos = pos.above();
        return level.canSeeSky(skyPos)
                && (level.isRainingAt(skyPos) || EnvironmentalEventHandler.isStormRainAt(level, skyPos));
    }

    public boolean fillBottle(Level level, ServerPlayer player, InteractionHand hand) {
        if (storedBottles <= 0) {
            player.sendSystemMessage(Component.translatable("message.EchoAshfallProtocol.rain_collector.empty"));
            return false;
        }

        ItemStack held = player.getItemInHand(hand);
        if (!held.is(Items.GLASS_BOTTLE)) {
            return false;
        }

        ItemStack dirtyWater = new ItemStack(ModItems.DIRTY_WATER_BOTTLE.get());
        ItemStack result = ItemUtils.createFilledResult(held, player, dirtyWater);
        player.setItemInHand(hand, result);
        storedBottles--;
        setChanged();
        level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);

        QuestData quest = QuestData.get(player);
        quest.visitLocation("special", "rain:collected");
        QuestData.saveAndSync(player, quest);
        AshfallAdapterCoreEarlyEventRuntime.dirtyWaterCollected(player, worldPosition);

        level.playSound(null, worldPosition, SoundEvents.BOTTLE_FILL, SoundSource.BLOCKS, 0.7F, 1.1F);
        player.sendSystemMessage(Component.translatable("message.EchoAshfallProtocol.rain_collector.filled"));
        return true;
    }

    public int getStoredBottles() {
        return storedBottles;
    }

    public int getCollectionPercent() {
        return Math.min(99, collectionProgress * 100 / TICKS_PER_BOTTLE);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putInt("storedBottles", storedBottles);
        output.putInt("collectionProgress", collectionProgress);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        storedBottles = Math.max(0, Math.min(CAPACITY_BOTTLES, input.getIntOr("storedBottles", 0)));
        collectionProgress = Math.max(0, Math.min(TICKS_PER_BOTTLE - 1, input.getIntOr("collectionProgress", 0)));
    }
}
