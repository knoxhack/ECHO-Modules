package com.knoxhack.echoruntimeguard.runtime;

import com.knoxhack.echoruntimeguard.RuntimeGuardConfig;
import com.knoxhack.echoruntimeguard.api.ClientMetricsSnapshot;
import com.knoxhack.echoruntimeguard.api.RuntimeMetricsSnapshot;
import com.knoxhack.echoruntimeguard.api.RuntimeMode;

public final class RuntimeModeService {
    public static final RuntimeModeService INSTANCE = new RuntimeModeService();
    private RuntimeMode manualMode;
    private boolean forcedEmergency;
    private boolean automaticEmergency;
    private int serverPressureTicks;
    private int serverRecoveryTicks;
    private int clientPressureTicks;
    private int clientRecoveryTicks;

    private RuntimeModeService() {
    }

    public RuntimeMode configuredMode() {
        return manualMode == null ? RuntimeGuardConfig.configuredMode() : manualMode;
    }

    public RuntimeMode configuredDefaultMode() {
        return RuntimeGuardConfig.configuredMode();
    }

    public RuntimeMode manualMode() {
        return manualMode;
    }

    public RuntimeMode mode() {
        return isEmergency() ? RuntimeMode.EMERGENCY : configuredMode();
    }

    public void setMode(RuntimeMode mode) {
        manualMode = mode == null ? RuntimeMode.BALANCED : mode;
        if (manualMode != RuntimeMode.EMERGENCY) {
            forcedEmergency = false;
        } else {
            forcedEmergency = true;
        }
    }

    public void forceEmergency(boolean forced) {
        forcedEmergency = forced;
        if (!forced) {
            automaticEmergency = false;
            serverPressureTicks = 0;
            clientPressureTicks = 0;
        }
    }

    public boolean isEmergency() {
        return forcedEmergency || automaticEmergency;
    }

    public boolean forcedEmergency() {
        return forcedEmergency;
    }

    public boolean automaticEmergency() {
        return automaticEmergency;
    }

    public String modeSource() {
        if (forcedEmergency) {
            return "forced emergency";
        }
        if (automaticEmergency) {
            return "automatic emergency";
        }
        return manualMode == null ? "configured" : "manual";
    }

    public void tickServer(RuntimeMetricsSnapshot metrics) {
        if (!RuntimeGuardConfig.enabled()
                || !RuntimeGuardConfig.safeBool(RuntimeGuardConfig.AUTO_EMERGENCY_MODE, true)
                || !RuntimeGuardConfig.safeBool(RuntimeGuardConfig.TPS_GUARD_ENABLED, true)
                || metrics == null) {
            return;
        }
        int triggerTicks = RuntimeGuardConfig.safeInt(RuntimeGuardConfig.EMERGENCY_TRIGGER_SECONDS, 10) * 20;
        boolean pressured = metrics.averageTps() <= RuntimeGuardConfig.safeDouble(RuntimeGuardConfig.EMERGENCY_TPS, 15.0D)
                || metrics.averageMspt() >= 1000.0D / Math.max(1.0D, RuntimeGuardConfig.safeDouble(RuntimeGuardConfig.EMERGENCY_TPS, 15.0D));
        if (pressured) {
            serverPressureTicks++;
            serverRecoveryTicks = 0;
        } else {
            serverPressureTicks = Math.max(0, serverPressureTicks - 20);
            if (metrics.averageTps() >= RuntimeGuardConfig.safeDouble(RuntimeGuardConfig.EMERGENCY_RECOVERY_TPS, 18.5D)) {
                serverRecoveryTicks++;
            }
        }
        if (serverPressureTicks >= triggerTicks) {
            automaticEmergency = true;
        }
        if (automaticEmergency && serverRecoveryTicks >= triggerTicks) {
            automaticEmergency = false;
            serverPressureTicks = 0;
            serverRecoveryTicks = 0;
        }
    }

    public void tickClient(ClientMetricsSnapshot metrics) {
        if (!RuntimeGuardConfig.enabled()
                || !RuntimeGuardConfig.safeBool(RuntimeGuardConfig.AUTO_EMERGENCY_MODE, true) || metrics == null
                || metrics.currentFps() < 0) {
            return;
        }
        int triggerTicks = RuntimeGuardConfig.safeInt(RuntimeGuardConfig.EMERGENCY_TRIGGER_SECONDS, 10) * 20;
        int emergencyFps = RuntimeGuardConfig.safeInt(RuntimeGuardConfig.EMERGENCY_FPS, 30);
        int warningFps = RuntimeGuardConfig.safeInt(RuntimeGuardConfig.WARNING_FPS, 50);
        if (metrics.averageFps() > 0.0D && metrics.averageFps() <= emergencyFps) {
            clientPressureTicks++;
            clientRecoveryTicks = 0;
        } else {
            clientPressureTicks = Math.max(0, clientPressureTicks - 20);
            if (metrics.averageFps() >= warningFps) {
                clientRecoveryTicks++;
            }
        }
        if (clientPressureTicks >= triggerTicks) {
            automaticEmergency = true;
        }
        if (automaticEmergency && clientRecoveryTicks >= triggerTicks) {
            automaticEmergency = false;
            clientPressureTicks = 0;
            clientRecoveryTicks = 0;
        }
    }

    public String summary() {
        return mode().displayName() + " / emergency " + (isEmergency() ? "on" : "off")
                + (forcedEmergency ? " forced" : automaticEmergency ? " auto" : "");
    }

    public void reset() {
        manualMode = null;
        forcedEmergency = false;
        automaticEmergency = false;
        serverPressureTicks = 0;
        serverRecoveryTicks = 0;
        clientPressureTicks = 0;
        clientRecoveryTicks = 0;
    }
}
