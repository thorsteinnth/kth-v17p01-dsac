/*
 * The MIT License
 *
 * Copyright 2017 Lars Kroll <lkroll@kth.se>.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package se.kth.id2203.kvstore;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.kth.id2203.broadcast.beb.BEBDeliver;
import se.kth.id2203.broadcast.beb.BestEffortBroadcastPort;
import se.kth.id2203.kvstore.OpResponse.Code;
import se.kth.id2203.multipaxos.Abort;
import se.kth.id2203.multipaxos.DecideResult;
import se.kth.id2203.multipaxos.MultiPaxosPort;
import se.kth.id2203.multipaxos.Propose;
import se.kth.id2203.networking.Message;
import se.kth.id2203.networking.NetAddress;
import se.kth.id2203.nnar.AtomicRegisterPort;
import se.kth.id2203.overlay.Routing;
import se.sics.kompics.*;
import se.sics.kompics.network.Network;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Queue;

public class KVService extends ComponentDefinition
{
    private final static Logger LOG = LoggerFactory.getLogger(KVService.class);

    // Ports
    protected final Positive<Network> net = requires(Network.class);
    protected final Positive<Routing> route = requires(Routing.class);
    protected final Positive<BestEffortBroadcastPort> beb = requires(BestEffortBroadcastPort.class);
    protected final Positive<AtomicRegisterPort> nnar = requires(AtomicRegisterPort.class);
    protected final Positive<MultiPaxosPort> mpaxos = requires(MultiPaxosPort.class);

    // Fields
    final NetAddress self = config().getValue("id2203.project.address", NetAddress.class);
    private HashMap<String, String> datastore;
    /**
     * Pending operations that are being processed by atomic register.
     * */
    // We are storing the pending operations in a queue. Only one operation at a time active in mpaxos
    // Doing it this way so we know what operation is aborted, instead of changing the paxos algorithm
    // to carry the operation ID with the abort message.
    private Queue<PendingOperation> pendingOperations;

    //region Handlers

    protected final ClassMatchedHandler<GetOperation, Message> getOpHandler = new ClassMatchedHandler<GetOperation, Message>()
    {
        @Override
        public void handle(GetOperation operation, Message message)
        {
            LOG.info("Got operation {}", operation);
            addPendingOperationAndSendIfFirst(operation, message.getSource());
        }
    };

    protected final ClassMatchedHandler<PutOperation, Message> putOpHandler = new ClassMatchedHandler<PutOperation, Message>()
    {
        @Override
        public void handle(PutOperation operation, Message message)
        {
            LOG.info("Got operation {}", operation);
            addPendingOperationAndSendIfFirst(operation, message.getSource());
        }
    };

    protected final ClassMatchedHandler<CASOperation, Message> casOpHandler = new ClassMatchedHandler<CASOperation, Message>()
    {
        @Override
        public void handle(CASOperation operation, Message message)
        {
            LOG.info("Got operation {}", operation);
            addPendingOperationAndSendIfFirst(operation, message.getSource());
        }
    };

    protected final Handler<BEBDeliver> incomingBroadcastHandler = new Handler<BEBDeliver>()
    {
        @Override
        public void handle(BEBDeliver bebDeliver)
        {
            LOG.info("Received BEB broadcast - Source: " + bebDeliver.source + " - Payload: " + bebDeliver.payload);
        }
    };

    //region NNAR handlers
    /*

    protected final Handler<ARReadResponse> arReadResponseHandler = new Handler<ARReadResponse>()
    {
        @Override
        public void handle(ARReadResponse arReadResponse)
        {
            LOG.info("Got read response from NNAR: " + arReadResponse.toString());

            NetAddress clientAddress = pendingOperations.get(arReadResponse.getOpId());

            if (clientAddress == null)
                return;

            if (arReadResponse.getValue() == null)
                trigger(new Message(self, clientAddress,
                        new OpResponse(arReadResponse.getOpId(), Code.NOT_FOUND, "")),
                        net);
            else
                trigger(new Message(self, clientAddress,
                        new OpResponse(arReadResponse.getOpId(), Code.OK, arReadResponse.getValue().toString())),
                        net);

            pendingOperations.remove(arReadResponse.getOpId());
        }
    };

    protected final Handler<ARWriteResponse> arWriteResponseHandler = new Handler<ARWriteResponse>()
    {
        @Override
        public void handle(ARWriteResponse arWriteResponse)
        {
            LOG.info("Got write response from NNAR: " + arWriteResponse.toString());

            NetAddress clientAddress = pendingOperations.get(arWriteResponse.getOpId());

            if (clientAddress == null)
                return;

            trigger(new Message(self, clientAddress,
                    new OpResponse(arWriteResponse.getOpId(), Code.OK, "Value was written")),
                    net);

            pendingOperations.remove(arWriteResponse.getOpId());
        }
    };

    */
    //endregion

    //region Paxos handlers

    private final Handler<DecideResult> decideResultHandler = new Handler<DecideResult>()
    {
        @Override
        public void handle(DecideResult decideResult)
        {
            LOG.info(self + " - Got decide result from mpaxos: " + decideResult);

            if (decideResult.operation instanceof PutOperation)
            {
                // Perform operation
                PutOperation operation = (PutOperation)decideResult.operation;
                String oldValue = datastore.put(operation.key, operation.value);
                printDataStore();

                // Send response to client, if we have have this operation in our pending queue.
                // Note that only the server that got the request from the client has it in its pending queue
                // and should respond to the client. Other servers do not communicate with the client.
                PendingOperation headPendingOperation = pendingOperations.peek();
                if (headPendingOperation != null && headPendingOperation.operation.equals(operation))
                {
                    // I am the server that is responsible for this operation.
                    // Should notify client.
                    trigger(new Message(self, headPendingOperation.clientAddress, new OpResponse(operation.id, Code.OK, oldValue)), net);
                    LOG.debug(self + " - Response sent to client: " + headPendingOperation.clientAddress);
                    removePendingOperationAndSendNextOp();
                }
            }
            else if (decideResult.operation instanceof GetOperation)
            {
                // Perform operation
                GetOperation operation = (GetOperation)decideResult.operation;
                String value = datastore.get(operation.key);
                printDataStore();

                // Send response to client, if we have have this operation in our pending queue.
                // Note that only the server that got the request from the client has it in its pending queue
                // and should respond to the client. Other servers do not communicate with the client.
                PendingOperation headPendingOperation = pendingOperations.peek();
                if (headPendingOperation != null && headPendingOperation.operation.equals(operation))
                {
                    // I am the server that is responsible for this operation.
                    // Should notify client.
                    if (value == null)
                        trigger(new Message(self, headPendingOperation.clientAddress, new OpResponse(operation.id, Code.NOT_FOUND, "")), net);
                    else
                        trigger(new Message(self, headPendingOperation.clientAddress, new OpResponse(operation.id, Code.OK, value)), net);

                    LOG.debug(self + " - Response sent to client: " + headPendingOperation.clientAddress);
                    removePendingOperationAndSendNextOp();
                }
            }
            else if (decideResult.operation instanceof CASOperation)
            {
                // Perform operation
                CASOperation operation = (CASOperation)decideResult.operation;
                String currentValue = datastore.get(operation.key);

                boolean success = false;
                String oldValue = "";

                if (currentValue.equals(operation.referenceValue))
                {
                    success = true;
                    oldValue = datastore.put(operation.key, operation.newValue);
                    printDataStore();
                }

                // Send response to client, if we have have this operation in our pending queue.
                // Note that only the server that got the request from the client has it in its pending queue
                // and should respond to the client. Other servers do not communicate with the client.
                PendingOperation headPendingOperation = pendingOperations.peek();
                if (headPendingOperation != null && headPendingOperation.operation.equals(operation))
                {
                    if (success)
                    {
                        OpResponse opResponse = new OpResponse(
                                operation.id,
                                Code.OK,
                                "Old val: " + oldValue + ", new val: " + operation.newValue
                        );
                        trigger(new Message(self, headPendingOperation.clientAddress, opResponse), net);
                    }
                    else
                    {
                        OpResponse opResponse = new OpResponse(
                                operation.id,
                                Code.NOT_SAME_VALUE,
                                "Current val: " + currentValue
                        );
                        trigger(new Message(self, headPendingOperation.clientAddress, opResponse), net);
                    }

                    LOG.debug(self + " - Response sent to client: " + headPendingOperation.clientAddress);
                    removePendingOperationAndSendNextOp();
                }
            }
            else
            {
                LOG.error(self + " - Unexpected decide result operation type: " + decideResult.operation.getClass());
            }
        }
    };

    private final Handler<Abort> abortHandler = new Handler<Abort>()
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
                OpResponse opResponse = new OpResponse(
                        headPendingOperation.operation.id,
                        Code.ABORT,
                        null
                );
                trigger(new Message(self, headPendingOperation.clientAddress, opResponse), net);
                LOG.debug(self + " - Response sent to client: " + headPendingOperation.clientAddress);
            }

            removePendingOperationAndSendNextOp();
        }
    };

    //endregion

    //endregion

    /**
     * Add a new operation to pending operation queue.
     * If the queue was not empty, and this operation is the only operation in the queue, send it to mpaxos.
     */
    private void addPendingOperationAndSendIfFirst(Operation operation, NetAddress clientAddress)
    {
        pendingOperations.add(new PendingOperation(clientAddress, operation));
        LOG.debug(self + " - Added pending operation: " + clientAddress + " - " + operation);
        printPendingOperations();

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
        printPendingOperations();

        PendingOperation nextPendingOperation = pendingOperations.peek();
        if (nextPendingOperation != null)
        {
            trigger(new Propose(nextPendingOperation.operation), mpaxos);
        }
    }

    private void printDataStore()
    {
        StringBuilder sb = new StringBuilder();
        sb.append("Datastore - size: " + datastore.size() + " \n");

        for (String key : datastore.keySet())
        {
            String value = datastore.get(key);
            sb.append("[" + key + " - " + value + "]");
        }

        LOG.debug(self.toString() + " - " + sb.toString());
    }

    private void printPendingOperations()
    {
        StringBuilder sb = new StringBuilder();
        sb.append("Pending operations - size: " + pendingOperations.size() + " \n");

        for (PendingOperation pendingOperation : pendingOperations)
        {
            sb.append("[" + pendingOperation.toString() + "]");
        }

        LOG.debug(self.toString() + " - " + sb.toString());
    }

    {
        subscribe(getOpHandler, net);
        subscribe(putOpHandler, net);
        subscribe(casOpHandler, net);
        subscribe(incomingBroadcastHandler, beb);
        //subscribe(arReadResponseHandler, nnar);
        //subscribe(arWriteResponseHandler, nnar);
        subscribe(decideResultHandler, mpaxos);
        subscribe(abortHandler, mpaxos);
        this.datastore = new HashMap<>();
        this.pendingOperations = new LinkedList<>();
    }
}
