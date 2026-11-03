package btw.forge.mixin;

import btw.forge.FcAttachSupport;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.BasePressurePlateBlock;
import net.minecraft.world.level.block.BaseRailBlock;
import net.minecraft.world.level.block.LadderBlock;
import net.minecraft.world.level.block.SnowLayerBlock;
import net.minecraft.world.level.block.TorchBlock;
import net.minecraft.world.level.block.WallTorchBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * FCMOD-INTEGRATION(1.20.1): attach_support_rules (INTEGRATIONS #055)
 *
 * <p><b>Feature:</b> 1.20.1's attachment test of the torch, the wall torch and the ladder: TorchBlock.canSurvive
 * (TorchBlock.java:35-37, a sturdy centre below), WallTorchBlock.canSurvive (WallTorchBlock.java:46-51, the sturdy face
 * of the block behind) and LadderBlock.canSurvive (LadderBlock.java:54-57, the same). Every 1.20.1 placement asks it:
 * StandingAndWallBlockItem.getPlacementState and canPlace, WallTorchBlock and LadderBlock.getStateForPlacement,
 * BlockItem.canPlace; and so does 1.20.1's own pop-off, updateShape (TorchBlock.java:31-33, WallTorchBlock.java:73-75,
 * LadderBlock.java:59-69). The soul torch and soul wall torch are plain TorchBlock / WallTorchBlock instances
 * (Blocks.SOUL_TORCH, Blocks.SOUL_WALL_TORCH), and RedstoneWallTorchBlock tests the facings it tries as WALL_TORCH
 * states (its getStateForPlacement re-keys Blocks.WALL_TORCH.getStateForPlacement) and passes its own state to
 * Blocks.WALL_TORCH.canSurvive. The same test of the floor attachments: SnowLayerBlock.canSurvive
 * (SnowLayerBlock.java:72-81, a full collision face below or the two snow-layer block tags), asked by 1.20.1's weather
 * (ServerLevel.tickChunk, ServerLevel.java:442-456, through Biome.shouldSnow, Biome.java:176-189), BlockItem.canPlace
 * (BlockItem.java:152-156) and its pop-off (updateShape, :83-85); BasePressurePlateBlock.canSurvive
 * (BasePressurePlateBlock.java:51-54, rigid or centre support below), asked by BlockItem.canPlace and its pop-off
 * (updateShape facing down, :47-49); BaseRailBlock.canSurvive (BaseRailBlock.java:51-53, rigid support below), asked by
 * BlockItem.canPlace only: a rail's survival is its own neighbour check, btw.forge.mixin.RailSupportMixin.</p>
 *
 * <p><b>Conflict with FC:</b> minecraft:torch and minecraft:wall_torch are FC's torch 50 (FCBlockTorchLegacy) and
 * minecraft:ladder FC's ladder 65 (FCBlockLegacyLadder). Their 4.B rule (Client FCBlockTorchBase.canPlaceTorchOn and
 * ValidateState's centre hard point, FCBlockTorchBase.java:236-281; FCBlockLadderBase.CanAttachToFacing,
 * FCBlockLadderBase.java:182-193) ran only in FC's neighbour check (BlockBehaviorMixin) and in FC's own item route,
 * which places BTW's torch and ladder. Where that route does not run (a sneaking player, a clicked block with no FC
 * identity or whose 1.20.1 use answers first, a mod calling useOn), 1.20.1's sturdy faces alone placed the vanilla torch
 * and ladder: on glowstone, redstone lamps, TNT, pistons, beacons, the tops of glass panes and iron bars and the sides of
 * glass and ice, which 4.B refuses; FC's neighbour check then popped them at the next block change beside them. The
 * soul torch (post-1.5.2, no FC identity, no FC route) took 1.20.1's rule on every click. minecraft:snow is FC's snow
 * cover 78 (FCBlockSnowCover), minecraft:stone_pressure_plate and minecraft:oak_pressure_plate FC's plates 70 and 72
 * (FCBlockPressurePlate), minecraft:rail and minecraft:detector_rail FC's rails 66 and 28, BTW's detector rails 235 and
 * 236 are 1.20.1 detector rails (FcDetectorRailBlock), and minecraft:powered_rail is 4.B's powered rail 27 (a
 * BlockRailBase in 4.B; the port's FC block 27 is a plain block, so it asks the rail's rule through FcAttachSupport's
 * support twin). The other wooden pressure plates and the polished blackstone plate have no FC identity: they ask the
 * oak plate's (72) and the stone plate's (70) rule through support twins too. 1.20.1's weather laid snow on glass,
 * TNT, pistons, unlit redstone lamps and a closed upper oak trapdoor, and a sneaking player placed snow layers, plates
 * and rails there and on glowstone, beacons and an eight-layer snow (plates and rails on ice, hoppers and cauldrons
 * too, plates on fences, walls, panes, iron bars and anvils; a snow layer is never motion-blocking, so the weather
 * does not stack one on an eight-layer snow): 4.B lays and places none of them there (vanilla/server World.canSnowAt,
 * World.java:2970-3002, asks FC's snow cover rule).</p>
 *
 * <p><b>Integration pattern:</b> D (FC-extend): 1.20.1's placement and pop-off executors stay; their support answer is
 * vetoed by FC's own predicate for the state's orientation ({@link FcAttachSupport#refuses}). Maintainer rulings
 * 2026-10-23: torches and ladders are refused by placement wherever 4.B refused them, with no pop-off window; soul torches
 * take the torch's 4.B support rule (without an FC identity: FcAttachSupport's support twins). Ruling 2026-10-31: with
 * 4.B's solid top, 1.20.1's paths that never ask FC (weather snow, a sneaking player's snow layer, plate and rail) follow
 * 4.B through the same veto, and their survival ends where 4.B's ends; the modern plates take their BTW relative's plate
 * rule, and modern fences, walls and stained glass panes answer every support question as their BTW relatives (the oak
 * fence, the cobblestone wall, the glass pane: FcAttachSupport's support relatives).</p>
 *
 * <p><b>Rebalance:</b> none (4.B parity). The veto only removes supports: the few supports FC accepts and 1.20.1
 * refuses (leaves tops, the stub weighted plates, activator rails, trapped chests; snow on bottom slabs, fences and
 * walls) stay refused on this path; BTW's own torch and ladder keep 4.B's whole rule through FC's item route. The
 * redstone torch and redstone wall torch (FC 76, Modern-Common's 1.5.2 BlockTorch stand-in) take 4.B's redstone torch
 * rule (FcAttachSupport's third family, ruling Q-P1 (a), the FOUND unit): a torch support below a standing one, a normal
 * cube behind a wall one. A redstone wall torch's placement also asks the wall torch's answer first (1.20.1 probes it
 * with WALL_TORCH states), whose refusals are a subset of the redstone rule's. The floor families veto only over a
 * support FC reads as itself (air, a block with an FC identity, or a support relative read as its BTW relative): a
 * modern block without a support relative keeps 1.20.1's answer (the weighted plates and the activator rail are FC's
 * stubs, with no attachment rule; the other modern plates take the oak or stone plate's rule through their twins).</p>
 *
 * <p><b>Injection strategy:</b> {@link Inject} at RETURN of {@code canSurvive(BlockState, LevelReader, BlockPos)}
 * (SRG {@code m_7898_}), cancellable, on TorchBlock, WallTorchBlock, LadderBlock, SnowLayerBlock,
 * BasePressurePlateBlock and BaseRailBlock (each declares it; RedstoneTorchBlock inherits TorchBlock's, the plates and
 * rails their base class's; SnowLayerBlock's has three returns). When 1.20.1 answers true and FC's rule refuses, the
 * answer is false. Keyed on the state's block, never on {@code this}: RedstoneWallTorchBlock calls WALL_TORCH's method
 * with its own state. The FC world is the server's WorldBridge, or on the client's render thread the read-only
 * ClientFcWorld, so the client predicts the server's refusal (1.20.1's server does not resend the held slot after a
 * refused BlockItem.place, ServerGamePacketListenerImpl.java:1037-1077). Any other reader keeps 1.20.1's answer: world
 * generation in a WorldGenRegion (templates, features, SnowAndFreezeFeature's snow). LevelChunk.postProcessGeneration
 * (LevelChunk.java:491-507) shape-checks the torches, wall torches and ladders of code-built structure pieces
 * (StructurePiece.java:49, :182-183) in the ServerLevel, where the veto runs; every vanilla one stands on a support 4.B
 * accepts (stone bricks, cobblestone, planks, a double smooth stone slab, fence tops, bedrock).</p>
 *
 * <p><b>Alternatives considered and rejected:</b> replacing 1.20.1's answer with FC's: it would stand torches on leaves
 * and on thin plates 1.20.1 draws as no support. A mixin on BlockStateBase.canSurvive or isFaceSturdy: every block,
 * every call. Separate mixins on StandingAndWallBlockItem, the two getStateForPlacement methods and updateShape: three
 * placement points, and the survival half missed. A mixin on Biome.shouldSnow: the player's snow layer and its pop-off
 * missed. A server-only veto: the client would shrink its stack for a torch the server refused.</p>
 */
// FCMOD-INTEGRATION(1.20.1): attach_support_rules: 1.20.1's torch, ladder, snow layer, pressure plate and rail placement, weather snow and pop-off ask canSurvive, so it refuses every support FC's own attachment rule refuses
@Mixin({TorchBlock.class, WallTorchBlock.class, LadderBlock.class, SnowLayerBlock.class, BasePressurePlateBlock.class,
        BaseRailBlock.class})
public abstract class AttachableSupportMixin {

    @Inject(method = "canSurvive(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/LevelReader;Lnet/minecraft/core/BlockPos;)Z",
            at = @At("RETURN"), cancellable = true)
    private void btw$fcSupport(BlockState state, LevelReader level, BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
        if (cir.getReturnValueZ() && FcAttachSupport.refuses(state, level, pos)) {
            cir.setReturnValue(false);
        }
    }
}
