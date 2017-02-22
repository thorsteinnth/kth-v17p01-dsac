package se.kth.id2203.simulation.broadcast;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.kth.id2203.networking.Message;
import se.kth.id2203.networking.NetAddress;
import se.sics.kompics.*;
import se.sics.kompics.network.Address;
import se.sics.kompics.network.Network;
import se.sics.kompics.simulator.util.GlobalView;
import se.sics.kompics.timer.CancelPeriodicTimeout;
import se.sics.kompics.timer.SchedulePeriodicTimeout;
import se.sics.kompics.timer.Timeout;
import se.sics.kompics.timer.Timer;

import java.util.Collection;
import java.util.UUID;

/**
 * Observer that waits for 10 nodes to be up and running before starting the
 * broadcast test.
 * Adapted from kompics tutorial.
 * */
public class BroadcastTestObserver extends ComponentDefinition
{
    private static final Logger LOG = LoggerFactory.getLogger(BroadcastTestObserver.class);

    Positive<Timer> timer = requires(Timer.class);
    Positive<Network> net = requires(Network.class);

    private UUID timerId;
    private final int minNumberOfRequiredNodes;
    private final NetAddress self = config().getValue("id2203.project.address", NetAddress.class);

    public BroadcastTestObserver()
    {
        // Hardcoded for now
        this.minNumberOfRequiredNodes = 10;

        subscribe(handleStart, control);
        subscribe(handleCheckTimeout, timer);
    }

    //region Handlers

    private Handler<Start> handleStart = new Handler<Start>()
    {
        @Override
        public void handle(Start event)
        {
            LOG.info("Starting up ...");
            schedulePeriodicCheckTimeout();
        }
    };

    @Override
    public void tearDown()
    {
        trigger(new CancelPeriodicTimeout(timerId), timer);
    }

    private Handler<CheckTimeout> handleCheckTimeout = new Handler<CheckTimeout>()
    {
        @Override
        public void handle(CheckTimeout event)
        {
            // Check if we have minNumberOfRequiredNodes nodes up and running
            // Start broadcast test when that happens

            GlobalView gv = config().getValue("simulation.globalview", GlobalView.class);

            if (gv.getAliveNodes().size() >= minNumberOfRequiredNodes + 1) // +1 is the observer itself
            {
                LOG.info("Have " + gv.getAliveNodes().size() + " alive nodes. Starting broadcast test on first node.");

                // Find first node in alive nodes that isn't myself, and make it start a broadcast
                Collection<Address> nodeAddresses = gv.getAliveNodes().values();
                for (Address address : nodeAddresses)
                {
                    NetAddress destNetAddress = new NetAddress(address.getIp(), address.getPort());
                    if (!destNetAddress.equals(self))
                    {
                        trigger(new Message(self, destNetAddress, new StartBroadcastTest()), net);
                        break;
                    }
                }

                // Cancel the timer, we have done what we need to do
                trigger(new CancelPeriodicTimeout(timerId), timer);
            }
        }
    };

    //endregion

    private void schedulePeriodicCheckTimeout()
    {
        long period = 1000;
        SchedulePeriodicTimeout spt = new SchedulePeriodicTimeout(period, period);
        CheckTimeout timeout = new CheckTimeout(spt);
        spt.setTimeoutEvent(timeout);
        trigger(spt, timer);
        timerId = timeout.getTimeoutId();
    }

    public static class CheckTimeout extends Timeout
    {
        public CheckTimeout(SchedulePeriodicTimeout spt)
        {
            super(spt);
        }
    }
}
