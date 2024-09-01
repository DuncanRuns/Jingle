package xyz.duncanruns.jingle.obs;

import xyz.duncanruns.jingle.Jingle;
import xyz.duncanruns.jingle.instance.InstanceState;
import xyz.duncanruns.jingle.util.FileUtil;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Objects;

public class OBSLink {
    private static final Path OUT = Jingle.FOLDER.resolve("obs-link-state");

    private static long lastUpdate = 0;

    private static String last = "";

    public static void tick() {
        long currentTime = System.currentTimeMillis();
        if (Math.abs(currentTime - lastUpdate) > 10) {
            lastUpdate = currentTime;
            String output = createOutput();
            if (!Objects.equals(output, last)) {
                last = output;
                try {
                    FileUtil.writeString(OUT, output);
                } catch (IOException e) {
                    Jingle.logError("Failed to write obs-link-state:", e);
                }
            }
        }
    }

    private static String createOutput() {
        return String.join("|",
                Jingle.stateTracker != null && Jingle.stateTracker.isCurrentState(InstanceState.WALL) ? "W" : "P", // 1: Wall vs Playing ('W' vs 'P')
                OBSProjector.shouldRequestProjector() ? "Y" : "N" // 2: Should open projector ('Y' for yes)
        );
    }
}
