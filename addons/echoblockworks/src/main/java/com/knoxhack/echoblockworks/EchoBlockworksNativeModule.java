package com.knoxhack.echoblockworks;

import dev.echo.nativeplatform.contracts.EchoNativeSurfaceModuleEntrypoint;

import com.knoxhack.echoblockworks.content.BlockworksBlockInfo;
import com.knoxhack.echoblockworks.content.BlockworksCatalog;
import com.knoxhack.echoblockworks.content.BlockworksPaletteKit;
import com.knoxhack.echoblockworks.content.BlockworksShapeKind;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class EchoBlockworksNativeModule implements EchoNativeSurfaceModuleEntrypoint {
   public static final String MODULE_ID = "echoblockworks";
   public static final String BLOCK_CATALOG_CONTRACT_ID = "echoblockworks:block/block_catalog";
   public static final String PATTERN_CUTTER_CONTRACT_ID = "echoblockworks:item/pattern_cutter";
   public static final String PALETTE_CONVERSION_CONTRACT_ID = "echoblockworks:recipe/palette_conversion";
   public static final String SHOWCASE_SITES_CONTRACT_ID = "echoblockworks:structure/showcase_sites";
   public static final String SCATTER_SITES_CONTRACT_ID = "echoblockworks:worldgen/scatter_sites";
   public static final List<String> CONTRACT_IDS = List.of(
      BLOCK_CATALOG_CONTRACT_ID,
      PATTERN_CUTTER_CONTRACT_ID,
      PALETTE_CONVERSION_CONTRACT_ID,
      SHOWCASE_SITES_CONTRACT_ID,
      SCATTER_SITES_CONTRACT_ID
   );

   public Map<String, Object> describeNativeSurfaces(Map<String, String> context) {
      Map<String, Object> referenceProbe = exerciseReferenceBehavior();
      Map<String, Object> result = new LinkedHashMap<>();
      result.put("activated", true);
      result.put("activationStage", "blockworks_native_contract_active");
      result.put("adapterCoreUsed", true);
      result.put("nativeAdapterCodeExecuted", true);
      result.put("serviceCodeExecuted", true);
      result.put("moduleId", MODULE_ID);
      result.put("packId", context.getOrDefault("packId", "unknown"));
      result.put("registeredFeatureContracts", CONTRACT_IDS);
      result.put("logicalRegistrationCount", CONTRACT_IDS.size());
      result.put("adapterDomains", List.of("blocks", "items", "recipes", "structures", "worldgen"));
      result.put("runtimeTargets", List.of("echo_native", "echo_runtime_standalone"));
      result.put("blockCatalogRoundTrip", referenceProbe.get("blockCatalogRoundTrip"));
      result.put("patternCutterRoundTrip", referenceProbe.get("patternCutterRoundTrip"));
      result.put("paletteConversionRoundTrip", referenceProbe.get("paletteConversionRoundTrip"));
      result.put("showcaseSiteRoundTrip", referenceProbe.get("showcaseSiteRoundTrip"));
      result.put("worldgenSiteRoundTrip", referenceProbe.get("worldgenSiteRoundTrip"));
      result.put("referenceProbe", referenceProbe);
      result.put("registryInjected", false);
      result.put("registryMutated", false);
      result.put("transformsPerformed", false);
      result.put("summary", "Blockworks native contract exercised catalog lookup, shape cycling, palette conversion, showcase, and worldgen site behavior.");
      return Map.copyOf(result);
   }

   public static void main(String[] args) {
      Map<String, Object> activation = new EchoBlockworksNativeModule()
         .describeNativeSurfaces(Map.of("packId", "agent4-blockworks-smoke"));
      require(Boolean.TRUE.equals(activation.get("activated")),
         "Blockworks native adapter should activate");
      require(Boolean.TRUE.equals(activation.get("blockCatalogRoundTrip")),
         "Blockworks native adapter should exercise block catalog behavior");
      require(Boolean.TRUE.equals(activation.get("patternCutterRoundTrip")),
         "Blockworks native adapter should exercise pattern cutter shape behavior");
      require(Boolean.TRUE.equals(activation.get("paletteConversionRoundTrip")),
         "Blockworks native adapter should exercise palette conversion behavior");
      require(Boolean.TRUE.equals(activation.get("worldgenSiteRoundTrip")),
         "Blockworks native adapter should exercise worldgen site behavior");
      System.out.println("blockworks native adapter smoke PASS contracts=" + CONTRACT_IDS.size());
   }

   private Map<String, Object> exerciseReferenceBehavior() {
      Optional<BlockworksBlockInfo> ashstoneBrick = BlockworksCatalog.blockInfo("ashstone_brick");
      Optional<BlockworksBlockInfo> ashstoneBrickSlab = BlockworksCatalog.target("ashstone", "brick", BlockworksShapeKind.SLAB);
      Optional<BlockworksBlockInfo> reinforcedPanelSlab = BlockworksCatalog.target("reinforced_metal", "panel", BlockworksShapeKind.SLAB);
      Optional<BlockworksPaletteKit> ashfallKit = BlockworksCatalog.paletteKit("ashfall_ruined_city");
      Optional<BlockworksBlockInfo> nextAshstoneShape = ashstoneBrick.flatMap(block -> BlockworksCatalog.cycle(block, false));
      List<BlockworksBlockInfo> conversionTargets = ashstoneBrick
         .map(BlockworksCatalog::conversionTargets)
         .orElse(List.of());
      List<BlockworksBlockInfo> kitTargets = ashstoneBrick
         .flatMap(block -> ashfallKit.map(kit -> BlockworksCatalog.conversionTargets(block, kit)))
         .orElse(List.of());

      Map<String, Object> result = new LinkedHashMap<>();
      result.put("familyCount", BlockworksCatalog.families().size());
      result.put("blockCount", BlockworksCatalog.blockInfos().size());
      result.put("detailCount", BlockworksCatalog.details().size());
      result.put("paletteKitCount", BlockworksCatalog.paletteKits().size());
      result.put("worldgenSiteCount", BlockworksCatalog.worldgenSites().size());
      result.put("blockCatalogRoundTrip", ashstoneBrick
         .map(block -> block.displayName().equals("Ashstone Brick")
            && BlockworksCatalog.blockInfos().size() > BlockworksCatalog.families().size())
         .orElse(false));
      result.put("patternCutterRoundTrip", ashstoneBrickSlab.isPresent()
         && reinforcedPanelSlab.isPresent()
         && nextAshstoneShape.map(block -> !block.blockId().equals("ashstone_brick")).orElse(false));
      result.put("paletteConversionRoundTrip", ashfallKit
         .map(kit -> kit.includesFamily("ashstone")
            && kit.includesBlock("ashstone_cracked_brick")
            && conversionTargets.size() > 1
            && kitTargets.stream().anyMatch(block -> block.blockId().equals("ashstone_cracked_brick")))
         .orElse(false));
      result.put("showcaseSiteRoundTrip", BlockworksCatalog.worldgenSites().stream()
         .anyMatch(site -> site.id().equals("ashfall_street_ruin")
            && site.structureTemplate().equals("showcase/ashfall_street_ruin")));
      result.put("worldgenSiteRoundTrip", ashfallKit
         .flatMap(BlockworksPaletteKit::worldgenSiteId)
         .map("ashfall_street_ruin"::equals)
         .orElse(false));
      result.put("sampleBlockId", ashstoneBrick.map(BlockworksBlockInfo::blockId).orElse("missing"));
      result.put("sampleSlabId", ashstoneBrickSlab.map(BlockworksBlockInfo::blockId).orElse("missing"));
      result.put("samplePaletteId", ashfallKit.map(BlockworksPaletteKit::id).orElse("missing"));
      return Map.copyOf(result);
   }

   private static void require(boolean condition, String message) {
      if (!condition) {
         throw new AssertionError(message);
      }
   }
}
