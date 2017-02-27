package se.kth.id2203.nnar.event;

import se.sics.kompics.KompicsEvent;

import java.io.Serializable;
import java.util.UUID;

public class READ implements KompicsEvent, Serializable
{
    private String key;
    private UUID opId;

    public READ(String key, UUID opId)
    {
        this.key = key;
        this.opId = opId;
    }

    public String getKey()
    {
        return key;
    }

    public UUID getOpId()
    {
        return opId;
    }

    @Override
    public String toString()
    {
        return "READ{" +
                "key='" + key + '\'' +
                ", opId=" + opId +
                '}';
    }
}
