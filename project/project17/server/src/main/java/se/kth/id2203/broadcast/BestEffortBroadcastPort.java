package se.kth.id2203.broadcast;

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
