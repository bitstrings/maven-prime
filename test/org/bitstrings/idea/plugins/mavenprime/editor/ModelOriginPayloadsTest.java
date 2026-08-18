package org.bitstrings.idea.plugins.mavenprime.editor;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import org.bitstrings.idea.plugins.mavenprime.model.ModelOrigin;
import org.junit.Test;

public class ModelOriginPayloadsTest
{
    private static final String FILE = "/home/dev/project/pom.xml";

    private static final int LINE = 42;

    @Test
    public void decode_anEncodedOrigin_readsBackTheFileAndLine()
    {
        ModelOrigin decoded =
            ModelOriginPayloads.decode(ModelOriginPayloads.encode(new ModelOrigin("m", FILE, LINE)));

        assertEquals("clicking the hint has to open the file the value came from", FILE, decoded.file());
        assertEquals(LINE, decoded.line());
    }

    @Test
    public void decode_aPathThatItselfContainsTheSeparator_stillReadsBackTheLine()
    {
        String odd = "/home/dev/we|rd/pom.xml";

        ModelOrigin decoded =
            ModelOriginPayloads.decode(ModelOriginPayloads.encode(new ModelOrigin("m", odd, LINE)));

        assertEquals("a path with a separator in it must not shift the line number", odd, decoded.file());
        assertEquals(LINE, decoded.line());
    }

    @Test
    public void decode_aPayloadFromSomeOtherProvider_isNotNavigable()
    {
        assertFalse(
            "a foreign payload would otherwise open whatever path it happens to spell",
            ModelOriginPayloads.decode("nothing-encoded-here").isNavigable());
    }
}
