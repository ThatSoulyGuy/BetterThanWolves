package btw.forge;

import net.minecraft.world.level.block.Block;
import net.minecraftforge.registries.ForgeRegistries;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Startup self-audit for the BTW Forge bridge — the runtime companion to the build-time
 * {@code tools/BridgeAudit.java} and {@code tools/LinkAudit.java}. It validates registry/asset
 * state that only exists once the mod is loaded, logging actionable warnings at launch so the
 * bug classes we kept hitting in game are caught at startup instead.
 *
 * <p>Checks here are COMMON (both sides). The client-only renderer-coverage check lives in
 * {@code FCEntityRenderer.auditRendererCoverage()} (it needs the client render map), run right
 * after the renderers register.</p>
 */
public final class BridgeSelfAudit {

    private static final Logger LOGGER = LogManager.getLogger("BTW-SelfAudit");

    private BridgeSelfAudit() {}

    /** Run all common-side checks. Called from FMLCommonSetupEvent (registries frozen by then). */
    public static void runCommon() {
        auditBlockSpeedFactors();
    }

    /**
     * Speed-factor sanity: MC 1.20.1 multiplies {@link Block#getSpeedFactor()} into the carried
     * {@code deltaMovement} every tick in {@code Entity.move()}. A factor &gt; 1.0 therefore
     * compounds into momentum and, while airborne, the retention product {@code 0.91 × factor}
     * exceeds 1.0 → unbounded speed ("flying"). This is exactly the bug BTW's +20% hard-surface
     * bonus caused when it was (wrongly) routed through getSpeedFactor. Any block reporting a
     * factor &gt; 1.0 is a regression of that class. (Slowdowns &lt; 1.0 are always safe.)
     */
    private static void auditBlockSpeedFactors() {
        int bad = 0, checked = 0;
        for (Block b : ForgeRegistries.BLOCKS) {
            try {
                float sf = b.getSpeedFactor();
                checked++;
                if (sf > 1.0F) {
                    LOGGER.warn("[SELF-AUDIT/speed] {} getSpeedFactor={} > 1.0 -> compounds into airborne "
                            + "momentum (flying-class regression; bonuses must go through getSpeed, not getSpeedFactor)",
                            ForgeRegistries.BLOCKS.getKey(b), sf);
                    bad++;
                }
            } catch (Throwable ignored) {}
        }
        LOGGER.info("[SELF-AUDIT/speed] checked {} blocks, {} with getSpeedFactor > 1.0 (0 expected).",
                checked, bad);
    }
}
