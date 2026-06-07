package com.knoxhack.echoblockworks.integration;

import com.knoxhack.echoblockworks.EchoBlockworks;
import com.knoxhack.echoblockworks.content.BlockworksBlockInfo;
import com.knoxhack.echoblockworks.content.BlockworksCatalog;
import com.knoxhack.echoblockworks.content.BlockworksFamily;
import com.knoxhack.echoblockworks.content.BlockworksPaletteKit;
import com.knoxhack.echoblockworks.content.BlockworksShapeKind;
import com.knoxhack.echoblockworks.content.BlockworksWorldgenSite;
import com.knoxhack.echoblockworks.registry.ModBlocks;
import com.knoxhack.echocore.api.EchoCoreServices;
import com.knoxhack.echocore.api.index.IIndexContentProvider;
import com.knoxhack.echocore.api.index.IIndexRegistry;
import com.knoxhack.echocore.api.index.IndexBuildContext;
import com.knoxhack.echocore.api.index.IndexCategory;
import com.knoxhack.echocore.api.index.IndexContentBuilder;
import com.knoxhack.echocore.api.index.IndexContentSnapshot;
import com.knoxhack.echocore.api.index.IndexEntry;
import com.knoxhack.echocore.api.index.IndexEntryState;
import java.util.List;
import java.util.Locale;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

public enum BlockworksIndexProvider implements IIndexContentProvider {
   INSTANCE;

   private static final Identifier CATEGORY_FAMILIES = id("families");
   private static final Identifier CATEGORY_DETAIL = id("details");
   private static final Identifier CATEGORY_WORLDGEN = id("worldgen_sites");
   private static final Identifier CATEGORY_KITS = id("palette_kits");

   public static void register() {
      EchoCoreServices.registerIndexContentProvider(INSTANCE);
   }

   @Override
   public Identifier id() {
      return id("provider/index");
   }

   @Override
   public IndexContentSnapshot snapshot(IndexBuildContext context) {
      IndexContentBuilder builder = IndexContentBuilder.create(id());
      register(builder);
      return builder.snapshot();
   }

   public void register(IIndexRegistry registry) {
      registry.registerCategory(new IndexCategory(
         CATEGORY_FAMILIES,
         "Blockworks Families",
         "Decorative block families for ECHO and Ashfall construction palettes.",
         familyIcon(BlockworksCatalog.families().get(0)),
         210,
         EchoBlockworks.MODID));
      registry.registerCategory(new IndexCategory(
         CATEGORY_DETAIL,
         "Blockworks Details",
         "Lights, pipes, cables, rubble, data walls, and station detail blocks.",
         blockStack("echo_strip_light"),
         211,
         EchoBlockworks.MODID));
      registry.registerCategory(new IndexCategory(
         CATEGORY_WORLDGEN,
         "Blockworks Worldgen",
         "Rare showcase structures and palettes for Ashfall, Terminal, Orbital, Nexus, Blackbox, Reclamation, and Convoy spaces.",
         blockStack("ashstone_debris"),
         212,
         EchoBlockworks.MODID));
      registry.registerCategory(new IndexCategory(
         CATEGORY_KITS,
         "Blockworks Palette Kits",
         "Curated builder kits for bases, ruins, depots, vaults, command rooms, and greenhouse domes.",
         blockStack("echo_blockworks_table"),
         213,
         EchoBlockworks.MODID));

      int sort = 0;
      for (BlockworksFamily family : BlockworksCatalog.families()) {
         registry.registerEntry(new IndexEntry(
            id("family/" + family.id()),
            CATEGORY_FAMILIES,
            family.displayName(),
            family.theme().name(),
            "Theme: " + title(family.theme().name()) + ". Unlock tier metadata: " + title(family.unlockTier().name()) + ".",
            family.displayName() + " contains eight variants and is available to Blockworks Table and Pattern Cutter conversion.",
            familyIcon(family),
            EchoBlockworks.MODID,
            List.of("blockworks", "decorative", family.theme().name().toLowerCase(Locale.ROOT), family.id()),
            IndexEntryState.VISIBLE,
            List.of(),
            familyItemIds(family),
            List.of(),
            20 + sort++));
      }

      registry.registerEntry(new IndexEntry(
         id("detail/decorative_set"),
         CATEGORY_DETAIL,
         "Detail Block Set",
         "Lights, monitors, pipes, rubble, cables, and projectors.",
         "Sixteen premium detail blocks add life to ECHO spaces without hard dependencies on other modules.",
         "Use detail blocks in Terminal rooms, Blackbox vaults, convoy depots, ruined cities, and orbital interiors.",
         blockStack("data_wall"),
         EchoBlockworks.MODID,
         List.of("blockworks", "details", "lighting", "pipes", "debris"),
         IndexEntryState.VISIBLE,
         List.of(),
         BlockworksCatalog.details().stream().map(detail -> itemId(blockStack(detail.id()))).toList(),
         List.of(),
         80));

      int siteSort = 0;
      for (BlockworksWorldgenSite site : BlockworksCatalog.worldgenSites()) {
         ItemStack icon = blockStack(siteIcon(site.id()));
         registry.registerEntry(new IndexEntry(
            id("worldgen/" + site.id()),
            CATEGORY_WORLDGEN,
            site.displayName(),
            "Palette: " + title(site.paletteId()),
            "Template: " + site.structureTemplate() + ". Palette: " + title(site.paletteId()) + ".",
            site.recommendedUsage(),
            icon,
            EchoBlockworks.MODID,
            List.of("blockworks", "worldgen", "palette", site.paletteId(), site.id()),
            IndexEntryState.VISIBLE,
            List.of(),
            List.of(itemId(icon)),
            List.of(),
            120 + siteSort++));
      }

      int kitSort = 0;
      for (BlockworksPaletteKit kit : BlockworksCatalog.paletteKits()) {
         ItemStack icon = kit.featuredBlockIds().stream()
            .findFirst()
            .map(BlockworksIndexProvider::blockStack)
            .orElse(blockStack("echo_blockworks_table"));
         registry.registerEntry(new IndexEntry(
            id("palette_kit/" + kit.id()),
            CATEGORY_KITS,
            kit.displayName(),
            "Theme: " + title(kit.theme().name()),
            kit.description(),
            kit.recommendedUsage(),
            icon,
            EchoBlockworks.MODID,
            kitTags(kit),
            IndexEntryState.VISIBLE,
            List.of(),
            kit.featuredBlockIds().stream().map(BlockworksIndexProvider::blockStack).map(BlockworksIndexProvider::itemId).toList(),
            kit.accentBlockIds().stream().map(BlockworksIndexProvider::blockStack).map(BlockworksIndexProvider::itemId).toList(),
            160 + kitSort++));
      }
   }

   private static String siteIcon(String siteId) {
      return switch (siteId) {
         case "ashfall_street_ruin" -> "ashstone_cracked_brick";
         case "crash_zone_fragment" -> "orbital_hull_damaged_hull";
         case "terminal_bunker_alcove" -> "terminal_panel_screen";
         case "orbital_airlock_remnant" -> "orbital_hull_airlock_frame";
         case "nexus_gate_shard" -> "nexus_crystal_rift_panel";
         case "blackbox_vault_breach" -> "blackbox_vault_locked_panel";
         case "reclamation_dome_remnant" -> "reclamation_glass_dome_panel";
         case "convoy_depot_pullout" -> "charred_concrete_road_plate";
         default -> "ashstone_debris";
      };
   }

   private static List<Identifier> familyItemIds(BlockworksFamily family) {
      return BlockworksCatalog.blockInfos().stream()
         .filter(info -> info.family().id().equals(family.id()))
         .filter(info -> info.shape() == BlockworksShapeKind.FULL)
         .map(info -> itemId(blockStack(info.blockId())))
         .toList();
   }

   private static List<String> kitTags(BlockworksPaletteKit kit) {
      return java.util.stream.Stream.of(
            java.util.stream.Stream.of("blockworks", "palette", "kit", kit.theme().name().toLowerCase(Locale.ROOT), kit.id()),
            kit.familyIds().stream(),
            kit.worldgenSiteId().stream()
         )
         .flatMap(stream -> stream)
         .toList();
   }

   private static ItemStack familyIcon(BlockworksFamily family) {
      return BlockworksCatalog.blockInfos().stream()
         .filter(info -> info.family().id().equals(family.id()))
         .filter(info -> info.shape() == BlockworksShapeKind.FULL)
         .findFirst()
         .map(BlockworksBlockInfo::blockId)
         .map(BlockworksIndexProvider::blockStack)
         .orElse(ItemStack.EMPTY);
   }

   private static ItemStack blockStack(String blockId) {
      return ModBlocks.blockForId(blockId)
         .map(block -> new ItemStack(block.get()))
         .orElse(ItemStack.EMPTY);
   }

   private static Identifier itemId(ItemStack stack) {
      Identifier id = stack.isEmpty() ? null : BuiltInRegistries.ITEM.getKey(stack.getItem());
      return id == null ? Identifier.withDefaultNamespace("air") : id;
   }

   private static Identifier id(String path) {
      return Identifier.fromNamespaceAndPath(EchoBlockworks.MODID, sanitize(path));
   }

   private static String sanitize(String path) {
      String clean = path == null ? "unknown" : path.trim().toLowerCase(Locale.ROOT);
      clean = clean.replace('\\', '/').replace(':', '/').replaceAll("[^a-z0-9_./-]", "_");
      while (clean.contains("//")) {
         clean = clean.replace("//", "/");
      }
      return clean.isBlank() ? "unknown" : clean;
   }

   private static String title(String raw) {
      String text = raw == null ? "" : raw.toLowerCase(Locale.ROOT).replace('_', ' ');
      StringBuilder builder = new StringBuilder(text.length());
      boolean nextUpper = true;
      for (char c : text.toCharArray()) {
         if (Character.isWhitespace(c)) {
            builder.append(c);
            nextUpper = true;
         } else if (nextUpper) {
            builder.append(Character.toUpperCase(c));
            nextUpper = false;
         } else {
            builder.append(c);
         }
      }
      return builder.toString();
   }
}
