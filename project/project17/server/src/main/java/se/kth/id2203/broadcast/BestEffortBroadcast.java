package se.kth.id2203.broadcast;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.kth.id2203.networking.Message;
import se.kth.id2203.networking.NetAddress;
import se.kth.id2203.overlay.Topology;
import se.sics.kompics.ComponentDefinition;
import se.sics.kompics.Handler;
import se.sics.kompics.Negative;
import se.sics.kompics.Positive;
import se.sics.kompics.network.Network;

import java.util.HashSet;
import java.util.Set;

public class BestEffortBroadcast extends ComponentDefinition
{
    private final static Logger LOG = LoggerFactory.getLogger(BestEffortBroadcast.class);

    // Ports
    private final Negative<BestEffortBroadcastPort> beb = provides(BestEffortBroadcastPort.class);
    private final Positive<Network> net = requires(Network.class);

    // Fields
    private final NetAddress self = config().getValue("id2203.project.address", NetAddress.class);
    private Set<NetAddress> topology = new HashSet<>();

    //region Handlers

    private final Handler<Topology> topologyHandler = new Handler<Topology>()
    {
        @Override
        public void handle(Topology e)
        {
            LOG.info("Received topology: " + e.nodes);
            topology = e.nodes;
        }
    };

    private final Handler<BEBBroadcast> broadcastRequestHandler = new Handler<BEBBroadcast>()
    {
        @Override
        public void handle(BEBBroadcast e)
        {
            LOG.info("Will best effort broadcast: " + e.payload);

            for (NetAddress node : topology)
            {
                trigger(new Message(self, node, e), net);
            }
        }
    };

    private final Handler<Message> broadcastIncomingHandler = new Handler<Message>()
    {
        // TODO Is it safe to receive just the Message class here?
        // TODO NOPE. Am receiving lots of unwanted messages. Probably have to wrap this or something.
        // Or is this OK? Kind of like pattern matching if we just ignore messages of the wrong type?
        // And assume that the payload in the event is a BEBBroadcast?
        // The exercises had this wrapped in a PL_Deliver event

        @Override
        public void handle(Message e)
        {
            if (e.payload instanceof BEBBroadcast)
            {
                BEBBroadcast bebBroadcast = (BEBBroadcast) e.payload;
                LOG.info("Received best effort broadcast: " + bebBroadcast.payload);
                trigger(new BEBDeliver(e.getSource(), bebBroadcast.payload), beb);
            }
            else
            {
                LOG.error("Received unexpected message of type: " + e.getClass());
            }
        }
    };

    //endregion

    {
        subscribe(topologyHandler, beb);
        subscribe(broadcastRequestHandler, beb);
        subscribe(broadcastIncomingHandler, net);
    }
}
