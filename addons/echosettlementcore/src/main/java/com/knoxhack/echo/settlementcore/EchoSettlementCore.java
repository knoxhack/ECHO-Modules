package com.knoxhack.echo.settlementcore;

import com.knoxhack.echo.adaptercore.EchoBackendLifecycleBridge;
import com.knoxhack.echo.adaptercore.EchoBackendWorldEventBridge;
import com.knoxhack.echo.hazardcore.api.HazardService;
import com.knoxhack.echo.settlementcore.api.SettlementService;
import com.knoxhack.echo.settlementcore.hazard.SettlementHazardResistanceProvider;
import com.knoxhack.echo.settlementcore.registry.ModBlockEntities;
import com.knoxhack.echo.settlementcore.registry.ModBlocks;
import com.knoxhack.echo.settlementcore.registry.ModCreativeTabs;
import com.knoxhack.echo.settlementcore.registry.ModItems;
import com.knoxhack.echo.settlementcore.registry.ModMenus;
import com.knoxhack.echo.settlementcore.settlement.SettlementManager;
import com.mojang.logging.LogUtils;
import java.util.List;
import net.minecraft.server.MinecraftServer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import org.slf4j.Logger;

@Mod(EchoSettlementCore.MODID)
public final class EchoSettlementCore {
    public static final String MODID = "echosettlementcore";
    public static final Logger LOGGER = LogUtils.getLogger();

    public static final List<String> REQUIRES = List.of(
        "echocore",
        "echoadaptercore",
        "echobasegrid",
        "echonpcore",
        "echologisticscore",
        "echoworldcore",
        "echohazardcore",
        "echoequipmentcore"
    );
    public static final List<String> PROVIDES = List.of(
        "settlement.registry",
        "settlement.jobs",
        "settlement.defense_score",
        "settlement.logistics_requests"
    );
    public static final List<String> MVP_CONTRACTS = List.of(
        "settlement_snapshot",
        "npc_job_contract",
        "defense_score_contract",
        "logistics_request_contract"
    );

    private static final String COMMON_SETUP_EVENT =
        "net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent";
    private static final String SERVER_TICK_POST_EVENT =
        "net.neoforged.neoforge.event.tick.ServerTickEvent$Post";

    private EchoSettlementCore() {
        this(null);
    }

    public EchoSettlementCore(IEventBus modEventBus) {
        ModBlocks.register(modEventBus);
        ModBlockEntities.register(modEventBus);
        ModItems.register(modEventBus);
        ModMenus.register(modEventBus);
        ModCreativeTabs.register(modEventBus);

        SettlementService.getInstance();

        EchoBackendLifecycleBridge.registerModListener(modEventBus, COMMON_SETUP_EVENT, this::commonSetup);
        EchoBackendLifecycleBridge.registerGameEventHandler(SERVER_TICK_POST_EVENT, this::onServerTickEvent);
        EchoBackendLifecycleBridge.registerOptionalGameTests(modEventBus,
            "com.knoxhack.echosettlementcore.registry.ModGameTests");
    }

    private void commonSetup(Object event) {
        LOGGER.info("ECHO SettlementCore online. Habitats, jobs, and logistics are now active.");
        EchoBackendLifecycleBridge.runCommonSetupWork(event, () -> {
            HazardService.find().registerResistanceProvider(new SettlementHazardResistanceProvider());
        });
    }

    private void onServerTickEvent(Object event) {
        MinecraftServer server = EchoBackendWorldEventBridge.serverTickServer(event);
        if (server != null) {
            SettlementManager.tick(server);
        }
    }

    public String moduleId() {
        return MODID;
    }

    public List<String> provides() {
        return PROVIDES;
    }
}
