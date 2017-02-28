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
import se.kth.id2203.nnar.event.ARReadRequest;
import se.kth.id2203.nnar.event.ARReadResponse;
import se.kth.id2203.nnar.event.ARWriteRequest;
import se.kth.id2203.nnar.event.ARWriteResponse;
import se.kth.id2203.overlay.Routing;
import se.sics.kompics.*;
import se.sics.kompics.network.Network;

import java.util.HashMap;
import java.util.UUID;

public class KVService extends ComponentDefinition {

    // TODO Use MultiPaxos and RSM operation sequence instead of NNAR

    private final static Logger LOG = LoggerFactory.getLogger(KVService.class);

    // Ports
    protected final Positive<Network> net = requires(Network.class);
    protected final Positive<Routing> route = requires(Routing.class);
    protected final Positive<BestEffortBroadcastPort> beb = requires(BestEffortBroadcastPort.class);
    protected final Positive<AtomicRegisterPort> nnar = requires(AtomicRegisterPort.class);
    protected final Positive<MultiPaxosPort> mpaxos = requires(MultiPaxosPort.class);

    // Fields
    final NetAddress self = config().getValue("id2203.project.address", NetAddress.class);
    /**
     * Pending operations that are being processed by atomic register.
     * */
    private HashMap<UUID, NetAddress> pendingOperations;

    //region Handlers

    protected final ClassMatchedHandler<GetOperation, Message> getOpHandler = new ClassMatchedHandler<GetOperation, Message>()
    {
        @Override
        public void handle(GetOperation operation, Message message)
        {
            LOG.info("Got operation {}", operation);

            // Send a read request to NNAR
            pendingOperations.put(operation.id, message.getSource());
            trigger(new ARReadRequest(operation), nnar);
        }
    };

    protected final ClassMatchedHandler<PutOperation, Message> putOpHandler = new ClassMatchedHandler<PutOperation, Message>()
    {
        @Override
        public void handle(PutOperation operation, Message message)
        {
            LOG.info("Got operation {}", operation);

            // Send write request to NNAR
            pendingOperations.put(operation.id, message.getSource());
            trigger(new ARWriteRequest(operation), nnar);

            // Send write request to mpaxos
            trigger(new Propose(operation), mpaxos);
        }
    };

    protected final ClassMatchedHandler<CASOperation, Message> casOpHandler = new ClassMatchedHandler<CASOperation, Message>()
    {
        @Override
        public void handle(CASOperation operation, Message message)
        {
            // TODO Implement
            LOG.info("Got operation {}", operation);
            trigger(new Message(self, message.getSource(), new OpResponse(operation.id, Code.NOT_IMPLEMENTED, "not implemented")), net);
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

    //region Paxos handlers

    private final Handler<DecideResult> decideResultHandler = new Handler<DecideResult>()
    {
        @Override
        public void handle(DecideResult decideResult)
        {
            LOG.info(self + " - Got decide result from mpaxos: " + decideResult);
        }
    };

    private final Handler<Abort> abortHandler = new Handler<Abort>()
    {
        @Override
        public void handle(Abort abort)
        {
            LOG.info(self + " - Got abort from mpaxos: " + abort);
        }
    };

    //endregion

    //endregion

    {
        subscribe(getOpHandler, net);
        subscribe(putOpHandler, net);
        subscribe(casOpHandler, net);
        subscribe(incomingBroadcastHandler, beb);
        subscribe(arReadResponseHandler, nnar);
        subscribe(arWriteResponseHandler, nnar);
        subscribe(decideResultHandler, mpaxos);
        subscribe(abortHandler, mpaxos);
        this.pendingOperations = new HashMap<>();
    }
}
