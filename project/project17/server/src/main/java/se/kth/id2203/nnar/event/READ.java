package se.kth.id2203.nnar.event;

import se.sics.kompics.KompicsEvent;

public class READ implements KompicsEvent {

    private int rId;

    public READ(int rId) {
        this.rId = rId;
    }

    public int getrId() {
        return this.rId;
    }
}
