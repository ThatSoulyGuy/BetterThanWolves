package btw.api;

public abstract class CommandBase {
    public abstract String getCommandName();
    public abstract String getCommandUsage(ICommandSender sender);
    public abstract void processCommand(ICommandSender sender, String[] args);
    public int getRequiredPermissionLevel() { return 4; }
    public boolean canCommandSenderUseCommand(ICommandSender sender) {
        return sender.canCommandSenderUseCommand(this.getRequiredPermissionLevel(), this.getCommandName());
    }
    public static int parseInt(ICommandSender sender, String input) { return 0; }
    public static double parseDouble(ICommandSender sender, String input) { return 0.0D; }
}
