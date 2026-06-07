package com.knoxhack.echoblackboxprotocol.registry;

import net.minecraft.core.registries.Registries;
import com.knoxhack.echo.adaptercore.EchoBackendRegistryBridge;
import com.knoxhack.echo.adaptercore.EchoBackendRegistryEntry;
import com.knoxhack.echoblackboxprotocol.item.BlackboxFragmentItem;
import com.knoxhack.echoblackboxprotocol.item.EndingDirectiveItem;
import com.knoxhack.echoblackboxprotocol.progression.BlackboxEnding;
import com.knoxhack.echoblackboxprotocol.progression.MemoryType;
import java.util.ArrayList;
import java.util.List;
import java.util.function.UnaryOperator;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.Item.Properties;

public final class ModItems {
   public static final Object ITEMS = EchoBackendRegistryBridge.create(Registries.ITEM, "echoblackboxprotocol");
   private static final List<EchoBackendRegistryEntry<? extends Item>> CREATIVE_ITEMS = new ArrayList<>();
   public static final EchoBackendRegistryEntry<Item> BLACK_METAL = simple("black_metal", p -> p.rarity(Rarity.UNCOMMON));
   public static final EchoBackendRegistryEntry<Item> CORRUPTED_FERRITE = simple("corrupted_ferrite", p -> p.rarity(Rarity.UNCOMMON));
   public static final EchoBackendRegistryEntry<Item> STATIC_FLUID = simple("static_fluid", p -> p.stacksTo(16).rarity(Rarity.RARE));
   public static final EchoBackendRegistryEntry<Item> PERSONAL_BLACKBOX_FRAGMENT = fragment("personal_blackbox_fragment", MemoryType.PERSONAL);
   public static final EchoBackendRegistryEntry<Item> ECHO_BLACKBOX_FRAGMENT = fragment("echo_blackbox_fragment", MemoryType.ECHO);
   public static final EchoBackendRegistryEntry<Item> SECURITY_BLACKBOX_FRAGMENT = fragment("security_blackbox_fragment", MemoryType.SECURITY);
   public static final EchoBackendRegistryEntry<Item> COMMAND_BLACKBOX_FRAGMENT = fragment("command_blackbox_fragment", MemoryType.COMMAND);
   public static final EchoBackendRegistryEntry<Item> CORE_BLACKBOX_FRAGMENT = fragment("core_blackbox_fragment", MemoryType.CORE);
   public static final EchoBackendRegistryEntry<Item> DELETED_BLACKBOX_FRAGMENT = fragment("deleted_blackbox_fragment", MemoryType.DELETED);
   public static final EchoBackendRegistryEntry<Item> PERSONAL_MEMORY_RECORD = simple("personal_memory_record", p -> p.rarity(Rarity.UNCOMMON));
   public static final EchoBackendRegistryEntry<Item> ECHO_MEMORY_RECORD = simple("echo_memory_record", p -> p.rarity(Rarity.UNCOMMON));
   public static final EchoBackendRegistryEntry<Item> SECURITY_MEMORY_RECORD = simple("security_memory_record", p -> p.rarity(Rarity.UNCOMMON));
   public static final EchoBackendRegistryEntry<Item> COMMAND_MEMORY_RECORD = simple("command_memory_record", p -> p.rarity(Rarity.RARE));
   public static final EchoBackendRegistryEntry<Item> CORE_MEMORY_RECORD = simple("core_memory_record", p -> p.rarity(Rarity.RARE));
   public static final EchoBackendRegistryEntry<Item> DELETED_MEMORY_RECORD = simple("deleted_memory_record", p -> p.rarity(Rarity.EPIC).fireResistant());
   public static final EchoBackendRegistryEntry<Item> ECHO_IDENTITY_FRAGMENT = simple("echo_identity_fragment", p -> p.rarity(Rarity.EPIC).fireResistant());
   public static final EchoBackendRegistryEntry<Item> MEMORY_STABILIZER_CORE = simple("memory_stabilizer_core", p -> p.rarity(Rarity.EPIC).fireResistant());
   public static final EchoBackendRegistryEntry<Item> COMMAND_KEY = simple("command_key", p -> p.rarity(Rarity.EPIC).fireResistant());
   public static final EchoBackendRegistryEntry<Item> PROTOCOL_EXTRACTOR_SCHEMATIC = simple("protocol_extractor_schematic", p -> p.rarity(Rarity.RARE));
   public static final EchoBackendRegistryEntry<Item> GUARDIAN_CORE = simple("guardian_core", p -> p.rarity(Rarity.EPIC).fireResistant());
   public static final EchoBackendRegistryEntry<Item> CORE_ACCESS_KEY_LEFT = simple("core_access_key_left", p -> p.rarity(Rarity.RARE));
   public static final EchoBackendRegistryEntry<Item> CORE_ACCESS_KEY_RIGHT = simple("core_access_key_right", p -> p.rarity(Rarity.RARE));
   public static final EchoBackendRegistryEntry<Item> CORE_ACCESS_KEY_MATRIX = simple("core_access_key_matrix", p -> p.rarity(Rarity.RARE));
   public static final EchoBackendRegistryEntry<Item> NEXUS_CORE_ACCESS_KEY = simple("nexus_core_access_key", p -> p.stacksTo(1).rarity(Rarity.EPIC).fireResistant());
   public static final EchoBackendRegistryEntry<Item> RESTORE_DIRECTIVE = directive("restore_directive", BlackboxEnding.RESTORE);
   public static final EchoBackendRegistryEntry<Item> CONTROL_DIRECTIVE = directive("control_directive", BlackboxEnding.CONTROL);
   public static final EchoBackendRegistryEntry<Item> DESTROY_DIRECTIVE = directive("destroy_directive", BlackboxEnding.DESTROY);
   public static final EchoBackendRegistryEntry<Item> MERGE_DIRECTIVE = directive("merge_directive", BlackboxEnding.MERGE);

   private ModItems() {
   }

   public static void register(Object eventBus) {
      EchoBackendRegistryBridge.registerEventBus(ITEMS, eventBus);
   }

   public static List<EchoBackendRegistryEntry<? extends Item>> creativeItems() {
      return List.copyOf(CREATIVE_ITEMS);
   }

   public static EchoBackendRegistryEntry<Item> fragmentFor(MemoryType type) {
      return switch (type) {
         case PERSONAL -> PERSONAL_BLACKBOX_FRAGMENT;
         case ECHO -> ECHO_BLACKBOX_FRAGMENT;
         case SECURITY -> SECURITY_BLACKBOX_FRAGMENT;
         case COMMAND -> COMMAND_BLACKBOX_FRAGMENT;
         case CORE -> CORE_BLACKBOX_FRAGMENT;
         case DELETED -> DELETED_BLACKBOX_FRAGMENT;
      };
   }

   public static EchoBackendRegistryEntry<Item> recordFor(MemoryType type) {
      return switch (type) {
         case PERSONAL -> PERSONAL_MEMORY_RECORD;
         case ECHO -> ECHO_MEMORY_RECORD;
         case SECURITY -> SECURITY_MEMORY_RECORD;
         case COMMAND -> COMMAND_MEMORY_RECORD;
         case CORE -> CORE_MEMORY_RECORD;
         case DELETED -> DELETED_MEMORY_RECORD;
      };
   }

   public static EchoBackendRegistryEntry<Item> directiveFor(BlackboxEnding ending) {
      return switch (ending) {
         case RESTORE -> RESTORE_DIRECTIVE;
         case CONTROL -> CONTROL_DIRECTIVE;
         case DESTROY -> DESTROY_DIRECTIVE;
         case MERGE -> MERGE_DIRECTIVE;
         case NONE -> RESTORE_DIRECTIVE;
      };
   }

   private static EchoBackendRegistryEntry<Item> fragment(String name, MemoryType type) {
      return tracked(EchoBackendRegistryBridge.registerItem(ITEMS, name, properties -> new BlackboxFragmentItem(type, properties), p -> p.rarity(Rarity.RARE).fireResistant()));
   }

   private static EchoBackendRegistryEntry<Item> directive(String name, BlackboxEnding ending) {
      return tracked(
         EchoBackendRegistryBridge.registerItem(ITEMS, name, properties -> new EndingDirectiveItem(ending, properties), p -> p.stacksTo(1).rarity(Rarity.EPIC).fireResistant())
      );
   }

   private static EchoBackendRegistryEntry<Item> simple(String name, UnaryOperator<Properties> properties) {
      return tracked(EchoBackendRegistryBridge.registerSimpleItem(ITEMS, name, properties));
   }

   private static <T extends Item> EchoBackendRegistryEntry<T> tracked(EchoBackendRegistryEntry<T> item) {
      CREATIVE_ITEMS.add(item);
      return item;
   }

   static {
      ModBlocks.ALL_BLOCKS.forEach(block -> tracked(EchoBackendRegistryBridge.registerSimpleBlockItem(ITEMS, block)));
   }
}
