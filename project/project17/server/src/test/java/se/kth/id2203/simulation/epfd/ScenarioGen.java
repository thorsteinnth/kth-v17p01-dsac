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
package se.kth.id2203.simulation.epfd;

import se.kth.id2203.networking.NetAddress;
import se.sics.kompics.Init;
import se.sics.kompics.network.Address;
import se.sics.kompics.simulator.SimulationScenario;
import se.sics.kompics.simulator.adaptor.Operation;
import se.sics.kompics.simulator.adaptor.Operation1;
import se.sics.kompics.simulator.adaptor.distributions.extra.BasicIntSequentialDistribution;
import se.sics.kompics.simulator.events.system.KillNodeEvent;
import se.sics.kompics.simulator.events.system.StartNodeEvent;
import sun.rmi.runtime.Log;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Map;

public abstract class ScenarioGen {

    private static final Operation1 startClient = new Operation1<StartNodeEvent, Integer>() {

        @Override
        public StartNodeEvent generate(final Integer self) {
            return new StartNodeEvent() {
                final NetAddress selfAdr;
                final NetAddress bsAdr;

                {
                    try {
                        selfAdr = new NetAddress(InetAddress.getByName("192.168.1." + self), 45678);
                        bsAdr = new NetAddress(InetAddress.getByName("192.168.0.1"), 45678);
                    } catch (UnknownHostException ex) {
                        throw new RuntimeException(ex);
                    }
                }

                @Override
                public Address getNodeAddress() {
                    return selfAdr;
                }

                @Override
                public Class getComponentDefinition() {
                    return ScenarioClientParent.class;
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
                public Map<String, Object> initConfigUpdate() {
                    HashMap<String, Object> config = new HashMap<>();
                    config.put("id2203.project.address", selfAdr);
                    config.put("id2203.project.bootstrap-address", bsAdr);
                    return config;
                }
            };
        }
    };

    static Operation startObserver = new Operation<StartNodeEvent>()
    {
        @Override
        public StartNodeEvent generate()
        {
            return new StartNodeEvent() {
                NetAddress selfAdr;

                {
                    try
                    {
                        selfAdr = new NetAddress(InetAddress.getByName("0.0.0.0"), 0);
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
                    return SimulationObserverParent.class;
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

    static Operation1 killClientOp = new Operation1<KillNodeEvent, Integer>() {
        @Override
        public KillNodeEvent generate(final Integer self) {
            return new KillNodeEvent() {
                NetAddress selfAdr;

                {
                    try {
                        selfAdr = new NetAddress(InetAddress.getByName("192.168.1." + self), 45678);
                    } catch (UnknownHostException ex) {
                        throw new RuntimeException(ex);
                    }
                }

                @Override
                public Address getNodeAddress() {
                    return selfAdr;
                }

                @Override
                public String toString() {
                    return "KillClient<" + selfAdr.toString() + ">";
                }
            };
        }
    };

    public static SimulationScenario epfdCompleteness(final int clients) {
        return new SimulationScenario() {
            {
                StochasticProcess startClients = new StochasticProcess() {
                    {
                        eventInterArrivalTime(constant(1000));
                        raise(clients, startClient, new BasicIntSequentialDistribution(1));
                    }
                };

                SimulationScenario.StochasticProcess observer = new SimulationScenario.StochasticProcess() {
                    {
                        raise(1, startObserver);
                    }
                };

                StochasticProcess killClients = new StochasticProcess() {
                    {
                        eventInterArrivalTime(constant(0));
                        raise(2, killClientOp, new BasicIntSequentialDistribution((1)));
                    }
                };

                startClients.start();
                observer.startAfterTerminationOf(0, startClients);
                killClients.startAfterTerminationOf(2000, startClients);
                terminateAfterTerminationOf(10*100000, startClients);
            }
        };
    }

    public static SimulationScenario epfdStrongAccuracy(final int clients) {
        return new SimulationScenario() {
            {
                StochasticProcess startClients = new StochasticProcess() {
                    {
                        eventInterArrivalTime(constant(0));
                        raise(clients, startClient, new BasicIntSequentialDistribution(1));
                    }
                };

                SimulationScenario.StochasticProcess observer = new SimulationScenario.StochasticProcess() {
                    {
                        raise(1, startObserver);
                    }
                };

                startClients.start();
                observer.startAfterTerminationOf(0, startClients);
                terminateAfterTerminationOf(10*100000, startClients);
            }
        };
    }
}
