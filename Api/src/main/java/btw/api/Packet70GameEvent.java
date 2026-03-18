package btw.api;

public class Packet70GameEvent extends Packet {
    public static final int START_RAIN = 1;
    public static final int STOP_RAIN = 2;
    public static final int CHANGE_GAME_MODE = 3;
    public static final int ENTER_CREDITS = 4;
    public static final int DEMO = 5;

    public int eventType;
    public int gameMode;

    public Packet70GameEvent() {}
    public Packet70GameEvent(int type, int mode) {
        this.eventType = type;
        this.gameMode = mode;
    }

    public int getPacketSize() { return 0; }
}
