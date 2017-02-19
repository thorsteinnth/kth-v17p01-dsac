package se.kth.id2203.broadcast.rb;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.kth.id2203.broadcast.OriginatedBroadcastMessage;
import se.kth.id2203.broadcast.beb.BEBBroadcast;
import se.kth.id2203.broadcast.beb.BestEffortBroadcastPort;
import se.kth.id2203.networking.NetAddress;
import se.sics.kompics.*;

import java.util.HashSet;

public class ReliableBroadcast extends ComponentDefinition
{
    // Eager reliable broadcast

    private final static Logger LOG = LoggerFactory.getLogger(ReliableBroadcast.class);

    // Ports
    private final Negative<ReliableBroadcastPort> rb = provides(ReliableBroadcastPort.class);
    private final Positive<BestEffortBroadcastPort> beb = requires(BestEffortBroadcastPort.class);

    // Fields
    private final NetAddress self = config().getValue("id2203.project.address", NetAddress.class);
    private HashSet<KompicsEvent> delivered = new HashSet<>();

    //region Handlers

    private final Handler<RBBroadcast> broadcastRequestHandler = new Handler<RBBroadcast>()
    {
        @Override
        public void handle(RBBroadcast rbBroadcast)
        {
            LOG.info("Will reliably broadcast: " + rbBroadcast.payload);
            trigger(new BEBBroadcast(new OriginatedBroadcastMessage(self, rbBroadcast.payload)), beb);
        }
    };

    //endregion

    {
        subscribe(broadcastRequestHandler, rb);
    }
}
