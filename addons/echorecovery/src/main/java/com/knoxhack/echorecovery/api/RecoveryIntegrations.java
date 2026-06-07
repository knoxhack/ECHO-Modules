package com.knoxhack.echorecovery.api;

import com.knoxhack.echorecovery.EchoRecovery;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

public final class RecoveryIntegrations {
    private static final List<RecoveryEventHooks> EVENT_HOOKS = new CopyOnWriteArrayList<>();
    private static final List<RecoverySignalProvider> SIGNAL_PROVIDERS = new CopyOnWriteArrayList<>();
    private static final List<RecoveryRemoteDeliveryProvider> DELIVERY_PROVIDERS = new CopyOnWriteArrayList<>();
    private static final List<GravePlacementProvider> PLACEMENT_PROVIDERS = new CopyOnWriteArrayList<>();

    private RecoveryIntegrations() {
    }

    public static void registerEventHooks(RecoveryEventHooks hooks) {
        if (hooks != null && !EVENT_HOOKS.contains(hooks)) {
            EVENT_HOOKS.add(hooks);
        }
    }

    public static void registerSignalProvider(RecoverySignalProvider provider) {
        if (provider != null && !SIGNAL_PROVIDERS.contains(provider)) {
            SIGNAL_PROVIDERS.add(provider);
        }
    }

    public static void registerRemoteDeliveryProvider(RecoveryRemoteDeliveryProvider provider) {
        if (provider != null && !DELIVERY_PROVIDERS.contains(provider)) {
            DELIVERY_PROVIDERS.add(provider);
        }
    }

    public static void registerPlacementProvider(GravePlacementProvider provider) {
        if (provider != null && !PLACEMENT_PROVIDERS.contains(provider)) {
            PLACEMENT_PROVIDERS.add(provider);
        }
    }

    public static void graveCreated(ServerPlayer player, RecoveryGraveSnapshot snapshot) {
        for (RecoveryEventHooks hooks : EVENT_HOOKS) {
            try {
                hooks.graveCreated(player, snapshot);
            } catch (RuntimeException exception) {
                EchoRecovery.LOGGER.debug("Recovery grave-created hook failed: {}", exception.getMessage());
            }
        }
    }

    public static void graveOpened(ServerPlayer player, RecoveryGraveSnapshot snapshot) {
        for (RecoveryEventHooks hooks : EVENT_HOOKS) {
            try {
                hooks.graveOpened(player, snapshot);
            } catch (RuntimeException exception) {
                EchoRecovery.LOGGER.debug("Recovery grave-opened hook failed: {}", exception.getMessage());
            }
        }
    }

    public static void graveRecovered(ServerPlayer player, RecoveryGraveSnapshot snapshot) {
        for (RecoveryEventHooks hooks : EVENT_HOOKS) {
            try {
                hooks.graveRecovered(player, snapshot);
            } catch (RuntimeException exception) {
                EchoRecovery.LOGGER.debug("Recovery grave-recovered hook failed: {}", exception.getMessage());
            }
        }
    }

    public static void graveExpired(ServerPlayer player, RecoveryGraveSnapshot snapshot) {
        for (RecoveryEventHooks hooks : EVENT_HOOKS) {
            try {
                hooks.graveExpired(player, snapshot);
            } catch (RuntimeException exception) {
                EchoRecovery.LOGGER.debug("Recovery grave-expired hook failed: {}", exception.getMessage());
            }
        }
    }

    public static void graveDeleted(ServerPlayer player, RecoveryGraveSnapshot snapshot) {
        for (RecoveryEventHooks hooks : EVENT_HOOKS) {
            try {
                hooks.graveDeleted(player, snapshot.pos(), snapshot.graveId());
            } catch (RuntimeException exception) {
                EchoRecovery.LOGGER.debug("Recovery grave-deleted hook failed: {}", exception.getMessage());
            }
        }
    }

    public static void remoteRecoveryRequested(ServerPlayer player, RecoveryGraveSnapshot snapshot) {
        for (RecoveryEventHooks hooks : EVENT_HOOKS) {
            try {
                hooks.remoteRecoveryRequested(player, snapshot);
            } catch (RuntimeException exception) {
                EchoRecovery.LOGGER.debug("Recovery remote-request hook failed: {}", exception.getMessage());
            }
        }
    }

    public static void remoteRecoveryCompleted(ServerPlayer player, RecoveryGraveSnapshot snapshot, boolean success) {
        for (RecoveryEventHooks hooks : EVENT_HOOKS) {
            try {
                hooks.remoteRecoveryCompleted(player, snapshot, success);
            } catch (RuntimeException exception) {
                EchoRecovery.LOGGER.debug("Recovery remote-complete hook failed: {}", exception.getMessage());
            }
        }
    }

    public static Optional<String> signalStatus(ServerPlayer player, RecoveryGraveSnapshot snapshot) {
        for (RecoverySignalProvider provider : SIGNAL_PROVIDERS) {
            try {
                Optional<String> status = provider.signalStatus(player, snapshot);
                if (status.isPresent()) {
                    return status;
                }
            } catch (RuntimeException exception) {
                EchoRecovery.LOGGER.debug("Recovery signal provider failed: {}", exception.getMessage());
            }
        }
        return Optional.empty();
    }

    public static Optional<String> requestDelivery(ServerPlayer player, RecoveryGraveSnapshot snapshot) {
        for (RecoveryRemoteDeliveryProvider provider : DELIVERY_PROVIDERS) {
            try {
                Optional<String> result = provider.requestDelivery(player, snapshot);
                if (result.isPresent()) {
                    return result;
                }
            } catch (RuntimeException exception) {
                EchoRecovery.LOGGER.debug("Recovery remote delivery provider failed: {}", exception.getMessage());
            }
        }
        return Optional.empty();
    }

    public static Optional<BlockPos> findPlacement(ServerPlayer player, ServerLevel level, BlockPos origin, String deathCause) {
        for (GravePlacementProvider provider : PLACEMENT_PROVIDERS) {
            try {
                Optional<BlockPos> pos = provider.findPlacement(player, level, origin, deathCause);
                if (pos.isPresent()) {
                    return pos;
                }
            } catch (RuntimeException exception) {
                EchoRecovery.LOGGER.debug("Recovery placement provider failed: {}", exception.getMessage());
            }
        }
        return Optional.empty();
    }
}
