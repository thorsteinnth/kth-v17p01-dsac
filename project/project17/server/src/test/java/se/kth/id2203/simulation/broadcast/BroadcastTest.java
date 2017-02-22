package se.kth.id2203.simulation.broadcast;

import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.kth.id2203.simulation.SimulationResultMap;
import se.kth.id2203.simulation.SimulationResultSingleton;
import se.sics.kompics.simulator.SimulationScenario;
import se.sics.kompics.simulator.run.LauncherComp;

public class BroadcastTest
{
    final static Logger LOG = LoggerFactory.getLogger(BroadcastTest.class);
    private final SimulationResultMap res = SimulationResultSingleton.getInstance();

    @Test
    public void broadcastTest()
    {
        LOG.info("RUNNING TEST: Broadcast");
        long seed = 123;
        SimulationScenario.setSeed(seed);
        SimulationScenario broadcastScenario = ScenarioGen.broadcast(10);
        broadcastScenario.simulate(LauncherComp.class);

        // Make sure all broadcast messages were delivered
        // Should really find a way to do this better ...
        for (String key : res.keySet())
        {
            if (key.startsWith("broadcast-sent-destination-"))
            {
                String[] splitKey = key.split("-");
                String destinationIP = splitKey[3];
                String sentMessage = res.get(key, String.class);

                if (!res.keySet().contains("broadcast-delivered-destination-" + destinationIP))
                    Assert.fail("Result does not contain broadcast-delivered-destination-" + destinationIP);

                Assert.assertEquals(
                        "Wrong value for broadcast-delivered-destination-" + destinationIP
                                + " - Expected: " + sentMessage
                                + " - Got: " + res.get("broadcast-delivered-destination-" + destinationIP, String.class),
                        sentMessage,
                        res.get("broadcast-delivered-destination-" + destinationIP, String.class)
                );
            }
        }
    }
}
