package org.bitstrings.idea.plugins.mavenprime.repository;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.PriorityQueue;

import com.intellij.openapi.util.text.StringUtil;

public final class ArtifactRanking
{
    static final int NO_MATCH = Integer.MAX_VALUE;

    private static final int EXACT_ARTIFACT_ID = 0;

    private static final int ARTIFACT_ID_PREFIX = 1;

    private static final int INSIDE_ARTIFACT_ID = 2;

    private static final int INSIDE_SHORT_FORM = 3;

    private static final Comparator<Ranked> BY_RANK =
        Comparator
            .comparingInt(Ranked::rank)
            .thenComparing(Ranked::coordinates, ArtifactCoordinates.BY_ARTIFACT)
            .thenComparing(entry -> entry.coordinates().toString());

    private ArtifactRanking()
    {
    }

    static int rankOf(ArtifactCoordinates candidate, String needle)
    {
        if (candidate.artifactId().equalsIgnoreCase(needle))
        {
            return EXACT_ARTIFACT_ID;
        }

        if (StringUtil.startsWithIgnoreCase(candidate.artifactId(), needle))
        {
            return ARTIFACT_ID_PREFIX;
        }

        if (StringUtil.containsIgnoreCase(candidate.artifactId(), needle))
        {
            return INSIDE_ARTIFACT_ID;
        }

        return StringUtil.containsIgnoreCase(candidate.shortForm(), needle) ? INSIDE_SHORT_FORM : NO_MATCH;
    }

    static List<ArtifactCoordinates> bestMatches(
        Iterable<ArtifactCoordinates> candidates, String needle, int limit)
    {
        if (limit <= 0)
        {
            return List.of();
        }

        PriorityQueue<Ranked> best = new PriorityQueue<>(BY_RANK.reversed());

        for (ArtifactCoordinates candidate : candidates)
        {
            int rank = rankOf(candidate, needle);

            if (rank != NO_MATCH)
            {
                offer(best, new Ranked(rank, candidate), limit);
            }
        }

        return ordered(best);
    }

    private static void offer(PriorityQueue<Ranked> best, Ranked entry, int limit)
    {
        if (best.size() < limit)
        {
            best.add(entry);

            return;
        }

        if (BY_RANK.compare(entry, best.peek()) < 0)
        {
            best.poll();
            best.add(entry);
        }
    }

    private static List<ArtifactCoordinates> ordered(PriorityQueue<Ranked> best)
    {
        List<Ranked> ranked = new ArrayList<>(best);

        ranked.sort(BY_RANK);

        List<ArtifactCoordinates> coordinates = new ArrayList<>(ranked.size());

        for (Ranked entry : ranked)
        {
            coordinates.add(entry.coordinates());
        }

        return List.copyOf(coordinates);
    }

    private record Ranked(int rank, ArtifactCoordinates coordinates)
    {
    }
}
