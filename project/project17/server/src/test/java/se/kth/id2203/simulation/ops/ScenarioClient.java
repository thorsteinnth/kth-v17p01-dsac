package se.kth.id2203.simulation.ops;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.kth.id2203.kvstore.OpResponse;
import se.kth.id2203.kvstore.PutOperation;
import se.kth.id2203.networking.Message;
import se.kth.id2203.networking.NetAddress;
import se.kth.id2203.overlay.RouteMsg;
import se.kth.id2203.simulation.SimulationResultMap;
import se.kth.id2203.simulation.SimulationResultSingleton;
import se.sics.kompics.*;
import se.sics.kompics.network.Network;
import se.sics.kompics.timer.Timer;

import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;

public class ScenarioClient extends ComponentDefinition {

    final static Logger LOG = LoggerFactory.getLogger(ScenarioClientGet.class);

    // Ports
    protected final Positive<Network> net = requires(Network.class);
    protected final Positive<Timer> timer = requires(Timer.class);

    // Fields
    private final NetAddress self = config().getValue("id2203.project.address", NetAddress.class);
    private final NetAddress server = config().getValue("id2203.project.bootstrap-address", NetAddress.class);
    private final SimulationResultMap res = SimulationResultSingleton.getInstance();
    private final Map<UUID, String> pending = new TreeMap<>();

    //region Handlers

    protected final Handler<Start> startHandler = new Handler<Start>()
    {
        @Override
        public void handle(Start event)
        {
            // TODO : just testing this out for one value
            LOG.info("OpsScenarioClient: Running start handler");
            PutOperation op = new PutOperation(Integer.toString(2), "This is datavalue " + Integer.toString(2));
            RouteMsg rm = new RouteMsg(op.key, op);
            trigger(new Message(self, server, rm), net);
            pending.put(op.id, "PUT-" + op.key);
            LOG.info("OpsScenarioClient: Sending {}", op);
        }
    };

    protected final ClassMatchedHandler<OpResponse, Message> responseHandler = new ClassMatchedHandler<OpResponse, Message>()
    {
        @Override
        public void handle(OpResponse content, Message context)
        {
            LOG.debug("OpsScenarioClient: Got OpResponse: {}", content);
            String key = pending.remove(content.id);
            if (key != null)
            {
                res.put(key, content.status.toString());
            }
            else
            {
                LOG.warn("ID {} was not pending! Ignoring response.", content.id);
            }
        }
    };

    //endregion

    {
        subscribe(startHandler, control);
        subscribe(responseHandler, net);
    }
}
