package xyz.duncanruns.jingle.instance;

import xyz.duncanruns.jingle.instance.LegacyInstanceState.InWorldState;

public interface LegacyStateTracker {
    boolean tryUpdate();

    long getLastOccurrenceOf(LegacyInstanceState state);

    long getLastStartOf(LegacyInstanceState state);

    LegacyInstanceState getInstanceState();

    boolean isCurrentState(LegacyInstanceState state);

    InWorldState getInWorldState();
}
