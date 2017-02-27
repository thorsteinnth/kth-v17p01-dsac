package se.kth.id2203.nnar.event;

import se.sics.kompics.KompicsEvent;

import java.io.Serializable;
import java.util.UUID;

public class ACK implements KompicsEvent, Serializable
{
    private UUID opId;

    public ACK(UUID opId)
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
        return "ACK{" +
                "opId=" + opId +
                '}';
    }
}
