package se.kth.id2203.broadcast.rb;

import se.kth.id2203.networking.NetAddress;
import se.sics.kompics.KompicsEvent;

public class RBDeliver implements KompicsEvent
{
    public final NetAddress source;
    public final KompicsEvent payload;

    public RBDeliver(NetAddress source, KompicsEvent payload)
    {
        this.source = source;
        this.payload = payload;
    }
}
