package org.bitstrings.idea.plugins.mavenprime.run;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.bitstrings.idea.plugins.mavenprime.execution.MavenPrimeRequest;
import org.junit.Test;

public class MavenPrimeTestConfigurationProducerTest
{
    private static final String MODULE_DIRECTORY = "/repo/service";

    private static final TestSelection METHOD =
        new TestSelection("CartTest#addsAnItem", "CartTest.addsAnItem", null);

    @Test
    public void requestFor_aSelectedTestMethod_runsSurefireOnThatMethodOnly()
    {
        MavenPrimeRequest request = MavenPrimeTestConfigurationProducer.requestFor(MODULE_DIRECTORY, METHOD);

        assertEquals(
            "without the test property Maven runs the whole module, which is not what the user clicked",
            "CartTest#addsAnItem",
            request.properties.get(MavenPrimeRequest.TEST_PROPERTY));
        assertEquals(List.of("test"), request.goals);
    }

    @Test
    public void requestFor_aSelectedTestMethod_runsInTheModuleThatOwnsIt()
    {
        assertEquals(
            "running from the reactor root would build every module before reaching this test",
            MODULE_DIRECTORY,
            MavenPrimeTestConfigurationProducer.requestFor(MODULE_DIRECTORY, METHOD).workingDirectory);
    }

    @Test
    public void requestFor_aSelectedTestMethod_marksTheRunAsSelectingTests()
    {
        assertTrue(
            "the test tree only appears for a run that selects tests, so this flag is what shows it",
            MavenPrimeTestConfigurationProducer.requestFor(MODULE_DIRECTORY, METHOD).selectsTests());
    }

    @Test
    public void requestFor_aSelectedTestClass_namesTheRunSoItIsNotMistakenForTheJunitEntry()
    {
        TestSelection testClass = new TestSelection("CartTest", "CartTest", null);

        String name = MavenPrimeTestConfigurationProducer.requestFor(MODULE_DIRECTORY, testClass).name;

        assertTrue(name, name.contains("CartTest"));
        assertTrue(
            "the gutter popup lists this beside JUnit's entry for the same test, so an identical name "
                + "leaves the user guessing which one runs Maven",
            name.contains("Maven Prime"));
    }
}
