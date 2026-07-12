package xyz.duncanruns.jingle.backcompat;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.annotations.SerializedName;
import me.duncanruns.kerykeion.listeners.HermesStateListener;
import me.duncanruns.kerykeion.listeners.HermesWorldLogListener;
import xyz.duncanruns.jingle.Jingle;
import xyz.duncanruns.jingle.plugin.PluginEvents;

import java.util.Objects;

/**
 * Adds hermes listeners for EXIT_WORLD and ENTER_WORLD
 * Plugins should be using a HermesWorldLogListener, but this keeps old plugins such as pace status going
 */
@SuppressWarnings("deprecation")
public class WorldEventsListener implements HermesWorldLogListener, HermesStateListener {
    private static final Gson GSON = new Gson();

    private JsonObject enteringWorld;

    @Override
    public void onWorldLogEntry(JsonObject instanceInfo, JsonObject entryJ, boolean isNew) {
        if (!Jingle.isCurrentInstance(instanceInfo)) return;
        WorldLogEntry entry = GSON.fromJson(entryJ, WorldLogEntry.class);
        String type = entry.type;
        if (type == null) return;
        if (type.equals("leave")) {
            PluginEvents.EXIT_WORLD.runAll();
        } else if (type.equals("entering")) {
            enteringWorld = entry.world;
        }
    }

    @Override
    public void onInstanceStateChange(JsonObject instanceInfo, JsonObject stateJ) {
        if (!Jingle.isCurrentInstance(instanceInfo)) return;
        InstanceState state = GSON.fromJson(stateJ, InstanceState.class);
        if (state.screen == null) return; // Screen object must have been parsed
        if (state.screen.className != null) return; // Screen object's class must be null, as in no screen open
        if (!Objects.equals(state.world, enteringWorld)) return;
        enteringWorld = null;
        PluginEvents.EXIT_WORLD.runAll();
    }

    private static class WorldLogEntry {
        String type;
        JsonObject world;
    }

    static class InstanceState {
        Screen screen;
        JsonObject world;

        static class Screen {
            @SerializedName("class")
            String className;
        }
    }
}
