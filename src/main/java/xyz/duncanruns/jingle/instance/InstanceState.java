package xyz.duncanruns.jingle.instance;

public enum InstanceState {
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
