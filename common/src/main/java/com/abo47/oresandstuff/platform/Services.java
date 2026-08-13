package com.abo47.oresandstuff.platform;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;

public final class Services {
    private static volatile PlatformService platform = new FallbackPlatformService();
    private static volatile PlatformHooks hooks = new FallbackPlatformHooks();

    private Services() {
    }

    public static PlatformService platform() {
        return platform;
    }

    public static void setPlatform(PlatformService service) {
        platform = Objects.requireNonNull(service, "service");
    }

    public static PlatformHooks hooks() {
        return hooks;
    }

    public static void setHooks(PlatformHooks service) {
        hooks = Objects.requireNonNull(service, "hooks");
    }

    private static final class FallbackPlatformService implements PlatformService {
        @Override
        public Path configDir() {
            return Paths.get("config");
        }

        @Override
        public String loaderName() {
            return "unknown";
        }

        @Override
        public String loaderVersion() {
            return "unknown";
        }

        @Override
        public String modVersion(String modId) {
            return "unknown";
        }
    }

    private static final class FallbackPlatformHooks implements PlatformHooks {
    }
}
