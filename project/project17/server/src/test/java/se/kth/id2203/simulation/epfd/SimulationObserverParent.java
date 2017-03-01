package se.kth.id2203.simulation.epfd;

import se.sics.kompics.*;
import se.sics.kompics.network.Network;
import se.sics.kompics.timer.Timer;

public class SimulationObserverParent extends ComponentDefinition
{
    // NOTE: Have to have a parent component to connect the child components

    Positive<Network> net = requires(Network.class);
    Positive<Timer> timer = requires(Timer.class);

    public SimulationObserverParent()
    {
        Class<? extends ComponentDefinition> epfdTestObserverClass = SimulationObserver.class;
        Component testObserver = create(epfdTestObserverClass, Init.NONE);

        connect(net, testObserver.getNegative(Network.class), Channel.TWO_WAY);
        connect(timer, testObserver.getNegative(Timer.class), Channel.TWO_WAY);
    }
}
