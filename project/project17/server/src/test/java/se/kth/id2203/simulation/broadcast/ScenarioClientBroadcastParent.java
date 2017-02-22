package se.kth.id2203.simulation.broadcast;

import se.kth.id2203.broadcast.beb.BestEffortBroadcast;
import se.kth.id2203.broadcast.beb.BestEffortBroadcastPort;
import se.kth.id2203.broadcast.rb.ReliableBroadcast;
import se.kth.id2203.broadcast.rb.ReliableBroadcastPort;
import se.sics.kompics.*;
import se.sics.kompics.network.Network;
import se.sics.kompics.timer.Timer;

public class ScenarioClientBroadcastParent extends ComponentDefinition
{
    // NOTE: Have to have a parent component to connect the child components

    Positive<Network> net = requires(Network.class);
    Positive<Timer> timer = requires(Timer.class);

    protected final Component beb = create(BestEffortBroadcast.class, Init.NONE);
    protected final Component rb = create(ReliableBroadcast.class, Init.NONE);

    public ScenarioClientBroadcastParent()
    {
        connect(net, beb.getNegative(Network.class), Channel.TWO_WAY);
        connect(beb.getPositive(BestEffortBroadcastPort.class), rb.getNegative(BestEffortBroadcastPort.class), Channel.TWO_WAY);

        Class<? extends ComponentDefinition> scenarioClientBroadcastClass = ScenarioClientBroadcast.class;
        Component scenarioClientBroadcast = create(scenarioClientBroadcastClass, Init.NONE);

        connect(net, scenarioClientBroadcast.getNegative(Network.class), Channel.TWO_WAY);
        connect(beb.getPositive(BestEffortBroadcastPort.class), scenarioClientBroadcast.getNegative(BestEffortBroadcastPort.class), Channel.TWO_WAY);
        connect(rb.getPositive(ReliableBroadcastPort.class), scenarioClientBroadcast.getNegative(ReliableBroadcastPort.class), Channel.TWO_WAY);
    }
}
