package xyz.duncanruns.jingle.event;

import java.util.LinkedList;
import java.util.List;

public class RunnableEventType {

    private final List<Runnable> runnables = new LinkedList<>();

    @SuppressWarnings("unused")
    public void register(Runnable runnable) {
        this.runnables.add(runnable);
    }

    public void runAll() {
        this.runnables.forEach(Runnable::run);
    }

    public void clear() {
        this.runnables.clear();
    }
}
