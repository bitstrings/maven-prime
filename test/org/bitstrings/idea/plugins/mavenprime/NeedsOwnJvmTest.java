package org.bitstrings.idea.plugins.mavenprime;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import org.junit.Test;
import org.junit.experimental.categories.Category;

import com.intellij.maven.testFramework.MavenImportingTestCase;
import com.intellij.testFramework.fixtures.LightJavaCodeInsightFixtureTestCase;

public class NeedsOwnJvmTest
{
    private static final String CLASS_SUFFIX = ".class";

    @Test
    public void category_aFixtureThatClosesTheSharedLightProject_isCarriedByEveryOne()
        throws IOException, URISyntaxException
    {
        List<String> uncategorised = new ArrayList<>();

        for (Class<?> testClass : testClasses())
        {
            if (closesTheSharedLightProject(testClass) && !needsOwnJvm(testClass))
            {
                uncategorised.add(testClass.getName());
            }
        }

        assertEquals(
            "the light project is one per JVM, so a fixture that closes it takes it away from every "
                + "platform test that runs after it",
            List.of(),
            uncategorised);
    }

    private static boolean closesTheSharedLightProject(Class<?> testClass)
    {
        return MavenImportingTestCase.class.isAssignableFrom(testClass)
            || LightJavaCodeInsightFixtureTestCase.class.isAssignableFrom(testClass);
    }

    private static boolean needsOwnJvm(Class<?> testClass)
    {
        Category category = testClass.getAnnotation(Category.class);

        return (category != null) && List.of(category.value()).contains(NeedsOwnJvm.class);
    }

    private static List<Class<?>> testClasses()
        throws IOException, URISyntaxException
    {
        Path root = packageRoot();

        List<Class<?>> classes = new ArrayList<>();

        try (Stream<Path> files = Files.walk(root))
        {
            for (Path file : files.filter(NeedsOwnJvmTest::isTopLevelClass).toList())
            {
                classes.add(load(root, file));
            }
        }

        if (classes.isEmpty())
        {
            throw new IllegalStateException("no compiled test classes under: " + root);
        }

        return classes;
    }

    private static Path packageRoot()
        throws URISyntaxException
    {
        URL self = NeedsOwnJvmTest.class.getResource(NeedsOwnJvmTest.class.getSimpleName() + CLASS_SUFFIX);

        if ((self == null) || !"file".equals(self.getProtocol()))
        {
            throw new IllegalStateException("the compiled test classes are not readable as files: " + self);
        }

        return Path.of(self.toURI()).getParent();
    }

    private static boolean isTopLevelClass(Path file)
    {
        String name = file.getFileName().toString();

        return name.endsWith(CLASS_SUFFIX) && !name.contains("$");
    }

    private static Class<?> load(Path root, Path file)
    {
        String path = root.relativize(file).toString();
        String name =
            NeedsOwnJvmTest.class.getPackageName()
                + "."
                + path.substring(0, path.length() - CLASS_SUFFIX.length()).replace(File.separatorChar, '.');

        try
        {
            return Class.forName(name, false, NeedsOwnJvmTest.class.getClassLoader());
        }
        catch (ClassNotFoundException ex)
        {
            throw new IllegalStateException("compiled but not loadable from the test classpath: " + name, ex);
        }
    }
}
