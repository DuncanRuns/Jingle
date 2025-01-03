package xyz.duncanruns.jingle.obs;

import xyz.duncanruns.jingle.Jingle;
import xyz.duncanruns.jingle.instance.InstanceState;
import xyz.duncanruns.jingle.util.FileUtil;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Objects;

public final class OBSLink {
    private static final Path OUT = Jingle.FOLDER.resolve("obs-link-state");

    private static long lastUpdate = 0;

    private static String last = "";

    private OBSLink() {
    }

    public static void tick() {
        long currentTime = System.currentTimeMillis();
        if (Math.abs(currentTime - lastUpdate) > 10) {
            lastUpdate = currentTime;
            String output = createOutput();
            if (Objects.equals(output, last)) return;
            last = output;
            try {
                FileUtil.writeString(OUT, output);
            } catch (IOException e) {
                Jingle.logError("Failed to write obs-link-state:", e);
            }
        }
    }

    private static String createOutput() {
        long requestProjectorTime = OBSProjector.getRequestProjectorTime();
        return String.join("|",
                Jingle.getMainInstance().map(i -> i.stateTracker.isCurrentState(InstanceState.WALL)).orElse(false) ? "W" : "P", // 1: Wall vs Playing ('W' vs 'P')
                requestProjectorTime == -1L ? "N" : "Y" + requestProjectorTime,  // 2: Should open projector ('N' for no, any other arbitrary string for requesting)
                OBSProjector.shouldCoverJultiMag() ? "Y" : "N" // 3: Should cover projector ('Y' for yes, any other arbitrary string for no)
        );
    }
}
