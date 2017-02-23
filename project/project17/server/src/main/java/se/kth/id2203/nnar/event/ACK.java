package se.kth.id2203.nnar.event;

import se.sics.kompics.KompicsEvent;

public class ACK implements KompicsEvent {

    private int rId;

    public ACK(int rId) {
        this.rId = rId;
    }

    public int getrId() {
        return this.rId;
    }
}
