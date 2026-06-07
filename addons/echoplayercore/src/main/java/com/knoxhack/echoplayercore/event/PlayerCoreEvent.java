package com.knoxhack.echoplayercore.event;

public abstract class PlayerCoreEvent {
    private boolean canceled;

    public final boolean isCanceled() {
        return canceled;
    }

    public final void setCanceled(boolean canceled) {
        this.canceled = canceled;
    }
}
