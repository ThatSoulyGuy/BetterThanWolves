package btw.modern;

public interface ICommandSender {
    String getCommandSenderName();
    void sendChatToPlayer(String message);
    boolean canCommandSenderUseCommand(int level, String command);
    ChunkCoordinates getPlayerCoordinates();
}
