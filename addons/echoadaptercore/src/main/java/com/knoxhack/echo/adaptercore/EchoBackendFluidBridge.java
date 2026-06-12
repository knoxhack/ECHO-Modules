package com.knoxhack.echo.adaptercore;

import java.util.function.Consumer;
import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.fluids.BaseFlowingFluid;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.transaction.SnapshotJournal;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;
import net.neoforged.bus.api.IEventBus;

/**
 * AdapterCore backend bridge for fluid registration and capability wiring.
 */
public final class EchoBackendFluidBridge {
    private EchoBackendFluidBridge() {
    }

    public interface EchoFluidHandlerDelegate {
        int size();

        Object getResource(int slot);

        long getAmountAsLong(int slot);

        long getCapacityAsLong(int slot, Object resource);

        boolean isValid(int slot, Object resource);

        int insert(int slot, Object resource, int maxAmount, Object transaction);

        int extract(int slot, Object resource, int maxAmount, Object transaction);
    }

    public static Object createFluidTypeRegistry(String modId) {
        return DeferredRegister.create(NeoForgeRegistries.Keys.FLUID_TYPES, modId);
    }

    public static Object createFluidRegistry(String modId) {
        return DeferredRegister.create(Registries.FLUID, modId);
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    public static void registerEventBus(Object registry, Object eventBus) {
        if (registry instanceof DeferredRegister deferredRegister && eventBus instanceof IEventBus bus) {
            deferredRegister.register(bus);
        }
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    public static EchoBackendRegistryEntry<?> registerFluidType(Object registry, String name, String modId,
            int temperature, int viscosity, Rarity rarity) {
        if (registry instanceof DeferredRegister deferredRegister) {
            return new EchoBackendRegistryEntry<>(deferredRegister.register(
                    name,
                    () -> new FluidType(
                            FluidType.Properties.create()
                                    .descriptionId("fluid." + modId + "." + name)
                                    .temperature(temperature)
                                    .viscosity(viscosity)
                                    .rarity(rarity)
                                    .canSwim(false)
                                    .canDrown(false))));
        }
        return new EchoBackendRegistryEntry<>((Supplier<Object>) () -> null);
    }

    public static EchoBackendRegistryEntry<Fluid> registerSourceFluid(Object registry, String name,
            Supplier<?> type, Supplier<? extends Fluid> source, Supplier<? extends Fluid> flowing) {
        return registerFluid(registry, name, () -> new BaseFlowingFluid.Source(
                new BaseFlowingFluid.Properties(() -> (FluidType) type.get(), source, flowing)));
    }

    public static EchoBackendRegistryEntry<Fluid> registerFlowingFluid(Object registry, String name,
            Supplier<?> type, Supplier<? extends Fluid> source, Supplier<? extends Fluid> flowing) {
        return registerFluid(registry, name, () -> new BaseFlowingFluid.Flowing(
                new BaseFlowingFluid.Properties(() -> (FluidType) type.get(), source, flowing)));
    }

    public static FluidResource emptyFluidResource() {
        return FluidResource.EMPTY;
    }

    public static FluidResource fluidResourceOf(Fluid fluid) {
        return fluid == null ? FluidResource.EMPTY : FluidResource.of(fluid);
    }

    public static boolean isEmptyFluidResource(Object resource) {
        return !(resource instanceof FluidResource fluidResource) || fluidResource.isEmpty();
    }

    public static Fluid fluidResourceFluid(Object resource) {
        return resource instanceof FluidResource fluidResource ? fluidResource.getFluid() : null;
    }

    public static ResourceHandler<FluidResource> createFluidHandler(EchoFluidHandlerDelegate delegate) {
        return new DelegatingFluidHandler(delegate);
    }

    public static Object fluidCapability(Level level, BlockPos pos, Direction side) {
        return level == null ? null : level.getCapability(Capabilities.Fluid.BLOCK, pos, side);
    }

    public static int fluidHandlerSize(Object handler) {
        return handler instanceof ResourceHandler<?> resourceHandler ? resourceHandler.size() : 0;
    }

    public static Object fluidHandlerResource(Object handler, int slot) {
        if (handler instanceof ResourceHandler<?> resourceHandler) {
            return resourceHandler.getResource(slot);
        }
        return FluidResource.EMPTY;
    }

    public static long fluidHandlerAmount(Object handler, int slot) {
        return handler instanceof ResourceHandler<?> resourceHandler ? resourceHandler.getAmountAsLong(slot) : 0L;
    }

    @SuppressWarnings("unchecked")
    public static int insertFluid(Object handler, Object resource, int maxAmount, Object transaction) {
        if (handler instanceof ResourceHandler<?> resourceHandler && resource instanceof FluidResource fluidResource) {
            return ((ResourceHandler<FluidResource>) resourceHandler).insert(
                    fluidResource,
                    maxAmount,
                    transaction instanceof TransactionContext context ? context : null);
        }
        return 0;
    }

    @SuppressWarnings("unchecked")
    public static int extractFluid(Object handler, int slot, Object resource, int maxAmount, Object transaction) {
        if (handler instanceof ResourceHandler<?> resourceHandler && resource instanceof FluidResource fluidResource) {
            return ((ResourceHandler<FluidResource>) resourceHandler).extract(
                    slot,
                    fluidResource,
                    maxAmount,
                    transaction instanceof TransactionContext context ? context : null);
        }
        return 0;
    }

    public static Object openRootTransaction() {
        return Transaction.openRoot();
    }

    public static void commitTransaction(Object transaction) {
        if (transaction instanceof Transaction tx) {
            tx.commit();
        }
    }

    public static void closeTransaction(Object transaction) {
        if (transaction instanceof AutoCloseable closeable) {
            try {
                closeable.close();
            } catch (Exception ignored) {
                // Transaction close does not expose checked failures in the backend used here.
            }
        }
    }

    public static Object createSnapshotJournal(Supplier<?> snapshotFactory, Consumer<Object> revert) {
        return new SnapshotJournal<>() {
            @Override
            protected Object createSnapshot() {
                return snapshotFactory.get();
            }

            @Override
            protected void revertToSnapshot(Object snapshot) {
                revert.accept(snapshot);
            }
        };
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    public static void updateSnapshots(Object journal, Object transaction) {
        if (journal instanceof SnapshotJournal snapshotJournal && transaction instanceof TransactionContext context) {
            snapshotJournal.updateSnapshots(context);
        }
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    public static void registerBlockEntityFluid(Object event, Object blockEntityType,
            EchoBlockEntityCapabilityProvider provider) {
        if (event instanceof RegisterCapabilitiesEvent capabilitiesEvent
                && blockEntityType instanceof BlockEntityType type) {
            capabilitiesEvent.registerBlockEntity(Capabilities.Fluid.BLOCK, type,
                    (blockEntity, side) -> (ResourceHandler<FluidResource>) provider.get(blockEntity, side));
        }
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static EchoBackendRegistryEntry<Fluid> registerFluid(Object registry, String name,
            Supplier<? extends Fluid> supplier) {
        if (registry instanceof DeferredRegister deferredRegister) {
            return new EchoBackendRegistryEntry<>(deferredRegister.register(name, supplier));
        }
        return new EchoBackendRegistryEntry<>((Supplier<Fluid>) () -> null);
    }

    private record DelegatingFluidHandler(EchoFluidHandlerDelegate delegate) implements ResourceHandler<FluidResource> {
        @Override
        public int size() {
            return delegate.size();
        }

        @Override
        public FluidResource getResource(int slot) {
            Object resource = delegate.getResource(slot);
            return resource instanceof FluidResource fluidResource ? fluidResource : FluidResource.EMPTY;
        }

        @Override
        public long getAmountAsLong(int slot) {
            return delegate.getAmountAsLong(slot);
        }

        @Override
        public long getCapacityAsLong(int slot, FluidResource resource) {
            return delegate.getCapacityAsLong(slot, resource);
        }

        @Override
        public boolean isValid(int slot, FluidResource resource) {
            return delegate.isValid(slot, resource);
        }

        @Override
        public int insert(int slot, FluidResource resource, int maxAmount, TransactionContext transaction) {
            return delegate.insert(slot, resource, maxAmount, transaction);
        }

        @Override
        public int extract(int slot, FluidResource resource, int maxAmount, TransactionContext transaction) {
            return delegate.extract(slot, resource, maxAmount, transaction);
        }
    }
}
