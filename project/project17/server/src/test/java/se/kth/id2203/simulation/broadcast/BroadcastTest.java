package se.kth.id2203.simulation.broadcast;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.kth.id2203.simulation.ScenarioGen;
import se.sics.kompics.simulator.SimulationScenario;
import se.sics.kompics.simulator.run.LauncherComp;

public class BroadcastTest
{
    final static Logger LOG = LoggerFactory.getLogger(BroadcastTest.class);

    @Test
    public void broadcastTest()
    {
        // TODO Asserts
        LOG.info("RUNNING TEST: Broadcast");
        long seed = 123;
        SimulationScenario.setSeed(seed);
        SimulationScenario broadcastScenario = ScenarioGen.broadcast(10);
        //res.put("messages", NUM_MESSAGES);
        broadcastScenario.simulate(LauncherComp.class);
        //for (int i = 0; i < NUM_MESSAGES; i++) {
        //    Assert.assertEquals("OK", res.get("test"+i, String.class));
        //}
    }
}
