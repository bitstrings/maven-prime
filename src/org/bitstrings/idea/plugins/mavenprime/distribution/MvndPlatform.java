package org.bitstrings.idea.plugins.mavenprime.distribution;

import com.intellij.openapi.util.SystemInfo;
import com.intellij.util.system.CpuArch;

public enum MvndPlatform
{
    LINUX_AMD64("linux-amd64"),
    LINUX_AARCH64("linux-aarch64"),
    DARWIN_AMD64("darwin-amd64"),
    DARWIN_AARCH64("darwin-aarch64"),
    WINDOWS_AMD64("windows-amd64");

    private final String classifier;

    MvndPlatform(String classifier)
    {
        this.classifier = classifier;
    }

    public String getClassifier()
    {
        return classifier;
    }

    public static MvndPlatform current()
    {
        return of(SystemInfo.isWindows, SystemInfo.isMac, CpuArch.isArm64());
    }

    static MvndPlatform of(boolean windows, boolean mac, boolean arm64)
    {
        if (windows)
        {
            return WINDOWS_AMD64;
        }

        if (mac)
        {
            return arm64 ? DARWIN_AARCH64 : DARWIN_AMD64;
        }

        return arm64 ? LINUX_AARCH64 : LINUX_AMD64;
    }
}
