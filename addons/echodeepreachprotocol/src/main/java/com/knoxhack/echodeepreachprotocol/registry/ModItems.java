package com.knoxhack.echodeepreachprotocol.registry;

import com.knoxhack.echo.adaptercore.EchoBackendRegistryBridge;
import com.knoxhack.echo.adaptercore.EchoBackendRegistryEntry;
import com.knoxhack.echodeepreachprotocol.EchoDeepReachProtocol;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.vehicle.boat.AbstractBoat;
import net.minecraft.world.item.BoatItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.item.component.TypedEntityData;

import java.util.function.Supplier;

/**
 * Item registry for ECHO: Deep Reach Protocol.
 */
public final class ModItems {
    public static final Object ITEMS = EchoBackendRegistryBridge.create(BuiltInRegistries.ITEM, EchoDeepReachProtocol.MODID);

    public static final EchoBackendRegistryEntry<Item> VENT_CRAB_SHELL = simple("vent_crab_shell");

    public static final EchoBackendRegistryEntry<Item> TWILIGHT_STALKER_SPAWN_EGG = spawnEgg("twilight_stalker_spawn_egg", ModEntities.TWILIGHT_STALKER);
    public static final EchoBackendRegistryEntry<Item> VENT_CRAB_SPAWN_EGG = spawnEgg("vent_crab_spawn_egg", ModEntities.VENT_CRAB);
    public static final EchoBackendRegistryEntry<Item> ABYSSAL_LEVIATHAN_SPAWN_EGG = spawnEgg("abyssal_leviathan_spawn_egg", ModEntities.ABYSSAL_LEVIATHAN);
    public static final EchoBackendRegistryEntry<Item> LATTICE_SENTINEL_SPAWN_EGG = spawnEgg("lattice_sentinel_spawn_egg", ModEntities.LATTICE_SENTINEL);
    public static final EchoBackendRegistryEntry<Item> BLOATER_SPAWN_EGG = spawnEgg("bloater_spawn_egg", ModEntities.BLOATER);
    public static final EchoBackendRegistryEntry<Item> HADAL_WRAITH_SPAWN_EGG = spawnEgg("hadal_wraith_spawn_egg", ModEntities.HADAL_WRAITH);

    public static final EchoBackendRegistryEntry<Item> REMORA_SUBMERSIBLE = boat("remora_submersible", ModEntities.REMORA_SUBMERSIBLE);

    private ModItems() {
    }

    public static void register(Object eventBus) {
        EchoBackendRegistryBridge.registerEventBus(ITEMS, eventBus);
    }

    private static EchoBackendRegistryEntry<Item> simple(String name) {
        return EchoBackendRegistryBridge.registerWithId(ITEMS, name, id -> new Item(properties(id)));
    }

    private static EchoBackendRegistryEntry<Item> spawnEgg(String name, Supplier<? extends EntityType<? extends Mob>> typeSupplier) {
        return EchoBackendRegistryBridge.registerWithId(ITEMS, name,
                id -> new SpawnEggItem(properties(id)
                        .component(DataComponents.ENTITY_DATA, TypedEntityData.of(typeSupplier.get(), new CompoundTag()))));
    }

    private static EchoBackendRegistryEntry<Item> boat(String name, Supplier<? extends EntityType<? extends AbstractBoat>> typeSupplier) {
        return EchoBackendRegistryBridge.registerWithId(ITEMS, name,
                id -> new BoatItem(typeSupplier.get(), properties(id).stacksTo(1)));
    }

    private static Item.Properties properties(Identifier id) {
        return new Item.Properties().setId(ResourceKey.create(Registries.ITEM, id));
    }
}
