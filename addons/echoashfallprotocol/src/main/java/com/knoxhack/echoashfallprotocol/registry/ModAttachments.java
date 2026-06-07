package com.knoxhack.echoashfallprotocol.registry;

import com.knoxhack.echo.adaptercore.EchoAttachmentHandle;
import com.knoxhack.echo.adaptercore.EchoBackendAttachmentBridge;
import com.knoxhack.echoashfallprotocol.EchoAshfallProtocol;
import com.knoxhack.echoashfallprotocol.endgame.PostNexusData;
import com.knoxhack.echoashfallprotocol.survival.MutationData;
import com.knoxhack.echoashfallprotocol.survival.SurvivalData;
import com.knoxhack.echoashfallprotocol.echo.QuestData;
import com.knoxhack.echoashfallprotocol.event.SmartEventData;
import com.knoxhack.echoashfallprotocol.entity.drone.CompanionDroneData;
import com.knoxhack.echoashfallprotocol.faction.AshfallFactionContractData;
import com.knoxhack.echoashfallprotocol.research.ResearchData;
import com.knoxhack.echoashfallprotocol.survival.ColdData;
import com.knoxhack.echoashfallprotocol.survival.CombatData;
import com.knoxhack.echoashfallprotocol.survival.PlayerTechTracker;
import com.knoxhack.echoashfallprotocol.world.FieldOpsData;

public class ModAttachments {
    public static final Object ATTACHMENT_TYPES =
            EchoBackendAttachmentBridge.createAttachmentRegistry(EchoAshfallProtocol.MODID);

    public static final EchoAttachmentHandle<SurvivalData> SURVIVAL_DATA = EchoBackendAttachmentBridge.registerSyncedCopyOnDeath(
            ATTACHMENT_TYPES,
            "survival_data",
            SurvivalData::new,
            SurvivalData.STREAM_CODEC
    );

    public static final EchoAttachmentHandle<CombatData> COMBAT_DATA = EchoBackendAttachmentBridge.registerSerializable(
            ATTACHMENT_TYPES,
            "combat_data",
            CombatData::new
    );

    public static final EchoAttachmentHandle<MutationData> MUTATION_DATA = EchoBackendAttachmentBridge.registerSerializableCopyOnDeath(
            ATTACHMENT_TYPES,
            "mutation_data",
            MutationData::new
    );

    public static final EchoAttachmentHandle<QuestData> QUEST_DATA = EchoBackendAttachmentBridge.registerSyncedCopyOnDeath(
            ATTACHMENT_TYPES,
            "quest_data",
            QuestData::new,
            QuestData.STREAM_CODEC
    );

    public static final EchoAttachmentHandle<SmartEventData> SMART_EVENT_DATA = EchoBackendAttachmentBridge.registerSerializable(
            ATTACHMENT_TYPES,
            "smart_event_data",
            SmartEventData::new
    );

    public static final EchoAttachmentHandle<ResearchData> RESEARCH_DATA = EchoBackendAttachmentBridge.registerSyncedCopyOnDeath(
            ATTACHMENT_TYPES,
            "research_data",
            ResearchData::new,
            ResearchData.STREAM_CODEC
    );

    public static final EchoAttachmentHandle<ColdData> COLD_DATA = EchoBackendAttachmentBridge.registerSyncedCopyOnDeath(
            ATTACHMENT_TYPES,
            "cold_data",
            ColdData::new,
            ColdData.STREAM_CODEC
    );

    public static final EchoAttachmentHandle<PlayerTechTracker.PlayerTechData> PLAYER_TECH_DATA =
            EchoBackendAttachmentBridge.registerSerializableCopyOnDeath(
            ATTACHMENT_TYPES,
            "player_tech_data",
            PlayerTechTracker.PlayerTechData::new
    );

    public static final EchoAttachmentHandle<com.knoxhack.echoashfallprotocol.fasttravel.RadioNetwork> RADIO_NETWORK =
            EchoBackendAttachmentBridge.registerSerializableCopyOnDeath(
            ATTACHMENT_TYPES,
            "radio_network",
            com.knoxhack.echoashfallprotocol.fasttravel.RadioNetwork::new
    );

    public static final EchoAttachmentHandle<com.knoxhack.echoashfallprotocol.data.MigrationData> MIGRATION_DATA =
            EchoBackendAttachmentBridge.registerCodecCopyOnDeath(
            ATTACHMENT_TYPES,
            "migration_data",
            com.knoxhack.echoashfallprotocol.data.MigrationData::new,
            com.knoxhack.echoashfallprotocol.data.MigrationData.CODEC
    );

    public static final EchoAttachmentHandle<PostNexusData> POST_NEXUS_DATA = EchoBackendAttachmentBridge.registerSyncedCopyOnDeath(
            ATTACHMENT_TYPES,
            "post_nexus_data",
            PostNexusData::new,
            PostNexusData.STREAM_CODEC
    );

    public static final EchoAttachmentHandle<FieldOpsData> FIELD_OPS_DATA = EchoBackendAttachmentBridge.registerSerializableCopyOnDeath(
            ATTACHMENT_TYPES,
            "field_ops_data",
            FieldOpsData::new
    );

    public static final EchoAttachmentHandle<CompanionDroneData> COMPANION_DRONE_DATA = EchoBackendAttachmentBridge.registerSyncedCopyOnDeath(
            ATTACHMENT_TYPES,
            "companion_drone_data",
            CompanionDroneData::new,
            CompanionDroneData.STREAM_CODEC
    );
    
    // --- Deeper Factions System ---
    
    public static final EchoAttachmentHandle<com.knoxhack.echoashfallprotocol.faction.FactionDiplomacy> FACTION_DIPLOMACY =
            EchoBackendAttachmentBridge.registerSyncedCopyOnDeath(
            ATTACHMENT_TYPES,
            "faction_diplomacy",
            com.knoxhack.echoashfallprotocol.faction.FactionDiplomacy::new,
            com.knoxhack.echoashfallprotocol.faction.FactionDiplomacy.STREAM_CODEC
    );
    
    public static final EchoAttachmentHandle<com.knoxhack.echoashfallprotocol.echo.EchoIntel> ECHO_INTEL =
            EchoBackendAttachmentBridge.registerSyncedCopyOnDeath(
            ATTACHMENT_TYPES,
            "echo_intel",
            com.knoxhack.echoashfallprotocol.echo.EchoIntel::new,
            com.knoxhack.echoashfallprotocol.echo.EchoIntel.STREAM_CODEC
    );
    
    public static final EchoAttachmentHandle<com.knoxhack.echoashfallprotocol.faction.FactionTerritory> FACTION_TERRITORY =
            EchoBackendAttachmentBridge.registerSyncedCopyOnDeath(
            ATTACHMENT_TYPES,
            "faction_territory",
            com.knoxhack.echoashfallprotocol.faction.FactionTerritory::new,
            com.knoxhack.echoashfallprotocol.faction.FactionTerritory.STREAM_CODEC
    );

    public static final EchoAttachmentHandle<AshfallFactionContractData> ASHFALL_FACTION_CONTRACT_DATA =
            EchoBackendAttachmentBridge.registerSyncedCopyOnDeath(
            ATTACHMENT_TYPES,
            "ashfall_faction_contract_data",
            AshfallFactionContractData::new,
            AshfallFactionContractData.STREAM_CODEC
    );
}
