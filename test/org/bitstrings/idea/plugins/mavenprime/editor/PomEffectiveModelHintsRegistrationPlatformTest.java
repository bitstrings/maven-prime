package org.bitstrings.idea.plugins.mavenprime.editor;

import java.util.List;

import com.intellij.codeInsight.hints.InlayGroup;
import com.intellij.codeInsight.hints.declarative.InlayHintsProvider;
import com.intellij.codeInsight.hints.declarative.InlayHintsProviderExtensionBean;
import com.intellij.openapi.extensions.ExtensionPointName;
import com.intellij.testFramework.fixtures.BasePlatformTestCase;

public class PomEffectiveModelHintsRegistrationPlatformTest
    extends BasePlatformTestCase
{
    private static final ExtensionPointName<InlayHintsProviderExtensionBean> EP =
        ExtensionPointName.create("com.intellij.codeInsight.declarativeInlayProvider");

    private static final String PROVIDER_ID = "mavenprime.effectiveModel";

    public void testEp_theInlayProvider_isRegisteredForPomFiles()
    {
        InlayHintsProviderExtensionBean bean = ourBean();

        assertNotNull(
            "without a registered bean the provider never appears in Inlay Hints and never draws a hint",
            bean);
        assertEquals("XML", bean.getLanguage());
    }

    public void testEp_theInlayProvider_landsInTheValuesGroup()
    {
        assertEquals(
            "the group decides where the switch shows up in Settings, so a wrong one hides it",
            InlayGroup.VALUES_GROUP,
            ourBean().group);
    }

    public void testEp_theInlayProvider_declaresEveryOneOfItsOptions()
    {
        assertEquals(
            "an option the bean did not parse cannot be switched, and a bean that fails to parse is "
                + "dropped along with the provider",
            3,
            ourBean().getOptions().size());
    }

    public void testEp_theInlayProvider_instantiatesTheMavenPrimeClass()
    {
        InlayHintsProvider instance = ourBean().getInstance();

        assertNotNull("a bean whose class cannot be instantiated is silently useless", instance);
        assertTrue(instance.getClass().getName(), instance instanceof PomEffectiveModelHints);
    }

    private static InlayHintsProviderExtensionBean ourBean()
    {
        List<InlayHintsProviderExtensionBean> beans = EP.getExtensionList();

        return beans
            .stream()
            .filter(bean -> PROVIDER_ID.equals(bean.getProviderId()))
            .findFirst()
            .orElse(null);
    }
}
