package se.kth.id2203.simulation.epfd;

import org.junit.Assert;
import org.junit.Test;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.kth.id2203.networking.NetAddress;
import se.kth.id2203.simulation.ScenarioClientGet;
import se.kth.id2203.simulation.SimulationResultMap;
import se.kth.id2203.simulation.SimulationResultSingleton;
import se.sics.kompics.simulator.SimulationScenario;
import se.sics.kompics.simulator.run.LauncherComp;

import java.util.Set;

import static se.kth.id2203.simulation.epfd.ScenarioClient.LOG;

public class EPFDTest {

    /*
    An EPFD, defined in a partially synchronous model, should satisfy the following properties:

    1. Completeness: Every process that crashes should be eventually suspected permanently by every correct process
    2. Eventual Strong Accuracy: No correct process should be eventually suspected by any other correct process
     */

    private final static Logger LOG = LoggerFactory.getLogger(ScenarioClientGet.class);
    private final SimulationResultMap res = SimulationResultSingleton.getInstance();

    @Test
    public void epfdCompleteness() {

        long seed = 123;
        SimulationScenario.setSeed(seed);
        SimulationScenario simpleEPFDScenario = ScenarioGen.epfdCompleteness(4);

        simpleEPFDScenario.simulate(LauncherComp.class);

        LOG.debug("EPFDTest - number of suspects = " + res.keySet().size());
        Assert.assertEquals(3, res.keySet().size());
    }
}
