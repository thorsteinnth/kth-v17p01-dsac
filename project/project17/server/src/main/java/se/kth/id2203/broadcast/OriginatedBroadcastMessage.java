package se.kth.id2203.broadcast;

import se.kth.id2203.networking.NetAddress;
import se.sics.kompics.KompicsEvent;

import java.io.Serializable;

public class OriginatedBroadcastMessage implements KompicsEvent, Serializable
{
    public NetAddress source;
    public KompicsEvent payload;

    public OriginatedBroadcastMessage(NetAddress source, KompicsEvent payload)
    {
        this.source = source;
        this.payload = payload;
    }
}
