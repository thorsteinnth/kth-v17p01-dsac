package se.kth.id2203.nnar;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.kth.id2203.broadcast.beb.BEBDeliver;
import se.kth.id2203.broadcast.beb.BestEffortBroadcastPort;
import se.kth.id2203.networking.Message;
import se.kth.id2203.networking.NetAddress;
import se.kth.id2203.nnar.event.ACK;
import se.kth.id2203.nnar.event.READ;
import se.kth.id2203.nnar.event.VALUE;
import se.kth.id2203.nnar.event.WRITE;
import se.sics.kompics.*;
import se.sics.kompics.network.Network;

import java.util.HashMap;
import java.util.Map;

public class AtomicRegister extends ComponentDefinition {

    private final static Logger LOG = LoggerFactory.getLogger(AtomicRegister.class);

    // Ports
    private final Positive<Network> net = requires(Network.class);
    private final Positive<BestEffortBroadcastPort> beb = requires(BestEffortBroadcastPort.class);
    private final Negative<AtomicRegisterPort> nnar = provides(AtomicRegisterPort.class);

    // Fields
    private final NetAddress self = config().getValue("id2203.project.address", NetAddress.class);

    private Tuple tuple; //ts and wr
    private Object value;
    private int acks;
    private Object readVal;
    private Object writeVal;
    private int rId;
    private Map<NetAddress, Tuple> readList;
    private boolean reading;

    // Handlers
    protected final Handler<Start> startHandler = new Handler<Start>() {

        @Override
        public void handle(Start event) {

            LOG.info("Starting Atomic Register on {}", self);
            tuple = new Tuple(0, 0);
            value = null;
            acks = 0;
            readVal = null;
            writeVal = null;
            rId = 0;
            readList = new HashMap<>();
            reading = false;
        }
    };

    protected final Handler<BEBDeliver> broadcastIncomingHandler = new Handler<BEBDeliver>()
    {
        @Override
        public void handle(BEBDeliver bebDeliver)
        {

            if (bebDeliver.payload instanceof READ) {
                LOG.info("NNAR: Got broadcast deliver READ");
                READ read = (READ) bebDeliver.payload;

                trigger(new Message(
                            self,
                            bebDeliver.source,
                            new VALUE(read.getrId(), tuple.getTs(), tuple.getWr(), value)), net
                );
            }
            else if (bebDeliver.payload instanceof WRITE) {
                LOG.info("NNAR: Got broadcast deliver WRITE");
                WRITE write = (WRITE) bebDeliver.payload;

                //TODO
            }
            else {
                LOG.error("Received unexpected message of type: " + bebDeliver.payload.getClass());
            }
        }
    };

    protected final Handler<Message> messageHandler = new Handler<Message>() {

        @Override
        public void handle(Message e) {

            if (e.payload instanceof ACK) {
                LOG.info("NNAR: pp2p message handler ACK");

                //TODO
            }
            else if (e.payload instanceof VALUE) {
                LOG.info("NNAR: pp2p message handler VALUE");

                //TODO
            }
        }
    };

    {
        subscribe(startHandler, control);
        subscribe(broadcastIncomingHandler, beb);
        subscribe(messageHandler, net);
    }
}
