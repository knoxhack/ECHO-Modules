package com.knoxhack.echoblockworks.registry;

import java.util.Objects;
import java.util.function.Supplier;

public final class NativeRegistryHolder<T> implements Supplier<T> {
   private final String id;
   private final Supplier<? extends T> supplier;

   private NativeRegistryHolder(String id, Supplier<? extends T> supplier) {
      this.id = id == null ? "" : id;
      this.supplier = Objects.requireNonNull(supplier, this.id);
   }

   public static <T> NativeRegistryHolder<T> of(String id, T value) {
      return new NativeRegistryHolder<>(id, () -> Objects.requireNonNull(value, id));
   }

   public static <T> NativeRegistryHolder<T> deferred(String id, Supplier<? extends T> supplier) {
      return new NativeRegistryHolder<>(id, supplier);
   }

   @Override
   public T get() {
      return supplier.get();
   }

   public String id() {
      return id;
   }
}
