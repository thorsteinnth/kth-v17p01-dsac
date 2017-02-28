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

import java.util.*;

public class AtomicRegister extends ComponentDefinition
{
    // TODO Stop using OriginatedBroadcastMessage?

    private final static Logger LOG = LoggerFactory.getLogger(AtomicRegister.class);

    // Ports
    private final Positive<Network> net = requires(Network.class);
    private final Positive<BestEffortBroadcastPort> beb = requires(BestEffortBroadcastPort.class);
    private final Negative<AtomicRegisterPort> nnar = provides(AtomicRegisterPort.class);

    // Fields
    private final NetAddress self = config().getValue("id2203.project.address", NetAddress.class);
    private Set<NetAddress> topology = new HashSet<>();
    private HashMap<UUID, NNARState> pendingOperations;
    private HashMap<String, Tuple> datastore;

    //region Handlers

    protected final Handler<Start> startHandler = new Handler<Start>()
    {
        @Override
        public void handle(Start event)
        {
            LOG.info("Starting Atomic Register on {}", self);

            pendingOperations = new HashMap<>();
            datastore = new HashMap<>();
        }
    };

    /**
     * Receive the topology of my replication group
     * */
    private final Handler<Topology> topologyHandler = new Handler<Topology>()
    {
        @Override
        public void handle(Topology e)
        {
            LOG.info("Received topology: " + e.nodes);
            topology = e.nodes;
        }
    };

    protected final Handler<ARReadRequest> readRequestHandler = new Handler<ARReadRequest>()
    {
        @Override
        public void handle(ARReadRequest arReadRequest)
        {
            LOG.info(self + " - NNAR: Got a read request: " + arReadRequest);

            NNARState state = new NNARState();
            state.setReading(true);

            pendingOperations.put(arReadRequest.operation.id, state);

            KompicsEvent payload = new READ(arReadRequest.operation.key, arReadRequest.operation.id);
            trigger(new BEBBroadcast(new OriginatedBroadcastMessage(self, payload)), beb);
        }
    };

    protected final Handler<ARWriteRequest> writeRequestHandler = new Handler<ARWriteRequest>()
    {
        @Override
        public void handle(ARWriteRequest arWriteRequest)
        {
            LOG.info(self + " - NNAR: Got a write request: " + arWriteRequest);

            NNARState state = new NNARState();
            state.setWriteVal(arWriteRequest.operation.value);

            pendingOperations.put(arWriteRequest.operation.id, state);

            KompicsEvent payload = new READ(arWriteRequest.operation.key, arWriteRequest.operation.id);
            trigger(new BEBBroadcast(new OriginatedBroadcastMessage(self, payload)), beb);
        }
    };

    protected final Handler<BEBDeliver> broadcastIncomingHandler = new Handler<BEBDeliver>()
    {
        @Override
        public void handle(BEBDeliver bebDeliver)
        {
            if (!(bebDeliver.payload instanceof OriginatedBroadcastMessage))
            {
                LOG.error("Received unexpected BEB deliver payload of type: " + bebDeliver.payload.getClass());
                return;
            }

            OriginatedBroadcastMessage bebPayload = (OriginatedBroadcastMessage) bebDeliver.payload;

            if (bebPayload.payload instanceof READ)
            {
                READ read = (READ) bebPayload.payload;
                LOG.info("NNAR: Got broadcast deliver: " + read.toString());

                // Find value in the datastore
                Tuple foundTuple = datastore.get(read.getKey());
                if (foundTuple == null)
                {
                    foundTuple = new Tuple(0, 0);
                }

                KompicsEvent payload = new VALUE(read.getKey(), read.getOpId(), foundTuple.getTs(), foundTuple.getWr(), foundTuple.getOptionalValue());
                trigger(new Message(self, bebDeliver.source, payload), net);
            }
            else if (bebPayload.payload instanceof WRITE)
            {
                // Write value to datastore

                WRITE write = (WRITE) bebPayload.payload;
                LOG.info("NNAR: Got broadcast deliver: " + write.toString());
                Tuple writeTuple = new Tuple(write.getTs(), write.getWr());

                // Find value in the datastore
                Tuple foundTuple = datastore.get(write.getKey());
                if (foundTuple == null)
                {
                    foundTuple = new Tuple(0, 0);
                }

                // If we receive a write value that has a higher timestamp
                // then we update our TS, WR and value
                // Wr (rank) used as tiebreakers
                // We are using biggerThan here not biggerOrEqual since
                // we do not want to overwrite with the same value
                if (writeTuple.biggerThan(foundTuple))
                {
                    foundTuple.setTs(write.getTs());
                    foundTuple.setWr(write.getWr());
                    foundTuple.setOptionalValue(write.getOptionalWriteValue());
                }

                // Update/add entry in datastore
                datastore.put(write.getKey(), foundTuple);

                // TODO Remove
                printDataStore();

                KompicsEvent payload = new ACK(write.getOpId());
                trigger(new Message(self, bebDeliver.source, payload), net);
            }
            else
            {
                LOG.error("Received unexpected BEB payload of type: " + bebPayload.payload.getClass());
            }
        }
    };

    protected final Handler<Message> messageHandler = new Handler<Message>()
    {
        @Override
        public void handle(Message e)
        {
            if (e.payload instanceof ACK)
            {
                ACK ack = (ACK) e.payload;
                LOG.info("NNAR: pp2p message handler: " + ack.toString());

                // Find the pending operation that this message is about
                NNARState state = pendingOperations.get(ack.getOpId());
                if (state != null)
                {
                    // We are handling this operation

                    state.setAcks(state.getAcks() + 1);

                    int N = topology.size();
                    if (state.getAcks() > N / 2)
                    {
                        state.setAcks(0);   // TODO Should probably just remove from pending operations instead of resetting state

                        if (state.isReading())
                        {
                            state.setReading(false);    // TODO Should probably just remove from pending operations instead of resetting state
                            trigger(new ARReadResponse(ack.getOpId(), state.getReadVal()), nnar);
                        }
                        else
                        {
                            trigger(new ARWriteResponse(ack.getOpId()), nnar);
                        }

                        // This operation has been handled, remove it from pending operations
                        pendingOperations.remove(ack.getOpId());
                    }
                }
            }
            else if (e.payload instanceof VALUE)
            {
                VALUE val = (VALUE) e.payload;
                LOG.info("NNAR: pp2p message handler: " + val);

                // Find the pending operation that this message is about
                NNARState state = pendingOperations.get(val.getOpId());
                if (state != null)
                {
                    // We are handling this operation

                    state.getReadList().put(e.getSource(), new Tuple(val.getTs(), val.getWr(), val.getOptionalValue()));

                    // Check if we have received values from majority of processes,
                    int N = topology.size();
                    if (state.getReadList().size() > N / 2)
                    {
                        // We have received values from the majority of processes

                        // Get the highest tuple (value) we have gotten (max timestamp, rank as tiebreaker)
                        Tuple highest = new Tuple(0, 0);
                        for (Tuple tuple : state.getReadList().values())
                        {
                            if (tuple.biggerOrEqual(highest))
                            {
                                highest = tuple;
                            }
                        }

                        state.setReadVal(highest.getOptionalValue());

                        Object broadcastVal = null;

                        if (state.isReading())
                        {
                            // This is a read operation, broadcast the read value
                            broadcastVal = state.getReadVal();
                        }
                        else
                        {
                            // This is a write operation
                            // Set new timestamp (1 higher then the highest so far) and rank for the write message
                            highest.setWr(getSelfRank());
                            highest.setTs(highest.getTs() + 1); // MaxTS
                            broadcastVal = state.getWriteVal();
                        }

                        KompicsEvent payload = new WRITE(val.getKey(), val.getOpId(), highest.getTs(), highest.getWr(), broadcastVal);
                        trigger(new BEBBroadcast(new OriginatedBroadcastMessage(self, payload)), beb);
                    }
                }
            }
        }
    };

    //endregion

    private int getSelfRank()
    {
        return self.hashCode();
    }

    private void printDataStore()
    {
        StringBuilder sb = new StringBuilder();
        sb.append("Datastore - size: " + datastore.size() + " \n");

        for (String key : datastore.keySet())
        {
            Tuple tuple = datastore.get(key);
            sb.append("[" + key + " - " + tuple.toString() + "]");
        }

        LOG.debug(self.toString() + " - " + sb.toString());
    }

    {
        subscribe(startHandler, control);
        subscribe(topologyHandler, nnar);
        subscribe(broadcastIncomingHandler, beb);
        subscribe(messageHandler, net);
        subscribe(readRequestHandler, nnar);
        subscribe(writeRequestHandler, nnar);
    }
}
