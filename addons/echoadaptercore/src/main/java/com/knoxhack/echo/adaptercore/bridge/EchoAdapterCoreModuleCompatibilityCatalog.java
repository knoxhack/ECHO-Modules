package com.knoxhack.echo.adaptercore.bridge;

import com.knoxhack.echo.adaptercore.EchoRuntimeHostCapabilities;
import com.knoxhack.echo.adaptercore.EchoRuntimeHostRegistry;
import com.knoxhack.echo.adaptercore.EchoUnsupportedRuntimeHost;

import java.util.Set;

/**
 * Generated AdapterCore compatibility surface for descriptor-backed ECHO modules.
 *
 * <p>Specific module truth bridges and runtime hosts register first. This catalog only
 * fills missing module runtime-host entries with an explicit unsupported host so
 * metadata-only AdapterCore claims have concrete runtime evidence to audit.
 */
public final class EchoAdapterCoreModuleCompatibilityCatalog {
    private static final Set<String> MODULE_IDS = Set.of(
            "echoaccessibilitycore",
            "echoadaptercore",
            "echoaddonapi",
            "echoaetherworks",
            "echoagentcore",
            "echoagriculturereclamation",
            "echoarcanacore",
            "echoarcanadivisionprotocol",
            "echoarcaneindex",
            "echoarmory",
            "echoashfallprotocol",
            "echoassetcore",
            "echoassetpipeline",
            "echoatmospherecore",
            "echobalancecore",
            "echobasegrid",
            "echobiomecore",
            "echoblackboxprotocol",
            "echoblockworks",
            "echoblueprintcore",
            "echobridgecore",
            "echocameracore",
            "echocapabilitycore",
            "echocinematiccore",
            "echocodexcore",
            "echocombatcore",
            "echocommonloot",
            "echocommunitybridge",
            "echocontentcore",
            "echoconvoyprotocol",
            "echocore",
            "echocreatorcore",
            "echocreaturecore",
            "echocreatureroles",
            "echocurationcore",
            "echocursecore",
            "echodatacore",
            "echodependencydoctor",
            "echodifficultycore",
            "echodisastercore",
            "echoeconomycore",
            "echoencountercore",
            "echoequipmentcore",
            "echoeventcore",
            "echoexpeditioncore",
            "echofactioncore",
            "echofamiliarcore",
            "echofoundationcore",
            "echogalacticcore",
            "echogalacticsurveyprotocol",
            "echogrimoire",
            "echoguidecore",
            "echohazardcore",
            "echohealthcore",
            "echoholomap",
            "echohudcore",
            "echoindex",
            "echoindustrialnexus",
            "echoinputcore",
            "echolens",
            "echolocalizationcore",
            "echologisticscore",
            "echologisticsnetwork",
            "echolootcore",
            "echolorecore",
            "echomachinecore",
            "echomaterialcore",
            "echometadatacore",
            "echomigrationcore",
            "echomissioncore",
            "echomodulegraph",
            "echomultiblockcore",
            "echonetcore",
            "echonexusprotocol",
            "echonotificationcore",
            "echonpcore",
            "echoopenlandsprotocol",
            "echoorbitalremnants",
            "echopackcore",
            "echopackdiff",
            "echoplatformcore",
            "echoplayercore",
            "echoplaytestcore",
            "echopolicycore",
            "echopowercore",
            "echopowergrid",
            "echopresencelink",
            "echoprimecore",
            "echoprogressioncore",
            "echoquestdirector",
            "echorecipecore",
            "echorecovery",
            "echorelictech",
            "echorendercore",
            "echoreportcore",
            "echoriftworlds",
            "echoritualcore",
            "echoruincore",
            "echoruntimeguard",
            "echoschemacore",
            "echoscreencore",
            "echoscriptcore",
            "echoseasoncore",
            "echoserveropscore",
            "echosessioncore",
            "echosettlementcore",
            "echosignalos",
            "echoskillcore",
            "echoskyrelayprotocol",
            "echosocialcore",
            "echosoundcore",
            "echospawncore",
            "echospellcore",
            "echostationcore",
            "echostationfall",
            "echostatuscore",
            "echostructurecore",
            "echosupplycore",
            "echotelemetrycore",
            "echoterminal",
            "echoterritorycore",
            "echotextureforge",
            "echothemecore",
            "echotoolcore",
            "echotutorialcore",
            "echovalidationcore",
            "echovehiclecore",
            "echoweathercore",
            "echowiki",
            "echoworldcore",
            "echoworldstarter",
            "signalosexample"
    );

    private static boolean registered;

    private EchoAdapterCoreModuleCompatibilityCatalog() {
    }

    public static Set<String> moduleIds() {
        return MODULE_IDS;
    }

    public static synchronized void registerAll() {
        if (registered) {
            return;
        }
        registered = true;

        for (String moduleId : MODULE_IDS) {
            registerIfMissing(moduleId);
        }
    }

    private static void registerIfMissing(String moduleId) {
        String runtimeHostId = moduleId + ":runtime_host";
        if (EchoRuntimeHostRegistry.global().resolve(runtimeHostId).isPresent()) {
            return;
        }

        EchoRuntimeHostRegistry.global().register(
                new EchoUnsupportedRuntimeHost(runtimeHostId),
                EchoRuntimeHostCapabilities.of(
                        runtimeHostId,
                        Set.of("EchoNativeRuntimeHost.Events"),
                        Set.of(moduleId + ".adaptercore_compat"),
                        Set.of(moduleId)));
    }
}
