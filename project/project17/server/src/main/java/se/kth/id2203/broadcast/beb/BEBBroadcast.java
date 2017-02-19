package se.kth.id2203.broadcast.beb;

import se.sics.kompics.KompicsEvent;

import java.io.Serializable;

public class BEBBroadcast implements KompicsEvent, Serializable
{
    public final KompicsEvent payload;

    public BEBBroadcast(KompicsEvent payload)
    {
        this.payload = payload;
    }
}
