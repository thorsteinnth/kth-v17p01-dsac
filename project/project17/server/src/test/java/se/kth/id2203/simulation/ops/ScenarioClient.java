package se.kth.id2203.simulation.ops;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.kth.id2203.kvstore.CASOperation;
import se.kth.id2203.kvstore.GetOperation;
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

            /**
             * The test performs 10 PUT operations, then 10 GET operations.
             * For each successful GET operation (OK response) we perform a CAS operation and
             * keep count of how many CAS operations we have done.
             */

            int putMessages = res.get("put_messages", Integer.class);
            int getMessages = res.get("get_messages", Integer.class);
            res.put("cas_messages", 0);

            for (int i = 0; i < putMessages; i++)
            {
                PutOperation put = new PutOperation(Integer.toString(i), Integer.toString(i));
                RouteMsg rm = new RouteMsg(put.key, put);
                trigger(new Message(self, server, rm), net);
                pending.put(put.id, "PUT-" + put.key);
                LOG.info("OpsScenarioClient: Sending {}", put);
                res.put("PUT-" + put.key, "SENT");
            }

            for (int i = 0; i < getMessages; i++)
            {
                GetOperation get = new GetOperation(Integer.toString(i));
                RouteMsg rm = new RouteMsg(get.key, get);
                trigger(new Message(self, server, rm), net);
                pending.put(get.id, "GET-" + get.key);
                LOG.info("OpsScenarioClient: Sending {}", get);
                res.put("GET-" + get.key, "SENT");
            }
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
                if (key.contains("GET") && (content.result != null) && content.status.name().equals("OK"))
                {
                    // If the key is "GET-2" then the content.result should be "2"
                    CASOperation cas = new CASOperation(content.result, content.result, "newVal " + content.result);
                    RouteMsg rm = new RouteMsg(cas.key, cas);
                    trigger(new Message(self, server, rm), net);
                    pending.put(cas.id, "CAS-" + cas.key);
                    LOG.info("OpsScenarioClient: Sending {}", cas);
                    res.put("CAS-" + cas.key, "SENT");

                    int currentNumberOfCasOpsSent = res.get("cas_messages", Integer.class);
                    res.put("cas_messages", ++currentNumberOfCasOpsSent);
                }

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
