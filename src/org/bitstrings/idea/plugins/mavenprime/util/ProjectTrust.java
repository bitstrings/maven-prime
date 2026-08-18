package org.bitstrings.idea.plugins.mavenprime.util;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;

public final class ProjectTrust
{
    static final String CURRENT_API = "com.intellij.ide.trustedProjects.TrustedProjects";

    static final String LEGACY_API = "com.intellij.ide.impl.TrustedProjects";

    private static final Logger LOG = Logger.getInstance(ProjectTrust.class);

    private static final Method RESOLVED = resolve();

    private ProjectTrust()
    {
    }

    public static boolean isTrusted(Project project)
    {
        if (RESOLVED == null)
        {
            return false;
        }

        try
        {
            return Boolean.TRUE.equals(RESOLVED.invoke(null, project));
        }
        catch (IllegalAccessException | InvocationTargetException unreadable)
        {
            LOG.warn("Maven Prime could not read the trusted state of " + project.getName(), unreadable);

            return false;
        }
    }

    static String resolvedApi()
    {
        return (RESOLVED == null) ? null : RESOLVED.getDeclaringClass().getName();
    }

    private static Method resolve()
    {
        Method current = staticReader(CURRENT_API, "isProjectTrusted");

        if (current != null)
        {
            return current;
        }

        Method legacy = staticReader(LEGACY_API, "isTrusted");

        if (legacy == null)
        {
            LOG.warn("Maven Prime found no trusted-project API, so shared build context stays off");
        }

        return legacy;
    }

    private static Method staticReader(String className, String methodName)
    {
        try
        {
            Method method = Class.forName(className).getMethod(methodName, Project.class);

            return Modifier.isStatic(method.getModifiers()) ? method : null;
        }
        catch (ClassNotFoundException | NoSuchMethodException absent)
        {
            return null;
        }
    }
}
