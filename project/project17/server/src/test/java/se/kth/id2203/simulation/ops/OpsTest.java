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

import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author Lars Kroll <lkroll@kth.se>
 */
public class OpsTest {
    
    private static final int NUM_PUT_MESSAGES = 100;
    private static final int NUM_GET_MESSAGES = 100;
    private final SimulationResultMap res = SimulationResultSingleton.getInstance();

    @Test
    public void simplePutTest()
    {
        res.clear();

        long seed = 123;
        SimulationScenario.setSeed(seed);
        SimulationScenario simplePutScenario = ScenarioGen.simplePut(9);
        res.put("put_messages", NUM_PUT_MESSAGES);
        simplePutScenario.simulate(LauncherComp.class);

        for (int i = 0; i < NUM_PUT_MESSAGES; i++)
        {
            String result = res.get("PUT-" + Integer.toString(i), String.class);
            Assert.assertTrue(result != null && (result.equals("OK") || result.equals("ABORT")));
        }
    }

    @Test
    public void simplePutGetTest()
    {
        /**
         * Test to do 10 PUT operations, then 10 GET operations and
         * assert that the GET operations return correct values.
         */

        res.clear();

        long seed = 123;
        SimulationScenario.setSeed(seed);
        SimulationScenario simpleBootScenario = ScenarioGen.simplePutGet(9);
        res.put("put_messages", NUM_PUT_MESSAGES);
        res.put("get_messages", NUM_GET_MESSAGES);
        simpleBootScenario.simulate(LauncherComp.class);

        List<Integer> keysToCheck = new ArrayList<>();

        for (int i = 0; i < NUM_PUT_MESSAGES; i++)
        {
            String result = res.get("PUT-" + Integer.toString(i), String.class);
            Assert.assertTrue(result != null && (result.equals("OK") || result.equals("ABORT")));

            if (result != null && result.equals("OK"))
                keysToCheck.add(i);
        }

        for (int i = 0; i < NUM_GET_MESSAGES; i++)
        {
            String result = res.get(Integer.toString(i), String.class);
            Assert.assertTrue(result != null);

            if (keysToCheck.contains(i))
            {
                // We know that the PUT operation worked for that key, let's assert that the GET operation works
                // as expected
                Assert.assertTrue(result.equals("This is datavalue " + Integer.toString(i)) || result.equals("ABORT"));
            }
            else
            {
                // We know that there is no value for this key, lets check if we got the right status
                Assert.assertTrue(result.equals("NOT_FOUND") || result.equals("ABORT"));
            }
        }
    }

    @Test
    public void opsTest()
    {
        /**
         * The test asserts that we have performed 10 PUT operations, 10 GET operations and
         * that is has got a response for every CAS operation performed (depends on the number of successful GET ops).
         */

        res.clear();

        long seed = 123;
        SimulationScenario.setSeed(seed);
        SimulationScenario opsSequenceScenario = ScenarioGen.ops(9);
        res.put("put_messages", NUM_PUT_MESSAGES);
        res.put("get_messages", NUM_GET_MESSAGES);
        opsSequenceScenario.simulate(LauncherComp.class);

        for (int i = 0; i < NUM_PUT_MESSAGES; i++)
        {
            String result = res.get("PUT-" + Integer.toString(i), String.class);
            Assert.assertTrue(result != null && (result.equals("OK") || result.equals("ABORT")));
        }

        for (int i = 0; i < NUM_GET_MESSAGES; i++)
        {
            String result = res.get("GET-" + Integer.toString(i), String.class);
            Assert.assertTrue(result != null && (result.equals("OK") || result.equals("NOT_FOUND") || result.equals("ABORT")));
        }

        int numberOfCasOpsSent = res.get("cas_messages", Integer.class);
        int numberOfCasOpsConfirmed = 0;
        for (int i = 0; i < NUM_GET_MESSAGES; i++)
        {
            String result = res.get("CAS-" + Integer.toString(i), String.class);

            if (result != null)
            {
                Assert.assertTrue(result.equals("OK") || result.equals("ABORT"));
                numberOfCasOpsConfirmed++;
            }
        }

        Assert.assertEquals(numberOfCasOpsSent, numberOfCasOpsConfirmed);
    }
}
