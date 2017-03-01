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
package se.kth.id2203.simulation.ops;

import junit.framework.Assert;
import org.junit.Test;
import se.kth.id2203.simulation.SimulationResultMap;
import se.kth.id2203.simulation.SimulationResultSingleton;
import se.sics.kompics.simulator.SimulationScenario;
import se.sics.kompics.simulator.run.LauncherComp;

/**
 *
 * @author Lars Kroll <lkroll@kth.se>
 */
public class OpsTest {
    
    private static final int NUM_MESSAGES = 10;
    private final SimulationResultMap res = SimulationResultSingleton.getInstance();

    // TODO Create tests for mpaxos
    // The tests should probably check that for every operation we send to paxos, the operation is either put into
    // the sequence (returned from paxos) or aborted (i.e. get abort message from paxos).
    // All operations that we get from paxos should be in the same order on all processes, i.e. the decided sequence
    // should be the same for all processes (so we get atomic multicast functionality).
    // Abort messages are a valid result I think, they just mean that they couldn't reach a consensus.
    // The application should then just decide how it wants to handle the abort messages (try again, abort the op ...)

    /*
    @Test
    public void simpleOpsTest()
    {
        // Test for put and get operations, first put then get.
        // (we do not have any pre loaded data at the moment)

        res.clear();

        long seed = 123;
        SimulationScenario.setSeed(seed);
        SimulationScenario simpleBootScenario = ScenarioGen.simpleOps(3);
        res.put("messages", NUM_MESSAGES);
        simpleBootScenario.simulate(LauncherComp.class);

        System.out.println("simpleOpsTest - RESULTMAP AFTER RUN: " + res.toString());

        for (int i = 0; i < NUM_MESSAGES; i++)
        {
            Assert.assertEquals("OK", res.get("PUT-" + Integer.toString(i), String.class));
        }

        // We have (NUM_MESSAGES-1) GET operations that should be ok
        for (int i = 0; i < NUM_MESSAGES-1; i++)
        {
            Assert.assertEquals("OK", res.get("GET-" + Integer.toString(i), String.class));
        }

        // And one more that should be not found
        Assert.assertEquals("NOT_FOUND", res.get("GET-" + "NONSENSE", String.class));
    }

    @Test
    public void simplePutTest()
    {
        res.clear();

        long seed = 123;
        SimulationScenario.setSeed(seed);
        SimulationScenario simplePutScenario = ScenarioGen.simplePut(4);

        res.put("messages", NUM_MESSAGES);
        simplePutScenario.simulate(LauncherComp.class);

        System.out.println("simplePutTest - RESULTMAP AFTER RUN: " + res.toString());

        for (int i = 0; i < NUM_MESSAGES; i++)
        {
            Assert.assertEquals("OK", res.get("PUT-" + Integer.toString(i), String.class));
        }
    }
    */
}
