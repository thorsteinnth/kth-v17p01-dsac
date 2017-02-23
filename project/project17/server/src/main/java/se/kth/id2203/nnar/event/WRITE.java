package se.kth.id2203.nnar.event;

import se.sics.kompics.KompicsEvent;

public class WRITE implements KompicsEvent {

    private int rId;
    private int ts;
    private int wr;

    private Object optionalWriteValue;

    public WRITE (int rId, int ts, int wr) {
        this.rId = rId;
        this.ts = ts;
        this.wr = wr;
    }

    public WRITE(int rId, int ts, int wr, Object optionalWriteValue) {
        this.rId = rId;
        this.ts = ts;
        this.wr = wr;
        this.optionalWriteValue = optionalWriteValue;
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

    public Object getOptionalWriteValue() {
        return optionalWriteValue;
    }
}
