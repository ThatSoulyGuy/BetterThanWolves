package btw.forge;

import btw.forge.compat.BTWCompat;
import btw.forge.compat.FcRefs;
import btw.forge.compat.thermal.BtwThermalTable;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.registries.ForgeRegistries;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

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
        // FCMOD-INTEGRATION(1.20.1): mod-compat: the compat data and the thermal table name BTW content by FC field; prove at launch that they still resolve
        auditCompatRefs();
        auditThermalTable();
        // FCMOD-INTEGRATION(1.20.1): modern_woods: log2 FCBlockLog at 165, both mappings, bark and work-stump textures
        ModernWoods.audit();
        // FCMOD-INTEGRATION(1.20.1): fc_particles: every particle name FC spawns resolves through the shared table
        FcParticles.audit();
        // FCMOD-INTEGRATION(1.20.1): fc_dig_face: the dig-face record is on ServerPlayerGameMode (the mixin's dig injects applied)
        DigFace.audit();
        // FCMOD-INTEGRATION(1.20.1): strata: the strata ids, the deepslate forms, the canonical stone forms, the height rule, the counts-as items and the blast hook
        StrataRule.audit();
    }

    private static boolean tickPolicyAudited;

    /**
     * Server-start checks, once per JVM. The tick audit reads the per-state caches, which are first filled at
     * the registry freeze (after FMLCommonSetupEvent, so every cached flag still reads false there) and rebuilt
     * by Blocks.rebuildCache when the server loads its resources, before ServerStartingEvent.
     */
    public static void runServerStart() {
        if (tickPolicyAudited) return;
        tickPolicyAudited = true;
        auditTickPolicy();
        // FCMOD-INTEGRATION(1.20.1): hook_ownership: owner and rule per managed block and hook, RULED(3)/(4)/(5) and ADAPTER rows, unclassified FC pairs, lost expected FC rows
        auditHookPolicy();
        // FCMOD-INTEGRATION(1.20.1): fc_beacon: the FC-backed block entity rows (mixin applied, FC tile entity, hook owners), FC's beacon table and potions
        auditFcBackedBlockEntities();
        auditTwins();
        // FCMOD-INTEGRATION(1.20.1): vanilla_pair_drops: the beds and doors 1.20.1 harvests, and the FC drop ids routed to 1.20.1 loot
        VanillaPairDrops.audit();
        // FCMOD-INTEGRATION(1.20.1): collapsed_identity: the blocks whose FC self-drop takes their own 1.20.1 identity; the flower subtypes 38:1-10 and the subtype flag of every FC form checked
        CollapsedIdentity.audit();
        // FCMOD-INTEGRATION(1.20.1): creative_targets: the target rule's reflective ghast slot resolves
        FcTargetRule.audit();
        // FCMOD-INTEGRATION(1.20.1): head_crab: FC's armour-bypassing mob hit has its damage type, tagged bypasses_armor
        DamageSourceMapping.audit();
        // FCMOD-INTEGRATION(1.20.1): fc_blast_rules: which FC-identity blocks follow FC's blast rules, the expected owners, the pending ids, the positional resistance ids
        BlastRules.audit();
        // FCMOD-INTEGRATION(1.20.1): fc_entity_views: every view override reaches the running 1.5.2 classes
        VanillaEntityViews.audit();
        // FCMOD-INTEGRATION(1.20.1): fc_redstone: BTW's detector rails are tagged 1.20.1 rails with FC's cart rule
        FcDetectorRailBlock.audit();
        // FCMOD-INTEGRATION(1.20.1): support_relatives: the support relatives and rule twins by FC target, and every minecraft fence, wall or stained glass pane without FC identity the relatives miss
        FcAttachSupport.audit();
        auditLightPolicy();
    }

    /**
     * Mapped ids whose 4.B light is below 1.20.1's on purpose ({@link LightPolicy}): the brown mushroom 39 (4.B
     * drops 1.5.2's {@code setLightValue(0.125F)}: vanilla/server Block.java:129 is FCMOD-commented, the active :205
     * sets none; 1.20.1 Blocks.java:208-210 gives 1). Any other FC-ticked emitter that goes dark is a
     * Block.lightValue gap in the port's 1.5.2 constructors (BlockEndPortal sets its 15 there).
     */
    private static final Set<Integer> EXPECTED_DARKENED_IDS = Set.of(39);

    /**
     * Light-emission self-audit ({@link LightPolicy}): logs each FC-mapped vanilla block whose emission the rule
     * changed from 1.20.1's, and WARNs on (a) a state whose cached emission differs from the rule's answer
     * (BlockStateBaseTickMixin not applied, or the answer changed after the bake) and (b) a mapped emitter made dark
     * that 4.B does not make dark ({@link #EXPECTED_DARKENED_IDS}).
     */
    private static void auditLightPolicy() {
        if (!BTWLifecycle.isFcReady()) return;   // the tick audit already warned
        int changed = 0, warnings = 0;
        for (Block b : ForgeRegistries.BLOCKS) {
            if (b instanceof ProxyBlock || ProxyRegistry.getFcBlock(b) == null) continue;
            Object key = ForgeRegistries.BLOCKS.getKey(b);
            try {
                int id = ProxyRegistry.getBlockId(b);
                List<BlockState> states = b.getStateDefinition().getPossibleStates();
                Set<String> transitions = new java.util.LinkedHashSet<>();
                int changedStates = 0;
                boolean darkened = false;
                boolean mismatch = false;
                for (BlockState s : states) {
                    int vanilla = ((FcBlockStateAccess) (Object) s).btw$vanillaLightEmission();
                    if (vanilla < 0) continue;   // never baked
                    int now = s.getLightEmission();
                    int fc = LightPolicy.fcEmission(s);
                    if (now != (fc >= 0 ? fc : vanilla)) mismatch = true;
                    if (now != vanilla) {
                        changedStates++;
                        transitions.add(vanilla + "->" + now);
                        if (now == 0) darkened = true;
                    }
                }
                if (mismatch) {
                    LOGGER.warn("[SELF-AUDIT/light] {} (id {}): a cached light emission differs from the LightPolicy "
                            + "answer (BlockStateBaseTickMixin not applied?)", key, id);
                    warnings++;
                }
                if (changedStates == 0) continue;
                changed++;
                LOGGER.info("[SELF-AUDIT/light] {} id={} emission {} ({}/{} states)", key, id,
                        String.join(", ", transitions), changedStates, states.size());
                if (darkened && !EXPECTED_DARKENED_IDS.contains(id)) {
                    LOGGER.warn("[SELF-AUDIT/light] {} (id {}) emits light in 1.20.1 but Block.lightValue[{}] is 0: "
                            + "check the 1.5.2 constructor light of its port base class", key, id, id);
                    warnings++;
                }
            } catch (Throwable t) {
                LOGGER.warn("[SELF-AUDIT/light] could not audit {}", key, t);
                warnings++;
            }
        }
        LOGGER.info("[SELF-AUDIT/light] {} mapped blocks emit 4.B light that differs from 1.20.1, {} warnings "
                + "(0 expected)", changed, warnings);
    }

    /**
     * Mapped ids whose 1.20.1 class overrides tick and is expected to keep its scheduled ticks (TickPolicy
     * class javadoc): leaves, dispenser/dropper, detector rail, stairs, chests, farmland/cactus/sugar cane
     * survival, plates, torches, buttons, soul sand, repeater, cauldron, lamp, tripwire, comparator, and BTW's
     * own detector rails 235 and 236 (FcDetectorRailBlock, 1.20.1 detector rails: fc_redstone).
     */
    private static final Set<Integer> EXPECTED_VANILLA_SCHED_IDS = Set.of(18, 23, 28, 53, 54, 60, 67, 70, 72, 76,
            77, 81, 83, 88, 93, 108, 109, 114, 118, 123, 128, 130, 131, 132, 134, 135, 136, 143, 146, 147, 148, 149,
            156, 158, 235, 236);

    /**
     * Legacy ids whose FC object random-ticks in 4.B (needsRandomTick after the 1.5.2 constructors), from the
     * 1.5.2 / FC constructor chains: BlockGrass.java:23, BlockFlower.java:30, BlockFluid.java:29,
     * BlockStationary.java:25-28 (lava), BlockLeaves.java:28, BlockDetectorRail.java:24, FCBlockTorchBase.java:36,
     * BlockFire.java:44, BlockCrops.java:23, FCBlockFarmlandBase.java:37, BlockBasePressurePlate.java:27,
     * FCBlockRedstoneOre.java:33 (glowing 74), BlockRedstoneTorch.java:73 (both 75 and 76), BlockButton.java:27,
     * FCBlockSnowCover.java:29, BlockIce.java:24, BlockSnowBlock.java:23, BlockCactus.java:23, BlockReed.java:25,
     * BlockPumpkin.java:24, BlockPortal.java:23, FCBlockJackOLantern.java:29, BlockCake.java:23,
     * FCBlockSilverfish.java:36, FCBlockStoneBrick.java:38, FCBlockGourd.java:36, BlockStem.java:32,
     * BlockVine.java:23, BlockMycelium.java:23, BlockNetherStalk.java:25, FCBlockEndPortal.java:31,
     * BlockCocoa.java:25, BlockTripWireSource.java:24, BlockTripWire.java:26.
     */
    private static final Set<Integer> EXPECTED_4B_RANDOM = Set.of(2, 6, 8, 10, 11, 18, 28, 31, 32, 37, 38, 39, 40,
            50, 51, 59, 60, 70, 72, 74, 75, 76, 77, 78, 79, 80, 81, 83, 86, 90, 91, 92, 97, 98, 103, 104, 105, 106, 110,
            111, 115, 119, 127, 131, 132, 141, 142, 143);

    /**
     * Accepted flag gaps, reported at INFO instead of WARN: blocksList[75/76] are the ConcreteTorchBlock
     * placeholders (Modern-Common Block.java:2093-2094), not FC data, so they cannot carry 4.B's redstone-torch
     * random re-validation. Redstone torches stay 1.20.1-owned (their 160-tick burnout recheck stands in).
     */
    private static final Set<Integer> KNOWN_FLAG_GAPS = Set.of(75, 76);

    /** Vanilla legacy ids run 1..174; ProxyBlocks (FC-only blocks) start at 175 (BTWRegistration). */
    private static final int FIRST_PROXY_ID = 175;

    /**
     * Tick-ownership self-audit ({@link TickPolicy}): logs each FC-mapped vanilla block's random and scheduled
     * owner and WARNs on
     * (a) a class that overrides tick but is neither FC_SCHED nor an expected 1.20.1-owned scheduled tick;
     * (b) a class that overrides randomTick while its random owner is VANILLA without an exception entry;
     * (c) a 4.B random-tick flag the port gets wrong (constructor parity of the Modern-Common 1.5.2 bases);
     * (d) a state whose cached isRandomlyTicking differs from the decided gate (BlockStateBaseTickMixin not
     *     applied, or the gate changed after the bake).
     */
    private static void auditTickPolicy() {
        if (!BTWLifecycle.isFcReady()) {
            LOGGER.warn("[SELF-AUDIT/tick] FC is not initialised; every block keeps its 1.20.1 ticks.");
            return;
        }
        int blocks = 0, fcRandomStates = 0, fcSchedBlocks = 0, warnings = 0;
        for (Block b : ForgeRegistries.BLOCKS) {
            if (b instanceof ProxyBlock || ProxyRegistry.getFcBlock(b) == null) continue;
            try {
                List<BlockState> states = b.getStateDefinition().getPossibleStates();
                TickPolicy.Decision first = null;
                int gated = 0;
                boolean gateMismatch = false;
                for (BlockState s : states) {
                    TickPolicy.Decision d = TickPolicy.decide(s);
                    if (d == null) continue;
                    if (first == null) first = d;
                    if (s.isRandomlyTicking()) gated++;
                    if (d.random() == TickPolicy.FC && s.isRandomlyTicking()) fcRandomStates++;
                    if (d.gate() != null && s.isRandomlyTicking() != d.gate()) gateMismatch = true;
                }
                Object key = ForgeRegistries.BLOCKS.getKey(b);
                if (first == null) {
                    LOGGER.warn("[SELF-AUDIT/tick] {} is FC-mapped but TickPolicy left it unmanaged", key);
                    warnings++;
                    continue;
                }
                blocks++;
                int id = ProxyRegistry.getBlockId(b);
                if (first.sched() == TickPolicy.FC) fcSchedBlocks++;
                LOGGER.info("[SELF-AUDIT/tick] {} id={} random={} gated={}/{} sched={}", key, first.fcId(),
                        ownerName(first.random()), gated, states.size(), ownerName(first.sched()));

                Class<?> cls = b.getClass();
                if (TickPolicy.overridesTick(cls) && !TickPolicy.isFcScheduled(b)
                        && !EXPECTED_VANILLA_SCHED_IDS.contains(id)) {
                    LOGGER.warn("[SELF-AUDIT/tick] {} (id {}, {}) overrides tick but is neither FC_SCHED nor an "
                            + "expected 1.20.1-owned scheduled tick: classify it in TickPolicy", key, id, cls.getName());
                    warnings++;
                }
                if (first.random() == TickPolicy.VANILLA && TickPolicy.overridesRandomTick(cls)
                        && !"redstone-ore".equals(first.rule()) && !"ice".equals(first.rule())) {
                    LOGGER.warn("[SELF-AUDIT/tick] {} (id {}, {}) overrides randomTick and keeps the 1.20.1 random "
                            + "tick without an exception entry (rule {})", key, id, cls.getName(), first.rule());
                    warnings++;
                }
                if (gateMismatch) {
                    LOGGER.warn("[SELF-AUDIT/tick] {} (id {}): a cached isRandomlyTicking differs from the TickPolicy "
                            + "gate (BlockStateBaseTickMixin not applied?)", key, id);
                    warnings++;
                }
            } catch (Throwable t) {
                LOGGER.warn("[SELF-AUDIT/tick] could not audit {}", ForgeRegistries.BLOCKS.getKey(b), t);
                warnings++;
            }
        }

        // (c) 4.B random-tick flags of the vanilla-id FC objects (mapped or not: 9, 11, 74 ... have no own state).
        btw.modern.Block[] list = btw.modern.Block.blocksList;
        for (int id = 1; id < FIRST_PROXY_ID && id < list.length; id++) {
            btw.modern.Block fc = list[id];
            if (fc == null || btw.modern.ModernTwins.isTwin(id)) continue;   // twins: auditTwins
            boolean expected = EXPECTED_4B_RANDOM.contains(id);
            if (expected == fc.getTickRandomly()) continue;
            if (KNOWN_FLAG_GAPS.contains(id)) {
                LOGGER.info("[SELF-AUDIT/tick] id {} ({}) getTickRandomly={}: known gap, 1.20.1 keeps its ticks",
                        id, fc.getClass().getSimpleName(), fc.getTickRandomly());
                continue;
            }
            LOGGER.warn("[SELF-AUDIT/tick] id {} ({}) getTickRandomly={} but 4.B {} it",
                    id, fc.getClass().getSimpleName(), fc.getTickRandomly(),
                    expected ? "random-ticks" : "does not random-tick");
            warnings++;
        }

        LOGGER.info("[SELF-AUDIT/tick] {} blocks, {} FC-random states, {} FC-scheduled blocks, {} warnings (0 expected)",
                blocks, fcRandomStates, fcSchedBlocks, warnings);
    }

    private static String ownerName(byte owner) {
        return switch (owner) {
            case TickPolicy.FC -> "FC";
            case TickPolicy.NONE -> "NONE";
            case TickPolicy.PACED -> "PACED";
            default -> "VANILLA";
        };
    }

    /**
     * Block-entity-guarded hooks FC declares in FC code, accepted until FC tile entities are bridged onto 1.20.1 block
     * entities (hook-ownership D7(a), Phase 4), reported at INFO: legacy id -&gt; HookPolicy hook mask. Note block USE
     * (Common FCBlockNote.java:36), furnace USE (Client FCBlockFurnace.java:84), ender chest USE and REMOVE (Client
     * FCBlockEnderChest.java:39, :104). The beacon's USE and REMOVE (138) left the map with fc_beacon: FC's beacon tile
     * entity rides on 1.20.1's beacon block entity ({@link FcBackedBlockEntities}).
     */
    private static final Map<Integer, Integer> KNOWN_TE_GAPS = Map.of(
            25, 1 << HookPolicy.USE,
            61, 1 << HookPolicy.USE,
            130, (1 << HookPolicy.USE) | (1 << HookPolicy.REMOVE));

    /**
     * The FC-backed block entities ({@link FcBackedBlockEntities}): WARN when a row's block entity class does not carry
     * the attachment (its mixin was not applied), when FC's block for the row makes no FC tile entity (null or a
     * btw.modern stand-in), when a block of the row is not FC USE / non-VANILLA REMOVE, when FC's beacon table is empty
     * (FCTileEntityBeacon.InitializeEffectsByBlockID did not run: Server FCBetterThanWolves.java:804, Client :803), or
     * when FC's potions 29-31 are not the BTW MobEffects. Expected: {@code 0 warnings}.
     */
    private static void auditFcBackedBlockEntities() {
        if (!BTWLifecycle.isFcReady()) return;
        int warnings = 0;
        for (FcBackedBlockEntities.Row r : FcBackedBlockEntities.ROWS) {
            if (!FcBackedBlockEntity.class.isAssignableFrom(r.blockEntity())) {
                LOGGER.warn("[SELF-AUDIT/fcbe] {} does not carry FC's tile entity (mixin not applied)", r.blockEntity().getName());
                warnings++;
            }
            btw.modern.Block fc = btw.modern.Block.blocksList[r.legacyId()];
            btw.modern.TileEntity te = null;
            try { te = fc == null ? null : fc.createNewTileEntity(null); } catch (Throwable ignored) { }
            if (te == null || te.getClass().getName().startsWith("btw.modern.")) {
                LOGGER.warn("[SELF-AUDIT/fcbe] FC block {} makes no FC tile entity ({})", r.legacyId(), te);
                warnings++;
            }
            for (net.minecraft.world.level.block.Block b : net.minecraftforge.registries.ForgeRegistries.BLOCKS) {
                if (!r.block().isInstance(b) || ProxyRegistry.getBlockId(b) != r.legacyId()) continue;
                HookPolicy.Decision d = HookPolicy.decide(b.defaultBlockState());
                if (d == null || d.owner(HookPolicy.USE) != HookPolicy.FC || d.owner(HookPolicy.REMOVE) == HookPolicy.VANILLA) {
                    LOGGER.warn("[SELF-AUDIT/fcbe] {}: USE {} REMOVE {} (expected FC, not VANILLA)",
                            net.minecraftforge.registries.ForgeRegistries.BLOCKS.getKey(b),
                            d == null ? "-" : d.owner(HookPolicy.USE), d == null ? "-" : d.owner(HookPolicy.REMOVE));
                    warnings++;
                }
            }
        }
        boolean table = false;
        try {
            java.lang.reflect.Field f = Class.forName("net.minecraft.src.btw.tileentity.FCTileEntityBeacon")
                    .getDeclaredField("m_beaconEffectsByBlockID");
            f.setAccessible(true);
            Object[] byId = (Object[]) f.get(null);
            table = byId != null && byId[41] instanceof java.util.List<?> l && !l.isEmpty();   // gold block (41): haste
        } catch (Throwable ignored) { }
        if (!table) {
            LOGGER.warn("[SELF-AUDIT/fcbe] FC's beacon table is empty (InitializeEffectsByBlockID did not run): no beacon powers");
            warnings++;
        }
        int potions = 0;
        String[] keys = {"true_sight", "looting", "fortune"};
        for (int id = 29; id <= 31; id++) {
            net.minecraft.world.effect.MobEffect e = PotionMapping.getEffect(id);
            net.minecraft.resources.ResourceLocation key = e == null ? null
                    : net.minecraftforge.registries.ForgeRegistries.MOB_EFFECTS.getKey(e);
            if (key != null && key.equals(new net.minecraft.resources.ResourceLocation(BTWForgeMod.MOD_ID, keys[id - 29]))) {
                potions++;
            } else {
                LOGGER.warn("[SELF-AUDIT/fcbe] FC potion {} maps to {} (expected {}:{})", id, key, BTWForgeMod.MOD_ID, keys[id - 29]);
                warnings++;
            }
        }
        LOGGER.info("[SELF-AUDIT/fcbe] {} rows, beacon table {}, potions {}/3, {} warnings (0 expected)",
                FcBackedBlockEntities.ROWS.size(), table ? "ok" : "EMPTY", potions, warnings);
    }

    /**
     * Hook-ownership self-audit ({@link HookPolicy}, hook_ownership): one line per managed block with the owner and
     * rule of every hook other than the plain general NOTIFY (RESIST only where it is FC: the positional resistance of
     * stone and the ores, fc_blast_rules), and a summary. WARNs on
     * (a) a pair the general rule hands to FC that is not an expected FC row (the anvils' USE, the carved pumpkin's
     *     and the jack o'lantern's PLACE): an override plus an FC-declared hook nobody classified. The reflection rule
     *     cannot see a super call, so classify it (EQUIVALENT, PENDING, RULED) or confirm it as an FC row;
     * (b) a block-entity-guarded hook FC declares in FC code, other than {@link #KNOWN_TE_GAPS} (INFO);
     * (c) a state whose cached owners differ from HookPolicy.decide (BlockStateHookMixin not applied, or the answer
     *     changed after the bake), or states of one block decided differently;
     * (d) a RULED or ADAPTER row on a block that no longer maps to the legacy id its ruling names, or a ruling's id
     *     with no such row (hook delta B9; {@link HookPolicy#RULED_IDS});
     * (e) an expected FC row ({@link HookPolicy#EXPECTED_FC}) that no block of its legacy id carries: an unresolved
     *     1.20.1 method name or a drifted FC signature turned it back to NOTIFY (no golem block, no FC anvil).
     * Expected: {@code 0 warnings}, with RULED(3) on the 16 beds' USE and RULED(4) on OAK_DOOR, IRON_DOOR,
     * OAK_TRAPDOOR and OAK_FENCE_GATE NEIGHBOR, and {@code 4 known block-entity gaps} on 3 blocks (counted per block
     * and hook: note and furnace USE, ender chest USE and REMOVE).
     */
    private static void auditHookPolicy() {
        if (!BTWLifecycle.isFcReady()) {
            LOGGER.warn("[SELF-AUDIT/hook] FC is not initialised; every hook keeps today's lookup (UNDECIDED)");
            return;
        }
        int blocks = 0, fcPairs = 0, vanillaPairs = 0, teGaps = 0, warnings = 0;
        Map<String, Set<Integer>> seenRuled = new TreeMap<>();
        // legacy id -> mask of the hooks some block of that id has FC-owned, for (e)
        Map<Integer, Integer> seenFc = new TreeMap<>();
        for (Block b : ForgeRegistries.BLOCKS) {
            if (b instanceof ProxyBlock || ProxyRegistry.getFcBlock(b) == null) continue;
            Object key = ForgeRegistries.BLOCKS.getKey(b);
            try {
                int id = ProxyRegistry.getBlockId(b);
                HookPolicy.Decision first = null;
                boolean cacheMismatch = false, varies = false;
                for (BlockState s : b.getStateDefinition().getPossibleStates()) {
                    HookPolicy.Decision d = HookPolicy.decide(s);
                    if (HookPolicy.cachedOwners(s) != (d == null ? 0 : d.owners())) cacheMismatch = true;
                    if (d == null) continue;
                    if (first == null) first = d;
                    else if (d.owners() != first.owners()) varies = true;
                }
                if (first == null) {
                    LOGGER.warn("[SELF-AUDIT/hook] {} is FC-mapped but HookPolicy left it unmanaged", key);
                    warnings++;
                    continue;
                }
                blocks++;
                btw.modern.Block fc = ProxyRegistry.getFcBlock(b);
                StringBuilder rows = new StringBuilder();
                for (int h = 0; h < HookPolicy.HOOKS; h++) {
                    int owner = first.owner(h);
                    String rule = first.rule(h);
                    if (owner == HookPolicy.FC) {
                        fcPairs++;
                        seenFc.merge(id, 1 << h, (x, y) -> x | y);
                    }
                    if (owner == HookPolicy.VANILLA) vanillaPairs++;
                    if (rule.startsWith("RULED(") || rule.startsWith("ADAPTER(")) {
                        seenRuled.computeIfAbsent(rule, k -> new TreeSet<>()).add(id);
                    }
                    if (owner == HookPolicy.FC && "general".equals(rule) && !HookPolicy.isExpectedFc(id, h)) {
                        LOGGER.warn("[SELF-AUDIT/hook] (a) {} (id {}, {}): the general rule hands {} to FC ({} overrides "
                                + "it, {} declares it) but no row expects it: classify it in HookPolicy", key, id,
                                b.getClass().getName(), HookPolicy.hookName(h), b.getClass().getSimpleName(),
                                fc.getClass().getSimpleName());
                        warnings++;
                    }
                    if ("te-guard".equals(rule) && HookPolicy.fcDeclares(fc.getClass(), h)) {
                        Integer known = KNOWN_TE_GAPS.get(id);
                        if (known != null && (known & (1 << h)) != 0) {
                            teGaps++;
                            LOGGER.info("[SELF-AUDIT/hook] {} (id {}): {}.{} stays 1.20.1's until its block entity is "
                                    + "bridged (Phase 4)", key, id, fc.getClass().getSimpleName(), HookPolicy.hookName(h));
                        } else {
                            LOGGER.warn("[SELF-AUDIT/hook] (b) {} (id {}): {} declares {} on a 1.20.1 block entity; the "
                                    + "block-entity guard keeps it from running", key, id, fc.getClass().getSimpleName(),
                                    HookPolicy.hookName(h));
                            warnings++;
                        }
                    }
                    if ((h == HookPolicy.RESIST && owner == HookPolicy.NOTIFY)
                            || (owner == HookPolicy.NOTIFY && "general".equals(rule))) continue;
                    rows.append(' ').append(HookPolicy.hookName(h)).append('=')
                            .append(HookPolicy.ownerName(owner)).append('[').append(rule).append(']');
                }
                LOGGER.info("[SELF-AUDIT/hook] {} id={}{} rest=NOTIFY[general]{}", key, id, rows,
                        varies ? " (states differ)" : "");
                if (cacheMismatch) {
                    LOGGER.warn("[SELF-AUDIT/hook] (c) {} (id {}): a cached hook owner differs from HookPolicy.decide "
                            + "(BlockStateHookMixin not applied?)", key, id);
                    warnings++;
                }
                if (varies) {
                    LOGGER.warn("[SELF-AUDIT/hook] (c) {} (id {}): its states have different hook owners; the line above "
                            + "shows the first", key, id);
                    warnings++;
                }
            } catch (Throwable t) {
                LOGGER.warn("[SELF-AUDIT/hook] could not audit {}", key, t);
                warnings++;
            }
        }

        // (d) every RULED / ADAPTER label covers exactly the legacy ids its ruling names (delta B9)
        for (Map.Entry<String, Set<Integer>> e : new TreeMap<>(HookPolicy.RULED_IDS).entrySet()) {
            Set<Integer> seen = seenRuled.getOrDefault(e.getKey(), Set.of());
            for (int id : seen) {
                if (!e.getValue().contains(id)) {
                    LOGGER.warn("[SELF-AUDIT/hook] (d) {} applies to legacy id {}, which its ruling does not name "
                            + "(expected {}): a mapping changed; confirm the ruling covers it", e.getKey(), id,
                            e.getValue());
                    warnings++;
                }
            }
            for (int id : e.getValue()) {
                if (!seen.contains(id)) {
                    LOGGER.warn("[SELF-AUDIT/hook] (d) {}: no block of legacy id {} carries the row any more (a ruled "
                            + "state no longer maps to its id); FC may own that hook again", e.getKey(), id);
                    warnings++;
                }
            }
        }
        for (String label : seenRuled.keySet()) {
            if (!HookPolicy.RULED_IDS.containsKey(label)) {
                LOGGER.warn("[SELF-AUDIT/hook] (d) row {} has no entry in HookPolicy.RULED_IDS", label);
                warnings++;
            }
        }

        // (e) every expected FC row is carried by some block of its legacy id: a 1.20.1 method name HookPolicy could
        // not resolve (no class then counts as overriding it) or a drifted FcSigs signature would hand the hook back
        // to NOTIFY without any other warning
        for (Map.Entry<Integer, Integer> e : new TreeMap<>(HookPolicy.EXPECTED_FC).entrySet()) {
            int missing = e.getValue() & ~seenFc.getOrDefault(e.getKey(), 0);
            for (int h = 0; h < HookPolicy.HOOKS; h++) {
                if ((missing & (1 << h)) != 0) {
                    LOGGER.warn("[SELF-AUDIT/hook] (e) no block of legacy id {} has an FC-owned {}: the expected FC "
                            + "row is lost (an unresolved 1.20.1 method or a drifted FC signature?) and 1.20.1 runs "
                            + "there", e.getKey(), HookPolicy.hookName(h));
                    warnings++;
                }
            }
        }

        LOGGER.info("[SELF-AUDIT/hook] {} blocks, {} FC-owned and {} VANILLA hook pairs, rows {}, {} known block-entity "
                + "gaps, {} warnings (0 expected)", blocks, fcPairs, vanillaPairs, seenRuled, teGaps, warnings);
    }

    /**
     * Post-1.5.2 plant self-audit ({@link TwinRegistry}): logs each twin id, twin family and paced plant, and WARNs on
     * (a) a twin that was not built, or whose blocksList slot is not the expected FC class, or whose id has a
     *     ProxyBlock, a 1.20.1 block or item mapping, or lies in the ProxyBlock range;
     * (b) a twin state FC's world reads do not see as its twin's id;
     * (c) a metadata codec that does not round-trip: {@code stateForMeta(s, deriveMeta(s)) == s} for every twin state,
     *     and for every crop level m, {@code deriveMeta(stateForMeta(s, m))} is the highest visible level not above m
     *     (the torchflower crop's level 7 is the TORCHFLOWER block);
     * (d) a cached owner, twin id, gate or pace that differs from TickPolicy (BlockStateBaseTickMixin not applied);
     * (e) an unclassified growth block: a minecraft block that overrides randomTick or is bonemealable and is neither
     *     FC-mapped, a twin, paced nor kept on 1.20.1 on purpose ({@link #isKeptOn1201}); other mods' blocks are
     *     reported at INFO.
     * Expected: {@code 6 twin families (11 blocks), 41 twin states, 173 paced states, 0 warnings}.
     */
    private static void auditTwins() {
        if (!BTWLifecycle.isFcReady()) return;   // the tick audit already warned
        int warnings = 0, families = 0, familyBlocks = 0, twinStates = 0, pacedStates = 0, modded = 0;

        // (a) the twin objects
        for (TwinRegistry.Kind kind : TwinRegistry.Kind.values()) {
            int id = TwinRegistry.idOf(kind);
            if (id <= 0) {
                LOGGER.warn("[SELF-AUDIT/twin] the {} twin ({}) was not built: its plants keep their 1.20.1 growth",
                        kind.name(), kind.fcClass);
                warnings++;
                continue;
            }
            btw.modern.Block fc = btw.modern.Block.blocksList[id];
            String cls = fc == null ? "null" : fc.getClass().getName();
            if (!kind.fcClass.equals(cls) || !btw.modern.ModernTwins.isTwin(id)) {
                LOGGER.warn("[SELF-AUDIT/twin] twin id {} holds {} (expected the {} twin, {})", id, cls, kind.name(),
                        kind.fcClass);
                warnings++;
            }
            if (id >= FIRST_PROXY_ID || ProxyRegistry.getProxy(id) != null || ProxyRegistry.isLegacyIdMapped(id)
                    || ProxyRegistry.getModernItem(id) != null) {
                LOGGER.warn("[SELF-AUDIT/twin] twin id {} collides with a ProxyBlock, a mapped 1.20.1 block or item, "
                        + "or the ProxyBlock range", id);
                warnings++;
            }
            LOGGER.info("[SELF-AUDIT/twin] id {} = {} ({} twin)", id, cls, kind.name());
        }

        // (b)-(d) the twin families
        for (TwinRegistry.Family family : TwinRegistry.Family.values()) {
            int id = TwinRegistry.idOf(family.kind);
            if (id <= 0) continue;
            List<Block> blocks = TwinRegistry.blocksOf(family);
            families++;
            familyBlocks += blocks.size();
            List<BlockState> states = new java.util.ArrayList<>();
            java.util.TreeSet<Integer> visible = new java.util.TreeSet<>();
            for (Block b : blocks) {
                for (BlockState s : b.getStateDefinition().getPossibleStates()) {
                    if (TwinRegistry.familyOf(s) != family) continue;
                    states.add(s);
                    visible.add(TwinRegistry.deriveMeta(s));
                }
            }
            boolean idMismatch = false, codecMismatch = false, cacheMismatch = false;
            for (BlockState s : states) {
                if (ProxyRegistry.getWorldBlockId(s, net.minecraft.world.level.EmptyBlockGetter.INSTANCE,
                        net.minecraft.core.BlockPos.ZERO) != id) {
                    idMismatch = true;
                }
                if (TwinRegistry.stateForMeta(s, TwinRegistry.deriveMeta(s)) != s) codecMismatch = true;
                if (family.crop) {
                    for (int m = 0; m <= 7; m++) {
                        BlockState t = TwinRegistry.stateForMeta(s, m);
                        if (TwinRegistry.familyOf(t) != family) {
                            if (!(family == TwinRegistry.Family.TORCHFLOWER && m == 7
                                    && t.is(net.minecraft.world.level.block.Blocks.TORCHFLOWER))) {
                                codecMismatch = true;
                            }
                            continue;
                        }
                        Integer expected = visible.floor(m);
                        if (expected == null || TwinRegistry.deriveMeta(t) != expected) codecMismatch = true;
                    }
                }
                TickPolicy.Decision d = TickPolicy.decide(s);
                FcBlockStateAccess cached = (FcBlockStateAccess) (Object) s;
                if (d == null || d.random() != TickPolicy.FC || d.fcId() != id
                        || cached.btw$randomOwner() != TickPolicy.FC || cached.btw$randomFcId() != id
                        || !s.isRandomlyTicking()) {
                    cacheMismatch = true;
                }
            }
            if (idMismatch) {
                LOGGER.warn("[SELF-AUDIT/twin] {}: a twin state does not read as twin id {} to FC", family.label, id);
                warnings++;
            }
            if (codecMismatch) {
                LOGGER.warn("[SELF-AUDIT/twin] {}: the FC metadata codec does not round-trip", family.label);
                warnings++;
            }
            if (cacheMismatch) {
                LOGGER.warn("[SELF-AUDIT/twin] {}: a cached owner, twin id or random-tick gate differs from TickPolicy "
                        + "(BlockStateBaseTickMixin not applied?)", family.label);
                warnings++;
            }
            twinStates += states.size();
            LOGGER.info("[SELF-AUDIT/twin] family {} id={} ({}) blocks={} states={} levels={}", family.label, id,
                    btw.modern.Block.blocksList[id].getClass().getSimpleName(), blocks.stream()
                            .map(b -> String.valueOf(ForgeRegistries.BLOCKS.getKey(b))).toList(),
                    states.size(), visible);
        }

        // (d) the paced plants
        for (Block b : TwinRegistry.pacedBlocks()) {
            int states = 0;
            TwinRegistry.Pace pace = null;
            boolean mismatch = false;
            for (BlockState s : b.getStateDefinition().getPossibleStates()) {
                TwinRegistry.Pace p = TwinRegistry.paceFor(s);
                if (p == null) continue;
                states++;
                pace = p;
                FcBlockStateAccess cached = (FcBlockStateAccess) (Object) s;
                FcPaceAccess cachedPace = (FcPaceAccess) (Object) s;
                if (cached.btw$randomOwner() != TickPolicy.PACED || cachedPace.btw$growthPace() != p.pace()
                        || cachedPace.btw$paceEndBan() != p.endBan() || s.isRandomlyTicking() != b.isRandomlyTicking(s)) {
                    mismatch = true;
                }
            }
            if (pace == null) continue;
            pacedStates += states;
            Object key = ForgeRegistries.BLOCKS.getKey(b);
            if (mismatch) {
                LOGGER.warn("[SELF-AUDIT/twin] {}: a cached owner, pace or random-tick gate differs from TickPolicy "
                        + "(BlockStateBaseTickMixin not applied?)", key);
                warnings++;
            }
            LOGGER.info("[SELF-AUDIT/twin] paced {} ({}) pace={} endBan={} states={}", key, pace.name(), pace.pace(),
                    pace.endBan(), states);
        }

        // (e) growth blocks nobody classified
        for (Block b : ForgeRegistries.BLOCKS) {
            if (b instanceof ProxyBlock || ProxyRegistry.getBlockId(b) != 0 || TwinRegistry.governsBlock(b)) continue;
            boolean grows = b instanceof net.minecraft.world.level.block.BonemealableBlock
                    || TickPolicy.overridesRandomTick(b.getClass());
            if (!grows || isKeptOn1201(b)) continue;
            net.minecraft.resources.ResourceLocation key = ForgeRegistries.BLOCKS.getKey(b);
            if (key != null && "minecraft".equals(key.getNamespace())) {
                LOGGER.warn("[SELF-AUDIT/twin] {} ({}) grows by random tick or bone meal but is neither FC-mapped, a "
                        + "twin, paced nor kept on 1.20.1: classify it in TwinRegistry", key, b.getClass().getName());
                warnings++;
            } else {
                modded++;
            }
        }
        if (modded > 0) {
            LOGGER.info("[SELF-AUDIT/twin] {} blocks of other mods grow by random tick or bone meal at their own pace",
                    modded);
        }

        LOGGER.info("[SELF-AUDIT/twin] {} twin families ({} blocks), {} twin states, {} paced states, {} warnings",
                families, familyBlocks, twinStates, pacedStates, warnings);
    }

    /**
     * 1.20.1 blocks that keep their own random tick or bone meal on purpose (TwinRegistry, kept list): leaves of the
     * new species (decay already at 4.B pacing, S13), nylium (S14), bone-meal-only spreaders (S15: moss, dripleaf,
     * rooted dirt, pink petals, tall flowers, glow lichen, sea pickles, seagrass, the vine and kelp bodies), and what is
     * not a plant (copper weathering, dripstone, amethyst, turtle eggs, frosted ice, redstone ore (both forms are FC 73
     * since StrataRule; TickPolicy keeps their 1.20.1 tick), stairs
     * delegating to their base block).
     */
    private static boolean isKeptOn1201(Block b) {
        return b instanceof net.minecraft.world.level.block.LeavesBlock
                || b instanceof net.minecraft.world.level.block.NyliumBlock
                || b instanceof net.minecraft.world.level.block.MossBlock
                || b instanceof net.minecraft.world.level.block.BigDripleafBlock
                || b instanceof net.minecraft.world.level.block.BigDripleafStemBlock
                || b instanceof net.minecraft.world.level.block.SmallDripleafBlock
                || b instanceof net.minecraft.world.level.block.RootedDirtBlock
                || b instanceof net.minecraft.world.level.block.PinkPetalsBlock
                || b instanceof net.minecraft.world.level.block.TallFlowerBlock
                || b instanceof net.minecraft.world.level.block.GlowLichenBlock
                || b instanceof net.minecraft.world.level.block.SeaPickleBlock
                || b instanceof net.minecraft.world.level.block.SeagrassBlock
                || b instanceof net.minecraft.world.level.block.GrowingPlantBodyBlock
                || b instanceof net.minecraft.world.level.block.WeatheringCopper
                || b instanceof net.minecraft.world.level.block.PointedDripstoneBlock
                || b instanceof net.minecraft.world.level.block.BuddingAmethystBlock
                || b instanceof net.minecraft.world.level.block.TurtleEggBlock
                || b instanceof net.minecraft.world.level.block.FrostedIceBlock
                || b instanceof net.minecraft.world.level.block.RedStoneOreBlock
                || b instanceof net.minecraft.world.level.block.StairBlock;
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

    /**
     * The datagen-written compat manifests ({@code data/betterthanwolves/compat_refs/<name>.json}, written through
     * {@link FcRefs#refsManifest}). Their keys were written with the default BTW ids.
     */
    private static final List<String> COMPAT_MANIFESTS = List.of("sereneseasons", "lso", "harvestcraft");

    /** True when the mod a compat manifest belongs to is loaded (the manifest's data is live, not inert). */
    private static boolean compatManifestOwnerLoaded(String manifest) {
        return switch (manifest) {
            case "sereneseasons" -> BTWCompat.isLoaded(BTWCompat.SERENE_SEASONS);
            case "lso" -> BTWCompat.isLoaded(BTWCompat.LSO);
            case "harvestcraft" -> BTWCompat.anyHarvestCraftLoaded();
            default -> false;
        };
    }

    /**
     * Compat-reference self-audit (INTEGRATIONS #020): every BTW key a compat manifest names must still be the key
     * this server's BTW ID config gives its FC field ({@link FcRefs#liveKey}). A moved id (BTWConfig.txt) strands data
     * written as {@code block_<id>} / {@code item_<id>}: WARN when the manifest's mod is loaded, INFO when it is absent
     * (the data is inert then). An absent manifest is skipped. Isolated: an exception escaping
     * FMLCommonSetupEvent.enqueueWork would abort mod loading.
     */
    private static void auditCompatRefs() {
        try {
            int entries = 0, stale = 0, warned = 0, inert = 0;
            List<String> read = new ArrayList<>();
            for (String manifest : COMPAT_MANIFESTS) {
                List<FcRefs.Expectation> expectations = FcRefs.readManifest(manifest);
                if (expectations.isEmpty()) continue;
                read.add(manifest);
                boolean ownerLoaded = compatManifestOwnerLoaded(manifest);
                for (FcRefs.Expectation e : expectations) {
                    entries++;
                    ResourceLocation live = FcRefs.liveKey(e);
                    if (e.writtenKey().equals(live)) continue;
                    stale++;
                    Object liveText = live != null ? live : "unresolved";
                    if (ownerLoaded) {
                        LOGGER.warn("[SELF-AUDIT/compat-refs] {} data names {} for {}, but this server's BTW ID config "
                                + "gives {}; that data no longer matches (INTEGRATIONS.md, Follow-up from #005)",
                                e.usedBy(), e.writtenKey(), e.fcField(), liveText);
                        warned++;
                    } else {
                        LOGGER.info("[SELF-AUDIT/compat-refs] {} data names {} for {}, but this server's BTW ID config "
                                + "gives {}; that data no longer matches (INTEGRATIONS.md, Follow-up from #005) "
                                + "(mod absent, data inert)", e.usedBy(), e.writtenKey(), e.fcField(), liveText);
                        inert++;
                    }
                }
            }
            LOGGER.info("[SELF-AUDIT/compat-refs] {} manifest(s) read {}, {} entries, {} stale ({} warned, {} inert)",
                    read.size(), read, entries, stale, warned, inert);
        } catch (Throwable t) {
            LOGGER.error("[SELF-AUDIT/compat-refs] audit failed", t);
        }
    }

    /**
     * Thermal-table self-audit ({@link BtwThermalTable}, INTEGRATIONS #020): every field resolves; every heat source is
     * a ProxyBlock with the META property and a metadata range inside 0-15 (one INFO line per row with its live key);
     * every clothing row is an ArmorItem of its slot's type; every textile has a 1.20.1 item; every hot meal is an FC
     * ItemFood with a 1.20.1 item; BTW fuels exist, nethercoal among them. Expected at default ids:
     * {@code 13 heat-source rows (12 blocks), 20 clothing, 4 textiles, 3 meals, f > 0 fuels, 0 warning(s)}. Isolated
     * like {@link #auditCompatRefs}.
     */
    private static void auditThermalTable() {
        try {
            if (!BTWLifecycle.isFcReady()) {
                LOGGER.warn("[SELF-AUDIT/thermal] FC is not initialised; the BTW thermal table cannot resolve");
                return;
            }
            int warnings = 0;
            for (String field : BtwThermalTable.allFields()) {
                if (FcRefs.fcField(field) == null) {
                    LOGGER.warn("[SELF-AUDIT/thermal] FCBetterThanWolves.{} did not resolve", field);
                    warnings++;
                }
            }

            Set<Block> heatBlocks = new java.util.LinkedHashSet<>();
            for (BtwThermalTable.HeatSource h : BtwThermalTable.heatSources()) {
                Block block = FcRefs.block(h.fcField());
                ResourceLocation key = block != null ? ForgeRegistries.BLOCKS.getKey(block) : null;
                if (block != null) heatBlocks.add(block);
                if (!(block instanceof ProxyBlock) || !block.defaultBlockState().hasProperty(ProxyBlock.META)) {
                    LOGGER.warn("[SELF-AUDIT/thermal] heat source {} {}: {} is not a ProxyBlock with the meta property",
                            h.tier(), h.fcField(), key != null ? key : "unresolved");
                    warnings++;
                }
                if (h.metaMin() < 0 || h.metaMin() > h.metaMax() || h.metaMax() > 15) {
                    LOGGER.warn("[SELF-AUDIT/thermal] heat source {} {}: meta range {}-{} is not inside 0-15",
                            h.tier(), h.fcField(), h.metaMin(), h.metaMax());
                    warnings++;
                }
                LOGGER.info("[SELF-AUDIT/thermal] {} {} -> {} meta {}-{}", h.tier(), h.fcField(),
                        key != null ? key : "unresolved", h.metaMin(), h.metaMax());
            }

            for (BtwThermalTable.Clothing c : BtwThermalTable.clothing()) {
                Item item = FcRefs.item(c.fcField());
                ArmorItem.Type expected = switch (c.slot()) {
                    case HEAD -> ArmorItem.Type.HELMET;
                    case CHEST -> ArmorItem.Type.CHESTPLATE;
                    case LEGS -> ArmorItem.Type.LEGGINGS;
                    case FEET -> ArmorItem.Type.BOOTS;
                };
                if (!(item instanceof ArmorItem armor) || armor.getType() != expected) {
                    LOGGER.warn("[SELF-AUDIT/thermal] clothing {} {} {}: {} is not {} armour", c.set(), c.slot(),
                            c.fcField(), item != null ? ForgeRegistries.ITEMS.getKey(item) : "unresolved", expected);
                    warnings++;
                }
            }

            for (BtwThermalTable.Textile t : BtwThermalTable.textiles()) {
                if (FcRefs.item(t.fcField()) == null) {
                    LOGGER.warn("[SELF-AUDIT/thermal] textile {} {}: no registered 1.20.1 item", t.kind(), t.fcField());
                    warnings++;
                }
            }

            for (BtwThermalTable.HotMeal m : BtwThermalTable.hotMeals()) {
                if (!(FcRefs.fcItem(m.fcField()) instanceof btw.modern.ItemFood) || FcRefs.item(m.fcField()) == null) {
                    LOGGER.warn("[SELF-AUDIT/thermal] meal {} {}: not an FC ItemFood with a registered 1.20.1 item",
                            m.tier(), m.fcField());
                    warnings++;
                }
            }

            List<BtwThermalTable.Fuel> fuels = BtwThermalTable.fuels();
            ResourceLocation nethercoalKey = FcRefs.itemKey("fcItemNethercoal");
            int nethercoalBurn = -1;
            for (BtwThermalTable.Fuel f : fuels) {
                if (f.key().equals(nethercoalKey)) {
                    nethercoalBurn = f.fcBurnTime();
                    break;
                }
            }
            if (nethercoalBurn < 0) {
                LOGGER.warn("[SELF-AUDIT/thermal] nethercoal ({}) is not among the {} BTW fuels",
                        nethercoalKey != null ? nethercoalKey : "unresolved", fuels.size());
                warnings++;
            }

            LOGGER.info("[SELF-AUDIT/thermal] {} heat-source rows ({} blocks), {} clothing, {} textiles, {} meals, "
                    + "{} fuels (nethercoal {} = {}); {} warning(s)", BtwThermalTable.heatSources().size(),
                    heatBlocks.size(), BtwThermalTable.clothing().size(), BtwThermalTable.textiles().size(),
                    BtwThermalTable.hotMeals().size(), fuels.size(), nethercoalKey != null ? nethercoalKey
                    : "unresolved", nethercoalBurn >= 0 ? nethercoalBurn : "absent", warnings);
        } catch (Throwable t) {
            LOGGER.error("[SELF-AUDIT/thermal] audit failed", t);
        }
    }
}
