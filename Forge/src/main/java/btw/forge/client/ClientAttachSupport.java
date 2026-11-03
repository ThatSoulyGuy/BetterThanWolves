package btw.forge.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.level.LevelReader;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import javax.annotation.Nullable;

/**
 * Client half of btw.forge.FcAttachSupport's world choice: the read-only {@link ClientFcWorld} over the client level, on
 * the render thread only (ClientFcWorld's rule), so 1.20.1's client-side BlockItem.place refuses a torch, soul torch,
 * ladder, snow layer, pressure plate or rail (and FC's own blocks of these families) exactly where the server refuses
 * it and neither draws the block nor shrinks the stack (the server does not resend the held slot after a refused
 * placement). The world is ClientFcWorld's support view ({@link ClientFcWorld#forSupport}): a modern fence, wall or
 * stained glass pane reads as its BTW relative there, as the server's stand-in answers through that relative. Reached
 * only through DistExecutor (Dist.CLIENT), so a dedicated server never loads it.
 */
// FCMOD-INTEGRATION(1.20.1): attach_support_rules: the client predicts FC's attachment support answers (torches, ladders, snow layers, plates, rails) on the read-only FC world's support view, as the server answers them
@OnlyIn(Dist.CLIENT)
public final class ClientAttachSupport {

    private ClientAttachSupport() {}

    /** ClientFcWorld's support view for a client level on the render thread, else null (no veto). */
    public static @Nullable btw.modern.World worldFor(LevelReader level) {
        if (level instanceof ClientLevel clientLevel && Minecraft.getInstance().isSameThread()) {
            // FCMOD-INTEGRATION(1.20.1): support_relatives: the support view, where support relatives read as their BTW relative
            return ClientFcWorld.forSupport(clientLevel);
        }
        return null;
    }
}
