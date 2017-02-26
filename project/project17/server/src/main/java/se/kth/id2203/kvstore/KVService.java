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
import se.kth.id2203.broadcast.rb.RBDeliver;
import se.kth.id2203.broadcast.rb.ReliableBroadcastPort;
import se.kth.id2203.kvstore.OpResponse.Code;
import se.kth.id2203.networking.Message;
import se.kth.id2203.networking.NetAddress;
import se.kth.id2203.overlay.Routing;
import se.sics.kompics.*;
import se.sics.kompics.network.Network;

import java.util.HashMap;

public class KVService extends ComponentDefinition {

    private final static Logger LOG = LoggerFactory.getLogger(KVService.class);

    // Ports
    protected final Positive<Network> net = requires(Network.class);
    protected final Positive<Routing> route = requires(Routing.class);
    // TODO Do we want to receive broadcast messages here in this component?
    protected final Positive<ReliableBroadcastPort> rb = requires(ReliableBroadcastPort.class);

    // Fields
    final NetAddress self = config().getValue("id2203.project.address", NetAddress.class);
    private HashMap<String, String> dataStore;

    //region Handlers

    protected final ClassMatchedHandler<GetOperation, Message> getOpHandler = new ClassMatchedHandler<GetOperation, Message>()
    {
        @Override
        public void handle(GetOperation operation, Message message) {

            // TODO NNAR
            LOG.info("Got operation {}", operation);
            String value = dataStore.get(operation.key);

            if (value == null)
                trigger(new Message(self, message.getSource(), new OpResponse(operation.id, Code.NOT_FOUND, value)), net);
            else
                trigger(new Message(self, message.getSource(), new OpResponse(operation.id, Code.OK, value)), net);
        }
    };

    protected final ClassMatchedHandler<PutOperation, Message> putOpHandler = new ClassMatchedHandler<PutOperation, Message>()
    {
        @Override
        public void handle(PutOperation operation, Message message)
        {
            // TODO NNAR - right now this is only putting the new value in one node in the replication group
            LOG.info("Got operation {}", operation);
            String previousValue = dataStore.put(operation.key, operation.value);
            trigger(new Message(self, message.getSource(), new OpResponse(operation.id, Code.OK, previousValue + " -> " + operation.value)), net);
        }
    };

    protected final ClassMatchedHandler<CASOperation, Message> casOpHandler = new ClassMatchedHandler<CASOperation, Message>()
    {
        @Override
        public void handle(CASOperation operation, Message message)
        {
            // TODO Implement
            // TODO: NNAR
            LOG.info("Got operation {}", operation);
            trigger(new Message(self, message.getSource(), new OpResponse(operation.id, Code.NOT_IMPLEMENTED, "not implemented")), net);
        }
    };

    protected final Handler<RBDeliver> incomingBroadcastHandler = new Handler<RBDeliver>()
    {
        @Override
        public void handle(RBDeliver rbDeliver)
        {
            LOG.info("Received RB broadcast - Source: " + rbDeliver.source + " - Payload: " + rbDeliver.payload);
        }
    };

    //endregion

    // TODO Remove this.
    // Just add temp preloaded data to all nodes. All nodes get the same data, no partitioning stuff.
    private void generatePreloadedData()
    {
        this.dataStore = new HashMap<>();

        for (int i = 0; i < 10; i++)
        {
            this.dataStore.put(Integer.toString(i), "This is datavalue " + Integer.toString(i));
        }
    }

    {
        subscribe(getOpHandler, net);
        subscribe(putOpHandler, net);
        subscribe(casOpHandler, net);
        subscribe(incomingBroadcastHandler, rb);
        generatePreloadedData();
    }
}
