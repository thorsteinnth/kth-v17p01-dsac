package se.kth.id2203.epfd.event;

import se.sics.kompics.KompicsEvent;

import java.io.Serializable;

public class SystemStable implements KompicsEvent, Serializable {

    private boolean isStable;

    public SystemStable(boolean isStable) {
        this.isStable = isStable;
    }

    public boolean getIsStable() {
        return this.isStable;
    }
}
