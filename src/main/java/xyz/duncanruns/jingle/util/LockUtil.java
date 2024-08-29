package xyz.duncanruns.jingle.util;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.FileLock;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Timer;
import java.util.TimerTask;
import java.util.function.Consumer;

public final class LockUtil {
    private LockUtil() {
    }

    private static RandomAccessFile toRAF(Path path) throws FileNotFoundException {
        return new RandomAccessFile(path.toAbsolutePath().toFile(), "rw");
    }

    public static boolean isLocked(Path path) {
        if (!Files.exists(path)) {
            return false;
        }
        try {
            RandomAccessFile file = LockUtil.toRAF(path);
            FileLock fileLock = file.getChannel().tryLock();
            file.write('a');
            fileLock.release();
            file.close();
            return false;
        } catch (Exception e) {
            return true;
        }
    }

    public static LockStuff lock(Path path) {
        try {
            RandomAccessFile file = LockUtil.toRAF(path);
            FileLock fileLock = file.getChannel().lock();
            file.write('a');
            return new LockStuff(fileLock, file);
        } catch (Exception e) {
            return null;
        }
    }

    public static void keepTryingLock(Path path, Consumer<LockStuff> onSuccess) {
        new Timer().schedule(new TimerTask() {
            @Override
            public void run() {
                if (isLocked(path)) {
                    keepTryingLock(path, onSuccess);
                } else {
                    onSuccess.accept(lock(path));
                }

            }
        }, 2000);
    }

    public static void releaseLock(LockStuff stuff) {
        if (stuff == null) return;
        try {
            stuff.fileLock.release();
            stuff.file.close();
        } catch (IOException ignored) {
        }
    }

    public static class LockStuff {
        public final FileLock fileLock;
        public final RandomAccessFile file;

        public LockStuff(FileLock fileLock, RandomAccessFile file) {
            this.fileLock = fileLock;
            this.file = file;
        }
    }
}
