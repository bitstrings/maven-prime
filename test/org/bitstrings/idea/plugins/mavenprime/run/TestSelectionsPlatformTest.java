package org.bitstrings.idea.plugins.mavenprime.run;

import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.testFramework.fixtures.LightJavaCodeInsightFixtureTestCase;

public class TestSelectionsPlatformTest
    extends LightJavaCodeInsightFixtureTestCase
{
    private static final String TEST_CLASS =
        "import org.junit.Test;\n"
            + "public class CartTest {\n"
            + "  @Test public void addsAnItem() { }\n"
            + "  public void helper() { }\n"
            + "}\n";

    private static final String PLAIN_CLASS = "public class Cart { public void add() { } }\n";

    @Override
    protected void setUp()
        throws Exception
    {
        super.setUp();

        myFixture.addClass("package org.junit; public @interface Test { }");
    }

    public void testOf_aTestMethod_selectsThatMethodOnly()
    {
        TestSelection selection = TestSelections.of(methodIn("CartTest.java", TEST_CLASS, "addsAnItem"));

        assertNotNull("a click inside a test method is the most common way to run one test", selection);
        assertEquals("CartTest#addsAnItem", selection.filter());
        assertEquals("CartTest.addsAnItem", selection.label());
    }

    public void testOf_aTestClass_selectsEveryTestInIt()
    {
        TestSelection selection = TestSelections.of(classIn("CartTest.java", TEST_CLASS));

        assertNotNull(selection);
        assertEquals("surefire takes a bare class name to mean every test in it", "CartTest", selection.filter());
    }

    public void testOf_aMethodThatIsNotATest_selectsTheClassRatherThanTheMethod()
    {
        TestSelection selection = TestSelections.of(methodIn("CartTest.java", TEST_CLASS, "helper"));

        assertNotNull(selection);
        assertEquals(
            "a helper is not runnable on its own, so the offer has to fall back to the class",
            "CartTest",
            selection.filter());
    }

    public void testOf_aClassThatIsNotATestAtAll_selectsNothing()
    {
        assertNull(
            "offering a Maven test run on production code puts a dead entry in the context menu",
            TestSelections.of(classIn("Cart.java", PLAIN_CLASS)));
    }

    private PsiClass classIn(String name, String text)
    {
        return PsiTreeUtil.findChildOfType(configure(name, text), PsiClass.class);
    }

    private PsiMethod methodIn(String name, String text, String method)
    {
        PsiClass owner = classIn(name, text);

        assertNotNull(owner);

        return owner.findMethodsByName(method, false)[0];
    }

    private PsiFile configure(String name, String text)
    {
        return myFixture.configureByText(name, text);
    }
}
