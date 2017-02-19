package se.kth.id2203.broadcast.beb;

import se.kth.id2203.overlay.Topology;
import se.sics.kompics.PortType;

public class BestEffortBroadcastPort extends PortType
{
    {
        indication(BEBDeliver.class);
        request(BEBBroadcast.class);
        request(Topology.class);
    }
}
