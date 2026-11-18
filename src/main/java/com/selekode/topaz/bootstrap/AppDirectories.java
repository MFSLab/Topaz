package com.selekode.topaz.bootstrap;

import java.nio.file.Path;

public class AppDirectories {

    public static String getAppDirectory() {
        String os = System.getProperty("os.name").toLowerCase();

        if (os.contains("win")) {
            return Path.of(
                    System.getenv("LOCALAPPDATA"),
                    "Topaz"
            ).toString();
        }

        if (os.contains("mac")) {
            return Path.of(
                    System.getProperty("user.home"),
                    "Library",
                    "Application Support",
                    "Topaz"
            ).toString();
        }

        return Path.of(
                System.getProperty("user.home"),
                ".local",
                "share",
                "Topaz"
        ).toString();
    }

    public static String getDataDirectory() {
        return Path.of(getAppDirectory(), "data").toString();
    }

    public static String getJcefBundleDirectory() {
        return Path.of(getAppDirectory(), "jcef-bundle").toString();
    }

    public static String getJcefCacheDirectory() {
        return Path.of(getAppDirectory(), "jcef-cache").toString();
    }
}