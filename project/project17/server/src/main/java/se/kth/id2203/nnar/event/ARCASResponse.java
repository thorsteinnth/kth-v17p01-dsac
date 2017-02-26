package se.kth.id2203.nnar.event;

import se.sics.kompics.KompicsEvent;

import java.io.Serializable;

public class ARCASResponse  implements KompicsEvent, Serializable {

    private boolean success;

    public ARCASResponse(boolean success) {
        this.success = success;
    }

    public boolean getSuccess() {
        return this.success;
    }
}
