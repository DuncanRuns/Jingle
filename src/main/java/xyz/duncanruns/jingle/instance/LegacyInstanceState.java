package xyz.duncanruns.jingle.instance;

public enum LegacyInstanceState {
    WAITING,
    INWORLD,
    TITLE,
    GENERATING,
    PREVIEWING,
    WALL;

    public enum InWorldState {
        UNPAUSED,
        PAUSED,
        GAMESCREENOPEN
    }
}
