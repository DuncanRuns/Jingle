package xyz.duncanruns.jingle.backcompat;

import com.google.gson.JsonObject;
import me.duncanruns.kerykeion.listeners.HermesWorldLogListener;
import xyz.duncanruns.jingle.Jingle;
import xyz.duncanruns.jingle.plugin.PluginEvents;

/**
 * Adds hermes listeners for EXIT_WORLD and ENTER_WORLD
 * Plugins should be using a HermesWorldLogListener, but this keeps old plugins such as pace status going
 */
public class WorldEventsListener implements HermesWorldLogListener {
    @Override
    @SuppressWarnings("deprecation")
    public void onWorldLogEntry(JsonObject instanceInfo, JsonObject entry, boolean isNew) {
        if (Jingle.isCurrentInstance(instanceInfo)) {
            String type = entry.get("type").getAsString();
            if (type.equals("leave")) {
                PluginEvents.EXIT_WORLD.runAll();
            } else if (type.equals("entering")) {
                PluginEvents.ENTER_WORLD.runAll();
            }
        }
    }
}
