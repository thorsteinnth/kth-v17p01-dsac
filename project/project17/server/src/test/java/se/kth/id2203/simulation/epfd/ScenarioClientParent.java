package se.kth.id2203.simulation.epfd;

import se.kth.id2203.epfd.EPFD;
import se.kth.id2203.epfd.EPFDPort;
import se.sics.kompics.*;
import se.sics.kompics.network.Network;
import se.sics.kompics.timer.Timer;

public class ScenarioClientParent extends ComponentDefinition {

    Positive<Network> network = requires(Network.class);
    Positive<Timer> timer = requires(Timer.class);

    protected final Component epfd = create(EPFD.class, Init.NONE);

    public ScenarioClientParent() {

        Component parent = create(ScenarioClient.class, Init.NONE);

        connect(parent.getNegative(Network.class), network, Channel.TWO_WAY);
        connect(parent.getNegative(Timer.class), timer, Channel.TWO_WAY);
        connect(epfd.getPositive(EPFDPort.class), parent.getNegative(EPFDPort.class), Channel.TWO_WAY);

        connect(network, epfd.getNegative(Network.class), Channel.TWO_WAY);
        connect(timer, epfd.getNegative(Timer.class), Channel.TWO_WAY);
    }
}
