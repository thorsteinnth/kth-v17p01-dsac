package se.kth.id2203.simulation.ops;

import se.kth.id2203.multipaxos.MultiPaxos;
import se.kth.id2203.multipaxos.MultiPaxosPort;
import se.sics.kompics.*;
import se.sics.kompics.network.Network;

public class ScenarioClientParent extends ComponentDefinition
{
    Positive<Network> net = requires(Network.class);

    protected final Component mpaxos = create(MultiPaxos.class, Init.NONE);

    public ScenarioClientParent()
    {
        Component parent = create(ScenarioClient.class, Init.NONE);

        connect(parent.getNegative(Network.class), net, Channel.TWO_WAY);
        connect(mpaxos.getPositive(MultiPaxosPort.class), parent.getNegative(MultiPaxosPort.class), Channel.TWO_WAY);

        connect(net, mpaxos.getNegative(Network.class), Channel.TWO_WAY);
    }
}
