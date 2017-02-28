package se.kth.id2203.multipaxos;

import se.sics.kompics.PortType;

public class MultiPaxosPort extends PortType
{
    public MultiPaxosPort()
    {
        request(Propose.class);
    }
}
