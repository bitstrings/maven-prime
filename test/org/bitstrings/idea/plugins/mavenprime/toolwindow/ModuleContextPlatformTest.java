package org.bitstrings.idea.plugins.mavenprime.toolwindow;

import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.idea.maven.project.MavenProject;

import com.intellij.testFramework.fixtures.BasePlatformTestCase;

public class ModuleContextPlatformTest
    extends BasePlatformTestCase
{
    private static final String MISSING_POM = "/gone/pom.xml";

    public void testResolve_nothingPinned_followsTheModuleInContext()
    {
        MavenProject followed = module("core");

        assertSame(followed, ModuleContext.resolve(StringUtils.EMPTY, List.of(followed), followed).module());
    }

    public void testResolve_nothingPinned_doesNotReportAPin()
    {
        MavenProject followed = module("core");

        assertFalse(ModuleContext.resolve(StringUtils.EMPTY, List.of(followed), followed).isPinned());
    }

    public void testResolve_pinnedModuleInTheReactor_usesThePinnedModuleRatherThanTheFollowedOne()
    {
        MavenProject core = module("core");
        MavenProject api = module("api");

        assertSame(api, ModuleContext.resolve(api.getFile().getPath(), List.of(core, api), core).module());
    }

    public void testResolve_pinnedModuleNotInTheReactor_doesNotFallBackToTheFollowedModule()
    {
        MavenProject core = module("core");

        assertNull(ModuleContext.resolve(MISSING_POM, List.of(core), core).module());
    }

    public void testResolve_pinnedModuleNotInTheReactor_stillReportsAPin()
    {
        MavenProject core = module("core");

        assertTrue(ModuleContext.resolve(MISSING_POM, List.of(core), core).isPinned());
    }

    public void testResolve_pinnedModuleNotInTheReactor_isDistinguishableFromNothingPinned()
    {
        MavenProject core = module("core");

        assertEquals(
            ModuleContext.Kind.PINNED_MISSING,
            ModuleContext.resolve(MISSING_POM, List.of(core), core).kind());
    }

    public void testResolve_nothingPinnedAndNoModuleInContext_reportsNoModuleWithoutAPin()
    {
        ModuleContext context = ModuleContext.resolve(StringUtils.EMPTY, List.of(), null);

        assertNull(context.module());
        assertFalse(context.isPinned());
    }

    public void testResolve_reactorNotImportedYet_keepsThePinRatherThanFollowingTheEditor()
    {
        MavenProject followed = module("core");

        ModuleContext context = ModuleContext.resolve(MISSING_POM, List.of(), followed);

        assertNull(context.module());
        assertTrue(context.isPinned());
    }

    private MavenProject module(String name)
    {
        return new MavenProject(myFixture.addFileToProject(name + "/pom.xml", "<project/>").getVirtualFile());
    }
}
