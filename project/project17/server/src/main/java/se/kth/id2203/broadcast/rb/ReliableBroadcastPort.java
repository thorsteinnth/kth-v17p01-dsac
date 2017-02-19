package se.kth.id2203.broadcast.rb;

import se.sics.kompics.PortType;

public class ReliableBroadcastPort extends PortType
{
    {
        indication(RBDeliver.class);
        request(RBBroadcast.class);
    }
}
