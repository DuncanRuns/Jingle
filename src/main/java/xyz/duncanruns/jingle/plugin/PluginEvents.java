package xyz.duncanruns.jingle.plugin;

import java.util.LinkedList;
import java.util.List;

public final class PluginEvents {
    private PluginEvents() {
    }

    public enum RunnableEventType {
        // Runs at the start of the main loop tick
        START_TICK,
        // Runs at the end of the main loop tick
        END_TICK,
        // Runs when Jingle's options.json is loaded, just before the start of the main loop
        OPTIONS_LOADED,
        // Runs when Jingle is shutting down
        STOP,
        // Runs when the instance changes, can be null
        MAIN_INSTANCE_CHANGED,
        // Runs when the instance's state changes
        STATE_CHANGE,
        // Runs when a world is exited
        EXIT_WORLD;

        private final List<Runnable> runnables = new LinkedList<>();

        @SuppressWarnings("unused")
        public void register(Runnable runnable) {
            this.runnables.add(runnable);
        }

        public void runAll() {
            this.runnables.forEach(Runnable::run);
        }
    }
}
