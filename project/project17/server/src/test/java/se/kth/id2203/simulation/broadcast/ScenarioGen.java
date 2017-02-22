package se.kth.id2203.simulation.broadcast;

import se.kth.id2203.networking.NetAddress;
import se.sics.kompics.Init;
import se.sics.kompics.network.Address;
import se.sics.kompics.simulator.SimulationScenario;
import se.sics.kompics.simulator.adaptor.Operation;
import se.sics.kompics.simulator.adaptor.Operation1;
import se.sics.kompics.simulator.adaptor.distributions.extra.BasicIntSequentialDistribution;
import se.sics.kompics.simulator.events.system.StartNodeEvent;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Map;

public abstract class ScenarioGen
{
    private static final String broadcastObserverIP = "0.0.0.0";

    static Operation startBroadcastObserver = new Operation<StartNodeEvent>()
    {
        @Override
        public StartNodeEvent generate()
        {
            return new StartNodeEvent() {
                NetAddress selfAdr;

                {
                    try
                    {
                        selfAdr = new NetAddress(InetAddress.getByName(broadcastObserverIP), 0);
                    }
                    catch (UnknownHostException ex)
                    {
                        throw new RuntimeException(ex);
                    }
                }

                @Override
                public Address getNodeAddress()
                {
                    return selfAdr;
                }

                @Override
                public Class getComponentDefinition()
                {
                    return BroadcastTestObserverParent.class;
                }

                @Override
                public Init getComponentInit()
                {
                    return Init.NONE;
                }

                @Override
                public Map<String, Object> initConfigUpdate()
                {
                    HashMap<String, Object> config = new HashMap<>();
                    config.put("id2203.project.address", selfAdr);
                    return config;
                }
            };
        }
    };

    private static final Operation1 startClientBroadcast = new Operation1<StartNodeEvent, Integer>() {

        @Override
        public StartNodeEvent generate(final Integer self) {
            return new StartNodeEvent()
            {
                final NetAddress selfAdr;

                {
                    try
                    {
                        selfAdr = new NetAddress(InetAddress.getByName("192.168.1." + self), 45678);
                    }
                    catch (UnknownHostException ex)
                    {
                        throw new RuntimeException(ex);
                    }
                }

                @Override
                public Address getNodeAddress() {
                    return selfAdr;
                }

                @Override
                public Class getComponentDefinition()
                {
                    return ScenarioClientBroadcastParent.class;
                }

                @Override
                public String toString() {
                    return "StartClient<" + selfAdr.toString() + ">";
                }

                @Override
                public Init getComponentInit() {
                    return Init.NONE;
                }

                @Override
                public Map<String, Object> initConfigUpdate()
                {
                    HashMap<String, Object> config = new HashMap<>();
                    config.put("id2203.project.address", selfAdr);

                    try
                    {
                        config.put("id2203.project.observerAddress",
                                new NetAddress(InetAddress.getByName(broadcastObserverIP), 0));
                    }
                    catch (UnknownHostException ex)
                    {
                        throw new RuntimeException(ex);
                    }

                    return config;
                }
            };
        }
    };

    public static SimulationScenario broadcast(final int clients)
    {
        return new SimulationScenario()
        {
            {
                SimulationScenario.StochasticProcess observer = new SimulationScenario.StochasticProcess() {
                    {
                        raise(1, startBroadcastObserver);
                    }
                };

                SimulationScenario.StochasticProcess startClients = new SimulationScenario.StochasticProcess() {
                    {
                        eventInterArrivalTime(constant(1000));
                        raise(clients, startClientBroadcast, new BasicIntSequentialDistribution(1));
                    }
                };

                observer.start();
                startClients.startAfterStartOf(10, observer);
                terminateAfterTerminationOf(100000, startClients);
            }
        };
    }
}
