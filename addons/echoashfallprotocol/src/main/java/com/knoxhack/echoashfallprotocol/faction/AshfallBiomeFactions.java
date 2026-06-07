package com.knoxhack.echoashfallprotocol.faction;

import com.knoxhack.echocore.api.EchoCoreServices;
import com.knoxhack.echocore.api.EchoDialogueTree;
import com.knoxhack.echocore.api.EchoFactionAction;
import com.knoxhack.echocore.api.EchoFactionContract;
import com.knoxhack.echocore.api.EchoFactionDefinition;
import com.knoxhack.echocore.api.EchoFactionPoiAffinity;
import com.knoxhack.echocore.api.EchoNpcRole;
import com.knoxhack.echoashfallprotocol.EchoAshfallProtocol;
import java.util.List;
import net.minecraft.resources.Identifier;

/**
 * Ashfall-owned faction definitions exposed through Echo Core.
 */
public final class AshfallBiomeFactions {
    public static final Identifier SURVIVOR_NETWORK = id("survivor_network");
    public static final Identifier ASHLAND_RANGERS = id("ashland_rangers");
    public static final Identifier DUSTLINE_FREEHOLDS = id("dustline_freeholds");
    public static final Identifier METRO_ARCHIVISTS = id("metro_archivists");
    public static final Identifier RUSTWORKS_UNION = id("rustworks_union");
    public static final Identifier SPOREBOUND_SANCTUM = id("sporebound_sanctum");
    public static final Identifier CRASHBREAK_SALVAGE = id("crashbreak_salvage");
    public static final Identifier RADWARDEN_COMPACT = id("radwarden_compact");
    public static final Identifier THAWBOUND_COLLECTIVE = id("thawbound_collective");
    public static final Identifier SCARBOUND_CONCLAVE = id("scarbound_conclave");

    private AshfallBiomeFactions() {
    }

    public static void register() {
        registerFaction("radwarden_compact", "Radwarden Compact", "Radwardens", "Containment Route",
                "Containment crews, old rangers, thaw technicians, and orbital wardens united around one rule: mark what can kill you before it kills someone else.",
                "Radiation, cryogenic failure, exposure fronts, and sealed military doctrine", "Rad meds, filters, warm gear, and a charged scanner",
                "Shelter routing, decon warnings, thermal support, and guardian containment briefs", 0xD8D65F, true,
                roles("Quartermaster", "Containment Guard", "Decon Tech"),
                "Compact Perimeter", "Reinforce a shelter or hazard perimeter before the hot zone edits another map.",
                "Scan a containment, shelter, or cryogenic route marker.", "Radwarden warning codes and field support.",
                List.of("survivor_cache", "radiation_zone", "cryogenic_ruins"));

        registerFaction("crashbreak_salvage", "Crashbreak Salvage", "Crashbreak", "Salvage Route",
                "Wreck crews, freehold brokers, archivists, and factory hands who decided civilization comes back one recovered part at a time.",
                "Wreck interiors, city blind spots, industrial machinery, and road-bandit pressure", "Fire resistance, blocks, spare tools, and a backup weapon",
                "Blackbox pulls, route manifests, workshop access, convoy context, and salvage contracts", 0xE5B85C, true,
                roles("Hull Cutter", "Route Broker", "Loadmaster"),
                "Blackbox Pull", "Recover a signal core or route record before scavengers strip out the last useful witness.",
                "Scan a crash, city, industrial, or convoy POI.", "Priority salvage callouts and workshop credit.",
                List.of("crash_zone_wasteland", "ruined_cityscape", "industrial_ruins"));

        registerFaction("sporebound_sanctum", "Sporebound Sanctum", "Sanctum", "Adaptation Route",
                "Adaptation circles, scar readers, and anomaly witnesses studying living contamination because fire cannot explain every wound the world kept.",
                "Spores, poison clouds, unstable bioforms, and Nexus shear", "Antitoxin, mask, anchors, and ranged control",
                "Biomass analysis, antitoxin priority, anomaly interpretation, and scar-route warnings", 0x68B86A, true,
                roles("Spore Tender", "Antitoxin Brewer", "Scar Reader"),
                "Spore Witness", "Gather a clean bio-anomaly read without letting the route learn your lungs or your name.",
                "Scan a toxic, bio, or Nexus scar POI.", "Sanctum antidotes and anomaly-route interpretation.",
                List.of("toxic_swamp", "bio_lab", "nexus_scar"));
    }

    private static void registerFaction(String path, String displayName, String shortName, String route,
            String summary, String hazard, String prepHint, String services, int accentColor, boolean landmark,
            List<EchoNpcRole> roles, String contractTitle, String contractSummary, String objective,
            String reward, List<String> poiProfiles) {
        Identifier factionId = id(path);
        EchoCoreServices.registerFaction(new EchoFactionDefinition(
                factionId,
                displayName,
                shortName,
                route,
                summary,
                hazard,
                prepHint,
                services,
                accentColor,
                landmark,
                roles,
                List.of(
                        new EchoFactionAction(id(path + "_talk"), "Open Dialogue", "Ask for local route context.", 0, false),
                        new EchoFactionAction(id(path + "_service"), "Request Service", services, 10, true),
                        new EchoFactionAction(id(path + "_contract"), "Review Contract", "Check the current field proof and route reward.", 0, false)),
                // Contract ids still follow path + "_field_contract", plus trusted/aligned tiers.
                AshfallFactionContracts.echoContracts(path, contractTitle, contractSummary, objective, reward, route),
                AshfallFactionContracts.poiTargets(path).stream()
                        .map(profile -> new EchoFactionPoiAffinity(profile, route, landmark ? 3 : 2, landmark))
                        .toList(),
                new EchoDialogueTree(
                        "Signal recognized. " + displayName + " keeps this route marked.",
                        List.of("Standing", "Services", "Route Risks", "Contracts"),
                        "Keep the route marked and the exit closer than pride.")));
    }

    private static List<EchoNpcRole> roles(String first, String second, String third) {
        return List.of(
                new EchoNpcRole(key(first), first, first + " route context and contact memory."),
                new EchoNpcRole(key(second), second, second + " service access and local hazard advice."),
                new EchoNpcRole(key(third), third, third + " contract proof and POI leads."));
    }

    private static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(EchoAshfallProtocol.MODID, path);
    }

    private static String key(String label) {
        return label.toLowerCase(java.util.Locale.ROOT).replaceAll("[^a-z0-9]+", "_").replaceAll("_+$", "");
    }
}
