package se.kth.id2203.simulation.epfd;

import se.sics.kompics.*;
import se.sics.kompics.network.Network;
import se.sics.kompics.timer.Timer;

public class ScenarioClientParent extends ComponentDefinition {

    Positive<Network> network = requires(Network.class);
    Positive<Timer> timer = requires(Timer.class);

    public ScenarioClientParent() {

        Component parent = create(ScenarioClient.class, Init.NONE);

        connect(parent.getNegative(Network.class), network, Channel.TWO_WAY);
        connect(parent.getNegative(Timer.class), timer, Channel.TWO_WAY);
    }
}
