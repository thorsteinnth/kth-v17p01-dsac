package se.kth.id2203.overlay;

import se.kth.id2203.networking.NetAddress;
import se.sics.kompics.KompicsEvent;

import java.util.Set;

public class Topology implements KompicsEvent
{
    public final Set<NetAddress> nodes;

    public Topology(Set<NetAddress> nodes)
    {
        this.nodes = nodes;
    }
}
