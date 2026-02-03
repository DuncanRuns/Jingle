package xyz.duncanruns.jingle.instance;

import com.google.gson.*;
import com.google.gson.annotations.SerializedName;
import me.duncanruns.kerykeion.Kerykeion;
import me.duncanruns.kerykeion.listeners.HermesInstanceListener;
import me.duncanruns.kerykeion.listeners.HermesStateListener;
import xyz.duncanruns.jingle.Jingle;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.Executor;
import java.util.function.BiConsumer;

// Ended up copying a lot of logic from LegacyStateTrackerImpl, so I guess LegacyStateTracker should have been abstract,
// but I'm definitely not changing that since this will soon be deprecated/removed.

/**
 * A LegacyStateTracker that uses output from the Hermes mod. Only used when State Output is not installed. Translates
 * Hermes states to LegacyInstanceStates, somewhat inaccurately.
 */
public class HermesFakeStateTracker implements LegacyStateTracker {
    private final BiConsumer<LegacyInstanceState, LegacyInstanceState> onStateChange;
    private final int pid;
    private final boolean wp;

    private HermesState lastHermesState = null;

    private LegacyInstanceState instanceState = LegacyInstanceState.TITLE;
    private LegacyInstanceState.InWorldState inWorldState = LegacyInstanceState.InWorldState.UNPAUSED;

    private final long[] lastStartArr;
    private final long[] lastOccurrenceArr;

    public HermesFakeStateTracker(BiConsumer<LegacyInstanceState, LegacyInstanceState> onStateChange, int pid, boolean wp) {
        this.onStateChange = onStateChange;
        this.pid = pid;
        this.wp = wp;

        int totalStates = LegacyInstanceState.values().length;
        this.lastStartArr = new long[totalStates];
        this.lastOccurrenceArr = new long[totalStates];
        for (int i = 0; i < totalStates; i++) {
            this.lastStartArr[i] = this.lastOccurrenceArr[i] = 0L;
        }
    }

    @Override
    public boolean tryUpdate() {
        boolean doOnStateChange = this.lastHermesState != null;
        HermesState hermesState = StateDepot.INSTANCE.states.get(this.pid);
        if (hermesState == null) return true;
        if (Objects.equals(hermesState, this.lastHermesState)) return true;
        this.lastHermesState = hermesState;

        LegacyInstanceState previousState = this.instanceState;

        setStateFromHermes(hermesState, previousState);

        long time = System.currentTimeMillis();
        if (previousState != this.instanceState && this.onStateChange != null) {
            // Set the last occurrence of the previous state to now, and the last start of the current state to now.
            this.lastOccurrenceArr[previousState.ordinal()] = time;
            this.lastStartArr[this.instanceState.ordinal()] = time;

            if (doOnStateChange) {
                this.onStateChange.accept(previousState, this.instanceState);
            }
        }
        System.out.println("instanceState = " + instanceState);
        System.out.println("inWorldState = " + inWorldState);

        return true;
    }

    private void setStateFromHermes(HermesState hermesState, LegacyInstanceState previousState) {
        String screenJC = hermesState.screen.javaClass;
        System.out.println(screenJC);
        System.out.println(hermesState.screen.isPause);
        if (screenJC != null) {
            if (screenJC.endsWith(".SeedQueueWallScreen")) {
                this.instanceState = LegacyInstanceState.WALL;
            } else if (screenJC.endsWith(".class_3928") || screenJC.endsWith(".LevelLoadingScreen")) {
                this.instanceState = wp ? LegacyInstanceState.PREVIEWING : LegacyInstanceState.GENERATING; // inaccurate without generation percent
            } else if (screenJC.endsWith(".class_435") || screenJC.endsWith(".ProgressScreen")
                    || screenJC.endsWith(".class_424") || screenJC.endsWith(".GenericMessageScreen")) {
                this.instanceState = LegacyInstanceState.WAITING;
            } else if (screenJC.endsWith(".class_442") || screenJC.endsWith(".TitleScreen")) {
                this.instanceState = LegacyInstanceState.TITLE;
            } else if (isNotNull(hermesState.world)) {
                this.instanceState = LegacyInstanceState.INWORLD;
                this.inWorldState = hermesState.screen.isPause ? LegacyInstanceState.InWorldState.PAUSED : LegacyInstanceState.InWorldState.GAMESCREENOPEN;
            }
        } else if (isNotNull(hermesState.world)) {
            this.instanceState = LegacyInstanceState.INWORLD;
            this.inWorldState = hermesState.screen.isPause ? LegacyInstanceState.InWorldState.PAUSED : LegacyInstanceState.InWorldState.UNPAUSED;
        }
    }

    /**
     * Returns true if the world is not null and not JsonNull.INSTANCE.
     */
    private static boolean isNotNull(JsonElement world) {
        return world != null && world != JsonNull.INSTANCE;
    }

    @Override
    public long getLastOccurrenceOf(LegacyInstanceState state) {
        if (this.instanceState.equals(state)) {
            return System.currentTimeMillis();
        }
        return this.lastOccurrenceArr[state.ordinal()];
    }

    @Override
    public long getLastStartOf(LegacyInstanceState state) {
        return this.lastStartArr[state.ordinal()];
    }

    @Override
    public LegacyInstanceState getInstanceState() {
        return this.instanceState;
    }

    @Override
    public boolean isCurrentState(LegacyInstanceState state) {
        return this.instanceState == state;
    }

    @Override
    public LegacyInstanceState.InWorldState getInWorldState() {
        return this.inWorldState;
    }

    private static class StateDepot implements HermesStateListener, HermesInstanceListener {
        private static final StateDepot INSTANCE = new StateDepot();
        private final Map<Integer, HermesState> states = new HashMap<>();
        private static final Gson GSON = new GsonBuilder().serializeNulls().create();

        @Override
        public void onInstanceStateChange(JsonObject instanceInfo, JsonObject state) {
            int pid;
            try {
                pid = instanceInfo.get("pid").getAsInt();
            } catch (Exception ignored) {
                return;
            }
            HermesState value;
            try {
                value = GSON.fromJson(state, HermesState.class);
            } catch (Exception e) {
                Jingle.logError("Failed to parse Hermes state:", e);
                return;
            }
            states.put(pid, value);
        }

        @Override
        public void onNewInstance(JsonObject instanceInfo, boolean isNew) {
        }

        @Override
        public void onInstanceClosed(JsonObject instanceInfo) {
            int pid;
            try {
                pid = instanceInfo.get("pid").getAsInt();
            } catch (Exception ignored) {
                return;
            }
            states.remove(pid);
        }
    }

    /* Example: wall
{
  "screen": {
    "class": "me.contaria.seedqueue.gui.wall.SeedQueueWallScreen",
    "title": {
      "text": ""
    },
    "is_pause": true
  },
  "last_world_joined": {
    "relative": true,
    "path": "saves/Random Speedrun #1015749"
  },
  "world": null
}
 */

    @SuppressWarnings("unused")
    private static class HermesState {
        public Screen screen;
        @SerializedName("last_world_joined")
        public JsonElement lastWorldJoined;
        public JsonElement world;

        private static class Screen {
            @SerializedName("class")
            public String javaClass;
            public JsonElement title;
            @SerializedName("is_pause")
            public boolean isPause;
        }
    }

    public static void registerKerykeionListener(Executor executor) {
        Kerykeion.addListener(StateDepot.INSTANCE, 1, executor);
    }
}
