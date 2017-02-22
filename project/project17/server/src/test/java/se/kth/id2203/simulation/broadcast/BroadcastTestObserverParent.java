package se.kth.id2203.simulation.broadcast;

import se.sics.kompics.*;
import se.sics.kompics.network.Network;
import se.sics.kompics.timer.Timer;

public class BroadcastTestObserverParent extends ComponentDefinition
{
    // NOTE: Have to have a parent component to connect the child components

    Positive<Network> net = requires(Network.class);
    Positive<Timer> timer = requires(Timer.class);

    public BroadcastTestObserverParent()
    {
        Class<? extends ComponentDefinition> broadcastTestObserverClass = BroadcastTestObserver.class;
        Component broadcastTestObserver = create(broadcastTestObserverClass, Init.NONE);

        connect(net, broadcastTestObserver.getNegative(Network.class), Channel.TWO_WAY);
        connect(timer, broadcastTestObserver.getNegative(Timer.class), Channel.TWO_WAY);
    }
}
