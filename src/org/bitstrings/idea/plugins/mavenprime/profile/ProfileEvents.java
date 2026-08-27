package org.bitstrings.idea.plugins.mavenprime.profile;

import java.util.Optional;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.bitstrings.idea.plugins.mavenprime.profile.ProfileEvent.DownloadTiming;
import org.bitstrings.idea.plugins.mavenprime.profile.ProfileEvent.ModuleFailed;
import org.bitstrings.idea.plugins.mavenprime.profile.ProfileEvent.ModuleTiming;
import org.bitstrings.idea.plugins.mavenprime.profile.ProfileEvent.MojoTiming;
import org.bitstrings.idea.plugins.mavenprime.profile.ProfileEvent.ReactorEdge;
import org.bitstrings.idea.plugins.mavenprime.spy.SpyProtocol;

public final class ProfileEvents
{
    private static final int TYPE = 0;

    private static final int MODULE = 1;

    private static final long UNPARSEABLE = -1L;

    private ProfileEvents()
    {
    }

    public static Optional<ProfileEvent> parse(String line)
    {
        String[] fields = SpyProtocol.decode(line);

        if (fields.length == 0)
        {
            return Optional.empty();
        }

        return switch (fields[TYPE])
        {
            case SpyProtocol.PROJECT_TIMING -> moduleTiming(fields);
            case SpyProtocol.MOJO_TIMING -> mojoTiming(fields);
            case SpyProtocol.REACTOR_EDGE -> reactorEdge(fields);
            case SpyProtocol.PROJECT_FAILED -> moduleFailed(fields);
            case SpyProtocol.ARTIFACT_DOWNLOADED -> downloadTiming(fields, DownloadKind.ARTIFACT);
            case SpyProtocol.METADATA_DOWNLOADED -> downloadTiming(fields, DownloadKind.METADATA);
            default -> Optional.empty();
        };
    }

    private static Optional<ProfileEvent> moduleTiming(String[] fields)
    {
        String module = field(fields, MODULE);

        long start = number(fields, 2);
        long duration = number(fields, 3);

        return (StringUtils.isBlank(module) || (start < 0) || (duration < 0))
            ? Optional.empty()
            : Optional.of(new ModuleTiming(module, start, duration, field(fields, 4)));
    }

    private static Optional<ProfileEvent> mojoTiming(String[] fields)
    {
        String module = field(fields, MODULE);
        String goal = field(fields, 2);

        long start = number(fields, 4);
        long duration = number(fields, 5);

        return (StringUtils.isBlank(module) || StringUtils.isBlank(goal) || (start < 0) || (duration < 0))
            ? Optional.empty()
            : Optional.of(
                new MojoTiming(
                    module, goal, field(fields, 3), start, duration, field(fields, 6), field(fields, 7)));
    }

    private static Optional<ProfileEvent> downloadTiming(String[] fields, DownloadKind kind)
    {
        String coordinates = field(fields, 1);

        long start = number(fields, 3);
        long duration = number(fields, 4);
        long bytes = NumberUtils.toLong(field(fields, 5), 0L);

        return (StringUtils.isBlank(coordinates) || (start < 0) || (duration < 0))
            ? Optional.empty()
            : Optional.of(
                new DownloadTiming(
                    kind, coordinates, field(fields, 2), start, duration, Math.max(0L, bytes)));
    }

    private static Optional<ProfileEvent> moduleFailed(String[] fields)
    {
        String module = field(fields, MODULE);

        return StringUtils.isBlank(module) ? Optional.empty() : Optional.of(new ModuleFailed(module));
    }

    private static Optional<ProfileEvent> reactorEdge(String[] fields)
    {
        String module = field(fields, MODULE);
        String upstream = field(fields, 2);

        return (StringUtils.isBlank(module) || StringUtils.isBlank(upstream) || module.equals(upstream))
            ? Optional.empty()
            : Optional.of(new ReactorEdge(module, upstream));
    }

    private static long number(String[] fields, int index)
    {
        return NumberUtils.toLong(field(fields, index), UNPARSEABLE);
    }

    private static String field(String[] fields, int index)
    {
        return (index < fields.length) ? fields[index] : StringUtils.EMPTY;
    }
}
