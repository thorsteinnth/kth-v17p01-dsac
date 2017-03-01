package se.kth.id2203.simulation.epfd;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.kth.id2203.epfd.EPFDPort;
import se.kth.id2203.epfd.event.Restore;
import se.kth.id2203.epfd.event.Suspect;
import se.kth.id2203.epfd.event.SystemStable;
import se.kth.id2203.networking.Message;
import se.kth.id2203.networking.NetAddress;
import se.kth.id2203.overlay.Topology;
import se.kth.id2203.simulation.SimulationResultMap;
import se.kth.id2203.simulation.SimulationResultSingleton;
import se.sics.kompics.ComponentDefinition;
import se.sics.kompics.Handler;
import se.sics.kompics.Positive;
import se.sics.kompics.Start;
import se.sics.kompics.network.Address;
import se.sics.kompics.network.Network;
import se.sics.kompics.simulator.network.identifier.Identifier;
import se.sics.kompics.simulator.util.GlobalView;
import se.sics.kompics.timer.Timer;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.*;

public class ScenarioClient extends ComponentDefinition {

    final static Logger LOG = LoggerFactory.getLogger(ScenarioClient.class);

    // Ports
    protected final Positive<Network> net = requires(Network.class);
    protected final Positive<Timer> timer = requires(Timer.class);
    protected final Positive<EPFDPort> epfd = requires(EPFDPort.class);

    // Fields
    private final NetAddress self = config().getValue("id2203.project.address", NetAddress.class);
    private NetAddress observerAddress;

    private final SimulationResultMap res = SimulationResultSingleton.getInstance();
    private List<String> suspected;
    private Topology topology;

    public ScenarioClient()
    {
        try
        {
            observerAddress = new NetAddress(InetAddress.getByName("0.0.0.0"), 0);
        }
        catch (UnknownHostException ex)
        {
            throw new RuntimeException(ex);
        }

        subscribe(suspectHandler, epfd);
        subscribe(restoreHandler, epfd);
        subscribe(systemStableHandler, epfd);
        subscribe(observerMessageHandler, net);
    }

    protected final Handler<Suspect> suspectHandler = new Handler<Suspect>() {

        @Override
        public void handle(Suspect suspect) {
            LOG.debug("EPFD Test: got a suspected process: " + suspect.getAddress());

            if (suspected != null && suspect.getAddress() != observerAddress) {
                suspected.add(suspect.getAddress().toString());
                res.put(self.toString(), suspected);
            }
        }
    };

    protected final Handler<Restore> restoreHandler = new Handler<Restore>() {

        @Override
        public void handle(Restore restore) {
            if (suspected != null && res.keySet().contains(restore.getAddress().toString())) {
                LOG.debug("EPFD Test: removing a suspect: " + restore.getAddress());

                suspected.remove(restore.getAddress().toString());
                res.put(self.toString(), suspected);
            }
        }
    };

    protected final Handler<SystemStable> systemStableHandler = new Handler<SystemStable>() {

        @Override
        public void handle(SystemStable systemStable) {
            if (systemStable.getIsStable()) {
                res.put("systemStable", true);
            }
        }
    };

    protected final Handler<Message> observerMessageHandler = new Handler<Message>()
    {
        @Override
        public void handle(Message message)
        {

            if (message.payload instanceof GetTopology)
            {
                suspected = new ArrayList<>();
                topology = new Topology(getNodes());

                LOG.info("Sending topology to EPFD, size=" + topology.nodes.size());
                trigger(topology, epfd);
            }
        }
    };

    private Set<NetAddress> getNodes()
    {
        HashSet<NetAddress> nodeSet = new HashSet<>();

        GlobalView gv = config().getValue("simulation.globalview", GlobalView.class);
        for (Address address : gv.getAliveNodes().values())
        {
            NetAddress netAddress = new NetAddress(address.getIp(), address.getPort());

            if (netAddress.equals(observerAddress))
                continue;

            nodeSet.add(netAddress);
        }

        return nodeSet;
    }
}
