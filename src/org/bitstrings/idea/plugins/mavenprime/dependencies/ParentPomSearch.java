package org.bitstrings.idea.plugins.mavenprime.dependencies;

import java.util.function.Function;

import org.jetbrains.idea.maven.dom.MavenDomProjectProcessorUtils;
import org.jetbrains.idea.maven.dom.model.MavenDomProjectModel;

import com.intellij.util.Processor;

public final class ParentPomSearch
{
    private ParentPomSearch()
    {
    }

    public static <T> T firstInAncestry(
        MavenDomProjectModel model, Function<MavenDomProjectModel, T> findIn)
    {
        Found<T> found = new Found<>();

        MavenDomProjectProcessorUtils.processParentProjects(model, stoppingAt(found, findIn));

        return found.value;
    }

    static <T> Processor<MavenDomProjectModel> stoppingAt(
        Found<T> found, Function<MavenDomProjectModel, T> findIn)
    {
        return parent ->
        {
            found.value = findIn.apply(parent);

            return found.value != null;
        };
    }

    static final class Found<T>
    {
        T value;
    }
}
