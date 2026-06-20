package com.knoxhack.echo.adaptercore;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;

/**
 * AdapterCore backend bridge for legacy deferred content registries.
 */
public final class EchoBackendRegistryBridge {
    private static final String DEFERRED_REGISTER_CLASS = "net.neoforged.neoforge.registries.DeferredRegister";
    private static final String EVENT_BUS_CLASS = "net.neoforged.bus.api.IEventBus";

    private EchoBackendRegistryBridge() {
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    public static Object create(Object registry, String modId) {
        Class<?> deferredRegister = optionalClass(DEFERRED_REGISTER_CLASS);
        if (deferredRegister != null) {
            if (registry instanceof Registry liveRegistry) {
                return invokeStatic(deferredRegister, "create",
                        new Class<?>[]{Registry.class, String.class},
                        new Object[]{liveRegistry, modId});
            }
            if (registry instanceof ResourceKey key) {
                return invokeStatic(deferredRegister, "create",
                        new Class<?>[]{ResourceKey.class, String.class},
                        new Object[]{key, modId});
            }
        }
        if (registry instanceof Registry<?> || registry instanceof ResourceKey<?>) {
            return new LocalRegistry(registry, modId);
        }
        throw new IllegalArgumentException("Unsupported backend registry key: " + registry);
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    public static <T> EchoBackendRegistryEntry<T> register(Object registry, String id, Supplier<? extends T> factory) {
        if (isDeferredRegister(registry)) {
            Object holder = invoke(registry, "register",
                    new Class<?>[]{String.class, Supplier.class},
                    new Object[]{id, factory});
            return new EchoBackendRegistryEntry<>(holder);
        }
        if (registry instanceof LocalRegistry localRegistry) {
            return new EchoBackendRegistryEntry<>(localRegistry.register(id, factory));
        }
        throw new IllegalArgumentException("Unsupported backend registry: " + registry);
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    public static <T> EchoBackendRegistryEntry<T> registerWithId(Object registry, String id,
            Function<Identifier, ? extends T> factory) {
        if (isDeferredRegister(registry)) {
            Object holder = invoke(registry, "register",
                    new Class<?>[]{String.class, Function.class},
                    new Object[]{id, (Function<Object, ? extends T>) key -> factory.apply((Identifier) key)});
            return new EchoBackendRegistryEntry<>(holder);
        }
        if (registry instanceof LocalRegistry localRegistry) {
            return new EchoBackendRegistryEntry<>(localRegistry.register(id, () -> factory.apply(id(localRegistry, id))));
        }
        throw new IllegalArgumentException("Unsupported backend registry: " + registry);
    }

    public static <T extends Block> EchoBackendRegistryEntry<T> registerBlock(Object registry, String id,
            Function<BlockBehaviour.Properties, ? extends T> factory,
            UnaryOperator<BlockBehaviour.Properties> properties) {
        return register(registry, id, () -> factory.apply(blockProperties(registry, id, properties)));
    }

    public static EchoBackendRegistryEntry<Block> registerSimpleBlock(Object registry, String id,
            UnaryOperator<BlockBehaviour.Properties> properties) {
        return registerBlock(registry, id, Block::new, properties);
    }

    public static <T extends Item> EchoBackendRegistryEntry<T> registerItem(Object registry, String id,
            Function<Item.Properties, ? extends T> factory) {
        return registerItem(registry, id, factory, UnaryOperator.identity());
    }

    public static <T extends Item> EchoBackendRegistryEntry<T> registerItem(Object registry, String id,
            Function<Item.Properties, ? extends T> factory,
            UnaryOperator<Item.Properties> properties) {
        return register(registry, id, () -> factory.apply(itemProperties(registry, id, properties)));
    }

    public static EchoBackendRegistryEntry<Item> registerSimpleItem(Object registry, String id,
            UnaryOperator<Item.Properties> properties) {
        return registerItem(registry, id, Item::new, properties);
    }

    public static EchoBackendRegistryEntry<BlockItem> registerSimpleBlockItem(Object registry, EchoBackendRegistryEntry<? extends Block> block) {
        return register(registry, block.id().getPath(), () -> new BlockItem(block.get(),
                new Item.Properties()
                        .setId(ResourceKey.create(Registries.ITEM, block.id()))
                        .useBlockDescriptionPrefix()));
    }

    @SuppressWarnings("rawtypes")
    public static void registerEventBus(Object registry, Object eventBus) {
        Class<?> eventBusClass = optionalClass(EVENT_BUS_CLASS);
        if (isDeferredRegister(registry) && eventBusClass != null && eventBusClass.isInstance(eventBus)) {
            invoke(registry, "register", new Class<?>[]{eventBusClass}, new Object[]{eventBus});
        }
    }

    @SuppressWarnings("rawtypes")
    public static List<EchoBackendRegistryEntry<?>> entries(Object registry) {
        if (isDeferredRegister(registry)) {
            Object entries = invoke(registry, "getEntries", new Class<?>[]{}, new Object[]{});
            if (entries instanceof Iterable<?> iterable) {
                List<EchoBackendRegistryEntry<?>> result = new ArrayList<>();
                for (Object holder : iterable) {
                    result.add(entry(holder));
                }
                return List.copyOf(result);
            }
        }
        if (registry instanceof LocalRegistry localRegistry) {
            return localRegistry.entries().stream()
                    .map(EchoBackendRegistryBridge::entry)
                    .toList();
        }
        return List.of();
    }

    public static EchoBackendRegistryEntry<?> entry(Object holder) {
        return holder instanceof EchoBackendRegistryEntry<?> entry ? entry : new EchoBackendRegistryEntry<>(holder);
    }

    private static BlockBehaviour.Properties blockProperties(Object registry, String id,
            UnaryOperator<BlockBehaviour.Properties> properties) {
        BlockBehaviour.Properties base = BlockBehaviour.Properties.of()
                .setId(ResourceKey.create(Registries.BLOCK, id(registry, id)));
        return properties == null ? base : properties.apply(base);
    }

    private static Item.Properties itemProperties(Object registry, String id, UnaryOperator<Item.Properties> properties) {
        Item.Properties base = new Item.Properties().setId(ResourceKey.create(Registries.ITEM, id(registry, id)));
        return properties == null ? base : properties.apply(base);
    }

    @SuppressWarnings("rawtypes")
    private static Identifier id(Object registry, String path) {
        if (isDeferredRegister(registry)) {
            Object namespace = invoke(registry, "getNamespace", new Class<?>[]{}, new Object[]{});
            return Identifier.fromNamespaceAndPath(String.valueOf(namespace), path);
        }
        if (registry instanceof LocalRegistry localRegistry) {
            return Identifier.fromNamespaceAndPath(localRegistry.modId(), path);
        }
        return Identifier.parse(path);
    }

    private static boolean isDeferredRegister(Object value) {
        Class<?> deferredRegister = optionalClass(DEFERRED_REGISTER_CLASS);
        return deferredRegister != null && deferredRegister.isInstance(value);
    }

    private static Class<?> optionalClass(String className) {
        try {
            return Class.forName(className, false, EchoBackendRegistryBridge.class.getClassLoader());
        } catch (ClassNotFoundException ignored) {
            return null;
        }
    }

    private static Object invokeStatic(Class<?> type, String methodName, Class<?>[] parameterTypes, Object[] arguments) {
        try {
            Method method = type.getMethod(methodName, parameterTypes);
            return method.invoke(null, arguments);
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException exception) {
            throw new IllegalStateException("Unable to call " + type.getName() + "." + methodName, unwrap(exception));
        }
    }

    private static Object invoke(Object target, String methodName, Class<?>[] parameterTypes, Object[] arguments) {
        try {
            Method method = target.getClass().getMethod(methodName, parameterTypes);
            return method.invoke(target, arguments);
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException exception) {
            throw new IllegalStateException("Unable to call " + target.getClass().getName() + "." + methodName, unwrap(exception));
        }
    }

    private static Throwable unwrap(Throwable throwable) {
        return throwable instanceof InvocationTargetException invocation && invocation.getCause() != null
                ? invocation.getCause()
                : throwable;
    }

    private record LocalRegistry(Object registryKey, String modId, List<LocalHolder<?>> holders) {
        private LocalRegistry(Object registryKey, String modId) {
            this(registryKey, modId == null || modId.isBlank() ? "echo" : modId, new ArrayList<>());
        }

        private <T> LocalHolder<T> register(String path, Supplier<? extends T> factory) {
            LocalHolder<T> holder = new LocalHolder<>(
                    Identifier.fromNamespaceAndPath(modId, path),
                    registryResourceKey(),
                    factory);
            holders.add(holder);
            return holder;
        }

        private List<LocalHolder<?>> entries() {
            return List.copyOf(holders);
        }

        @SuppressWarnings("unchecked")
        private ResourceKey<?> registryResourceKey() {
            if (registryKey instanceof ResourceKey<?> key) {
                return key;
            }
            if (registryKey instanceof Registry<?> registry) {
                return registry.key();
            }
            return Registries.ITEM;
        }
    }

    static final class LocalHolder<T> implements Supplier<T> {
        private final Identifier id;
        private final ResourceKey<?> registry;
        private final ResourceKey<T> key;
        private final Supplier<? extends T> factory;
        private T value;

        @SuppressWarnings({"unchecked", "rawtypes"})
        private LocalHolder(Identifier id, ResourceKey<?> registry, Supplier<? extends T> factory) {
            this.id = id;
            this.registry = registry == null ? Registries.ITEM : registry;
            this.key = (ResourceKey<T>) ResourceKey.create((ResourceKey) this.registry, id);
            this.factory = factory;
        }

        @Override
        public T get() {
            if (value == null) {
                value = resolvedOrFallback();
            }
            return value;
        }

        ResourceKey<T> key() {
            return key;
        }

        Identifier id() {
            return id;
        }

        @SuppressWarnings("unchecked")
        private T resolvedOrFallback() {
            if (Registries.ITEM.equals(registry)) {
                Item item = BuiltInRegistries.ITEM.getOptional(id).orElse(Items.AIR);
                return (T) item;
            }
            if (Registries.BLOCK.equals(registry)) {
                Block block = BuiltInRegistries.BLOCK.getOptional(id).orElse(Blocks.AIR);
                return (T) block;
            }
            try {
                return factory.get();
            } catch (IllegalStateException exception) {
                if (exception.getMessage() != null && exception.getMessage().contains("Registry is already frozen")) {
                    return null;
                }
                throw exception;
            }
        }
    }
}
