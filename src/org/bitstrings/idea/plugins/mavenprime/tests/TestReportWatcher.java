package org.bitstrings.idea.plugins.mavenprime.tests;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

public final class TestReportWatcher
{
    private static final List<String> REPORT_DIRECTORIES = List.of("surefire-reports", "failsafe-reports");

    private static final String REPORT_GLOB = "TEST-*.xml";

    private final List<Path> buildDirectories;

    private final long notBefore;

    private final Consumer<String> messages;

    private final Set<Path> reported = new HashSet<>();

    public TestReportWatcher(List<Path> buildDirectories, long notBefore, Consumer<String> messages)
    {
        this.buildDirectories = List.copyOf(buildDirectories);
        this.notBefore = notBefore;
        this.messages = messages;
    }

    public void scan()
    {
        for (Path buildDirectory : buildDirectories)
        {
            for (String reportDirectory : REPORT_DIRECTORIES)
            {
                scanDirectory(buildDirectory.resolve(reportDirectory));
            }
        }
    }

    public boolean foundAnyReport()
    {
        return !reported.isEmpty();
    }

    private void scanDirectory(Path directory)
    {
        if (!Files.isDirectory(directory))
        {
            return;
        }

        try (DirectoryStream<Path> reports = Files.newDirectoryStream(directory, REPORT_GLOB))
        {
            for (Path report : reports)
            {
                report(report);
            }
        }
        catch (IOException unreadable)
        {
            return;
        }
    }

    private void report(Path report)
    {
        if (reported.contains(report) || leftOverFromAnEarlierBuild(report))
        {
            return;
        }

        List<TestCaseResult> results;

        try
        {
            results = SurefireReport.parse(Files.readString(report, StandardCharsets.UTF_8));
        }
        catch (IOException stillBeingWritten)
        {
            return;
        }

        reported.add(report);

        TestTreeMessages.of(results).forEach(messages);
    }

    private boolean leftOverFromAnEarlierBuild(Path report)
    {
        try
        {
            return Files.getLastModifiedTime(report).toMillis() < notBefore;
        }
        catch (IOException gone)
        {
            return true;
        }
    }
}
