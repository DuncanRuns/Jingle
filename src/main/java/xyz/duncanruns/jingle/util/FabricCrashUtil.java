package xyz.duncanruns.jingle.util;

public final class FabricCrashUtil {
    private FabricCrashUtil() {
    }

    public void onInitialize() {
        throw new RuntimeException("Jingle is not supposed to be ran as a mod!");
    }
}
