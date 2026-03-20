package btw.forge;

import btw.modern.*;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Concrete implementation of btw.modern.WorldServer that delegates to a real
 * MC 1.20.1 {@link ServerLevel}. This bridges the legacy FC world access
 * (int IDs, block coords) to the modern block state system.
 *
 * For now, provides minimal stubs sufficient for BTW initialization.
 * Full world interaction will be implemented incrementally.
 */
public class ForgeWorldServerWrapper extends WorldServer {

    private static final Logger LOGGER = LogManager.getLogger("BTW-WorldWrapper");

    private final ServerLevel serverLevel;

    public ForgeWorldServerWrapper(ServerLevel serverLevel) {
        this.serverLevel = serverLevel;
        // Propagate the dimension/provider id
        this.provider = new ForgeWorldProviderWrapper(serverLevel);
    }

    /**
     * Returns the underlying modern ServerLevel.
     */
    public ServerLevel getServerLevel() {
        return serverLevel;
    }

    // ================================================================
    // Block access -- delegate to ServerLevel via IDMappingService
    // ================================================================

    @Override
    public int getBlockId(int x, int y, int z) {
        BlockState state = serverLevel.getBlockState(new BlockPos(x, y, z));
        return ProxyRegistry.getBlockId(state.getBlock());
    }

    @Override
    public int getBlockMetadata(int x, int y, int z) {
        BlockState state = serverLevel.getBlockState(new BlockPos(x, y, z));
        if (state.hasProperty(ProxyBlock.META)) {
            return state.getValue(ProxyBlock.META);
        }
        // For vanilla blocks, metadata is encoded differently in 1.20.1
        return 0;
    }

    @Override
    public boolean isAirBlock(int x, int y, int z) {
        return serverLevel.isEmptyBlock(new BlockPos(x, y, z));
    }

    @Override
    public boolean setBlock(int x, int y, int z, int blockID, int metadata, int flags) {
        ProxyBlock proxy = ProxyRegistry.getProxy(blockID);
        if (proxy != null) {
            BlockState state = proxy.defaultBlockState()
                    .setValue(ProxyBlock.META, Math.min(Math.max(metadata, 0), 15));
            return serverLevel.setBlock(new BlockPos(x, y, z), state, flags);
        }
        return false;
    }

    @Override
    public boolean setBlockToAir(int x, int y, int z) {
        return serverLevel.setBlock(new BlockPos(x, y, z),
            Blocks.AIR.defaultBlockState(), 3);
    }

    @Override
    public boolean setBlockMetadata(int x, int y, int z, int metadata, int flags) {
        BlockPos pos = new BlockPos(x, y, z);
        BlockState current = serverLevel.getBlockState(pos);
        if (current.hasProperty(ProxyBlock.META)) {
            BlockState newState = current.setValue(ProxyBlock.META, Math.min(Math.max(metadata, 0), 15));
            return serverLevel.setBlock(pos, newState, flags);
        }
        return false;
    }

    @Override
    public boolean isRaining() {
        return serverLevel.isRaining();
    }

    @Override
    public boolean canBlockSeeTheSky(int x, int y, int z) {
        return serverLevel.canSeeSky(new BlockPos(x, y, z));
    }

    // ================================================================
    // Minimal WorldProvider wrapper
    // ================================================================

    private static class ForgeWorldProviderWrapper extends WorldProvider {
        private final ServerLevel level;

        ForgeWorldProviderWrapper(ServerLevel level) {
            this.level = level;
        }

        // WorldProvider stubs are sufficient for initialization
    }
}
