package se.kth.id2203.nnar;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.kth.id2203.broadcast.beb.BestEffortBroadcastPort;
import se.kth.id2203.networking.NetAddress;
import se.sics.kompics.*;

import java.util.HashMap;
import java.util.Map;

public class AtomicRegister extends ComponentDefinition {

    private final static Logger LOG = LoggerFactory.getLogger(AtomicRegister.class);

    // Ports
    private final Negative<AtomicRegisterPort> nnar = provides(AtomicRegisterPort.class);
    private final Positive<BestEffortBroadcastPort> beb = requires(BestEffortBroadcastPort.class);

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

    {
        subscribe(startHandler, control);
    }
}
