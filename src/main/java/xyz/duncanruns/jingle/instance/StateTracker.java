package xyz.duncanruns.jingle.instance;

import org.apache.logging.log4j.Level;
import xyz.duncanruns.jingle.Jingle;
import xyz.duncanruns.jingle.instance.InstanceState.InWorldState;
import xyz.duncanruns.jingle.util.ExceptionUtil;
import xyz.duncanruns.jingle.util.FileUtil;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.function.BiConsumer;
import java.util.regex.Pattern;

public class StateTracker {
    private static final Pattern PROGRESS_PATTERN = Pattern.compile("^(?:previewing|generating),(?:0|[1-9]\\d?|100)$");

    private final Path path;
    private final BiConsumer<InstanceState, InstanceState> onStateChange;

    private boolean fileExists = false;
    private long mTime = 0L;

    private InstanceState instanceState = InstanceState.TITLE;
    private InWorldState inWorldState = InWorldState.UNPAUSED;
    private byte loadingPercent = 0;

    private final long[] lastStartArr;
    private final long[] lastOccurrenceArr;


    public StateTracker(Path path, BiConsumer<InstanceState, InstanceState> onStateChange) {
        this.path = path;
        this.onStateChange = onStateChange;

        int totalStates = InstanceState.values().length;
        this.lastStartArr = new long[totalStates];
        this.lastOccurrenceArr = new long[totalStates];
        for (int i = 0; i < totalStates; i++) {
            this.lastStartArr[i] = this.lastOccurrenceArr[i] = 0L;
        }
    }

    private void update() throws IOException {
        boolean doOnStateChange = true;

        // Check existence
        if (!this.fileExists) {
            if (Files.exists(this.path)) {
                doOnStateChange = false;
                this.fileExists = true;
            } else {
                return;
            }
        }

        // Check for modification
        long newMTime = Files.getLastModifiedTime(this.path).toMillis();
        if (this.mTime == newMTime) {
            return;
        }

        // Store previous state
        InstanceState previousState = this.instanceState;
        byte previousPercentage = this.loadingPercent;

        if (!this.trySetStatesFromFile()) {
            return;
        }

        this.mTime = newMTime;

        long time = System.currentTimeMillis();

        if (previousState != this.instanceState && this.onStateChange != null) {
            // Set the last occurrence of the previous state to now, and the last start of the current state to now.
            this.lastOccurrenceArr[previousState.ordinal()] = time;
            this.lastStartArr[this.instanceState.ordinal()] = time;

            if (doOnStateChange) {
                this.onStateChange.accept(previousState, this.instanceState);
            }
        }
    }

    private boolean trySetStatesFromFile() throws IOException {
        // Read
        String out = FileUtil.readString(this.path);

        // Couldn't get output or output is empty (?)
        if (out.isEmpty()) {
            return false;
        }

        // Check for literal states
        switch (out) {
            case "waiting":
                this.setState(InstanceState.WAITING);
                return true;
            case "title":
                this.setState(InstanceState.TITLE);
                return true;
            case "inworld,paused":
                this.setState(InstanceState.INWORLD);
                this.inWorldState = InWorldState.PAUSED;
                return true;
            case "inworld,unpaused":
                this.setState(InstanceState.INWORLD);
                this.inWorldState = InWorldState.UNPAUSED;
                return true;
            case "inworld,gamescreenopen":
                this.setState(InstanceState.INWORLD);
                this.inWorldState = InWorldState.GAMESCREENOPEN;
                return true;
            case "wall":
                this.setState(InstanceState.WALL);
                return true;
        }

        // Literal failed, should be generating/previewing
        if (!PROGRESS_PATTERN.matcher(out).matches()) {
            Jingle.log(Level.DEBUG, "Invalid state in " + this.path + ": \"" + out + "\"");
            return false;
        }

        String[] args = out.split(",");


        // Get previewing vs generating
        // Checking if the previous state was previewing fixes a bug where world preview states "generating" at around 98%
        if (this.instanceState == InstanceState.PREVIEWING || args[0].equals("previewing")) {
            this.setState(InstanceState.PREVIEWING);
        } else if (args[0].equals("generating")) {
            this.setState(InstanceState.GENERATING);
        } else {
            // This should never happen
            Jingle.log(Level.DEBUG, "Invalid state in " + this.path + ": \"" + out + "\"");
            return false;
        }

        if (args.length > 1) {
            // Get loading percent
            try {
                this.loadingPercent = Byte.parseByte(args[1]);
            } catch (NumberFormatException e) {
                // This should never happen
                Jingle.log(Level.DEBUG, "Invalid state in " + this.path + ": \"" + out + "\"");
            }
        } else {
            // This should never happen
            Jingle.log(Level.DEBUG, "Invalid state in " + this.path + ": \"" + out + "\"");
            return false;
        }
        return true;
    }

    private void setState(InstanceState state) {
        this.instanceState = state;
    }

    public boolean tryUpdate() {
        try {
            this.update();
            return true;
        } catch (IOException e) {
            Jingle.log(Level.ERROR, "Error during state checking:\n" + ExceptionUtil.toDetailedString(e));
            return false;
        }
    }

    public long getLastOccurrenceOf(InstanceState state) {
        if (this.instanceState.equals(state)) {
            return System.currentTimeMillis();
        }
        return this.lastOccurrenceArr[state.ordinal()];
    }

    public long getLastStartOf(InstanceState state) {
        return this.lastStartArr[state.ordinal()];
    }

    public InstanceState getInstanceState() {
        return this.instanceState;
    }

    public boolean isCurrentState(InstanceState state) {
        return this.instanceState == state;
    }

    public byte getLoadingPercent() {
        return this.loadingPercent;
    }

    public InWorldState getInWorldState() {
        return this.inWorldState;
    }

    @Override
    public String toString() {
        return "StateTracker{" +
                "path=" + this.path +
                ", fileExists=" + this.fileExists +
                ", mTime=" + this.mTime +
                ", instanceState=" + this.instanceState +
                ", inWorldState=" + this.inWorldState +
                ", loadingPercent=" + this.loadingPercent +
                ", lastStartArr=" + Arrays.toString(this.lastStartArr) +
                ", lastOccurrenceArr=" + Arrays.toString(this.lastOccurrenceArr) +
                '}';
    }
}
