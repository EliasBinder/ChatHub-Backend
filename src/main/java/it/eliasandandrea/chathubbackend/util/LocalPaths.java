package it.eliasandandrea.chathubbackend.util;

import java.nio.file.Path;
import java.nio.file.Paths;

public class LocalPaths {
    private static final Path config;
    private static final Path data;
    private static final Path cache;

    static {
        final String os = System.getProperty("os.name");
        final String home = System.getProperty("user.home");
        if (os.contains("Mac")) {
            config = Paths.get(home, "Library", "Application Support");
            data = config;
            cache = config;
        } else if (os.contains("Windows")) {
            String version = System.getProperty("os.version");
            if (version.startsWith("5.")) {
                config = getFromEnv("APPDATA", false,
                        Paths.get(home, "Application Data"));
                data = config;
                cache = Paths.get(home, "Local Settings", "Application Data");
            } else {
                config = getFromEnv("APPDATA", false,
                        Paths.get(home, "AppData", "Roaming"));
                data = config;
                cache = getFromEnv("LOCALAPPDATA", false,
                        Paths.get(home, "AppData", "Local"));
            }
        } else {
            config = getFromEnv("XDG_CONFIG_HOME", true,
                    Paths.get(home, ".config"));
            data = getFromEnv("XDG_DATA_HOME", true,
                    Paths.get(home, ".local", "share"));
            cache = getFromEnv("XDG_CACHE_HOME", true,
                    Paths.get(home, ".cache"));
        }
    }

    private static Path getFromEnv(String envVar,
                                   boolean mustBeAbsolute,
                                   Path defaultPath) {
        Path dir;
        String envDir = System.getenv(envVar);
        if (envDir == null || envDir.isEmpty()) {
            dir = defaultPath;
            Log.info(String.format(envVar + " not defined in environment"
                            + ", falling back on \"{0}\"", dir));
        } else {
            dir = Paths.get(envDir);
            if (mustBeAbsolute && !dir.isAbsolute()) {
                dir = defaultPath;
                Log.info(String.format(envVar + " is not an absolute path"
                                + ", falling back on \"{0}\"", dir));
            }
        }
        return dir;
    }

    public static Path getConfig() {
        return config;
    }

    public static Path getData() {
        return data;
    }

    public static Path getCache() {
        return cache;
    }

    public static Path concat(Path root, String... rest) {
        return Paths.get(root.toAbsolutePath().toString(), rest);
    }
}
