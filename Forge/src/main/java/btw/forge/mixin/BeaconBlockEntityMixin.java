package btw.forge.mixin;

import btw.forge.FcBackedBlockEntities;
import btw.forge.FcBackedBlockEntity;
import btw.forge.FcBeacons;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BeaconBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import javax.annotation.Nullable;

/**
 * The 1.20.1 beacon block entity carries FC's beacon (FcBackedBlockEntities, INTEGRATIONS #053): FC ticks, saves and
 * decides the power; 1.20.1 keeps the block entity, the beam, its hum and the advancements; no 1.20.1 menu or powers.
 */
// FCMOD-INTEGRATION(1.20.1): fc_beacon: the 1.20.1 beacon block entity carries FC's beacon (FCTileEntityBeacon): FC ticks, saves and decides the power; 1.20.1 keeps the block entity, the beam, its hum and the advancements; no 1.20.1 menu, powers or activate/deactivate sounds
@Mixin(BeaconBlockEntity.class)
public abstract class BeaconBlockEntityMixin extends BlockEntity implements FcBackedBlockEntity {
    private BeaconBlockEntityMixin(BlockEntityType<?> t, BlockPos p, BlockState s) { super(t, p, s); }

    @Unique private btw.modern.TileEntity btw$fcTile;
    @Unique private CompoundTag btw$pendingFcData;

    @Override
    public btw.modern.TileEntity btw$fcTile() {
        btw.modern.TileEntity te = btw$fcTile;
        if (te == null) {
            te = FcBackedBlockEntities.create(this, btw$pendingFcData);
            if (te != null) {
                btw$fcTile = te;
                btw$pendingFcData = null;
            }
        }
        return te;
    }

    @Override
    public void btw$setFcTile(@Nullable btw.modern.TileEntity te) {
        btw$fcTile = te;
        if (te != null) FcBackedBlockEntities.bind(te, this);
    }

    @Inject(method = "load(Lnet/minecraft/nbt/CompoundTag;)V", at = @At("TAIL"))
    private void btw$loadFc(CompoundTag tag, CallbackInfo ci) {
        btw$pendingFcData = FcBackedBlockEntities.load(btw$fcTile, this, tag, btw$pendingFcData);
    }

    @Inject(method = "saveAdditional(Lnet/minecraft/nbt/CompoundTag;)V", at = @At("TAIL"))
    private void btw$saveFc(CompoundTag tag, CallbackInfo ci) {
        FcBackedBlockEntities.save(btw$fcTile, btw$pendingFcData, tag);
    }

    @Inject(method = "setLevel(Lnet/minecraft/world/level/Level;)V", at = @At("TAIL"))
    private void btw$rebindFc(Level level, CallbackInfo ci) {
        if (btw$fcTile != null) FcBackedBlockEntities.bind(btw$fcTile, this);
    }

    /** FC's updateEntity, both sides (1.5.2 ticked every tile entity on both); the 1.20.1 body then runs its beam scan. */
    @Inject(method = "tick(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/block/entity/BeaconBlockEntity;)V",
            at = @At("HEAD"))
    private static void btw$tickFc(Level level, BlockPos pos, BlockState state, BeaconBlockEntity be, CallbackInfo ci) {
        FcBeacons.tick(level, pos, be);
    }

    /** The pyramid level 1.20.1 stores and renders (getBeamSections, the CONSTRUCT_BEACON advancement) is FC's. */
    @Inject(method = "updateBase(Lnet/minecraft/world/level/Level;III)I", at = @At("HEAD"), cancellable = true)
    private static void btw$fcLevels(Level level, int x, int y, int z, CallbackInfoReturnable<Integer> cir) {
        int mirrored = FcBeacons.mirroredLevels(level, x, y, z);
        if (mirrored >= 0) cir.setReturnValue(mirrored);
    }

    /** FC applies its own effects (FCTileEntityBeacon.ApplyEffectToPlayersWithinRange); 1.20.1's never run for an FC beacon. */
    @Inject(method = "applyEffects(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;ILnet/minecraft/world/effect/MobEffect;Lnet/minecraft/world/effect/MobEffect;)V",
            at = @At("HEAD"), cancellable = true)
    private static void btw$noVanillaEffects(Level level, BlockPos pos, int levels, @Nullable MobEffect primary,
                                             @Nullable MobEffect secondary, CallbackInfo ci) {
        if (FcBeacons.isFcBeacon(level, pos)) ci.cancel();
    }

    /** tick's three playSound calls (ambient, activate, deactivate): FC sounds the power edges itself; the hum stays. */
    @Redirect(method = "tick(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/block/entity/BeaconBlockEntity;)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/entity/BeaconBlockEntity;playSound(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/sounds/SoundEvent;)V"))
    private static void btw$tickSound(Level level, BlockPos pos, SoundEvent sound,
                                      Level tLevel, BlockPos tPos, BlockState tState, BeaconBlockEntity be) {
        if (!FcBeacons.silences(be, sound)) BeaconBlockEntity.playSound(level, pos, sound);
    }

    /** setRemoved's deactivate sound: FCBlockBeacon.breakBlock plays the wither death for a powered FC beacon. */
    @Redirect(method = "setRemoved()V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/entity/BeaconBlockEntity;playSound(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/sounds/SoundEvent;)V"))
    private void btw$removedSound(Level level, BlockPos pos, SoundEvent sound) {
        if (!FcBeacons.silences(this, sound)) BeaconBlockEntity.playSound(level, pos, sound);
    }

    /** No 1.20.1 menu for an FC beacon, by any path (BeaconBlock.use, getMenuProvider, other mods): ServerPlayer.openMenu
     *  answers an empty OptionalInt for a null menu (1.20.1 ServerPlayer.java:954-969). */
    @Inject(method = "createMenu(ILnet/minecraft/world/entity/player/Inventory;Lnet/minecraft/world/entity/player/Player;)Lnet/minecraft/world/inventory/AbstractContainerMenu;",
            at = @At("HEAD"), cancellable = true)
    private void btw$noMenu(int id, Inventory inv, Player player, CallbackInfoReturnable<AbstractContainerMenu> cir) {
        if (FcBackedBlockEntities.isBacked(this)) cir.setReturnValue(null);
    }
}
