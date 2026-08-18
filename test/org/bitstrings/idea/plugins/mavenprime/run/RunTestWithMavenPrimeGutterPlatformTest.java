package org.bitstrings.idea.plugins.mavenprime.run;

import com.intellij.psi.PsiFile;
import com.intellij.testFramework.fixtures.LightJavaCodeInsightFixtureTestCase;

public class RunTestWithMavenPrimeGutterPlatformTest
    extends LightJavaCodeInsightFixtureTestCase
{
    private static final int REMOVES_AN_ITEM_LINE = 3;

    private static final String TEST_CLASS =
        "import org.junit.Test;\n"
            + "public class CartTest {\n"
            + "  @Test public void addsAnItem() { <caret> }\n"
            + "  @Test public void removesAnItem() { }\n"
            + "}\n";

    @Override
    protected void setUp()
        throws Exception
    {
        super.setUp();

        myFixture.addClass("package org.junit; public @interface Test { }");
    }

    public void testElementFor_aGutterClickOnAnotherTestsLine_picksTheClickedLine()
    {
        assertEquals(
            "clicking the gutter never moves the caret, so reading the caret runs a test the user did "
                + "not click",
            "CartTest#removesAnItem",
            selectionOn(Integer.valueOf(REMOVES_AN_ITEM_LINE)));
    }

    public void testElementFor_aContextWithNoGutterClick_picksTheCaret()
    {
        assertEquals(
            "the editor context menu carries no clicked line, and there the caret is the user's choice",
            "CartTest#addsAnItem",
            selectionOn(null));
    }

    public void testElementFor_aClickedLinePastTheEndOfTheFile_fallsBackToTheCaret()
    {
        assertEquals(
            "a line number the document no longer holds would throw out of the gutter, killing the update",
            "CartTest#addsAnItem",
            selectionOn(Integer.valueOf(999)));
    }

    private String selectionOn(Integer clickedLine)
    {
        PsiFile file = myFixture.configureByText("CartTest.java", TEST_CLASS);

        TestSelection selection =
            TestSelections.of(
                RunTestWithMavenPrimeAction.elementFor(file, myFixture.getEditor(), clickedLine));

        assertNotNull(selection);

        return selection.filter();
    }
}
