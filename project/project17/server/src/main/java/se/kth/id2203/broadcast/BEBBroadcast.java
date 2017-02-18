package se.kth.id2203.broadcast;

import se.sics.kompics.KompicsEvent;

public class BEBBroadcast implements KompicsEvent
{
    public final KompicsEvent payload;

    public BEBBroadcast(KompicsEvent payload)
    {
        this.payload = payload;
    }
}
