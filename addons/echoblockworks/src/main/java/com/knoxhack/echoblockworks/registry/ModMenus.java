package com.knoxhack.echoblockworks.registry;

import com.knoxhack.echoblockworks.EchoBlockworks;
import com.knoxhack.echoblockworks.block.entity.BlockworksTableBlockEntity;
import com.knoxhack.echoblockworks.menu.BlockworksTableMenu;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.SimpleContainerData;

public final class ModMenus {
   public static final NativeRegistryHolder<MenuType<BlockworksTableMenu>> BLOCKWORKS_TABLE =
      NativeRegistryHolder.of("blockworks_table", new MenuType<>(
         (containerId, inventory) -> new BlockworksTableMenu(
            containerId,
            inventory,
            new SimpleContainer(2),
            new SimpleContainerData(BlockworksTableBlockEntity.DATA_COUNT)),
         null));

   private ModMenus() {
   }

   public static void register() {
   }
}
