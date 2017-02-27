package se.kth.id2203.nnar.event;

import se.sics.kompics.KompicsEvent;

import java.io.Serializable;
import java.util.UUID;

public class VALUE implements KompicsEvent, Serializable
{
    private String key;
    private UUID opId;
    private int ts;
    private int wr;
    private Object optionalValue;

    public VALUE(String key, UUID opId, int ts, int wr)
    {
        this.key = key;
        this.opId = opId;
        this.ts = ts;
        this.wr = wr;
    }

    public VALUE(String key, UUID opId, int ts, int wr, Object optionalValue)
    {
        this.key = key;
        this.opId = opId;
        this.ts = ts;
        this.wr = wr;
        this.optionalValue = optionalValue;
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

    public Object getOptionalValue()
    {
        return optionalValue;
    }

    @Override
    public String toString()
    {
        return "VALUE{" +
                "key='" + key + '\'' +
                ", opId=" + opId +
                ", ts=" + ts +
                ", wr=" + wr +
                ", optionalValue=" + optionalValue +
                '}';
    }
}
