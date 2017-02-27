package se.kth.id2203.epfd;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.kth.id2203.bootstrapping.BSTimeout;
import se.kth.id2203.networking.Message;
import se.kth.id2203.networking.NetAddress;
import se.kth.id2203.overlay.Topology;
import se.sics.kompics.*;
import se.sics.kompics.network.Network;
import se.sics.kompics.timer.ScheduleTimeout;
import se.sics.kompics.timer.Timer;

import java.util.HashSet;
import java.util.Set;

import static se.sics.kompics.network.netty.serialization.Serializers.LOG;

public class EPFD extends ComponentDefinition{

    private final static Logger LOG = LoggerFactory.getLogger(EPFD.class);

    // Ports
    protected final Positive<Timer> timer = requires(Timer.class);
    protected final Positive<Network> net = requires(Network.class);
    protected final Negative<EPFDPort> epfd = provides(EPFDPort.class);

    // Fields
    // - Configuration parameters
    private final NetAddress self = config().getValue("id2203.project.address", NetAddress.class);
    private Set<NetAddress> topology = new HashSet<>();
    private long delta = config().getValue("id2203.project.epfd.delay.debug", Long.class);

    // - Mutable state
    private Set<NetAddress> alive;
    private Set<NetAddress> suspected;
    private int seqNum;
    private Long delay;

    // Handlers
    protected final Handler<Start> startHandler = new Handler<Start>() {

        @Override
        public void handle(Start event) {

            LOG.info("Starting EPFD on {}", self);
            seqNum = 0;
            alive = new HashSet<>();
            suspected = new HashSet<>();
            delay = delta;
        }
    };

    private final Handler<Topology> topologyHandler = new Handler<Topology>()
    {
        @Override
        public void handle(Topology e)
        {
            LOG.info("Received topology: " + e.nodes);
            topology = e.nodes;

            alive.addAll(topology);
            startTimer(delay);
        }
    };

    protected final Handler<CheckTimeout> timeoutHandler = new Handler<CheckTimeout>() {

        @Override
        public void handle(CheckTimeout e) {

            //LOG.info("EPFD received timeout");
            Set<NetAddress> suspectedAndAlive = new HashSet<>(alive);
            suspectedAndAlive.retainAll(suspected);

            if(!suspectedAndAlive.isEmpty()) {
                delay = delay + delta;
            }

            seqNum = seqNum + 1;

            for (NetAddress process : topology) {

                if(!alive.contains(process) && !suspected.contains(process)) {
                    LOG.info("EPFD suspects: " + process.toString());
                    suspected.add(process);
                    trigger(new Suspect(process), epfd);
                }
                else if (alive.contains(process) && suspected.contains(process)) {
                    LOG.info("EPFD restores: " + process.toString());
                    suspected.remove(process);
                    trigger(new Restore(process), epfd);
                }

                KompicsEvent heartbeatRequest = new HeartbeatRequest(seqNum);
                trigger(new Message(self, process, heartbeatRequest), net);
            }

            alive.clear();
            startTimer(delay);
        }
    };

    protected final Handler<Message> messageHandler = new Handler<Message>() {

        @Override
        public void handle(Message e) {

            if (e.payload instanceof HeartbeatRequest) {

                HeartbeatRequest heartbeatRequest = (HeartbeatRequest) e.payload;

                //LOG.info("EPFD: received REQUEST from " + e.getSource());

                KompicsEvent heartbeatReply = new HeartbeatReply(heartbeatRequest.getSeq());
                trigger(new Message(self, e.getSource(), heartbeatReply), net);
            }
            else if (e.payload instanceof HeartbeatReply) {

                HeartbeatReply heartbeatReply = (HeartbeatReply) e.payload;

                //LOG.info("EPFD: received REPLY from " + e.getSource());

                if (heartbeatReply.getSeq() == seqNum || suspected.contains(e.getSource())) {
                    alive.add(e.getSource());
                }
            }
        }
    };

    private void startTimer(Long delay) {

        //LOG.info("EPFD start timer with delay " + delay.toString());
        ScheduleTimeout scheduleTimeout = new ScheduleTimeout(delay);
        scheduleTimeout.setTimeoutEvent(new CheckTimeout(scheduleTimeout));
        trigger(scheduleTimeout, timer);
    }

    {
        subscribe(startHandler, control);
        subscribe(topologyHandler, epfd);
        subscribe(timeoutHandler, timer);
        subscribe(messageHandler, net);
    }

}
