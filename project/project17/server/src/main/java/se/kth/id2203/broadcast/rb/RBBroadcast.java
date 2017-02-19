package se.kth.id2203.broadcast.rb;

import se.sics.kompics.KompicsEvent;

import java.io.Serializable;

public class RBBroadcast implements KompicsEvent, Serializable
{
    public final KompicsEvent payload;

    public RBBroadcast(KompicsEvent payload)
    {
        this.payload = payload;
    }
}
