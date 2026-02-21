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
    // Runs when the GUI loses focus, right before saving options
    public static RunnableEventType GUI_LOST_FOCUS = new RunnableEventType();

    private PluginEvents() {
    }
}
