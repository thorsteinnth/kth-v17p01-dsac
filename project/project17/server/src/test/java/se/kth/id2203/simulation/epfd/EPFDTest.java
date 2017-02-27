package se.kth.id2203.simulation.epfd;

import org.junit.Assert;
import org.junit.Test;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.kth.id2203.simulation.SimulationResultMap;
import se.kth.id2203.simulation.SimulationResultSingleton;
import se.sics.kompics.simulator.SimulationScenario;
import se.sics.kompics.simulator.run.LauncherComp;

import java.util.ArrayList;
import java.util.List;

public class EPFDTest {

    /*
    An EPFD, defined in a partially synchronous model, should satisfy the following properties:

    1. Completeness: Every process that crashes should be eventually suspected permanently by every correct process
    2. Eventual Strong Accuracy: No correct process should be eventually suspected by any other correct process
     */

    private final static Logger LOG = LoggerFactory.getLogger(EPFDTest.class);
    private final SimulationResultMap res = SimulationResultSingleton.getInstance();

    @Test
    public void epfdCompleteness() {

        /*
        In this test we start up four clients, then kill two of the clients.
        We then check for each of the two alive clients if they have the two crashed clients suspected.
         */

        long seed = 123;
        SimulationScenario.setSeed(seed);
        SimulationScenario simpleEPFDScenario = ScenarioGen.epfdCompleteness(4);
        simpleEPFDScenario.simulate(LauncherComp.class);

        // The system should be stable by now
        Assert.assertEquals(true, res.get("systemStable", Boolean.class));
        res.remove("systemStable");

        // First check if there are two correct processes that have suspects
        Assert.assertEquals(2, res.keySet().size());

        for (String process : res.keySet()) {

            // Then we check that each correct process suspects the two crashed processes
            List<String> suspected = res.get(process, new ArrayList<String>().getClass());
            LOG.debug("EPFD Test: " + process + " suspects " + suspected.toString());
            Assert.assertEquals(2, suspected.size());
        }

        // Finish by clearing the result map
        res.clear();
    }

    @Test
    public void epfdStrongAccuracy() {

        /*
        In this test we start up four clients and let them all live.
        We then wait and check if the system is stable, and then check if any of the processes have
        any other processes suspected, if not the test has passed.
         */

        long seed = 123;
        SimulationScenario.setSeed(seed);
        SimulationScenario simpleEPFDScenario = ScenarioGen.epfdStrongAccuracy(4);
        simpleEPFDScenario.simulate(LauncherComp.class);

        // The system should be stable by now
        Assert.assertEquals(true, res.get("systemStable", Boolean.class));
        res.remove("systemStable");

        // No process should suspect another
        Assert.assertEquals(0, res.keySet().size());

        // Finish by clearing the result map
        res.clear();
    }
}
