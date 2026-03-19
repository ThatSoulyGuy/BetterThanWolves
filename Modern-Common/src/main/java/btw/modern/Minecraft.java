package btw.modern;

import java.io.File;

public class Minecraft {
    private static Minecraft instance;

    // Field types must EXACTLY match vanilla's net.minecraft.client.Minecraft
    // to avoid field shadowing issues at runtime.
    public PlayerControllerMP playerController;
    public WorldClient theWorld;
    public EntityClientPlayerMP thePlayer;
    public EffectRenderer effectRenderer;
    public RenderEngine renderEngine;
    public FontRenderer fontRenderer;
    public FontRenderer standardGalacticFontRenderer;
    public GameSettings gameSettings;

    public static Minecraft getMinecraft() {
        return instance;
    }

    public static void setInstance(Minecraft mc) {
        instance = mc;
    }

    public static File getMinecraftDir() {
        return new File(".");
    }

    public static boolean isAmbientOcclusionEnabled() {
        return false;
    }

    public boolean isSingleplayer() {
        return false;
    }

    public MinecraftServer getIntegratedServer() {
        return MinecraftServer.getServer();
    }

    public void displayGuiScreen(GuiScreen guiScreen) {}

    public ILogAgent getLogAgent() { return null; }
}
