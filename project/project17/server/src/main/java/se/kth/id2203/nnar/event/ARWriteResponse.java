package se.kth.id2203.nnar.event;

import se.sics.kompics.KompicsEvent;

import java.io.Serializable;
import java.util.UUID;

public class ARWriteResponse implements KompicsEvent, Serializable
{
    private UUID opId;

    public ARWriteResponse(UUID opId)
    {
        this.opId = opId;
    }

    public UUID getOpId()
    {
        return opId;
    }

    @Override
    public String toString()
    {
        return "ARWriteResponse{" +
                "opId=" + opId +
                '}';
    }
}
