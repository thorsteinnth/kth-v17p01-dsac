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
package se.kth.id2203.overlay;

import com.larskroll.common.J6;
import java.util.Collection;
import java.util.HashSet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.kth.id2203.bootstrapping.*;
import se.kth.id2203.broadcast.beb.BestEffortBroadcastPort;
import se.kth.id2203.broadcast.rb.ReliableBroadcastPort;
import se.kth.id2203.epfd.EPFDPort;
import se.kth.id2203.epfd.event.Restore;
import se.kth.id2203.epfd.event.Suspect;
import se.kth.id2203.epfd.event.SystemStable;
import se.kth.id2203.multipaxos.MultiPaxosPort;
import se.kth.id2203.networking.Message;
import se.kth.id2203.networking.NetAddress;
import se.kth.id2203.nnar.AtomicRegisterPort;
import se.sics.kompics.ClassMatchedHandler;
import se.sics.kompics.ComponentDefinition;
import se.sics.kompics.Handler;
import se.sics.kompics.Negative;
import se.sics.kompics.Positive;
import se.sics.kompics.network.Network;
import se.sics.kompics.timer.Timer;

/**
 * The V(ery)S(imple)OverlayManager.
 * <p>
 * Keeps all nodes in a single partition in one replication group.
 * <p>
 * Note: This implementation does not fulfill the project task. You have to
 * support multiple partitions!
 * <p>
 * @author Lars Kroll <lkroll@kth.se>
 */
public class VSOverlayManager extends ComponentDefinition
{

    final static Logger LOG = LoggerFactory.getLogger(VSOverlayManager.class);

    // Ports
    protected final Negative<Routing> route = provides(Routing.class);
    protected final Positive<Bootstrapping> boot = requires(Bootstrapping.class);
    protected final Positive<Network> net = requires(Network.class);
    protected final Positive<Timer> timer = requires(Timer.class);
    protected final Positive<BestEffortBroadcastPort> beb = requires(BestEffortBroadcastPort.class);
    protected final Positive<EPFDPort> epfd = requires(EPFDPort.class);
    protected final Positive<AtomicRegisterPort> nnar = requires(AtomicRegisterPort.class);
    protected final Positive<MultiPaxosPort> mpaxos = requires(MultiPaxosPort.class);

    // Fields
    final NetAddress self = config().getValue("id2203.project.address", NetAddress.class);
    private LookupTable lut = null;

    //region Handlers

    protected final Handler<GetInitialAssignments> initialAssignmentHandler = new Handler<GetInitialAssignments>()
    {

        @Override
        public void handle(GetInitialAssignments event)
        {
            LOG.info(self + " - Generating lookup table...");
            LookupTable lut = LookupTable.generate(event.nodes);
            LOG.debug(self + " - Generated lookup table:\n{}", lut);
            trigger(new InitialAssignments(lut), boot);
        }
    };

    protected final Handler<Booted> bootHandler = new Handler<Booted>()
    {
        @Override
        public void handle(Booted event)
        {
            if (event.assignment instanceof LookupTable)
            {
                LOG.info(self + " - Got NodeAssignment, overlay ready.");
                lut = (LookupTable) event.assignment;
                LOG.debug(self + " - Lookup table:\n{}", lut);
                sendTopologyToBroadcaster();
                sendTopologyToFailureDetector();
                //sendTopologyToAtomicRegister();
                sendTopologyToMultiPaxos();
            }
            else
            {
                LOG.error(self + " - Got invalid NodeAssignment type. Expected: LookupTable; Got: {}", event.assignment.getClass());
            }
        }
    };

    protected final ClassMatchedHandler<RouteMsg, Message> routeHandler = new ClassMatchedHandler<RouteMsg, Message>()
    {

        @Override
        public void handle(RouteMsg content, Message context)
        {
            Collection<NetAddress> partition = lut.lookup(content.key);
            NetAddress target = J6.randomElement(partition);
            // Always selecting the first node would make the Multi Paxos algorithm more efficient
            //NetAddress target = partition.iterator().next();
            LOG.info(self + " - Forwarding message for key {} to {}", content.key, target);
            trigger(new Message(context.getSource(), target, content.msg), net);
        }
    };

    protected final Handler<RouteMsg> localRouteHandler = new Handler<RouteMsg>()
    {

        @Override
        public void handle(RouteMsg event)
        {
            Collection<NetAddress> partition = lut.lookup(event.key);
            NetAddress target = J6.randomElement(partition);
            // Always selecting the first node would make the Multi Paxos algorithm more efficient
            //NetAddress target = partition.iterator().next();
            LOG.info(self + " - Routing message for key {} to {}", event.key, target);
            trigger(new Message(self, target, event.msg), net);
        }
    };

    protected final ClassMatchedHandler<Connect, Message> connectHandler = new ClassMatchedHandler<Connect, Message>()
    {

        @Override
        public void handle(Connect content, Message context)
        {
            if (lut != null)
            {
                LOG.debug(self + " - Accepting connection request from {}", context.getSource());
                int size = lut.getNodes().size();
                trigger(new Message(self, context.getSource(), content.ack(size)), net);
            } else
            {
                LOG.info(self + " - Rejecting connection request from {}, as system is not ready, yet.", context.getSource());
            }
        }
    };

    // EPFD handlers
    protected final Handler<Suspect> suspectHandler = new Handler<Suspect>() {

        @Override
        public void handle(Suspect suspect)
        {
            LOG.debug(self + " - EPFD Suspected: " + suspect.getAddress());
            lut.removeNode(suspect.getAddress());
            LOG.debug(self + " - Lookup table:\n{}", lut);
            sendTopologyToBroadcaster();
            sendTopologyToMultiPaxos();
        }
    };

    protected final Handler<Restore> restoreHandler = new Handler<Restore>() {

        @Override
        public void handle(Restore restore)
        {
            LOG.debug(self + " - EPFD Restore process: " + restore.getAddress());
            // TODO Restore process, put it back into the correct place in the topology
            // Here we would have to also make sure that the restore process has the correct data
        }
    };

    //endregion

    private void sendTopologyToBroadcaster()
    {
        // Sending topology directly to the BEB broadcaster
        // (not sending it through the reliable broadcaster)
        LOG.info(self + " - Sending replication group to BEB broadcaster");
        trigger(new Topology(new HashSet<>(getReplicationGroup())), beb);
    }

    private void sendTopologyToFailureDetector()
    {
        // Sending the entire topology to the failure detector
        LOG.info(self + " - Sending topology to EPFD");
        trigger(new Topology(new HashSet<>(lut.getNodes())), epfd);
    }

    /*
    private void sendTopologyToAtomicRegister()
    {
        LOG.info("Sending topology to NNAR");
        trigger(new Topology(new HashSet<>(lut.getNodes())), nnar);
    }
    */

    private void sendTopologyToMultiPaxos()
    {
        LOG.info(self + " - Sending replication group to multi paxos");
        trigger(new Topology(new HashSet<>(getReplicationGroup())), mpaxos);
    }

    private Collection<NetAddress> getReplicationGroup()
    {
        // TODO Find better way to do this.
        // This should perhaps be an explicit message from bootstrap instead
        return lut.getPartitionForNode(self);
    }


    {
        subscribe(initialAssignmentHandler, boot);
        subscribe(bootHandler, boot);
        subscribe(routeHandler, net);
        subscribe(localRouteHandler, route);
        subscribe(connectHandler, net);
        subscribe(suspectHandler, epfd);
        subscribe(restoreHandler, epfd);
    }
}
