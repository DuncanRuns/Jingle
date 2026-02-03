package xyz.duncanruns.jingle.instance;

import com.google.gson.JsonObject;
import me.duncanruns.kerykeion.listeners.HermesInstanceListener;
import org.apache.logging.log4j.Level;
import xyz.duncanruns.jingle.Jingle;
import xyz.duncanruns.jingle.util.ExceptionUtil;

import java.util.*;

/**
 * Collects instance infos from Hermes/Kerykeion.
 * Should be not used by plugins.
 */
public class HermesInstanceDepot implements HermesInstanceListener {
    private static final HermesInstanceDepot INSTANCE = new HermesInstanceDepot();
    private final Map<Integer, JsonObject> instances = new HashMap<>();
    private final List<Integer> newPids = new ArrayList<>();

    public static HermesInstanceDepot get() {
        return INSTANCE;
    }

    @Override
    public void onNewInstance(JsonObject jsonObject, boolean b) {
        try {
            if (jsonObject.get("is_server").getAsBoolean()) return;
            int pid = jsonObject.get("pid").getAsInt();
            this.instances.put(pid, jsonObject);
            this.newPids.add(pid);
        } catch (Exception e) {
            Jingle.log(Level.ERROR, "Failed to get pid from Hermes instance: " + ExceptionUtil.toDetailedString(e));
        }
    }

    @Override
    public void onInstanceClosed(JsonObject jsonObject) {
        try {
            int pid = jsonObject.get("pid").getAsInt();
            this.instances.remove(pid);
        } catch (Exception e) {
            Jingle.log(Level.ERROR, "Failed to get pid from Hermes instance: " + ExceptionUtil.toDetailedString(e));
        }
    }

    public Optional<JsonObject> getInstance(int pid) {
        return Optional.ofNullable(this.instances.get(pid));
    }

    public List<Integer> getNewPids() {
        List<Integer> newPids = new ArrayList<>(this.newPids);
        this.newPids.clear();
        return newPids;
    }
}
