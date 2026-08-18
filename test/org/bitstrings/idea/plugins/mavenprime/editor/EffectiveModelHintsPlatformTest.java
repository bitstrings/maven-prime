package org.bitstrings.idea.plugins.mavenprime.editor;

import org.bitstrings.idea.plugins.mavenprime.editor.EffectiveModelHints.Hint;

import com.intellij.testFramework.fixtures.BasePlatformTestCase;

public class EffectiveModelHintsPlatformTest
    extends BasePlatformTestCase
{
    public void testPropertyValue_aPlaceholderTheModelResolved_showsTheValueAndWhereItComesFrom()
    {
        Hint hint = hints().propertyValue("${revision}");

        assertNotNull("a placeholder the model resolved is the whole point of the hint", hint);
        assertEquals(ProvenanceFixture.REVISION, hint.text());
        assertEquals(
            "the hint has to lead to the declaration, which is the question a reader actually has",
            ProvenanceFixture.PARENT,
            hint.origin());
    }

    public void testPropertyValue_textWithNoPlaceholder_showsNothing()
    {
        assertNull(
            "a hint beside a literal value repeats what the line already says",
            hints().propertyValue("1.4.0"));
    }

    public void testPropertyValue_aPlaceholderTheModelNeverSaw_showsNothingRatherThanGuessing()
    {
        assertNull(
            "a guessed value is worse than no value, because the reader cannot tell it is a guess",
            hints().propertyValue("${nobody.declared.this}"));
    }

    public void testPropertyValue_oneResolvedPlaceholderBesideAnUnknownOne_showsNothingAtAll()
    {
        assertNull(
            "a half-resolved value reads as the real thing, so the reader trusts a value that is wrong",
            hints().propertyValue("${revision}-${nobody.declared.this}"));
    }

    public void testPropertyValue_severalPlaceholdersInOneValue_resolvesThemAllWithoutClaimingOneOrigin()
    {
        Hint hint = hints().propertyValue("${revision}-${stamp}");

        assertNotNull(hint);
        assertEquals(ProvenanceFixture.REVISION + "-42", hint.text());
        assertFalse(
            "with two declarations behind one value, clicking through would pick one arbitrarily",
            hint.origin().isNavigable());
    }

    public void testManagedVersion_aDependencyDeclaringNoVersion_showsTheVersionItInherits()
    {
        Hint hint =
            hints().managedVersion("org.apache.commons", "commons-lang3", "", "");

        assertNotNull("the inherited version is invisible in the POM, which is why it is worth showing", hint);
        assertEquals(ProvenanceFixture.LANG3_VERSION, hint.text());
    }

    public void testManagedVersion_aDependencyTheModelNeverResolved_showsNothing()
    {
        assertNull(hints().managedVersion("org.example", "not-a-dependency", "", ""));
    }

    public void testManagementKey_aDependencyWithoutAnExplicitType_isKeyedAsAJar()
    {
        assertEquals(
            "Maven keys a dependency by its type, defaulting to jar, so a missing type must not miss",
            "g:a:jar",
            EffectiveModelHints.managementKey("g", "a", "", ""));
    }

    public void testPropertyValue_theProjectVersionBuiltIn_showsTheModuleVersion()
    {
        Hint hint = hints().propertyValue("${project.version}");

        assertNotNull(
            "project.version is the placeholder every pom uses, and it is not a declared property, so "
                + "reading only declared properties left the commonest case blank",
            hint);
        assertEquals("5.2.0-SNAPSHOT", hint.text());
    }

    public void testPropertyValue_theProjectGroupIdBuiltIn_showsTheModuleGroup()
    {
        Hint hint = hints().propertyValue("${project.groupId}");

        assertNotNull(hint);
        assertEquals("org.example", hint.text());
    }

    public void testPropertyValue_theLegacyPomAlias_resolvesLikeTheProjectForm()
    {
        Hint hint = hints().propertyValue("${pom.version}");

        assertNotNull("older poms still use the pom. prefix, and Maven still resolves it", hint);
        assertEquals("5.2.0-SNAPSHOT", hint.text());
    }

    public void testPropertyValue_aParentCoordinate_resolvesFromTheParentOfTheModule()
    {
        Hint hint = hints().propertyValue("${project.parent.artifactId}");

        assertNotNull(hint);
        assertEquals("parent", hint.text());
    }

    public void testPropertyValue_aPropertyTheIdeResolvedButMavenPrimeNeverRead_stillShowsTheValue()
    {
        EffectiveModelHints hints =
            ProvenanceFixture.hintsFor(getProject(), java.util.Map.of("spring.version", "6.1.4"), java.util.Map.of());

        Hint hint = hints.propertyValue("${spring.version}");

        assertNotNull(
            "the IDE already resolved this property, so waiting for a Maven Prime model read showed nothing",
            hint);
        assertEquals("6.1.4", hint.text());
    }

    public void testManagedVersion_aVersionTheIdeResolvedButMavenPrimeNeverRead_stillShowsIt()
    {
        EffectiveModelHints hints =
            ProvenanceFixture.hintsFor(
                getProject(), java.util.Map.of(), java.util.Map.of("com.google.guava:guava", "33.0.0-jre"));

        Hint hint = hints.managedVersion("com.google.guava", "guava", "", "");

        assertNotNull("the IDE knows every resolved dependency version without any Maven Prime build", hint);
        assertEquals("33.0.0-jre", hint.text());
    }

    public void testManagedPluginVersion_aPluginDeclaringNoVersion_showsTheVersionItInherits()
    {
        Hint hint = hints().managedPluginVersion("org.apache.maven.plugins", "maven-compiler-plugin");

        assertNotNull(
            "a plugin version lives in plugin management or the super POM, so the POM being read never "
                + "shows it",
            hint);
        assertEquals(ProvenanceFixture.COMPILER_PLUGIN_VERSION, hint.text());
    }

    public void testManagedPluginVersion_aPluginTheIdeNeverResolved_showsNothing()
    {
        assertNull(hints().managedPluginVersion("org.example", "not-a-plugin"));
    }

    public void testManagedPluginVersion_aPluginTheModelRead_leadsToWhereItsVersionWasDeclared()
    {
        Hint hint = hints().managedPluginVersion("org.apache.maven.plugins", "maven-surefire-plugin");

        assertNotNull(hint);
        assertEquals(ProvenanceFixture.SUREFIRE_PLUGIN_VERSION, hint.text());
        assertEquals(
            "the IDE resolves a plugin version without saying where it came from, so the hint led nowhere",
            ProvenanceFixture.PARENT,
            hint.origin());
    }

    public void testManagedPluginVersion_aPluginDeclaredWithoutItsGroupId_resolvesAsAnApachePlugin()
    {
        Hint hint = hints().managedPluginVersion("", "maven-surefire-plugin");

        assertNotNull(
            "omitting the groupId of an Apache plugin is the normal form in a pom, not an edge case",
            hint);
        assertEquals(ProvenanceFixture.PARENT, hint.origin());
    }

    private EffectiveModelHints hints()
    {
        return ProvenanceFixture.hintsFor(getProject());
    }
}
