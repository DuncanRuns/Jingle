package xyz.duncanruns.jingle.script;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ScriptInterrupter {
    private static final List<ScriptInterrupter> ACTIVE = Collections.synchronizedList(new ArrayList<>());

    private final Runnable interruptFunction;

    private ScriptInterrupter(Runnable interruptFunction) {
        this.interruptFunction = interruptFunction;
    }

    public static void interruptAll() {
        ACTIVE.forEach(ScriptInterrupter::interrupt);
    }

    public static ScriptInterrupter add(Runnable interruptFunction) {
        ScriptInterrupter scriptInterrupter = new ScriptInterrupter(interruptFunction);
        ACTIVE.add(scriptInterrupter);
        return scriptInterrupter;
    }

    public void interrupt() {
        ACTIVE.remove(this);
        this.interruptFunction.run();
    }

    public void invalidate() {
        ACTIVE.remove(this);
    }
}
