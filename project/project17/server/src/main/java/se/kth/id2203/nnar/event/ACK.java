package se.kth.id2203.nnar.event;

import se.sics.kompics.KompicsEvent;

import java.io.Serializable;

public class ACK implements KompicsEvent, Serializable
{
    private int rId;

    public ACK(int rId) {
        this.rId = rId;
    }

    public int getrId() {
        return this.rId;
    }
}
