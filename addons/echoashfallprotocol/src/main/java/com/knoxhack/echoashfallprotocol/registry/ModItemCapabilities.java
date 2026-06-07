package com.knoxhack.echoashfallprotocol.registry;

import com.knoxhack.echo.adaptercore.EchoBackendItemBridge;
import com.knoxhack.echoashfallprotocol.block.entity.HopperHandler;
import net.minecraft.world.level.block.entity.BlockEntity;

public final class ModItemCapabilities {
    private ModItemCapabilities() {
    }

    public static void register(Object event) {
        EchoBackendItemBridge.registerBlockEntityInventory(event, ModBlockEntities.HAND_RECYCLER.get(),
                (be, side) -> inventoryHandler(be));
        EchoBackendItemBridge.registerBlockEntityInventory(event, ModBlockEntities.WATER_PURIFIER.get(),
                (be, side) -> inventoryHandler(be));
        EchoBackendItemBridge.registerBlockEntityInventory(event, ModBlockEntities.FILTER_WORKBENCH.get(),
                (be, side) -> inventoryHandler(be));
        EchoBackendItemBridge.registerBlockEntityInventory(event, ModBlockEntities.SCRAP_PRESS.get(),
                (be, side) -> inventoryHandler(be));
        EchoBackendItemBridge.registerBlockEntityInventory(event, ModBlockEntities.ORE_GRINDER.get(),
                (be, side) -> inventoryHandler(be));
        EchoBackendItemBridge.registerBlockEntityInventory(event, ModBlockEntities.THERMAL_BURNER.get(),
                (be, side) -> inventoryHandler(be));
        EchoBackendItemBridge.registerBlockEntityInventory(event, ModBlockEntities.DEEP_CORE_MINER.get(),
                (be, side) -> inventoryHandler(be));
    }

    private static <T> T inventoryHandler(BlockEntity be) {
        if (be instanceof HopperHandler handler) {
            return EchoBackendItemBridge.backendContainerHandler(handler.getInventory());
        }
        return null;
    }
}
