package se.kth.id2203.multipaxos;

import se.kth.id2203.overlay.Topology;
import se.sics.kompics.PortType;

public class MultiPaxosPort extends PortType
{
    public MultiPaxosPort()
    {
        request(Propose.class);
        request(Topology.class);
        indication(Abort.class);
        indication(DecideResult.class);
    }
}
