package com.knoxhack.signalos.item;

import com.knoxhack.signalos.api.SignalOsDriveData;
import com.knoxhack.signalos.registry.ModDataComponents;
import java.util.function.Consumer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;

public class SignalOsDataDriveItem extends Item {
    public SignalOsDataDriveItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        SignalOsDriveData data = data(stack);
        if (stack.get(ModDataComponents.DRIVE_DATA.get()) == null) {
            stack.set(ModDataComponents.DRIVE_DATA.get(), SignalOsDriveData.blankV2());
            data = SignalOsDriveData.blankV2();
        }
        player.sendSystemMessage(Component.literal("SignalOS drive '" + data.label() + "' contains "
                + data.records().size() + " record(s), schema V" + data.schemaVersion() + ". Install it in a terminal"
                + " as a boot drive or in a Server Rack to expose records."));
        return InteractionResult.SUCCESS_SERVER;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display,
            Consumer<Component> tooltip, TooltipFlag flag) {
        SignalOsDriveData data = data(stack);
        tooltip.accept(Component.literal(data.label()));
        tooltip.accept(Component.literal(data.isV2Supported() ? "SignalOS Drive V2" : "Legacy SignalOS Drive"));
        tooltip.accept(Component.literal(data.records().size() + " SignalOS record(s)"));
    }

    public static SignalOsDriveData data(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return SignalOsDriveData.EMPTY;
        }
        SignalOsDriveData data = stack.get(ModDataComponents.DRIVE_DATA.get());
        return data == null && stack.getItem() instanceof SignalOsDataDriveItem ? SignalOsDriveData.blankV2()
                : data == null ? SignalOsDriveData.EMPTY : data;
    }

    public static boolean canInstall(ItemStack stack) {
        if (stack == null || stack.isEmpty() || !(stack.getItem() instanceof SignalOsDataDriveItem)) {
            return false;
        }
        SignalOsDriveData data = stack.get(ModDataComponents.DRIVE_DATA.get());
        return data == null || data.isV2Supported();
    }

    public static boolean ensureV2Data(ItemStack stack) {
        if (stack == null || stack.isEmpty() || !(stack.getItem() instanceof SignalOsDataDriveItem)) {
            return false;
        }
        SignalOsDriveData data = stack.get(ModDataComponents.DRIVE_DATA.get());
        if (data == null) {
            stack.set(ModDataComponents.DRIVE_DATA.get(), SignalOsDriveData.blankV2());
            return true;
        }
        return data.isV2Supported();
    }
}
