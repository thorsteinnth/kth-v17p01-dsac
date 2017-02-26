package se.kth.id2203.nnar;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.kth.id2203.broadcast.OriginatedBroadcastMessage;
import se.kth.id2203.broadcast.beb.BEBBroadcast;
import se.kth.id2203.broadcast.beb.BEBDeliver;
import se.kth.id2203.broadcast.beb.BestEffortBroadcastPort;
import se.kth.id2203.networking.Message;
import se.kth.id2203.networking.NetAddress;
import se.kth.id2203.nnar.event.*;
import se.kth.id2203.overlay.Topology;
import se.sics.kompics.*;
import se.sics.kompics.network.Network;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class AtomicRegister extends ComponentDefinition {

    // TODO NNAR needs to work on a datastore (hashmap). This is just for storing one value right now.

    private final static Logger LOG = LoggerFactory.getLogger(AtomicRegister.class);

    // Ports
    private final Positive<Network> net = requires(Network.class);
    private final Positive<BestEffortBroadcastPort> beb = requires(BestEffortBroadcastPort.class);
    private final Negative<AtomicRegisterPort> nnar = provides(AtomicRegisterPort.class);

    // Fields
    private final NetAddress self = config().getValue("id2203.project.address", NetAddress.class);
    private Set<NetAddress> topology = new HashSet<>();

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

    // Here we are supposed to receive all the nodes that belong to the replication group
    private final Handler<Topology> topologyHandler = new Handler<Topology>()
    {
        @Override
        public void handle(Topology e)
        {
            LOG.info("Received topology: " + e.nodes);
            topology = e.nodes;
        }
    };

    protected final Handler<ARReadRequest> readRequestHandler = new Handler<ARReadRequest>() {
        @Override
        public void handle(ARReadRequest arReadRequest) {

            LOG.info("NNAR: Got a read request!");

            rId = rId + 1;
            acks = 0;
            readList.clear();
            reading = true;

            KompicsEvent payload = new READ(rId);
            trigger(new BEBBroadcast(new OriginatedBroadcastMessage(self, payload)), beb);
        }
    };

    protected final Handler<ARWriteRequest> writeRequestHandler = new Handler<ARWriteRequest>() {
        @Override
        public void handle(ARWriteRequest arWriteRequest) {

            LOG.info("NNAR: Got a write request!");

            rId = rId + 1;
            acks = 0;
            readList.clear();
            writeVal = arWriteRequest.getValue();

            KompicsEvent payload = new READ(rId);
            trigger(new BEBBroadcast(new OriginatedBroadcastMessage(self, payload)), beb);
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

                KompicsEvent payload = new VALUE(read.getrId(), tuple.getTs(), tuple.getWr(), value);
                trigger(new Message(self, bebDeliver.source, payload), net);
            }
            else if (bebDeliver.payload instanceof WRITE) {
                LOG.info("NNAR: Got broadcast deliver WRITE");

                WRITE write = (WRITE) bebDeliver.payload;
                Tuple writeTuple = new Tuple(write.getTs(), write.getWr());

                // If we receive a write value that has a higher timestamp/rank then we have
                // then we update our TS, WR and value
                if(writeTuple.biggerThan(tuple)) {
                    tuple.setTs(write.getTs());
                    tuple.setWr(write.getWr());

                    if(write.getOptionalWriteValue() != null) {
                        value = write.getOptionalWriteValue();
                    }
                }

                KompicsEvent payload = new ACK(write.getrId());
                trigger(new Message(self, bebDeliver.source, payload), net);
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

                ACK ack = (ACK) e.payload;

                if (ack.getrId() == rId) {
                    acks = acks + 1;

                    int N = topology.size();
                    if (acks > N/2) {
                        acks = 0;

                        if (reading) {
                            reading = false;
                            trigger(new ARReadResponse(readVal), nnar);
                        }
                        else {
                            trigger(new ARWriteResponse(), nnar);
                        }
                    }
                }
            }
            else if (e.payload instanceof VALUE) {
                LOG.info("NNAR: pp2p message handler VALUE");

                VALUE val = (VALUE) e.payload;

                if(val.getrId() == rId) {
                    readList.put(e.getSource(), new Tuple(val.getTs(), val.getWr(), val.getOptionalValue()));

                    /*
                    Check if we have received values from majority of processes,
                    then select the max TS/Rank and get value.
                    If read operation, broadcast the value.
                    If write operation, increase the TS and broadcast the write value
                     */

                    int N = topology.size();
                    if (readList.size() > N/2) {
                        Tuple highest = new Tuple(0, 0);

                        for (Tuple tuple : readList.values()) {

                            if (tuple.biggerOrEqual(highest)) {
                                highest = tuple;
                            }
                        }

                        if (highest.getOptionalValue() != null) {
                            readVal = highest.getOptionalValue();
                        }

                        Object broadcastVal = null;

                        if (reading) {
                            broadcastVal = readVal;
                        }
                        else {
                            highest.setWr(self.hashCode());     // SelfRank
                            highest.setTs(highest.getTs() + 1); // MaxTS
                            broadcastVal = writeVal;
                        }

                        KompicsEvent payload = new WRITE(rId, highest.getTs(), highest.getWr(), broadcastVal);
                        trigger(new BEBBroadcast(new OriginatedBroadcastMessage(self, payload)), beb);
                    }
                }
            }
        }
    };

    {
        subscribe(startHandler, control);
        subscribe(topologyHandler, nnar);
        subscribe(broadcastIncomingHandler, beb);
        subscribe(messageHandler, net);
        subscribe(readRequestHandler, nnar);
        subscribe(writeRequestHandler, nnar);
    }
}
