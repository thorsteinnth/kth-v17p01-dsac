package se.kth.id2203.nnar.event;

import se.sics.kompics.KompicsEvent;

import java.io.Serializable;
import java.util.UUID;

public class ARReadResponse implements KompicsEvent, Serializable
{
    private UUID opId;
    private Object value;

    public ARReadResponse(UUID opId)
    {
        this.opId = opId;
    }

    public ARReadResponse(UUID opId, Object value)
    {
        this.opId = opId;
        this.value = value;
    }

    public UUID getOpId()
    {
        return opId;
    }

    public Object getValue()
    {
        return this.value;
    }

    @Override
    public String toString()
    {
        return "ARReadResponse{" +
                "opId=" + opId +
                ", value=" + value +
                '}';
    }
}
