package se.kth.id2203.simulation.broadcast;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.kth.id2203.broadcast.BroadcastMessage;
import se.kth.id2203.broadcast.beb.BestEffortBroadcastPort;
import se.kth.id2203.broadcast.rb.RBBroadcast;
import se.kth.id2203.broadcast.rb.RBDeliver;
import se.kth.id2203.broadcast.rb.ReliableBroadcastPort;
import se.kth.id2203.networking.Message;
import se.kth.id2203.networking.NetAddress;
import se.kth.id2203.overlay.Topology;
import se.sics.kompics.*;
import se.sics.kompics.network.Address;
import se.sics.kompics.network.Network;
import se.sics.kompics.simulator.util.GlobalView;

import java.util.*;

/**
 * Sends a broadcast to servers
 * */
public class ScenarioClientBroadcast extends ComponentDefinition
{
    final static Logger LOG = LoggerFactory.getLogger(ScenarioClientBroadcast.class);

    // Ports
    protected final Positive<Network> net = requires(Network.class);
    protected final Positive<BestEffortBroadcastPort> beb = requires(BestEffortBroadcastPort.class);
    protected final Positive<ReliableBroadcastPort> rb = requires(ReliableBroadcastPort.class);

    // Fields
    private final NetAddress self = config().getValue("id2203.project.address", NetAddress.class);

    //region Handlers

    private final Handler<Message> observerMessageHandler = new Handler<Message>()
    {
        @Override
        public void handle(Message message)
        {
            if (message.payload instanceof StartBroadcastTest)
            {
                LOG.info(self + " - Starting broadcast");

                // Broadcast a message
                trigger(new RBBroadcast(new BroadcastMessage("test message")), rb);
            }
            else if (message.payload instanceof GetTopology)
            {
                LOG.info(self + " - Sending topology to BEB: " + getNodes());

                // Give the BEB implementation the topology
                trigger(new Topology(getNodes()), beb);
            }
        }
    };

    protected final Handler<RBDeliver> incomingBroadcastHandler = new Handler<RBDeliver>()
    {
        @Override
        public void handle(RBDeliver rbDeliver)
        {
            LOG.info(self + " - Received RB broadcast - Source: " + rbDeliver.source + " - Payload: " + rbDeliver.payload);
        }
    };

    //endregion

    private Set<NetAddress> getNodes()
    {
        HashSet<NetAddress> nodeSet = new HashSet<>();

        GlobalView gv = config().getValue("simulation.globalview", GlobalView.class);
        for (Address address : gv.getAliveNodes().values())
            nodeSet.add(new NetAddress(address.getIp(), address.getPort()));

        return  nodeSet;
    }

    {
        subscribe(observerMessageHandler, net);
        subscribe(incomingBroadcastHandler, rb);
    }
}
