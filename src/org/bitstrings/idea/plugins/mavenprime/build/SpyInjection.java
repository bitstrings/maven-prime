package org.bitstrings.idea.plugins.mavenprime.build;

import java.io.File;
import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.bitstrings.idea.plugins.mavenprime.spy.SpyProtocol;

import com.intellij.openapi.application.PathManager;

public final class SpyInjection
{
    private SpyInjection()
    {
    }

    public static Map<String, String> propertiesFor(SpyHandshake handshake, String declaredExtClassPath)
    {
        Map<String, String> properties = new LinkedHashMap<>();

        if (!handshake.isUsable())
        {
            return properties;
        }

        String jar = PathManager.getJarPathForClass(SpyProtocol.class);

        if (StringUtils.isBlank(jar))
        {
            return properties;
        }

        properties.put(SpyProtocol.EXT_CLASS_PATH_PROPERTY, alongside(declaredExtClassPath, jar));
        properties.put(SpyProtocol.PORT_PROPERTY, String.valueOf(handshake.port()));
        properties.put(SpyProtocol.TOKEN_PROPERTY, handshake.token());
        properties.put(SpyProtocol.BUILD_PROPERTY, handshake.buildName());

        return properties;
    }

    private static String alongside(String declared, String jar)
    {
        return StringUtils.isBlank(declared) ? jar : (declared.trim() + File.pathSeparatorChar + jar);
    }
}
