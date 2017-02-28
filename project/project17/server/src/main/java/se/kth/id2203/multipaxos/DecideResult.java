package se.kth.id2203.multipaxos;

import se.sics.kompics.KompicsEvent;

import java.io.Serializable;

public class DecideResult implements KompicsEvent, Serializable
{
    public Object object;

    public DecideResult(Object object)
    {
        this.object = object;
    }
}
