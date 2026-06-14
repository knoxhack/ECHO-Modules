package com.knoxhack.echo.hazardcore;

import com.knoxhack.echo.adaptercore.EchoBackendLifecycleBridge;
import com.knoxhack.echo.hazardcore.api.HazardService;
import com.knoxhack.echo.hazardcore.event.HazardPlayerTickHandler;
import com.knoxhack.echo.hazardcore.source.BlockCorruptionSource;
import com.knoxhack.echo.hazardcore.source.DecompressionSicknessSource;
import com.knoxhack.echo.hazardcore.source.DepthPressureSource;
import com.knoxhack.echo.hazardcore.source.OxygenDeprivationSource;
import com.knoxhack.echo.hazardcore.source.ThermalSource;
import java.util.List;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;

@Mod(EchoHazardCore.MODID)
public final class EchoHazardCore {
    public static final String MODID = "echohazardcore";
    public static final List<String> REQUIRES = List.of(
            "echocore",
            "echoadaptercore",
            "echostatuscore",
            "echohealthcore",
            "echoweathercore",
            "echoworldcore"
        );
    public static final List<String> PROVIDES = List.of(
            "hazard.registry",
            "hazard.exposure",
            "hazard.resistance",
            "hazard.world_hooks"
        );
    public static final List<String> MVP_CONTRACTS = List.of(
            "hazard_registry",
            "exposure_contract",
            "resistance_contract",
            "world_hazard_hooks"
        );

    private EchoHazardCore() {
        this(null);
    }

    public EchoHazardCore(IEventBus modEventBus) {
        bootstrap(modEventBus);
    }

    public void bootstrap(IEventBus modEventBus) {
        HazardService service = HazardService.getInstance();
        service.registerSource(DepthPressureSource.INSTANCE);
        service.registerSource(OxygenDeprivationSource.INSTANCE);
        service.registerSource(ThermalSource.INSTANCE);
        service.registerSource(BlockCorruptionSource.INSTANCE);
        service.registerSource(DecompressionSicknessSource.INSTANCE);

        if (modEventBus != null) {
            EchoBackendLifecycleBridge.registerGameEventHandler(
                    "net.neoforged.neoforge.event.tick.PlayerTickEvent$Post",
                    HazardPlayerTickHandler::onPlayerTick);
        }
    }

    public String moduleId() {
        return MODID;
    }

    public List<String> provides() {
        return PROVIDES;
    }
}
