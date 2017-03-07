package se.kth.id2203.simulation.ops;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.kth.id2203.kvstore.Operation;
import se.kth.id2203.kvstore.PendingOperation;
import se.kth.id2203.multipaxos.Abort;
import se.kth.id2203.multipaxos.DecideResult;
import se.kth.id2203.multipaxos.MultiPaxosPort;
import se.kth.id2203.multipaxos.Propose;
import se.kth.id2203.networking.NetAddress;
import se.kth.id2203.simulation.SimulationResultMap;
import se.kth.id2203.simulation.SimulationResultSingleton;
import se.sics.kompics.ComponentDefinition;
import se.sics.kompics.Handler;
import se.sics.kompics.Positive;
import se.sics.kompics.Start;
import se.sics.kompics.network.Network;

import java.util.LinkedList;
import java.util.Queue;

public class ScenarioClient extends ComponentDefinition
{
    final static Logger LOG = LoggerFactory.getLogger(ScenarioClient.class);

    // Ports
    protected final Positive<Network> net = requires(Network.class);
    protected final Positive<MultiPaxosPort> mpaxos = requires(MultiPaxosPort.class);

    // Fields
    private final NetAddress self = config().getValue("id2203.project.address", NetAddress.class);
    private final SimulationResultMap res = SimulationResultSingleton.getInstance();

    // We are storing the pending operations in a queue. Only one operation at a time active in mpaxos
    // Doing it this way so we know what operation is aborted, instead of changing the paxos algorithm
    // to carry the operation ID with the abort message.
    private Queue<PendingOperation> pendingOperations;

    public ScenarioClient()
    {
        subscribe(startHandler, control);
        subscribe(decideResultHandler, mpaxos);
        subscribe(abortHandler, mpaxos);
        this.pendingOperations = new LinkedList<>();
    }

    // Handlers
    protected final Handler<Start> startHandler = new Handler<Start>() {

        @Override
        public void handle(Start event) {

            LOG.info("Starting Multi Paxos scenario client on " + self.toString());
        }
    };

    private final Handler<DecideResult> decideResultHandler = new Handler<DecideResult>()
    {
        @Override
        public void handle(DecideResult decideResult)
        {
            LOG.info(self + " - Got decide result from mpaxos: " + decideResult);

            // TODO : check operation type
        }
    };

    protected final Handler<Abort> abortHandler = new Handler<Abort>()
    {
        @Override
        public void handle(Abort abort)
        {
            LOG.info(self + " - Got abort from mpaxos: " + abort);

            // Since we have a pending operation queue we know that the first operation in the queue is the one
            // that we were trying to perform, and the abort is meant for that operation.
            // Notify client, remove operation from queue and try the next one.

            PendingOperation headPendingOperation = pendingOperations.peek();

            if (headPendingOperation != null)
            {
                // Do something with the abort result
            }

            removePendingOperationAndSendNextOp();
        }
    };

    // Pending operation

    /**
     * Add a new operation to pending operation queue.
     * If the queue was not empty, and this operation is the only operation in the queue, send it to mpaxos.
     */
    private void addPendingOperationAndSendIfFirst(Operation operation, NetAddress clientAddress)
    {
        pendingOperations.add(new PendingOperation(clientAddress, operation));

        if (pendingOperations.size() == 1)
            trigger(new Propose(operation), mpaxos);
    }

    /**
     * Remove operation from the pending operation queue.
     * If the queue is not empty after removing the operation, send the next operation to mpaxos.
     */
    private void removePendingOperationAndSendNextOp()
    {
        PendingOperation removedOperation = pendingOperations.remove();
        LOG.debug(self + " - Removed operation from pending: " + removedOperation);

        PendingOperation nextPendingOperation = pendingOperations.peek();
        if (nextPendingOperation != null)
        {
            trigger(new Propose(nextPendingOperation.operation), mpaxos);
        }
    }
}
