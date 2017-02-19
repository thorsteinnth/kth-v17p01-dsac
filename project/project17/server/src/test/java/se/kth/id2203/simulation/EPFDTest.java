package se.kth.id2203.simulation;

import org.junit.Assert;
import org.junit.Test;
import se.sics.kompics.config.Config;
import se.sics.kompics.simulator.SimulationScenario;
import se.sics.kompics.simulator.run.LauncherComp;
import se.sics.kompics.simulator.util.GlobalView;

public class EPFDTest {

    /*
    An EPFD, defined in a partially synchronous model, should satisfy the following properties:

    1. Completeness: Every process that crashes should be eventually suspected permanently by every correct process
    2. Eventual Strong Accuracy: No correct process should be eventually suspected by any other correct process
     */

    private final SimulationResultEPFD res = SimulationResultEPFD.getInstance();

    @Test
    public void epfdCompleteness() {
        long seed = 122;
        SimulationScenario.setSeed(seed);
        SimulationScenario simpleEPFDScenario = ScenarioGen.epfdCompleteness(3);

        simpleEPFDScenario.simulate(LauncherComp.class);

        Assert.assertEquals(0, res.getSuspected().size());
    }
}
