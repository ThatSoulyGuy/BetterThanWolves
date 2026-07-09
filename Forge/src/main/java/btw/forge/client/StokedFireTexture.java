package btw.forge.client;

import btw.forge.BTWForgeMod;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Authentic runtime procedural stoked-fire (Hibachi) flame — a direct port of FC's
 * {@code FCClientAnimationFire}. Each client tick it runs FC's cellular fire simulation
 * (a bottom row seeded with random intensity, drifting upward with a weighted decay) and
 * re-uploads the generated RGBA frame straight onto the block atlas at the stoked-fire
 * sprite's location — exactly what FC's 1.5.2 {@code TextureFX} did. {@code FCBakedModel}
 * already bakes the block's custom flame geometry against that sprite, so the flame
 * animates smoothly (white-hot core, orange edges) with no baked frames or .mcmeta.
 *
 * <p>Two independent simulations drive the two checkerboard variants (fcblockfirestokedstub
 * _0/_1) FC alternates between adjacent fire blocks. The stoked variant reads the hotter
 * lower half of the 16x32 intensity field (see CopyStokedFireFrameToByteBuffer).</p>
 *
 * <p>Caveat: only atlas mip level 0 is re-uploaded, so at a distance the flame falls back to
 * the static stub texture's mips. Fine for a full-bright emissive flame viewed up close.</p>
 */
@Mod.EventBusSubscriber(modid = BTWForgeMod.MOD_ID, value = Dist.CLIENT)
public final class StokedFireTexture {

    private static final Logger LOGGER = LogManager.getLogger("BTW-StokedFire");

    private static final int W = 16, H = 32, TEX_H = 16, PIXELS = TEX_H * W; // 256
    private static final float DECAY = 1F + 0.08F;      // FC: 1 + 0.08*(16/texH)
    private static final float DECAY_TOP = 1F + 0.07F;
    private static final double DIST_MOD = 0.123D;      // FC: 0.123*(16/texW)
    private static final int CENTER = W / 2;

    // Color-ramp thresholds (FCClientAnimationFire)
    private static final float BLUE_TO_WHITE = 0.39F;
    private static final float WHITE_TO_RED = 0.66F;
    private static final float INVIS_TOP = 0.87F;
    private static final float INVIS_BOTTOM = 0.001F;

    private static final String[] SPRITE_NAMES = {
            "block/fcblockfirestokedstub_0", "block/fcblockfirestokedstub_1"
    };

    private static final Fire[] FIRES = { new Fire(), new Fire() };
    private static boolean disabled = false;
    private static int warnCount = 0;

    private StokedFireTexture() {}

    private static final class Fire {
        final float[] field = new float[W * H];
        final NativeImage image = new NativeImage(NativeImage.Format.RGBA, W, TEX_H, false);
        TextureAtlasSprite sprite;

        void tick() {
            driftUpwards();
            generateBottomRow();
        }

        // FCClientAnimationFire.DriftFireUpwards (in-place; FC aliases prev=cur after frame 1)
        void driftUpwards() {
            for (int i = 0; i < W; i++) {
                for (int j = 0; j < H - 1; j++) {
                    int weight = 18;
                    float ni = field[i + (j + 1) * W] * 18F;
                    for (int ti = i - 1; ti <= i + 1; ti++) {
                        for (int tj = j; tj <= j + 1; tj++) {
                            if (ti >= 0 && tj >= 0 && ti < W && tj < H) {
                                ni += field[ti + tj * W];
                            }
                            weight++;
                        }
                    }
                    ni /= (weight * (j < TEX_H ? DECAY_TOP : DECAY));
                    field[i + j * W] = ni;
                }
            }
        }

        // FCClientAnimationFire.GenerateNewBottomRow
        void generateBottomRow() {
            for (int i = 0; i < W; i++) {
                double distFromCenter = CENTER - Math.abs(i - (CENTER - 1));
                double base = Math.random() * Math.random() * Math.random() * 4D
                        + Math.random() * 0.10000000149011612D + 0.2D;
                double dm = distFromCenter * DIST_MOD;
                dm = dm * dm;
                field[i + (H - 1) * W] = (float) (base + dm);
            }
        }

        // FCClientAnimationFire.CopyStokedFireFrameToByteBuffer -> NativeImage (ABGR)
        void fillImage() {
            for (int p = 0; p < PIXELS; p++) {
                float intensity = field[p + PIXELS]; // stoked = hotter lower half
                if (intensity > 1F) intensity = 1F; else if (intensity < 0F) intensity = 0F;
                float cm = 1F - intensity;
                int r = 0, g = 0, b = 0, a = 255;
                if (cm > INVIS_TOP || cm < INVIS_BOTTOM) {
                    a = 0;
                } else if (cm < BLUE_TO_WHITE) {
                    float f = cm / BLUE_TO_WHITE, f2 = f * f;
                    r = (int) (f2 * 255F);
                    g = (int) (f2 * 255F);
                    b = (int) (f * 100F) + 155;
                } else if (cm < WHITE_TO_RED) {
                    r = 255; g = 255; b = 255;
                } else {
                    float d = 1F - ((cm - WHITE_TO_RED) / (1F - WHITE_TO_RED));
                    r = (int) (d * 120F) + 135;
                    float d2 = d * d;
                    g = (int) (d2 * 225F) + 30;
                    float bm = d2 * d2;
                    bm *= bm;
                    b = (int) (bm * 255F);
                }
                image.setPixelRGBA(p % W, p / W,
                        (a << 24) | ((b & 0xFF) << 16) | ((g & 0xFF) << 8) | (r & 0xFF));
            }
        }
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END || disabled) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || !RenderSystem.isOnRenderThread()) return;
        try {
            TextureAtlas atlas = (TextureAtlas) mc.getTextureManager().getTexture(TextureAtlas.LOCATION_BLOCKS);
            if (atlas == null) return;

            if (FIRES[0].sprite == null) {
                for (int v = 0; v < FIRES.length; v++) {
                    ResourceLocation want = new ResourceLocation(BTWForgeMod.MOD_ID, SPRITE_NAMES[v]);
                    TextureAtlasSprite s = atlas.getSprite(want);
                    // getSprite returns the missing sprite (not null) for unstitched textures.
                    if (s == null || !want.equals(s.contents().name())) {
                        disabled = true;
                        LOGGER.warn("Stoked-fire sprite '{}' not stitched; procedural flame disabled.", want);
                        return;
                    }
                    FIRES[v].sprite = s;
                }
                LOGGER.info("Stoked-fire procedural flame active ({} variants).", FIRES.length);
            }

            RenderSystem.bindTexture(atlas.getId());
            for (Fire f : FIRES) {
                f.tick();
                f.fillImage();
                f.image.upload(0, f.sprite.getX(), f.sprite.getY(), 0, 0, W, TEX_H, false, false);
            }
        } catch (Throwable t) {
            if (warnCount++ < 3) {
                LOGGER.warn("Stoked-fire update failed ({}): {}", warnCount, t.toString());
            }
            if (warnCount >= 3) disabled = true;
        }
    }
}
