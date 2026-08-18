package org.bitstrings.idea.plugins.mavenprime.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class TestReportWatcherTest
{
    private static final String REPORT =
        "<testsuite name=\"org.example.CartTest\">"
            + "<testcase name=\"addsAnItem\" classname=\"org.example.CartTest\" time=\"0.1\"/>"
            + "</testsuite>";

    private static final String TRUNCATED = "<testsuite name=\"org.example.CartTest\"><testcase name=\"ad";

    private static final long BEFORE_THE_RUN = 0L;

    private Path root;

    private final List<String> messages = new ArrayList<>();

    @Before
    public void createReactor()
        throws IOException
    {
        root = Files.createTempDirectory("mavenprime-reports");
    }

    @After
    public void deleteReactor()
        throws IOException
    {
        try (var paths = Files.walk(root))
        {
            paths.sorted(Comparator.reverseOrder()).forEach(TestReportWatcherTest::delete);
        }
    }

    @Test
    public void scan_aReportSurefireFinishedWriting_reportsItsCases()
        throws IOException
    {
        writeReport(root, "surefire-reports", "TEST-org.example.CartTest.xml", REPORT);

        watcher().scan();

        assertTrue(messages.toString(), messages.stream().anyMatch(m -> m.contains("addsAnItem")));
    }

    @Test
    public void scan_calledAgainWithNothingNew_reportsEachCaseOnlyOnce()
        throws IOException
    {
        writeReport(root, "surefire-reports", "TEST-org.example.CartTest.xml", REPORT);

        TestReportWatcher watcher = watcher();

        watcher.scan();

        int afterFirstScan = messages.size();

        watcher.scan();

        assertEquals(
            "polling has to be idempotent, or the tree grows a duplicate on every tick",
            afterFirstScan,
            messages.size());
    }

    @Test
    public void scan_aReportStillBeingWritten_waitsForItInsteadOfReportingHalfOfIt()
        throws IOException
    {
        Path report = writeReport(root, "surefire-reports", "TEST-org.example.CartTest.xml", TRUNCATED);

        TestReportWatcher watcher = watcher();

        watcher.scan();

        assertEquals("a truncated report must not reach the tree", List.of(), messages);

        Files.writeString(report, REPORT, StandardCharsets.UTF_8);

        watcher.scan();

        assertTrue(
            "once the report is complete the next tick has to pick it up",
            messages.stream().anyMatch(m -> m.contains("addsAnItem")));
    }

    @Test
    public void scan_aFailsafeReportInAnotherModule_isReportedToo()
        throws IOException
    {
        Path module = Files.createDirectories(root.resolve("service"));

        writeReport(module, "failsafe-reports", "TEST-org.example.CartIT.xml", REPORT);

        new TestReportWatcher(
            List.of(root.resolve("target"), module.resolve("target")), BEFORE_THE_RUN, messages::add)
            .scan();

        assertTrue(
            "integration tests live in failsafe-reports, in whichever module ran them",
            messages.stream().anyMatch(m -> m.contains("addsAnItem")));
    }

    @Test
    public void scan_aReportLeftBehindByAnEarlierBuild_isNotShownAsThisRunsResult()
        throws IOException
    {
        Path report = writeReport(root, "surefire-reports", "TEST-org.example.CartTest.xml", REPORT);

        Files.setLastModifiedTime(report, FileTime.fromMillis(1_000L));

        new TestReportWatcher(List.of(root.resolve("target")), 2_000L, messages::add).scan();

        assertEquals(
            "a report from the previous build reads as this run's result, and the reader trusts it",
            List.of(),
            messages);
    }

    @Test
    public void foundAnyReport_aRunThatNeverWroteOne_staysFalseSoTheEmptyTreeCanBeExplained()
    {
        TestReportWatcher watcher = watcher();

        watcher.scan();

        assertFalse(
            "an empty tree with no explanation is the failure the user cannot act on",
            watcher.foundAnyReport());
    }

    @Test
    public void foundAnyReport_aRunThatWroteOne_saysSo()
        throws IOException
    {
        writeReport(root, "surefire-reports", "TEST-org.example.CartTest.xml", REPORT);

        TestReportWatcher watcher = watcher();

        watcher.scan();

        assertTrue(watcher.foundAnyReport());
    }

    private TestReportWatcher watcher()
    {
        return new TestReportWatcher(List.of(root.resolve("target")), BEFORE_THE_RUN, messages::add);
    }

    private static Path writeReport(Path moduleDirectory, String reportDirectory, String name, String xml)
        throws IOException
    {
        Path directory = Files.createDirectories(moduleDirectory.resolve("target").resolve(reportDirectory));

        return Files.writeString(directory.resolve(name), xml, StandardCharsets.UTF_8);
    }

    private static void delete(Path path)
    {
        try
        {
            Files.deleteIfExists(path);
        }
        catch (IOException failure)
        {
            throw new IllegalStateException("the temporary reactor outlived the test: " + path, failure);
        }
    }
}
