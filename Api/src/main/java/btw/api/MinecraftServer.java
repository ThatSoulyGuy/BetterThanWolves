package btw.api;

public abstract class MinecraftServer {
    private static MinecraftServer instance;

    public WorldServer[] worldServers;

    public static MinecraftServer getServer() {
        return instance;
    }

    public static void setInstance(MinecraftServer server) {
        instance = server;
    }

    public abstract boolean isSinglePlayer();

    public abstract ServerConfigurationManager getConfigurationManager();

    public abstract WorldServer worldServerForDimension(int dimension);

    public abstract ILogAgent getLogAgent();

    public boolean getPublic() { return false; }
}
