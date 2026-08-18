package org.bitstrings.idea.plugins.mavenprime.build;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.bitstrings.idea.plugins.mavenprime.build.MavenLogEvent.BuildFinished;
import org.bitstrings.idea.plugins.mavenprime.build.MavenLogEvent.ModuleResult;
import org.bitstrings.idea.plugins.mavenprime.build.MavenLogEvent.ModuleStarted;
import org.bitstrings.idea.plugins.mavenprime.build.MavenLogEvent.MojoStarted;
import org.bitstrings.idea.plugins.mavenprime.build.MavenLogEvent.Problem;
import org.bitstrings.idea.plugins.mavenprime.build.MavenLogEvent.Severity;
import org.bitstrings.idea.plugins.mavenprime.spy.SpyProtocol;

public final class SpyEvents
{
    private static final int TYPE = 0;

    private static final int KEY = 1;

    private static final int DETAIL = 2;

    private static final int EXTRA = 3;

    private static final Pattern POSITION_PATTERN =
        Pattern.compile("^(\\S.*?):\\[(\\d{1,9}),(\\d{1,9})]\\s*(.*)$");

    private SpyEvents()
    {
    }

    public static Optional<MavenLogEvent> parse(String line)
    {
        String[] fields = SpyProtocol.decode(line);

        if (fields.length == 0)
        {
            return Optional.empty();
        }

        return switch (fields[TYPE])
        {
            case SpyProtocol.PROJECT_STARTED -> moduleStarted(fields);
            case SpyProtocol.PROJECT_SUCCEEDED -> result(fields, true, false);
            case SpyProtocol.PROJECT_FAILED -> result(fields, false, false);
            case SpyProtocol.PROJECT_SKIPPED -> result(fields, false, true);
            case SpyProtocol.MOJO_STARTED -> mojoStarted(fields);
            case SpyProtocol.MOJO_FAILED -> mojoFailed(fields);
            case SpyProtocol.MOJO_PROBLEM -> mojoProblem(fields);
            case SpyProtocol.SESSION_ENDED -> Optional.of(new BuildFinished(StringUtils.isBlank(field(fields, KEY))));
            default -> Optional.empty();
        };
    }

    public static long droppedCount(String line)
    {
        String[] fields = SpyProtocol.decode(line);

        if ((fields.length == 0) || !SpyProtocol.DROPPED.equals(fields[TYPE]))
        {
            return 0L;
        }

        return Math.max(0L, NumberUtils.toLong(field(fields, KEY), 0L));
    }

    private static Optional<MavenLogEvent> moduleStarted(String[] fields)
    {
        String key = field(fields, KEY);

        if (StringUtils.isBlank(key))
        {
            return Optional.empty();
        }

        return Optional.of(
            new ModuleStarted(StringUtils.substringBefore(key, ":"), StringUtils.substringAfter(key, ":")));
    }

    private static Optional<MavenLogEvent> result(String[] fields, boolean success, boolean skipped)
    {
        String key = field(fields, KEY);

        return StringUtils.isBlank(key)
            ? Optional.empty()
            : Optional.of(new ModuleResult(key, success, skipped, field(fields, DETAIL)));
    }

    private static Optional<MavenLogEvent> mojoStarted(String[] fields)
    {
        String goal = field(fields, DETAIL);

        return StringUtils.isBlank(goal)
            ? Optional.empty()
            : Optional.of(new MojoStarted(goal, field(fields, EXTRA), field(fields, KEY)));
    }

    private static Optional<MavenLogEvent> mojoFailed(String[] fields)
    {
        String failure = field(fields, EXTRA);

        return Optional.of(
            Problem.withoutPosition(
                Severity.ERROR,
                StringUtils.isBlank(failure) ? field(fields, DETAIL) : failure));
    }

    private static Optional<MavenLogEvent> mojoProblem(String[] fields)
    {
        String diagnostic = field(fields, DETAIL);

        if (StringUtils.isBlank(diagnostic))
        {
            return Optional.empty();
        }

        Matcher position = POSITION_PATTERN.matcher(diagnostic);

        if (!position.matches())
        {
            return Optional.of(Problem.withoutPosition(Severity.ERROR, diagnostic));
        }

        return Optional.of(
            new Problem(
                Severity.ERROR,
                position.group(4).trim(),
                position.group(1).trim(),
                Integer.parseInt(position.group(2)),
                Integer.parseInt(position.group(3))));
    }

    private static String field(String[] fields, int index)
    {
        return (index < fields.length) ? fields[index] : StringUtils.EMPTY;
    }
}
