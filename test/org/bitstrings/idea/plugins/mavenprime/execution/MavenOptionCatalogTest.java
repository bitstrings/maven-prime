package org.bitstrings.idea.plugins.mavenprime.execution;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Set;

import org.junit.Test;

public class MavenOptionCatalogTest
{
    private static final String MAVEN_3_HELP =
        """
        usage: mvn [options] [<goal(s)>] [<phase(s)>]

        Options:
         -am,--also-make                         If project list is specified,
                                                 also build projects required by
                                                 the list
         -D,--define <arg>                       Define a user property
         -itr,--ignore-transitive-repositories   If set, Maven will ignore remote
         -rf,--resume-from <arg>                 Resume reactor from specified
        """;

    private static final String MAVEN_3_10_HELP =
        """
         -UA,--update-artifacts                  Forces checks for missing
                                                 artifacts (retries cached
                                                 retrieval errors)
         -UM,--update-metadata                   Forces updates of remote
            --artifacts-update-policy <arg>      The update policy to apply onto
        """;

    private static final String MAVEN_4_HELP =
        """
         -D <arg>                                          Define a user property
            --debug                                        Launch the JVM in debug
         -r,--resume                                       Resume reactor from the
        """;

    private static final String MVND_HELP =
        """
         -r,--resume                             Resume reactor from the last failed project
         -Dmvnd.noDaemon=<boolean>               If true, the client and daemon will run in the same JVM
        """;

    @Test
    public void parseOptions_shortAndLongForm_collectsBoth()
    {
        assertTrue(MavenOptionCatalog.parseOptions(MAVEN_3_HELP).containsAll(Set.of("-am", "--also-make")));
    }

    @Test
    public void parseOptions_wrappedDescriptionLines_areNotReadAsOptions()
    {
        assertFalse(MavenOptionCatalog.parseOptions(MAVEN_3_HELP).contains("the"));
    }

    @Test
    public void parseOptions_usageHeading_isNotReadAsAnOption()
    {
        assertFalse(MavenOptionCatalog.parseOptions(MAVEN_3_HELP).contains("usage:"));
    }

    @Test
    public void parseOptions_maven310UpdateOptions_areCollected()
    {
        assertTrue(MavenOptionCatalog.parseOptions(MAVEN_3_10_HELP).containsAll(Set.of("-UA", "-UM")));
    }

    @Test
    public void parseOptions_longOnlyOptionWithoutShortForm_isCollected()
    {
        assertTrue(MavenOptionCatalog.parseOptions(MAVEN_3_10_HELP).contains("--artifacts-update-policy"));
    }

    @Test
    public void parseOptions_maven4DefineWithoutLongForm_isCollected()
    {
        assertTrue(MavenOptionCatalog.parseOptions(MAVEN_4_HELP).contains("-D"));
    }

    @Test
    public void parseOptions_maven4Resume_isCollected()
    {
        assertTrue(MavenOptionCatalog.parseOptions(MAVEN_4_HELP).contains("-r"));
    }

    @Test
    public void parseOptions_maven3HelpWithoutResume_omitsIt()
    {
        assertFalse(MavenOptionCatalog.parseOptions(MAVEN_3_HELP).contains("-r"));
    }

    @Test
    public void parseOptions_resumeFrom_isNotConfusedWithResume()
    {
        assertTrue(MavenOptionCatalog.parseOptions(MAVEN_3_HELP).contains("-rf"));
    }

    @Test
    public void parseOptions_daemonPropertyOption_isCollectedWithoutItsValue()
    {
        assertTrue(MavenOptionCatalog.parseOptions(MVND_HELP).contains("-Dmvnd.noDaemon"));
    }

    @Test
    public void parseOptions_emptyHelpText_yieldsNoOptions()
    {
        assertEquals(Set.of(), MavenOptionCatalog.parseOptions(""));
    }
}
