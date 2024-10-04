package xyz.duncanruns.jingle.plugin;

import xyz.duncanruns.jingle.event.RunnableEventType;

public final class PluginEvents {
    // Runs at the start of the main loop tick
    public static RunnableEventType START_TICK = new RunnableEventType();
    // Runs at the end of the main loop tick
    public static RunnableEventType END_TICK = new RunnableEventType();
    // Runs when Jingle is shutting down
    public static RunnableEventType STOP = new RunnableEventType();
    // Runs when the instance changes, can be null
    public static RunnableEventType MAIN_INSTANCE_CHANGED = new RunnableEventType();
    // Runs when the instance's state changes
    public static RunnableEventType STATE_CHANGE = new RunnableEventType();
    // Runs when a world is exited
    public static RunnableEventType EXIT_WORLD = new RunnableEventType();
    // Runs when a world is entered
    public static RunnableEventType ENTER_WORLD = new RunnableEventType();
    // Runs when a measuring projector is supposed to be shown/dumped
    public static RunnableEventType SHOW_PROJECTOR = new RunnableEventType();
    public static RunnableEventType DUMP_PROJECTOR = new RunnableEventType();

    private PluginEvents() {
    }
}
