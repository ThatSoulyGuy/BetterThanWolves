package btw.modern;

public class BlockRedstoneLight extends Block {

    // FCMOD-INTEGRATION(1.20.1): legacy_icons: 1.5.2 keeps the lit flag (vanilla/client BlockRedstoneLight.java:22,
    // :27), which registerIcons (:39-49) reads: "redstoneLight" (123) or "redstoneLight_lit" (124).
    // FCBlockRedstoneLight inherits it.
    private final boolean powered;

    protected BlockRedstoneLight(int id, boolean powered) {
        super(id, Material.glass);
        this.powered = powered;

        // 1.5.2 BlockRedstoneLight ctor (vanilla/server BlockRedstoneLight.java:23-32): the lit lamp (124) emits
        // full light, the unlit one (123) none (Block.lightValue, inherited by FCBlockRedstoneLight; read by
        // FCBlockLens).
        if (powered) {
            setLightValue(1.0F);
        }
    }

    // 1.5.2 BlockRedstoneLight.registerIcons (vanilla/client BlockRedstoneLight.java:39-49)
    @Override
    public void registerIcons(IconRegister register) {
        if (this.powered) {
            this.blockIcon = register.registerIcon("redstoneLight_lit");
        } else {
            this.blockIcon = register.registerIcon("redstoneLight");
        }
    }
}
