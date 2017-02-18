package se.kth.id2203.broadcast;

import se.kth.id2203.networking.NetAddress;
import se.sics.kompics.KompicsEvent;

public class BEBDeliver implements KompicsEvent
{
    public final NetAddress source;
    public final KompicsEvent payload;

    public BEBDeliver(NetAddress source, KompicsEvent payload)
    {
        this.source = source;
        this.payload = payload;
    }
}
