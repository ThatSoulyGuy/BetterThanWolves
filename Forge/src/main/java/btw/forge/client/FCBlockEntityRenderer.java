package btw.forge.client;

import btw.forge.ItemStackHelper;
import btw.forge.ProxyBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Renders FC tile entity visuals: cooking items on campfires/furnaces,
 * placed tools, basket lids, etc.
 *
 * <p>Instead of running FC's TileEntitySpecialRenderer (which requires
 * a full RenderManager bridge), this reads FC tile entity state directly
 * and renders items using MC 1.20.1's native ItemRenderer.</p>
 */
public class FCBlockEntityRenderer implements BlockEntityRenderer<ProxyBlockEntity> {

    @Override
    public boolean shouldRenderOffScreen(ProxyBlockEntity blockEntity) {
        // Always render — FC tile entities may have items/effects that extend beyond the block
        return true;
    }

    @Override
    public int getViewDistance() {
        return 64;
    }

    private static final Logger LOGGER = LogManager.getLogger("BTW-BERenderer");
    private final ItemRenderer itemRenderer;

    public FCBlockEntityRenderer(BlockEntityRendererProvider.Context ctx) {
        this.itemRenderer = ctx.getItemRenderer();
    }

    @Override
    public void render(ProxyBlockEntity blockEntity, float partialTick,
                       PoseStack poseStack, MultiBufferSource bufferSource,
                       int packedLight, int packedOverlay) {
        btw.modern.TileEntity fcTe = blockEntity.getFcTileEntity();
        if (fcTe == null) return;

        String className = fcTe.getClass().getSimpleName();

        // Try rendering cook stack for any tile entity that has one
        btw.modern.ItemStack cookStack = getCookStack(fcTe);
        if (cookStack != null) {
            if (className.contains("Campfire")) {
                renderCampfireCookItem(cookStack, poseStack, bufferSource, packedLight, packedOverlay, blockEntity);
            } else {
                renderGenericCookItem(cookStack, poseStack, bufferSource, packedLight, packedOverlay);
            }
        }

        // Emit cooking particles for crucible/cauldron when fire is below
        if (className.contains("CookingVessel") || className.contains("Crucible") || className.contains("Cauldron")) {
            emitCookingParticles(fcTe, blockEntity);
        }
    }

    /** Emits steam/smoke particles from cooking vessels when they have fire below. */
    private void emitCookingParticles(btw.modern.TileEntity fcTe, ProxyBlockEntity blockEntity) {
        // Check m_iFireUnderType via reflection (0=none, 1=normal fire, 2=stoked)
        int fireType = 0;
        try {
            var f = fcTe.getClass().getField("m_iFireUnderType");
            fireType = f.getInt(fcTe);
        } catch (Exception e) {
            try {
                var f = fcTe.getClass().getDeclaredField("m_iFireUnderType");
                f.setAccessible(true);
                fireType = f.getInt(fcTe);
            } catch (Exception ignored) {}
        }

        if (fireType <= 0) return;

        var level = Minecraft.getInstance().level;
        if (level == null) return;

        var pos = blockEntity.getBlockPos();
        var rand = level.getRandom();

        // Normal fire: gentle steam. Stoked fire: more intense
        int chance = fireType == 2 ? 2 : 4;
        if (rand.nextInt(chance) == 0) {
            double px = pos.getX() + 0.2 + rand.nextFloat() * 0.6;
            double py = pos.getY() + 0.9 + rand.nextFloat() * 0.2;
            double pz = pos.getZ() + 0.2 + rand.nextFloat() * 0.6;

            if (fireType == 2) {
                // Stoked: larger smoke
                level.addParticle(net.minecraft.core.particles.ParticleTypes.LARGE_SMOKE,
                        px, py, pz, 0, 0.05, 0);
            } else {
                // Normal: gentle steam
                level.addParticle(net.minecraft.core.particles.ParticleTypes.CAMPFIRE_COSY_SMOKE,
                        px, py, pz, 0, 0.03, 0);
            }
        }
    }

    private void renderCampfireCookItem(btw.modern.ItemStack fcCookStack,
                                         PoseStack poseStack, MultiBufferSource bufferSource,
                                         int packedLight, int packedOverlay,
                                         ProxyBlockEntity blockEntity) {
        ItemStack mcStack = ItemStackHelper.toMcStack(fcCookStack);
        if (mcStack.isEmpty()) return;

        // Check spit alignment from block metadata
        boolean iAligned = false;
        try {
            int meta = 0;
            if (blockEntity.getBlockState().hasProperty(btw.forge.ProxyBlock.META)) {
                meta = blockEntity.getBlockState().getValue(btw.forge.ProxyBlock.META);
            }
            // GetIsIAligned checks (metadata & 4) != 0
            iAligned = (meta & 4) != 0;
        } catch (Exception ignored) {}

        poseStack.pushPose();
        poseStack.translate(0.5, 14.0 / 16.0, 0.5);

        // Rotate based on spit alignment (matching FC's renderer)
        if (!iAligned) {
            poseStack.mulPose(Axis.YP.rotationDegrees(90));
        }

        poseStack.scale(0.5f, 0.5f, 0.5f);

        itemRenderer.renderStatic(mcStack, ItemDisplayContext.FIXED,
                packedLight, packedOverlay, poseStack, bufferSource,
                Minecraft.getInstance().level, 0);
        poseStack.popPose();

        // Spawn cooking particles only when the campfire is lit (fire level > 0).
        // Unlit campfire = block 1013 (m_iFireLevel=0), lit = 1014/1015/1016.
        boolean isLit = false;
        if (blockEntity.getBlockState().getBlock() instanceof btw.forge.ProxyBlock pb) {
            isLit = pb.getLegacyId() >= 1014 && pb.getLegacyId() <= 1016;
        }
        if (isLit && Minecraft.getInstance().level != null) {
            var level = Minecraft.getInstance().level;
            var pos = blockEntity.getBlockPos();
            var rand = level.getRandom();
            if (rand.nextInt(3) == 0) {
                double px = pos.getX() + 0.375 + rand.nextFloat() * 0.25;
                double py = pos.getY() + 0.75 + rand.nextFloat() * 0.1;
                double pz = pos.getZ() + 0.375 + rand.nextFloat() * 0.25;
                level.addParticle(
                        net.minecraft.core.particles.ParticleTypes.CAMPFIRE_COSY_SMOKE,
                        px, py, pz, 0, 0.02, 0);
            }
        }
    }

    private void renderGenericCookItem(btw.modern.ItemStack fcCookStack,
                                        PoseStack poseStack, MultiBufferSource bufferSource,
                                        int packedLight, int packedOverlay) {
        ItemStack mcStack = ItemStackHelper.toMcStack(fcCookStack);
        if (mcStack.isEmpty()) return;

        poseStack.pushPose();
        poseStack.translate(0.5, 6.5 / 16.0, 0.5);
        poseStack.scale(0.4f, 0.4f, 0.4f);

        itemRenderer.renderStatic(mcStack, ItemDisplayContext.FIXED,
                packedLight, packedOverlay, poseStack, bufferSource,
                Minecraft.getInstance().level, 0);
        poseStack.popPose();
    }

    private static final java.util.Set<Integer> loggedPositions = java.util.Collections.synchronizedSet(new java.util.HashSet<>());
    private static boolean logRendererOnce(int posHash, String className, Object cookStack) {
        int key = posHash ^ (cookStack != null ? 1 : 0);
        return loggedPositions.add(key);
    }

    /** Reads the cook/display stack from an FC tile entity via reflection. */
    private static btw.modern.ItemStack getCookStack(btw.modern.TileEntity fcTe) {
        btw.modern.ItemStack result = null;

        // Try GetCookStack() method (campfire, furnace)
        try {
            var method = fcTe.getClass().getMethod("GetCookStack");
            result = (btw.modern.ItemStack) method.invoke(fcTe);
        } catch (Exception ignored) {}

        // Try GetDisplayStack() or similar
        if (result == null) {
            try {
                var method = fcTe.getClass().getMethod("GetDisplayStack");
                result = (btw.modern.ItemStack) method.invoke(fcTe);
            } catch (Exception ignored) {}
        }

        // Treat itemID 0 (air) or stackSize 0 as empty
        if (result != null && (result.itemID <= 0 || result.stackSize <= 0)) {
            return null;
        }

        return result;
    }
}
