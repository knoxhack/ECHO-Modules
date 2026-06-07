package com.knoxhack.echo.adaptercore;

import net.minecraft.world.Container;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.item.VanillaContainerWrapper;

/**
 * AdapterCore backend bridge for live inventory/container capabilities.
 */
public final class EchoBackendItemBridge {
    private EchoBackendItemBridge() {
    }

    @SuppressWarnings("unchecked")
    public static <T> T backendContainerHandler(Object container) {
        if (container instanceof Container liveContainer) {
            return (T) VanillaContainerWrapper.of(liveContainer);
        }
        return null;
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    public static void registerBlockEntityInventory(Object event, Object blockEntityType,
            EchoBlockEntityCapabilityProvider provider) {
        if (event instanceof RegisterCapabilitiesEvent capabilitiesEvent
                && blockEntityType instanceof BlockEntityType type) {
            capabilitiesEvent.registerBlockEntity(Capabilities.Item.BLOCK, type,
                    (blockEntity, side) -> (ResourceHandler<ItemResource>) provider.get(blockEntity, side));
        }
    }
}
