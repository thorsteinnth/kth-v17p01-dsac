package se.kth.id2203.simulation.epfd;

import org.junit.Assert;
import org.junit.Test;

import se.sics.kompics.simulator.SimulationScenario;
import se.sics.kompics.simulator.run.LauncherComp;

public class EPFDTest {

    /*
    An EPFD, defined in a partially synchronous model, should satisfy the following properties:

    1. Completeness: Every process that crashes should be eventually suspected permanently by every correct process
    2. Eventual Strong Accuracy: No correct process should be eventually suspected by any other correct process
     */

    private final SimulationResult res = SimulationResult.getInstance();

    @Test
    public void epfdCompleteness() {

        long seed = 123;
        SimulationScenario.setSeed(seed);
        SimulationScenario simpleEPFDScenario = ScenarioGen.epfdCompleteness(4);


        simpleEPFDScenario.simulate(LauncherComp.class);

        Assert.assertEquals(0, res.getSuspected().size());
    }
}
