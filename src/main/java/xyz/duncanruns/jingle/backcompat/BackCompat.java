package xyz.duncanruns.jingle.backcompat;

import me.duncanruns.kerykeion.Kerykeion;
import xyz.duncanruns.jingle.Jingle;

public final class BackCompat {
    private BackCompat() {
    }

    public static void init() {
        Kerykeion.addListener(new WorldEventsListener(),5, Jingle.EXECUTOR);
    }
}
