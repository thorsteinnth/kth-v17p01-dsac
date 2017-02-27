package se.kth.id2203.nnar.event;

import se.sics.kompics.KompicsEvent;

import java.io.Serializable;
import java.util.UUID;

public class WRITE implements KompicsEvent, Serializable
{
    private String key;
    private UUID opId;
    private int ts;
    private int wr;

    private Object optionalWriteValue;

    public WRITE(String key, UUID opId, int ts, int wr)
    {
        this.key = key;
        this.opId = opId;
        this.ts = ts;
        this.wr = wr;
    }

    public WRITE(String key, UUID opId, int ts, int wr, Object optionalWriteValue)
    {
        this.key = key;
        this.opId = opId;
        this.ts = ts;
        this.wr = wr;
        this.optionalWriteValue = optionalWriteValue;
    }

    public String getKey()
    {
        return key;
    }

    public UUID getOpId()
    {
        return opId;
    }

    public int getTs()
    {
        return ts;
    }

    public int getWr()
    {
        return wr;
    }

    public Object getOptionalWriteValue()
    {
        return optionalWriteValue;
    }

    @Override
    public String toString()
    {
        return "WRITE{" +
                "key='" + key + '\'' +
                ", opId=" + opId +
                ", ts=" + ts +
                ", wr=" + wr +
                ", optionalWriteValue=" + optionalWriteValue +
                '}';
    }
}
