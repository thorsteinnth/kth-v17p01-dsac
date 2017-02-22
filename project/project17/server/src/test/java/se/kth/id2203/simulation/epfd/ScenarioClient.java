package se.kth.id2203.simulation.epfd;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.kth.id2203.epfd.EPFDPort;
import se.kth.id2203.epfd.Restore;
import se.kth.id2203.epfd.Suspect;
import se.kth.id2203.networking.NetAddress;
import se.kth.id2203.simulation.ScenarioClientGet;
import se.sics.kompics.ComponentDefinition;
import se.sics.kompics.Handler;
import se.sics.kompics.Positive;
import se.sics.kompics.Start;
import se.sics.kompics.network.Address;
import se.sics.kompics.network.Network;
import se.sics.kompics.simulator.network.identifier.Identifier;
import se.sics.kompics.simulator.util.GlobalView;
import se.sics.kompics.timer.Timer;

import java.util.Map;

public class ScenarioClient extends ComponentDefinition {

    final static Logger LOG = LoggerFactory.getLogger(ScenarioClientGet.class);

    // Ports
    protected final Positive<Network> net = requires(Network.class);
    protected final Positive<Timer> timer = requires(Timer.class);
    protected final Positive<EPFDPort> epfd = requires(EPFDPort.class);

    // Fields
    private final NetAddress self = config().getValue("id2203.project.address", NetAddress.class);
    private final NetAddress server = config().getValue("id2203.project.bootstrap-address", NetAddress.class);

    private final SimulationResult res = SimulationResult.getInstance();

    protected final Handler<Start> startHandler = new Handler<Start>() {

        @Override
        public void handle(Start event) {

            GlobalView globalView = config().getValue("simulation.globalview", GlobalView.class);

            for (Map.Entry<Identifier, Address> entry : globalView.getAliveNodes().entrySet())
            {
                LOG.debug("EPFD Test: Printing global view");
                LOG.debug(entry.getKey() + "/" + entry.getValue());
            }
        }
    };

    protected final Handler<Suspect> suspectHandler = new Handler<Suspect>() {

        @Override
        public void handle(Suspect suspect) {
            LOG.debug("EPFD Test: got a suspected process: " + suspect.getAddress());
            res.addSuspect(suspect.getAddress());
        }
    };

    protected final Handler<Restore> restoreHandler = new Handler<Restore>() {

        @Override
        public void handle(Restore restore) {
            LOG.debug("EPFD Test: got a restoree process: " + restore.getAddress());

            if (res.getSuspected().contains(restore.getAddress())) {
                LOG.debug("EPFD Test: removing a suspect: " + restore.getAddress());
                res.removeSuspect(restore.getAddress());
            }
        }
    };

    {
        subscribe(startHandler, control);
        subscribe(suspectHandler, epfd);
        subscribe(restoreHandler, epfd);
    }
}
