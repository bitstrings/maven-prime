package org.bitstrings.idea.plugins.mavenprime;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Properties;
import java.util.regex.Pattern;

import org.junit.Test;

public class BundleIntegrityTest
{
    private static final Pattern PARAMETERIZED = Pattern.compile("\\{\\d");

    private static final Pattern LONE_APOSTROPHE = Pattern.compile("(?<!')'(?!')");

    @Test
    public void bundleKeys_everyParameterizedMessage_escapesItsApostrophes()
        throws IOException
    {
        Properties bundle = bundle();

        List<String> unescaped = new ArrayList<>();

        for (String key : bundle.stringPropertyNames())
        {
            String value = bundle.getProperty(key);

            if (PARAMETERIZED.matcher(value).find() && LONE_APOSTROPHE.matcher(value).find())
            {
                unescaped.add(key);
            }
        }

        assertEquals(
            "MessageFormat runs only when a message takes parameters, and it silently eats a single "
                + "apostrophe, so the text ships subtly wrong with nothing to notice",
            List.of(),
            unescaped);
    }

    @Test
    public void bundleKeys_theBuildContextSummary_readsCorrectlyInSingularAndPlural()
        throws IOException
    {
        String pattern = bundle().getProperty("mavenprime.toolwindow.context.summary");

        assertNotNull(pattern);
        assertEquals("No profiles · no properties", format(pattern, 0, 0));
        assertEquals("1 profile · 1 property", format(pattern, 1, 1));
        assertEquals("2 profiles · 3 properties", format(pattern, 2, 3));
    }

    private static String format(String pattern, int profiles, int properties)
    {
        return new MessageFormat(pattern, Locale.ROOT)
            .format(new Object[] { Integer.valueOf(profiles), Integer.valueOf(properties) });
    }

    private static Properties bundle()
        throws IOException
    {
        Properties bundle = new Properties();

        try (InputStream stream = resource("/messages/MavenPrimeBundle.properties"))
        {
            bundle.load(new InputStreamReader(stream, StandardCharsets.UTF_8));
        }

        return bundle;
    }

    private static InputStream resource(String path)
    {
        InputStream stream = BundleIntegrityTest.class.getResourceAsStream(path);

        if (stream == null)
        {
            throw new IllegalStateException("not on the test classpath: " + path);
        }

        return stream;
    }
}
