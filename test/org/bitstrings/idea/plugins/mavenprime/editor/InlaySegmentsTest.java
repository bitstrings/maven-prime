package org.bitstrings.idea.plugins.mavenprime.editor;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.junit.Test;

public class InlaySegmentsTest
{
    private static final String THIRTY = "123456789012345678901234567890";

    private static final String LONG_VALUE = "com.expretio.appia5.connector.core";

    @Test
    public void of_aValueTheInlayShowsWhole_staysOneSegment()
    {
        assertEquals(
            "splitting a value that already fits would build a collapsed form nobody needs to expand",
            List.of(THIRTY),
            InlaySegments.of(THIRTY));
    }

    @Test
    public void of_aValueOneCharacterTooLong_splitsRatherThanLosingTheTail()
    {
        assertEquals(List.of(THIRTY, "1"), InlaySegments.of(THIRTY + "1"));
    }

    @Test
    public void of_aLongValue_keepsEveryCharacterAcrossItsSegments()
    {
        assertEquals(
            "a lost character is exactly the ellipsis the platform would have drawn",
            LONG_VALUE,
            String.join("", InlaySegments.of(LONG_VALUE)));
    }

    @Test
    public void of_aLongValue_holdsEverySegmentInsideThePlatformsLimit()
    {
        for (String segment : InlaySegments.of(LONG_VALUE))
        {
            assertTrue(
                "a segment over the limit is truncated by the platform, which is what we are avoiding",
                segment.length() <= InlaySegments.MAX_SEGMENT_LENGTH);
        }
    }

    @Test
    public void of_aLongValue_holdsNoEllipsisOfItsOwn()
    {
        for (String segment : InlaySegments.of(LONG_VALUE))
        {
            assertFalse(
                "the expanded value is the whole value, so an ellipsis inside it would be a lie",
                segment.contains(InlaySegments.ELLIPSIS));
        }
    }

    @Test
    public void head_aLongValueBehindAPrefix_capsThePairAtOneSegment()
    {
        assertEquals(
            "a placeholder over the limit is cut again by the platform, showing two ellipses",
            InlaySegments.MAX_SEGMENT_LENGTH,
            " = ".length() + InlaySegments.head(LONG_VALUE, " = ".length()).length());
    }

    @Test
    public void head_aLongValue_marksThatMoreIsHidden()
    {
        assertTrue(InlaySegments.head(LONG_VALUE, 3).endsWith(InlaySegments.ELLIPSIS));
    }

    @Test
    public void head_aPrefixLeavingNoRoomAtAll_isJustTheMark()
    {
        assertEquals(
            "a budget of nothing must not index past the start of the value",
            InlaySegments.ELLIPSIS,
            InlaySegments.head(LONG_VALUE, InlaySegments.MAX_SEGMENT_LENGTH));
    }
}
