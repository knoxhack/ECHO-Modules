package com.knoxhack.echo.npcore.registry;

import com.knoxhack.echo.adaptercore.EchoBackendRegistryBridge;
import com.knoxhack.echo.adaptercore.EchoBackendRegistryEntry;
import com.knoxhack.echo.npcore.EchoNpcCore;
import java.util.List;
import java.util.function.Supplier;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.item.component.TypedEntityData;

public final class ModItems {
    public static final Object ITEMS = EchoBackendRegistryBridge.create(BuiltInRegistries.ITEM, EchoNpcCore.MODID);

    public static final EchoBackendRegistryEntry<Item> ECHO_NPC_SPAWN_EGG =
            registerSpawnEgg("echo_npc_spawn_egg", ModEntities.ECHO_NPC::get);

    private ModItems() {
    }

    public static void register(Object eventBus) {
        EchoBackendRegistryBridge.registerEventBus(ITEMS, eventBus);
    }

    public static List<ItemStack> creativeItems() {
        return List.of(ECHO_NPC_SPAWN_EGG.get().getDefaultInstance());
    }

    private static EchoBackendRegistryEntry<Item> registerSpawnEgg(String name, Supplier<? extends EntityType<? extends Mob>> typeSupplier) {
        return EchoBackendRegistryBridge.registerWithId(ITEMS, name, id -> new SpawnEggItem(withId(new Item.Properties(), id)
                .component(DataComponents.ENTITY_DATA, TypedEntityData.of(typeSupplier.get(), new CompoundTag()))));
    }

    private static Item.Properties withId(Item.Properties properties, Identifier id) {
        return properties.setId(ResourceKey.create(Registries.ITEM, id));
    }
}
