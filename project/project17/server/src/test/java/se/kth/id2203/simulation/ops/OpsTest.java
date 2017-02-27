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

    @Test
    public void simpleOpsTest()
    {
        // TODO FIX LATER
        return;

        /*
        long seed = 123;
        SimulationScenario.setSeed(seed);
        SimulationScenario simpleBootScenario = ScenarioGen.simpleOps(3);
        res.put("messages", NUM_MESSAGES);
        simpleBootScenario.simulate(LauncherComp.class);

        // We have (NUM_MESSAGES-1) operations that should be ok
        for (int i = 0; i < NUM_MESSAGES-1; i++)
        {
            Assert.assertEquals("OK", res.get(Integer.toString(i), String.class));
        }

        // And one more that should be not found
        Assert.assertEquals("NOT_FOUND", res.get("NONSENSE", String.class));
        */
    }

    @Test
    public void simplePutTest() {

        long seed = 123;
        SimulationScenario.setSeed(seed);
        SimulationScenario simplePutScenario = ScenarioGen.simplePut(4);

        // TODO : Finish up simple put test
    }

}
