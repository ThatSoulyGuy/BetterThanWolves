package btw.forge.client;

import btw.forge.ItemStackHelper;
import btw.forge.NamedIcon;
import btw.forge.ProxyRegistry;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.client.multiplayer.ClientLevel;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;

/**
 * A BakedModel for FC items with subtypes that have different textures
 * per damage value (tree bark, tuning forks, etc.).
 *
 * <p>Uses FC's {@code getIconFromDamage(int)} to look up per-damage
 * texture names, then resolves them to MC sprites and generates flat
 * item quads on demand.</p>
 */
public class FCItemSubtypeModel implements BakedModel {

    private static final Logger LOGGER = LogManager.getLogger("BTW-ItemSubtypeModel");

    private final BakedModel baseModel;
    private final int fcItemId;
    private final Map<Integer, BakedModel> damageModels = new HashMap<>();
    private final SubtypeOverrides overrides = new SubtypeOverrides();

    public FCItemSubtypeModel(BakedModel baseModel, int fcItemId) {
        this.baseModel = baseModel;
        this.fcItemId = fcItemId;
    }

    /**
     * Scans all FC items with subtypes and creates FCItemSubtypeModels
     * for those that have genuinely different textures per damage value.
     *
     * @param models the model map to inject into
     * @param iconCapturer an IconRegister that creates NamedIcons
     * @return number of items with subtype models
     */
    public static int injectSubtypeModels(Map<net.minecraft.resources.ResourceLocation, BakedModel> models,
                                           btw.modern.IconRegister iconCapturer) {
        int count = 0;

        for (int id = 478; id < btw.modern.Item.itemsList.length; id++) {
            btw.modern.Item fcItem = btw.modern.Item.itemsList[id];
            if (fcItem == null || !fcItem.getHasSubtypes()) continue;

            // Call registerIcons to populate icon fields
            try {
                fcItem.registerIcons(iconCapturer);
            } catch (Exception ignored) {}

            // Collect unique texture names across damage values
            Map<Integer, String> damageToTexture = new LinkedHashMap<>();
            Set<String> uniqueTextures = new HashSet<>();

            for (int dmg = 0; dmg < 64; dmg++) {
                try {
                    btw.modern.Icon icon = fcItem.getIconFromDamage(dmg);
                    if (icon instanceof NamedIcon ni) {
                        String texName = ni.getIconName();
                        if (texName != null && !texName.isEmpty()) {
                            damageToTexture.put(dmg, texName);
                            uniqueTextures.add(texName);
                        }
                    }
                } catch (Exception ignored) {}
            }

            // Only create subtype model if there are multiple distinct textures
            if (uniqueTextures.size() <= 1) continue;

            // Find the MC item's model resource location
            net.minecraft.world.item.Item modernItem = ProxyRegistry.getModernItem(id);
            if (modernItem == null) continue;

            net.minecraft.resources.ResourceLocation regName =
                    net.minecraftforge.registries.ForgeRegistries.ITEMS.getKey(modernItem);
            if (regName == null) continue;

            net.minecraft.client.resources.model.ModelResourceLocation mrl =
                    new net.minecraft.client.resources.model.ModelResourceLocation(regName, "inventory");

            BakedModel existing = models.get(mrl);
            if (existing == null) continue;

            FCItemSubtypeModel subtypeModel = new FCItemSubtypeModel(existing, id);

            // Pre-build per-damage models: prefer pre-baked variant models,
            // fall back to flat sprite quads
            for (Map.Entry<Integer, String> entry : damageToTexture.entrySet()) {
                String texName = entry.getValue();

                // Try to find a pre-baked model for this texture name
                // (e.g., betterthanwolves:item/fcitembarkoak)
                BakedModel variantModel = null;
                net.minecraft.resources.ResourceLocation variantLoc =
                        new net.minecraft.resources.ResourceLocation(
                                btw.forge.BTWForgeMod.MOD_ID, "item/" + texName);
                variantModel = models.get(variantLoc);

                if (variantModel != null) {
                    subtypeModel.damageModels.put(entry.getKey(), variantModel);
                } else {
                    // Fall back to flat sprite quad
                    TextureAtlasSprite sprite = FCBakedModel.lookupSprite(texName);
                    if (sprite != null && !sprite.contents().name().toString().contains("missingno")) {
                        subtypeModel.damageModels.put(entry.getKey(),
                                new FlatItemModel(sprite, existing));
                    }
                }
            }

            if (!subtypeModel.damageModels.isEmpty()) {
                models.put(mrl, subtypeModel);
                count++;
            }
        }

        return count;
    }

    @Override
    public List<BakedQuad> getQuads(BlockState state, Direction side, RandomSource rand) {
        return baseModel.getQuads(state, side, rand);
    }

    @Override public boolean useAmbientOcclusion() { return baseModel.useAmbientOcclusion(); }
    @Override public boolean isGui3d() { return baseModel.isGui3d(); }
    @Override public boolean usesBlockLight() { return baseModel.usesBlockLight(); }
    @Override public boolean isCustomRenderer() { return false; }
    @Override public TextureAtlasSprite getParticleIcon() { return baseModel.getParticleIcon(); }
    @Override public ItemTransforms getTransforms() { return baseModel.getTransforms(); }

    @Override
    public ItemOverrides getOverrides() {
        return overrides;
    }

    private class SubtypeOverrides extends ItemOverrides {
        @Override
        public BakedModel resolve(BakedModel model, ItemStack stack,
                                  ClientLevel level, LivingEntity entity, int seed) {
            int damage = stack.getDamageValue();
            BakedModel sub = damageModels.get(damage);
            return sub != null ? sub : baseModel;
        }
    }

    /**
     * A simple flat item model (like item/generated) using a single sprite.
     */
    private static class FlatItemModel implements BakedModel {
        private final List<BakedQuad> quads;
        private final TextureAtlasSprite sprite;
        private final BakedModel template;

        FlatItemModel(TextureAtlasSprite sprite, BakedModel template) {
            this.sprite = sprite;
            this.template = template;
            this.quads = generateFlatQuads(sprite);
        }

        private static List<BakedQuad> generateFlatQuads(TextureAtlasSprite sprite) {
            List<BakedQuad> result = new ArrayList<>();
            // Generate a flat 16x16 item quad (front face, south-facing)
            float u0 = sprite.getU0(), u1 = sprite.getU1();
            float v0 = sprite.getV0(), v1 = sprite.getV1();

            // Front face (south, Z=0.5)
            int[] vertexData = makeQuad(
                    0, 0, 0.5f,  u0, v1,
                    1, 0, 0.5f,  u1, v1,
                    1, 1, 0.5f,  u1, v0,
                    0, 1, 0.5f,  u0, v0
            );
            result.add(new BakedQuad(vertexData, -1, Direction.SOUTH, sprite, true));

            // Back face (north, Z=0.5)
            int[] backData = makeQuad(
                    0, 0, 0.5f,  u1, v1,
                    0, 1, 0.5f,  u1, v0,
                    1, 1, 0.5f,  u0, v0,
                    1, 0, 0.5f,  u0, v1
            );
            result.add(new BakedQuad(backData, -1, Direction.NORTH, sprite, true));

            return result;
        }

        private static int[] makeQuad(
                float x0, float y0, float z0, float u0, float v0,
                float x1, float y1, float z1, float u1, float v1,
                float x2, float y2, float z2, float u2, float v2,
                float x3, float y3, float z3, float u3, float v3) {
            int[] data = new int[32]; // 4 vertices * 8 ints
            int stride = DefaultVertexFormat.BLOCK.getIntegerSize();
            putVertex(data, 0 * stride, x0, y0, z0, u0, v0);
            putVertex(data, 1 * stride, x1, y1, z1, u1, v1);
            putVertex(data, 2 * stride, x2, y2, z2, u2, v2);
            putVertex(data, 3 * stride, x3, y3, z3, u3, v3);
            return data;
        }

        private static void putVertex(int[] data, int offset, float x, float y, float z,
                                       float u, float v) {
            data[offset] = Float.floatToRawIntBits(x);
            data[offset + 1] = Float.floatToRawIntBits(y);
            data[offset + 2] = Float.floatToRawIntBits(z);
            data[offset + 3] = 0xFFFFFFFF; // color (white, full alpha)
            data[offset + 4] = Float.floatToRawIntBits(u);
            data[offset + 5] = Float.floatToRawIntBits(v);
            data[offset + 6] = 0; // packed light
            data[offset + 7] = 0; // packed normal
        }

        @Override
        public List<BakedQuad> getQuads(BlockState state, Direction side, RandomSource rand) {
            return side == null ? quads : Collections.emptyList();
        }

        @Override public boolean useAmbientOcclusion() { return false; }
        @Override public boolean isGui3d() { return false; }
        @Override public boolean usesBlockLight() { return false; }
        @Override public boolean isCustomRenderer() { return false; }
        @Override public TextureAtlasSprite getParticleIcon() { return sprite; }
        @Override public ItemTransforms getTransforms() { return template.getTransforms(); }
        @Override public ItemOverrides getOverrides() { return ItemOverrides.EMPTY; }
    }
}
