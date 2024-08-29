package xyz.duncanruns.jingle.script.lua;

import org.luaj.vm2.*;
import org.luaj.vm2.lib.DebugLib;

public class InterruptibleDebugLib extends DebugLib {
    private boolean interrupted = false;

    public static boolean unInterruptGlobals(Globals globals) {
        DebugLib debuglib = globals.debuglib;
        if (!(debuglib instanceof InterruptibleDebugLib)) return false;
        ((InterruptibleDebugLib) debuglib).unInterrupt();
        return true;

    }

    public static boolean interruptGlobals(Globals globals) {
        DebugLib debuglib = globals.debuglib;
        if (!(debuglib instanceof InterruptibleDebugLib)) return false;
        ((InterruptibleDebugLib) debuglib).interrupt();
        return true;
    }

    public void interrupt() {
        this.interrupted = true;
    }

    public void unInterrupt() {
        this.interrupted = false;
    }

    @Override
    public LuaValue call(LuaValue modname, LuaValue env) {
        Globals globals = env.checkglobals();
        globals.debuglib = this;
        return new LuaTable();
    }

    @Override
    public void onCall(LuaFunction f) {
    }

    @Override
    public void onCall(LuaClosure c, Varargs varargs, LuaValue[] stack) {
    }

    @Override
    public void onInstruction(int pc, Varargs v, int top) {
        if (this.interrupted) {
            this.interrupted = false;
            throw new LuaInterruptedException();
        }
    }

    @Override
    public void onReturn() {
    }

    public static class LuaInterruptedException extends RuntimeException {
    }
}
