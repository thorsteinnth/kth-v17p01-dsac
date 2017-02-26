package se.kth.id2203.nnar.event;

import se.sics.kompics.KompicsEvent;

import java.io.Serializable;

public class VALUE implements KompicsEvent, Serializable
{
    private int rId;
    private int ts;
    private int wr;
    private Object optionalValue;

    public VALUE(int rId, int ts, int wr) {
        this.rId = rId;
        this.ts = ts;
        this.wr = wr;
    }

    public VALUE(int rId, int ts, int wr, Object optionalValue) {
        this.rId = rId;
        this.ts = ts;
        this.wr = wr;
        this.optionalValue = optionalValue;
    }

    public int getrId() {
        return rId;
    }

    public int getTs() {
        return ts;
    }

    public int getWr() {
        return wr;
    }

    public Object getOptionalValue() {
        return optionalValue;
    }
}
