package btw.forge;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Generates Forge model JSON files for BTW blocks and items based on available textures.
 * Run this class to regenerate all models when textures are added/changed.
 */
public class ModelGenerator {

    // Base paths relative to project root
    private static final String ASSETS_PATH = "Forge/src/main/resources/assets/betterthanwolves";
    private static final String TEXTURES_BLOCK = ASSETS_PATH + "/textures/block";
    private static final String TEXTURES_ITEM = ASSETS_PATH + "/textures/item";
    private static final String MODELS_BLOCK = ASSETS_PATH + "/models/block";
    private static final String MODELS_ITEM = ASSETS_PATH + "/models/item";
    private static final String BLOCKSTATES = ASSETS_PATH + "/blockstates";

    // Texture groupings by base name
    private Map<String, List<String>> blockTextureGroups = new HashMap<>();
    private Map<String, List<String>> itemTextureGroups = new HashMap<>();

    // Known block ID to texture mappings (extracted from FCBetterThanWolves.java)
    private static final Map<Integer, String> BLOCK_TEXTURE_MAP = new LinkedHashMap<>();

    // Known item ID to texture mappings
    private static final Map<Integer, String> ITEM_TEXTURE_MAP = new LinkedHashMap<>();

    static {
        // Initialize known block mappings
        // Format: blockID -> texture base name (without fcBlock prefix and .png suffix)

        // IDs 175-255 (original FC blocks)
        // BLOCK_TEXTURE_MAP.put(175, "SlabSandAndGravel"); // uses vanilla sand/gravel textures
        BLOCK_TEXTURE_MAP.put(176, "Vessel"); // ArcaneVessel
        BLOCK_TEXTURE_MAP.put(177, "Axle"); // AxlePowerSource
        BLOCK_TEXTURE_MAP.put(178, "DecorativeBlackStone"); // SidingAndCornerBlackStone
        BLOCK_TEXTURE_MAP.put(179, "DecorativeBlackStone"); // MouldingAndDecorativeBlackStone
        BLOCK_TEXTURE_MAP.put(180, "Dung"); // AestheticOpaqueEarth - uses dung texture
        BLOCK_TEXTURE_MAP.put(181, "Candle");
        BLOCK_TEXTURE_MAP.put(182, "DecorativeSandstone");
        BLOCK_TEXTURE_MAP.put(183, "DecorativeSandstone");
        BLOCK_TEXTURE_MAP.put(184, "DecorativeWoodOak");
        BLOCK_TEXTURE_MAP.put(185, "DecorativeStone"); // SmoothStoneSidingAndCorner
        BLOCK_TEXTURE_MAP.put(186, "DecorativeBrick");
        BLOCK_TEXTURE_MAP.put(187, "DecorativeBrick");
        BLOCK_TEXTURE_MAP.put(188, "DecorativeNetherBrick");
        BLOCK_TEXTURE_MAP.put(189, "DecorativeNetherBrick");
        BLOCK_TEXTURE_MAP.put(190, "DecorativeWhiteStone"); // WhiteStoneStairs
        BLOCK_TEXTURE_MAP.put(191, "DecorativeWhiteStone");
        BLOCK_TEXTURE_MAP.put(192, "DecorativeWhiteStone");
        BLOCK_TEXTURE_MAP.put(193, "StakeString");
        BLOCK_TEXTURE_MAP.put(194, "Stake");
        BLOCK_TEXTURE_MAP.put(195, "ScrewPump");
        BLOCK_TEXTURE_MAP.put(196, "DecorativeWoodSpruce");
        BLOCK_TEXTURE_MAP.put(197, "DecorativeWoodSpruce");
        BLOCK_TEXTURE_MAP.put(198, "DecorativeWoodBirch");
        BLOCK_TEXTURE_MAP.put(199, "DecorativeWoodBirch");
        BLOCK_TEXTURE_MAP.put(200, "DecorativeWoodJungle");
        BLOCK_TEXTURE_MAP.put(201, "DecorativeWoodJungle");
        BLOCK_TEXTURE_MAP.put(202, "DecorativeStoneBrick");
        BLOCK_TEXTURE_MAP.put(203, "DecorativeStoneBrick");
        BLOCK_TEXTURE_MAP.put(204, "FarmlandFertilized");
        BLOCK_TEXTURE_MAP.put(205, "Padding"); // WoolSlabTop
        BLOCK_TEXTURE_MAP.put(206, "SlabDirt"); // DirtSlab
        BLOCK_TEXTURE_MAP.put(207, "Groth"); // BloodMoss/NetherGrowth
        BLOCK_TEXTURE_MAP.put(208, "InfernalEnchanter");
        BLOCK_TEXTURE_MAP.put(209, "SoulforgedSteel");
        BLOCK_TEXTURE_MAP.put(210, "DetectorBlock"); // DetectorGlowingLogic
        BLOCK_TEXTURE_MAP.put(211, "LeavesBloodWood");
        BLOCK_TEXTURE_MAP.put(212, "BloodWood");
        BLOCK_TEXTURE_MAP.put(213, "Weeds"); // AestheticVegetation
        BLOCK_TEXTURE_MAP.put(214, "DecorativeStone"); // SmoothStoneMouldingAndDecorative
        BLOCK_TEXTURE_MAP.put(215, "Ender"); // AestheticOpaque - uses ender texture
        BLOCK_TEXTURE_MAP.put(216, "Slats"); // AestheticNonOpaque - uses slats
        BLOCK_TEXTURE_MAP.put(217, "MiningCharge");
        BLOCK_TEXTURE_MAP.put(218, "BuddyBlock");
        BLOCK_TEXTURE_MAP.put(219, "FurnaceBrick"); // Kiln
        BLOCK_TEXTURE_MAP.put(220, "Padding"); // WoolSlab
        BLOCK_TEXTURE_MAP.put(221, "Anvil"); // Soulforge
        BLOCK_TEXTURE_MAP.put(222, "LightBlock"); // LightBulbOff
        BLOCK_TEXTURE_MAP.put(223, "LightBlock"); // LightBulbOn
        BLOCK_TEXTURE_MAP.put(224, "Hibachi"); // BBQ
        BLOCK_TEXTURE_MAP.put(225, "Hopper");
        BLOCK_TEXTURE_MAP.put(226, "Saw");
        BLOCK_TEXTURE_MAP.put(227, "Platform");
        BLOCK_TEXTURE_MAP.put(228, "Cement");
        BLOCK_TEXTURE_MAP.put(229, "Pulley");
        BLOCK_TEXTURE_MAP.put(230, "PressurePlateSoulforgedSteel");
        BLOCK_TEXTURE_MAP.put(231, "DecorativeWoodOak"); // WoodOakMouldingAndDecorative
        BLOCK_TEXTURE_MAP.put(232, "DecorativeStone"); // LegacySmoothstoneAndOakCorner
        BLOCK_TEXTURE_MAP.put(233, "BlockDispenser");
        BLOCK_TEXTURE_MAP.put(234, "Cauldron");
        BLOCK_TEXTURE_MAP.put(235, "DetectorRailWood");
        BLOCK_TEXTURE_MAP.put(236, "DetectorRailSoulforgedSteel");
        BLOCK_TEXTURE_MAP.put(237, "CompanionCube");
        BLOCK_TEXTURE_MAP.put(238, "DetectorBlock");
        BLOCK_TEXTURE_MAP.put(239, "DetectorBlock"); // DetectorLogic
        BLOCK_TEXTURE_MAP.put(240, "Lens");
        BLOCK_TEXTURE_MAP.put(241, "Hemp"); // HempCrop
        BLOCK_TEXTURE_MAP.put(242, "HandCrank");
        BLOCK_TEXTURE_MAP.put(243, "MillStone");
        BLOCK_TEXTURE_MAP.put(244, "Anchor");
        BLOCK_TEXTURE_MAP.put(245, "Rope");
        BLOCK_TEXTURE_MAP.put(246, "Slats"); // LegacySmoothstoneAndOakSiding
        BLOCK_TEXTURE_MAP.put(247, "Axle");
        BLOCK_TEXTURE_MAP.put(248, "RedstoneClutch");
        BLOCK_TEXTURE_MAP.put(249, "Turntable");
        BLOCK_TEXTURE_MAP.put(250, "Bellows");
        BLOCK_TEXTURE_MAP.put(251, "FireStokedStub"); // FireStoked
        BLOCK_TEXTURE_MAP.put(252, "UnfiredPottery");
        BLOCK_TEXTURE_MAP.put(253, "Crucible");
        BLOCK_TEXTURE_MAP.put(254, "Planter");
        BLOCK_TEXTURE_MAP.put(255, "Vase");

        // IDs 1000+ (extended FC blocks from FCBetterThanWolves.java)
        BLOCK_TEXTURE_MAP.put(1000, "RottenFlesh");
        BLOCK_TEXTURE_MAP.put(1001, "Shaft");
        BLOCK_TEXTURE_MAP.put(1002, "SoulforgeDormant");
        BLOCK_TEXTURE_MAP.put(1003, "Stone"); // SmoothstoneStairs
        BLOCK_TEXTURE_MAP.put(1004, "RottenFleshSlab");
        BLOCK_TEXTURE_MAP.put(1005, "BoneSlab");
        // BLOCK_TEXTURE_MAP.put(1006, "Pumpkin"); // PumpkinFresh - uses vanilla pumpkin texture
        BLOCK_TEXTURE_MAP.put(1007, "DecorativeWoodBlood");
        BLOCK_TEXTURE_MAP.put(1008, "DecorativeWoodBlood");
        BLOCK_TEXTURE_MAP.put(1009, "Planks_blood"); // WoodBloodStairs
        BLOCK_TEXTURE_MAP.put(1010, "LogChewedOak"); // LogDamaged
        BLOCK_TEXTURE_MAP.put(1011, "DirtLoose");
        BLOCK_TEXTURE_MAP.put(1012, "DirtLoose"); // DirtLooseSlab
        BLOCK_TEXTURE_MAP.put(1013, "Campfire"); // CampfireUnlit
        BLOCK_TEXTURE_MAP.put(1014, "Campfire"); // CampfireSmall
        BLOCK_TEXTURE_MAP.put(1015, "Campfire"); // CampfireMedium
        BLOCK_TEXTURE_MAP.put(1016, "Campfire"); // CampfireLarge
        BLOCK_TEXTURE_MAP.put(1017, "UnfiredBrick");
        BLOCK_TEXTURE_MAP.put(1018, "CookedBrick");
        BLOCK_TEXTURE_MAP.put(1019, "BrickLoose");
        BLOCK_TEXTURE_MAP.put(1020, "BrickLoose"); // BrickLooseSlab
        BLOCK_TEXTURE_MAP.put(1021, "CobblestoneLoose");
        BLOCK_TEXTURE_MAP.put(1022, "CobblestoneLoose"); // CobblestoneLooseSlab
        BLOCK_TEXTURE_MAP.put(1023, "FurnaceBrick"); // FurnaceBrickIdle
        BLOCK_TEXTURE_MAP.put(1024, "FurnaceBrick"); // FurnaceBrickBurning
        BLOCK_TEXTURE_MAP.put(1025, "TorchFiniteIdle"); // TorchFiniteUnlit
        BLOCK_TEXTURE_MAP.put(1026, "TorchFiniteBurning");
        BLOCK_TEXTURE_MAP.put(1027, "StoneRough"); // StoneRough
        BLOCK_TEXTURE_MAP.put(1028, "StoneRough"); // StoneRoughMidStrata
        BLOCK_TEXTURE_MAP.put(1029, "StoneRough"); // StoneRoughDeepStrata
        BLOCK_TEXTURE_MAP.put(1030, "WorkStumpOak"); // WorkStump
        BLOCK_TEXTURE_MAP.put(1031, "BasketWicker");
        BLOCK_TEXTURE_MAP.put(1032, "LogChewedOak"); // LogSpike - shares textures with LogChewedOak
        BLOCK_TEXTURE_MAP.put(1033, "TorchIdle"); // TorchNetherUnlit
        BLOCK_TEXTURE_MAP.put(1034, "TableWoodOak"); // Workbench
        BLOCK_TEXTURE_MAP.put(1035, "Barrel"); // Chest
        // BLOCK_TEXTURE_MAP.put(1036, "Planks"); // DoorWood - uses vanilla planks texture
        BLOCK_TEXTURE_MAP.put(1037, "Web");
        BLOCK_TEXTURE_MAP.put(1038, "UnfiredClay");
        BLOCK_TEXTURE_MAP.put(1039, "MyceliumSlab");
        BLOCK_TEXTURE_MAP.put(1040, "Shovel"); // ToolPlaced
        BLOCK_TEXTURE_MAP.put(1041, "BrickLoose"); // BrickLooseStairs
        BLOCK_TEXTURE_MAP.put(1042, "CobblestoneLoose"); // CobblestoneLooseStairs
        BLOCK_TEXTURE_MAP.put(1043, "LogSmouldering");
        BLOCK_TEXTURE_MAP.put(1044, "WoodCinders");
        BLOCK_TEXTURE_MAP.put(1045, "StumpCharred");
        BLOCK_TEXTURE_MAP.put(1046, "AshGroundCover");
        BLOCK_TEXTURE_MAP.put(1047, "SnowLoose");
        BLOCK_TEXTURE_MAP.put(1048, "SnowLooseSlab");
        BLOCK_TEXTURE_MAP.put(1049, "SnowSolid");
        BLOCK_TEXTURE_MAP.put(1050, "SnowSolidSlab");
        // BLOCK_TEXTURE_MAP.put(1051, "Ladder"); // uses vanilla ladder texture
        // BLOCK_TEXTURE_MAP.put(1052, "Ladder"); // LadderOnFire - uses vanilla ladder texture
        BLOCK_TEXTURE_MAP.put(1053, "Shovel");
        BLOCK_TEXTURE_MAP.put(1054, "Hamper");
        BLOCK_TEXTURE_MAP.put(1055, "CreeperOysters");
        BLOCK_TEXTURE_MAP.put(1056, "CreeperOystersSlab");
        BLOCK_TEXTURE_MAP.put(1057, "TorchNetherBurning");
        BLOCK_TEXTURE_MAP.put(1058, "BucketEmpty");
        BLOCK_TEXTURE_MAP.put(1059, "Bucket_water"); // BucketWater
        BLOCK_TEXTURE_MAP.put(1060, "Cement"); // BucketCement - uses cement texture
        BLOCK_TEXTURE_MAP.put(1061, "Bucket_milk"); // BucketMilk
        BLOCK_TEXTURE_MAP.put(1062, "Bucket_chocolate"); // BucketMilkChocolate
        BLOCK_TEXTURE_MAP.put(1063, "Milk");
        BLOCK_TEXTURE_MAP.put(1064, "MilkChocolate");
        BLOCK_TEXTURE_MAP.put(1065, "GearBox");
        BLOCK_TEXTURE_MAP.put(1066, "SpikeIron");
        BLOCK_TEXTURE_MAP.put(1067, "LightningRod");
        BLOCK_TEXTURE_MAP.put(1068, "ChunkOreIron");
        BLOCK_TEXTURE_MAP.put(1069, "ChunkOreGold");
        BLOCK_TEXTURE_MAP.put(1070, "StoneBrickLoose");
        BLOCK_TEXTURE_MAP.put(1071, "StoneBrickLoose"); // StoneBrickLooseSlab
        BLOCK_TEXTURE_MAP.put(1072, "StoneBrickLoose"); // StoneBrickLooseStairs
        BLOCK_TEXTURE_MAP.put(1073, "NetherBrickLoose");
        BLOCK_TEXTURE_MAP.put(1074, "NetherBrickLoose"); // NetherBrickLooseSlab
        BLOCK_TEXTURE_MAP.put(1075, "NetherBrickLoose"); // NetherBrickLooseStairs
        BLOCK_TEXTURE_MAP.put(1076, "NetherrackGrothed"); // NetherrackFalling
        BLOCK_TEXTURE_MAP.put(1077, "LavaPillow");
        BLOCK_TEXTURE_MAP.put(1078, "Stub"); // MushroomCapBrown
        BLOCK_TEXTURE_MAP.put(1079, "Stub"); // MushroomCapRed
        BLOCK_TEXTURE_MAP.put(1080, "ChunkOreStorageIron");
        BLOCK_TEXTURE_MAP.put(1081, "ChunkOreStorageGold");
        BLOCK_TEXTURE_MAP.put(1082, "Wicker");
        BLOCK_TEXTURE_MAP.put(1083, "SlabWicker"); // WickerSlab
        BLOCK_TEXTURE_MAP.put(1084, "Wicker"); // WickerPane
        BLOCK_TEXTURE_MAP.put(1085, "Grate");
        BLOCK_TEXTURE_MAP.put(1086, "Slats");
        BLOCK_TEXTURE_MAP.put(1087, "FarmlandFertilized"); // Farmland
        BLOCK_TEXTURE_MAP.put(1088, "FarmlandFertilized"); // FarmlandFertilized
        BLOCK_TEXTURE_MAP.put(1089, "WheatCrop");
        BLOCK_TEXTURE_MAP.put(1090, "WheatCropTop");
        BLOCK_TEXTURE_MAP.put(1091, "Weeds");
        BLOCK_TEXTURE_MAP.put(1092, "Planter"); // PlanterSoil

        // Item texture mappings (extracted from FCBetterThanWolves.java)
        // Maps item ID -> texture base name (without fcItem prefix)

        // IDs 222-287 (original FC items)
        ITEM_TEXTURE_MAP.put(222, "BucketCement");
        ITEM_TEXTURE_MAP.put(223, "VANILLA:porkchop"); // Wolf Chop Raw = pork chop texture
        ITEM_TEXTURE_MAP.put(224, "VANILLA:cooked_porkchop"); // Wolf Chop Cooked
        ITEM_TEXTURE_MAP.put(225, "Nethercoal");
        ITEM_TEXTURE_MAP.put(226, "SeedsHemp");
        ITEM_TEXTURE_MAP.put(227, "Hemp");
        ITEM_TEXTURE_MAP.put(228, "Gear");
        ITEM_TEXTURE_MAP.put(229, "Flour");
        ITEM_TEXTURE_MAP.put(230, "FibersHemp");
        ITEM_TEXTURE_MAP.put(231, "LeatherScoured");
        ITEM_TEXTURE_MAP.put(232, "Donut");
        ITEM_TEXTURE_MAP.put(233, "Rope");
        ITEM_TEXTURE_MAP.put(234, "Slats"); // SlatsOld (legacy)
        ITEM_TEXTURE_MAP.put(235, "Dung");
        ITEM_TEXTURE_MAP.put(236, "WaterWheel");
        ITEM_TEXTURE_MAP.put(237, "BladeWindMill");
        ITEM_TEXTURE_MAP.put(238, "WindMill");
        ITEM_TEXTURE_MAP.put(239, "pFabric"); // fcItempFabric (typo in original FC code)
        ITEM_TEXTURE_MAP.put(240, "Grate"); // GrateOld (legacy)
        ITEM_TEXTURE_MAP.put(241, "Wicker"); // WickerPaneOld (legacy)
        ITEM_TEXTURE_MAP.put(242, "LeatherTanned");
        ITEM_TEXTURE_MAP.put(243, "Strap");
        ITEM_TEXTURE_MAP.put(244, "Belt");
        ITEM_TEXTURE_MAP.put(245, "FoulFood");
        ITEM_TEXTURE_MAP.put(246, "BladeWood");
        ITEM_TEXTURE_MAP.put(247, "Glue");
        ITEM_TEXTURE_MAP.put(248, "Tallow");
        ITEM_TEXTURE_MAP.put(249, "Haft");
        ITEM_TEXTURE_MAP.put(250, "IngotSteel");
        ITEM_TEXTURE_MAP.put(251, "PickAxeRefined");
        ITEM_TEXTURE_MAP.put(252, "ShovelRefined");
        ITEM_TEXTURE_MAP.put(253, "HoeRefined");
        ITEM_TEXTURE_MAP.put(254, "AxeBattle");
        ITEM_TEXTURE_MAP.put(255, "SwordRefined");
        ITEM_TEXTURE_MAP.put(256, "NetherrackGround");
        ITEM_TEXTURE_MAP.put(257, "DustHellfire");
        ITEM_TEXTURE_MAP.put(258, "ConcentratedHellfire");
        ITEM_TEXTURE_MAP.put(259, "ArmorPlate");
        ITEM_TEXTURE_MAP.put(260, "HelmetPlate");
        ITEM_TEXTURE_MAP.put(261, "ChestplatePlate");
        ITEM_TEXTURE_MAP.put(262, "LeggingsPlate");
        ITEM_TEXTURE_MAP.put(263, "BootsPlate");
        ITEM_TEXTURE_MAP.put(264, "BowComposite");
        ITEM_TEXTURE_MAP.put(265, "ArrowheadBroadhead");
        ITEM_TEXTURE_MAP.put(266, "ArrowBroadhead");
        ITEM_TEXTURE_MAP.put(267, "DustCoal");
        ITEM_TEXTURE_MAP.put(268, "Padding");
        ITEM_TEXTURE_MAP.put(269, "Filament");
        ITEM_TEXTURE_MAP.put(270, "RedstoneEye"); // Polished Lapis
        ITEM_TEXTURE_MAP.put(271, "Urn");
        ITEM_TEXTURE_MAP.put(272, "UrnSoul");
        ITEM_TEXTURE_MAP.put(273, "EggPoached"); // HardBoiledEgg
        ITEM_TEXTURE_MAP.put(274, "Potash");
        ITEM_TEXTURE_MAP.put(275, "Soap");
        ITEM_TEXTURE_MAP.put(276, "DustSaw");
        ITEM_TEXTURE_MAP.put(277, "GimpHelm");
        ITEM_TEXTURE_MAP.put(278, "GimpChest");
        ITEM_TEXTURE_MAP.put(279, "GimpLeggings");
        ITEM_TEXTURE_MAP.put(280, "GimpBoots");
        ITEM_TEXTURE_MAP.put(281, "Dynamite");
        ITEM_TEXTURE_MAP.put(282, "HarnessBreeding");
        ITEM_TEXTURE_MAP.put(283, "DustSoul");
        ITEM_TEXTURE_MAP.put(284, "Mattock");
        ITEM_TEXTURE_MAP.put(285, "HatchetRefined"); // Refined Axe
        ITEM_TEXTURE_MAP.put(286, "NetherSludge");
        ITEM_TEXTURE_MAP.put(287, "BrickNether");

        // IDs 2295-2308 (low-numbered extended items)
        ITEM_TEXTURE_MAP.put(2295, "Wool");
        ITEM_TEXTURE_MAP.put(2296, "CocoaPowder"); // CocoaBeans - using cocoa powder texture
        ITEM_TEXTURE_MAP.put(2297, "Chocolate");
        ITEM_TEXTURE_MAP.put(2298, "BucketChocolateMilk");
        ITEM_TEXTURE_MAP.put(2299, "SoulFlux");
        ITEM_TEXTURE_MAP.put(2300, "EnderSlag");
        ITEM_TEXTURE_MAP.put(2301, "PastryUncookedCake");
        ITEM_TEXTURE_MAP.put(2302, "PastryUncookedCookies");
        ITEM_TEXTURE_MAP.put(2303, "PastryUncookedPumpkinPie");
        ITEM_TEXTURE_MAP.put(2304, "MysteriousGland");
        ITEM_TEXTURE_MAP.put(2305, "BeastLiverRaw");
        ITEM_TEXTURE_MAP.put(2306, "BeastLiverCooked");
        ITEM_TEXTURE_MAP.put(2307, "AncientProphecy");
        ITEM_TEXTURE_MAP.put(2308, "StumpRemover");

        // IDs 22222-22339 (high-numbered extended items)
        ITEM_TEXTURE_MAP.put(22222, "TuningFork");
        ITEM_TEXTURE_MAP.put(22223, "ScrollArcane");
        ITEM_TEXTURE_MAP.put(22224, "Candle");
        ITEM_TEXTURE_MAP.put(22225, "SporesNetherGroth"); // BloodMossSpores
        ITEM_TEXTURE_MAP.put(22226, "Mould");
        ITEM_TEXTURE_MAP.put(22227, "Canvas");
        ITEM_TEXTURE_MAP.put(22228, "Kibble"); // DogFood
        ITEM_TEXTURE_MAP.put(22229, "EggRaw");
        ITEM_TEXTURE_MAP.put(22230, "EggFried");
        ITEM_TEXTURE_MAP.put(22231, "Screw");
        ITEM_TEXTURE_MAP.put(22232, "ArrowRotten");
        ITEM_TEXTURE_MAP.put(22233, "OcularOfEnder");
        ITEM_TEXTURE_MAP.put(22234, "EnderSpectacles");
        ITEM_TEXTURE_MAP.put(22235, "Stake");
        ITEM_TEXTURE_MAP.put(22236, "Brimstone");
        ITEM_TEXTURE_MAP.put(22237, "Nitre");
        ITEM_TEXTURE_MAP.put(22238, "Element");
        ITEM_TEXTURE_MAP.put(22239, "Fuse");
        ITEM_TEXTURE_MAP.put(22240, "BlastingOil");
        ITEM_TEXTURE_MAP.put(22241, "WindMillVertical");
        ITEM_TEXTURE_MAP.put(22242, "PotatoBoiled");
        ITEM_TEXTURE_MAP.put(22243, "MuttonRaw");
        ITEM_TEXTURE_MAP.put(22244, "MuttonCooked");
        ITEM_TEXTURE_MAP.put(22245, "WitchWart");
        ITEM_TEXTURE_MAP.put(22246, "CarrotCooked");
        ITEM_TEXTURE_MAP.put(22247, "SandwichTasty");
        ITEM_TEXTURE_MAP.put(22248, "SteakAndPotatoes");
        ITEM_TEXTURE_MAP.put(22249, "HamAndEggs");
        ITEM_TEXTURE_MAP.put(22250, "DinnerSteak");
        ITEM_TEXTURE_MAP.put(22251, "DinnerPork");
        ITEM_TEXTURE_MAP.put(22252, "DinnerWolf");
        ITEM_TEXTURE_MAP.put(22253, "KebabRaw");
        ITEM_TEXTURE_MAP.put(22254, "KebabCooked");
        ITEM_TEXTURE_MAP.put(22255, "SoupChicken");
        ITEM_TEXTURE_MAP.put(22256, "Chowder"); // FishSoup
        ITEM_TEXTURE_MAP.put(22257, "StewHearty");
        ITEM_TEXTURE_MAP.put(22258, "MushroomRed");
        ITEM_TEXTURE_MAP.put(22259, "MushroomBrown");
        ITEM_TEXTURE_MAP.put(22260, "NuggetIron");
        ITEM_TEXTURE_MAP.put(22261, "Mail");
        ITEM_TEXTURE_MAP.put(22262, "VANILLA:beef"); // Raw Mystery Meat = beef texture
        ITEM_TEXTURE_MAP.put(22263, "VANILLA:cooked_beef"); // Cooked Mystery Meat
        ITEM_TEXTURE_MAP.put(22264, "MushroomOmletRaw");
        ITEM_TEXTURE_MAP.put(22265, "MushroomOmletCooked");
        ITEM_TEXTURE_MAP.put(22266, "EggScrambledRaw");
        ITEM_TEXTURE_MAP.put(22267, "EggScrambledCooked");
        ITEM_TEXTURE_MAP.put(22268, "CreeperOysters");
        ITEM_TEXTURE_MAP.put(22269, "WoolHelm");
        ITEM_TEXTURE_MAP.put(22270, "WoolChest");
        ITEM_TEXTURE_MAP.put(22271, "WoolLeggings");
        ITEM_TEXTURE_MAP.put(22272, "WoolBoots");
        ITEM_TEXTURE_MAP.put(22273, "PaddedHelm");
        ITEM_TEXTURE_MAP.put(22274, "PaddedChest");
        ITEM_TEXTURE_MAP.put(22275, "PaddedLeggings");
        ITEM_TEXTURE_MAP.put(22276, "PaddedBoots");
        ITEM_TEXTURE_MAP.put(22277, "TannedHelm");
        ITEM_TEXTURE_MAP.put(22278, "TannedChest");
        ITEM_TEXTURE_MAP.put(22279, "TannedLeggings");
        ITEM_TEXTURE_MAP.put(22280, "TannedBoots");
        ITEM_TEXTURE_MAP.put(22281, "IngotDiamond");
        ITEM_TEXTURE_MAP.put(22282, "LeatherCut");
        ITEM_TEXTURE_MAP.put(22283, "LeatherTannedCut");
        ITEM_TEXTURE_MAP.put(22284, "LeatherScouredCut");
        ITEM_TEXTURE_MAP.put(22285, "FishingRodBaited");
        ITEM_TEXTURE_MAP.put(22286, "PileDirt");
        ITEM_TEXTURE_MAP.put(22287, "PileSand");
        ITEM_TEXTURE_MAP.put(22288, "PileGravel");
        ITEM_TEXTURE_MAP.put(22289, "BatWing");
        ITEM_TEXTURE_MAP.put(22290, "DungGolden");
        ITEM_TEXTURE_MAP.put(22291, "BarkOak"); // Bark (default to oak)
        ITEM_TEXTURE_MAP.put(22292, "PileSoulSand");
        ITEM_TEXTURE_MAP.put(22293, "RedstoneLatch");
        ITEM_TEXTURE_MAP.put(22294, "NuggetSteel");
        ITEM_TEXTURE_MAP.put(22309, "ChiselWood");
        ITEM_TEXTURE_MAP.put(22310, "Stone");
        ITEM_TEXTURE_MAP.put(22311, "ChiselStone");
        ITEM_TEXTURE_MAP.put(22312, "Club"); // ClubWood
        ITEM_TEXTURE_MAP.put(22313, "FireStarterSticks");
        ITEM_TEXTURE_MAP.put(22314, "FireStarterBow");
        ITEM_TEXTURE_MAP.put(22315, "ChunkIronOre");
        ITEM_TEXTURE_MAP.put(22316, "PileIronOre");
        ITEM_TEXTURE_MAP.put(22317, "ChiselIron");
        ITEM_TEXTURE_MAP.put(22318, "ChunkGoldOre");
        ITEM_TEXTURE_MAP.put(22319, "PileGoldOre");
        ITEM_TEXTURE_MAP.put(22320, "WickerPiece");
        ITEM_TEXTURE_MAP.put(22321, "KnittingNeedles");
        ITEM_TEXTURE_MAP.put(22322, "Knitting");
        ITEM_TEXTURE_MAP.put(22323, "WoolKnit");
        ITEM_TEXTURE_MAP.put(22324, "ClubBone");
        ITEM_TEXTURE_MAP.put(22325, "MeatCured");
        ITEM_TEXTURE_MAP.put(22326, "MetalFragment");
        ITEM_TEXTURE_MAP.put(22327, "PileClay");
        ITEM_TEXTURE_MAP.put(22328, "MeatBurned");
        ITEM_TEXTURE_MAP.put(22329, "ChickenFeed");
        ITEM_TEXTURE_MAP.put(22330, "FishHookBone");
        ITEM_TEXTURE_MAP.put(22331, "CarvingBone");
        ITEM_TEXTURE_MAP.put(22332, "BrickStone");
        ITEM_TEXTURE_MAP.put(22333, "WickerWeaving");
        ITEM_TEXTURE_MAP.put(22334, "Wheat");
        ITEM_TEXTURE_MAP.put(22335, "WheatSeeds");
        ITEM_TEXTURE_MAP.put(22336, "BreadDough");
        ITEM_TEXTURE_MAP.put(22337, "Straw");
        ITEM_TEXTURE_MAP.put(22338, "BrickUnfired");
        ITEM_TEXTURE_MAP.put(22339, "BrickNetherUnfired");
    }

    public static void main(String[] args) throws IOException {
        String projectRoot = args.length > 0 ? args[0] : ".";
        ModelGenerator generator = new ModelGenerator();
        generator.generate(projectRoot);
        generator.updateBlockstates(projectRoot);
    }

    public void generate(String projectRoot) throws IOException {
        System.out.println("Generating BTW models...");

        // Load texture files
        loadTextures(projectRoot);

        // Auto-discover block ID to texture mappings from source
        autoDiscoverBlockMappings(projectRoot);

        // Generate block models
        generateBlockModels(projectRoot);

        // Generate item models for blocks
        generateBlockItemModels(projectRoot);

        // Generate item models for standalone items
        generateItemModels(projectRoot);

        System.out.println("Model generation complete!");
    }

    /**
     * Automatically discovers block ID to texture mappings by parsing FCBetterThanWolves.java
     */
    private void autoDiscoverBlockMappings(String projectRoot) throws IOException {
        Path sourcePath = Paths.get(projectRoot,
            "Server/src/main/java/net/minecraft/src/btw/core/FCBetterThanWolves.java");

        if (!Files.exists(sourcePath)) {
            System.out.println("Could not find FCBetterThanWolves.java for auto-discovery");
            return;
        }

        List<String> lines = Files.readAllLines(sourcePath);
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(
            "new (FC[A-Za-z]+)\\s*\\(\\s*ParseID\\s*\\(\\s*\"[^\"]+\"\\s*,\\s*(\\d+)");

        int discovered = 0;
        for (String line : lines) {
            java.util.regex.Matcher matcher = pattern.matcher(line);
            if (matcher.find()) {
                String className = matcher.group(1);
                int blockId = Integer.parseInt(matcher.group(2));

                // Skip if already mapped
                if (BLOCK_TEXTURE_MAP.containsKey(blockId)) continue;

                // Try to match class name to texture
                String textureName = matchClassToTexture(className);
                if (textureName != null) {
                    BLOCK_TEXTURE_MAP.put(blockId, textureName);
                    discovered++;
                }
            }
        }
        System.out.println("Auto-discovered " + discovered + " additional block mappings");
    }

    private String matchClassToTexture(String className) {
        // Remove FCBlock prefix
        String name = className;
        if (name.startsWith("FCBlock")) {
            name = name.substring(7);
        } else if (name.startsWith("FC")) {
            name = name.substring(2);
        }

        // Direct match first
        for (String textureGroup : blockTextureGroups.keySet()) {
            if (textureGroup.equalsIgnoreCase(name)) {
                return textureGroup;
            }
        }

        // Try matching with common patterns
        String[] patterns = {
            name,                          // Direct name
            name.replace("Siding", ""),    // Remove Siding suffix
            name.replace("AndCorner", ""), // Remove AndCorner
            name.replace("Moulding", ""),  // Remove Moulding
            name.replace("Decorative", ""),// Remove Decorative prefix
            name.replace("Loose", ""),     // Remove Loose suffix
            name.replace("Slab", ""),      // Remove Slab suffix
            name.replace("Stairs", ""),    // Remove Stairs suffix
        };

        for (String pattern : patterns) {
            if (pattern.isEmpty()) continue;
            for (String textureGroup : blockTextureGroups.keySet()) {
                if (textureGroup.equalsIgnoreCase(pattern)) {
                    return textureGroup;
                }
                // Try contains match for longer names
                if (pattern.length() > 5 &&
                    (textureGroup.toLowerCase().contains(pattern.toLowerCase()) ||
                     pattern.toLowerCase().contains(textureGroup.toLowerCase()))) {
                    return textureGroup;
                }
            }
        }
        return null;
    }

    private void loadTextures(String projectRoot) throws IOException {
        // Load block textures
        Path blockTexturePath = Paths.get(projectRoot, TEXTURES_BLOCK);
        if (Files.exists(blockTexturePath)) {
            Files.list(blockTexturePath)
                .filter(p -> p.toString().endsWith(".png"))
                .forEach(p -> {
                    String name = p.getFileName().toString().replace(".png", "");
                    String baseName = getTextureBaseName(name);
                    blockTextureGroups.computeIfAbsent(baseName, k -> new ArrayList<>()).add(name);
                });
        }

        // Load item textures
        Path itemTexturePath = Paths.get(projectRoot, TEXTURES_ITEM);
        if (Files.exists(itemTexturePath)) {
            Files.list(itemTexturePath)
                .filter(p -> p.toString().endsWith(".png"))
                .forEach(p -> {
                    String name = p.getFileName().toString().replace(".png", "");
                    String baseName = getTextureBaseName(name);
                    itemTextureGroups.computeIfAbsent(baseName, k -> new ArrayList<>()).add(name);
                });
        }

        System.out.println("Loaded " + blockTextureGroups.size() + " block texture groups");
        System.out.println("Loaded " + itemTextureGroups.size() + " item texture groups");
    }

    private String getTextureBaseName(String textureName) {
        // Extract base name from texture file
        // e.g., fcBlockAxle_side -> Axle, fcBlockBellows_front -> Bellows
        // e.g., fcItemGear -> Gear, fcItemHemp_fiber -> Hemp
        String name = textureName;

        // Remove fcBlock/fcItem prefix (case-insensitive since filenames are now lowercase)
        String nameLower = name.toLowerCase();
        if (nameLower.startsWith("fcblock")) {
            name = name.substring(7);
        } else if (nameLower.startsWith("fcitem")) {
            name = name.substring(6);
        } else if (nameLower.startsWith("fc")) {
            name = name.substring(2);
        }
        // Normalize base name to lowercase for consistent lookups
        name = name.toLowerCase();

        // Remove all suffixes - strip everything from the first underscore that precedes a known suffix pattern
        // This handles compound suffixes like _grass_side, _open_top, etc.
        String[] suffixes = {"_side", "_top", "_bottom", "_front", "_back", "_end", "_on", "_off",
                            "_lit", "_input", "_output", "_00", "_01", "_02", "_03", "_c00", "_c01",
                            "_dry", "_wet", "_drying", "_open", "_closed", "_leg", "_nub", "_open_top",
                            "_fast", "_slow", "_idle", "_burning", "_sputtering", "_interior",
                            "_nozzle", "_band", "_wide", "_strata", "_mid", "_deep", "_grass",
                            "_fertilized", "_unfired", "_cooked", "_charged", "_uncharged", "_snow",
                            "_0", "_1", "_2", "_3", "_4", "_5", "_6", "_7", "_8", "_9",
                            "_old", "_new", "_dirty", "_burned", "_spit", "_support", "_grown",
                            "_contents", "_grate", "_ironbars", "_ladder", "_slats", "_soulsand",
                            "_chiseled", "_lines", "_xp", "_item", "_overlay", "_roots", "_half"};

        // Keep stripping suffixes until no more are found
        boolean found;
        do {
            found = false;
            for (String suffix : suffixes) {
                if (name.toLowerCase().endsWith(suffix.toLowerCase())) {
                    name = name.substring(0, name.length() - suffix.length());
                    found = true;
                    break;
                }
            }
        } while (found && name.length() > 0);

        return name;
    }

    private void generateBlockModels(String projectRoot) throws IOException {
        Path modelsPath = Paths.get(projectRoot, MODELS_BLOCK);
        Files.createDirectories(modelsPath);

        // Generate a model for each texture group
        for (Map.Entry<String, List<String>> entry : blockTextureGroups.entrySet()) {
            String baseName = entry.getKey();
            List<String> textures = entry.getValue();

            // Skip placeholder and overlay textures
            if (baseName.contains("placeholder") || baseName.contains("overlay")) {
                continue;
            }

            // Determine model type based on available textures
            Map<String, Object> model = createBlockModel(baseName, textures);

            // Write model file (all lowercase for MC 1.13+ resource path requirements)
            String modelFileName = "fcblock" + baseName + ".json";
            Path modelPath = modelsPath.resolve(modelFileName);
            try (FileWriter writer = new FileWriter(modelPath.toFile())) {
                writer.write(mapToJson(model));
            }
        }

        System.out.println("Generated block models in " + modelsPath);
    }

    private Map<String, Object> createBlockModel(String baseName, List<String> textures) {
        Map<String, Object> model = new LinkedHashMap<>();
        Map<String, String> textureMap = new LinkedHashMap<>();

        // Determine texture variants
        boolean hasTop = textures.stream().anyMatch(t -> t.toLowerCase().contains("_top"));
        boolean hasBottom = textures.stream().anyMatch(t -> t.toLowerCase().contains("_bottom"));
        boolean hasSide = textures.stream().anyMatch(t -> t.toLowerCase().contains("_side"));
        boolean hasFront = textures.stream().anyMatch(t -> t.toLowerCase().contains("_front"));
        boolean hasEnd = textures.stream().anyMatch(t -> t.toLowerCase().contains("_end"));

        // Find base texture (no suffix)
        String baseTexture = textures.stream()
            .filter(t -> !t.contains("_"))
            .findFirst()
            .orElse(textures.get(0));

        if (hasFront && hasSide && hasTop) {
            // Directional block like dispenser, furnace
            model.put("parent", "minecraft:block/orientable");
            textureMap.put("front", "betterthanwolves:block/" + findTexture(textures, "_front"));
            textureMap.put("side", "betterthanwolves:block/" + findTexture(textures, "_side"));
            textureMap.put("top", "betterthanwolves:block/" + findTexture(textures, "_top"));
        } else if (hasTop && hasBottom && hasSide) {
            // Column block like pillar, log
            model.put("parent", "minecraft:block/cube_column");
            textureMap.put("end", "betterthanwolves:block/" + findTexture(textures, "_top"));
            textureMap.put("side", "betterthanwolves:block/" + findTexture(textures, "_side"));
        } else if (hasTop && hasSide) {
            // Top-side block
            model.put("parent", "minecraft:block/cube_column");
            textureMap.put("end", "betterthanwolves:block/" + findTexture(textures, "_top"));
            textureMap.put("side", "betterthanwolves:block/" + findTexture(textures, "_side"));
        } else if (hasEnd && hasSide) {
            // Axis block like axle
            model.put("parent", "minecraft:block/cube_column");
            textureMap.put("end", "betterthanwolves:block/" + findTexture(textures, "_end"));
            textureMap.put("side", "betterthanwolves:block/" + findTexture(textures, "_side"));
        } else {
            // Simple cube with all same texture
            model.put("parent", "minecraft:block/cube_all");
            textureMap.put("all", "betterthanwolves:block/" + baseTexture);
        }

        model.put("textures", textureMap);
        return model;
    }

    private String findTexture(List<String> textures, String suffix) {
        return textures.stream()
            .filter(t -> t.toLowerCase().contains(suffix.toLowerCase()))
            .findFirst()
            .orElse(textures.get(0));
    }

    private void generateBlockItemModels(String projectRoot) throws IOException {
        Path modelsPath = Paths.get(projectRoot, MODELS_ITEM);
        Files.createDirectories(modelsPath);

        Path blockstatesPath = Paths.get(projectRoot, BLOCKSTATES);

        // Get list of block IDs from blockstates
        if (!Files.exists(blockstatesPath)) return;

        List<Integer> blockIds = Files.list(blockstatesPath)
            .filter(p -> p.getFileName().toString().startsWith("block_"))
            .map(p -> {
                String name = p.getFileName().toString();
                return Integer.parseInt(name.replace("block_", "").replace(".json", ""));
            })
            .sorted()
            .collect(Collectors.toList());

        int realTextureCount = 0;
        for (int blockId : blockIds) {
            Map<String, Object> model = new LinkedHashMap<>();

            // Try to find the correct texture for this block ID
            String textureName = BLOCK_TEXTURE_MAP.get(blockId);
            String textureKey = textureName != null ? textureName.toLowerCase() : null;
            if (textureKey != null && blockTextureGroups.containsKey(textureKey)) {
                // Use the actual texture model (all lowercase for MC 1.13+)
                model.put("parent", "betterthanwolves:block/fcblock" + textureKey);
                realTextureCount++;
            } else {
                // Fall back to placeholder
                model.put("parent", "betterthanwolves:block/btw_placeholder_cube");
            }

            Path modelPath = modelsPath.resolve("block_" + blockId + ".json");
            try (FileWriter writer = new FileWriter(modelPath.toFile())) {
                writer.write(mapToJson(model));
            }
        }

        System.out.println("Generated " + blockIds.size() + " block item models (" + realTextureCount + " with real textures)");
    }

    private void generateItemModels(String projectRoot) throws IOException {
        Path modelsPath = Paths.get(projectRoot, MODELS_ITEM);
        Files.createDirectories(modelsPath);

        // Generate models for item textures
        for (Map.Entry<String, List<String>> entry : itemTextureGroups.entrySet()) {
            String baseName = entry.getKey();
            List<String> textures = entry.getValue();

            if (baseName.contains("placeholder")) continue;

            // Find base texture
            String baseTexture = textures.stream()
                .filter(t -> !t.contains("_pull") && !t.contains("_overlay"))
                .findFirst()
                .orElse(textures.get(0));

            Map<String, Object> model = new LinkedHashMap<>();
            model.put("parent", "minecraft:item/generated");

            Map<String, String> textureMap = new LinkedHashMap<>();
            textureMap.put("layer0", "betterthanwolves:item/" + baseTexture);
            model.put("textures", textureMap);

            String modelFileName = baseTexture + ".json";
            Path modelPath = modelsPath.resolve(modelFileName);
            try (FileWriter writer = new FileWriter(modelPath.toFile())) {
                writer.write(mapToJson(model));
            }
        }

        // Generate item_XXX.json models for all items in ITEM_TEXTURE_MAP
        // These reference the actual item textures where available
        int mappedItems = 0;
        int placeholderItems = 0;

        // Create models for ALL items in the ITEM_TEXTURE_MAP
        // NOTE: MC 1.5.2 items have a +256 ID offset (Item constructor adds 256 to the passed ID).
        // ITEM_TEXTURE_MAP uses FC's ParseID values, but Forge registers items at ParseID+256.
        final int ITEM_ID_OFFSET = 256;

        for (Map.Entry<Integer, String> entry : ITEM_TEXTURE_MAP.entrySet()) {
            int fcItemId = entry.getKey();
            int actualItemId = fcItemId + ITEM_ID_OFFSET;
            String textureName = entry.getValue();
            String textureKey = textureName != null ? textureName.toLowerCase() : null;

            Path modelPath = modelsPath.resolve("item_" + actualItemId + ".json");
            Map<String, Object> model = new LinkedHashMap<>();

            if (textureName != null && textureName.startsWith("VANILLA:")) {
                // Use vanilla Minecraft texture
                String vanillaTexture = textureName.substring("VANILLA:".length());
                model.put("parent", "minecraft:item/generated");
                Map<String, String> textureMap = new LinkedHashMap<>();
                textureMap.put("layer0", "minecraft:item/" + vanillaTexture);
                model.put("textures", textureMap);
                mappedItems++;
            } else if (textureKey != null && itemTextureGroups.containsKey(textureKey)) {
                model.put("parent", "minecraft:item/generated");

                Map<String, String> textureMap = new LinkedHashMap<>();
                // Get the actual texture file name from the group (already lowercase from filesystem)
                List<String> textures = itemTextureGroups.get(textureKey);
                String baseTexture = textures.stream()
                    .filter(t -> !t.contains("_pull") && !t.contains("_overlay"))
                    .findFirst()
                    .orElse(textures.get(0));
                textureMap.put("layer0", "betterthanwolves:item/" + baseTexture);
                model.put("textures", textureMap);
                mappedItems++;
            } else {
                // Fallback to placeholder
                model.put("parent", "minecraft:item/generated");
                Map<String, String> textureMap = new LinkedHashMap<>();
                textureMap.put("layer0", "betterthanwolves:item/btw_placeholder");
                model.put("textures", textureMap);
                placeholderItems++;
                System.out.println("  Warning: No texture found for item " + fcItemId + " (actual: " + actualItemId + ", texture: " + textureName + " -> " + textureKey + ")");
            }

            try (FileWriter writer = new FileWriter(modelPath.toFile())) {
                writer.write(mapToJson(model));
            }
        }

        // Also update any existing item_XXX.json files that aren't in the map
        // but might have been created by other code
        try {
            Files.list(modelsPath)
                .filter(p -> p.getFileName().toString().matches("item_\\d+\\.json"))
                .forEach(p -> {
                    try {
                        String name = p.getFileName().toString();
                        int itemId = Integer.parseInt(name.replace("item_", "").replace(".json", ""));

                        // Skip if already processed via ITEM_TEXTURE_MAP
                        if (ITEM_TEXTURE_MAP.containsKey(itemId)) return;

                        // Check if there's a matching texture by item ID patterns
                        // This is a fallback for items not in the map
                    } catch (Exception e) {
                        // Ignore parse errors
                    }
                });
        } catch (IOException e) {
            // Ignore errors listing directory
        }

        System.out.println("Generated " + mappedItems + " item models with real textures, " + placeholderItems + " with placeholder");
    }

    /**
     * Updates blockstate JSON files to use the correct models based on metadata.
     */
    public void updateBlockstates(String projectRoot) throws IOException {
        Path blockstatesPath = Paths.get(projectRoot, BLOCKSTATES);
        if (!Files.exists(blockstatesPath)) return;

        Files.list(blockstatesPath)
            .filter(p -> p.getFileName().toString().startsWith("block_"))
            .forEach(p -> {
                try {
                    String name = p.getFileName().toString();
                    int blockId = Integer.parseInt(name.replace("block_", "").replace(".json", ""));

                    String textureName = BLOCK_TEXTURE_MAP.get(blockId);
                    String textureKey = textureName != null ? textureName.toLowerCase() : null;
                    String modelRef = textureKey != null && blockTextureGroups.containsKey(textureKey)
                        ? "betterthanwolves:block/fcblock" + textureKey
                        : "betterthanwolves:block/btw_placeholder_cube";

                    Map<String, Object> blockstate = new LinkedHashMap<>();
                    Map<String, Object> variants = new LinkedHashMap<>();

                    for (int meta = 0; meta <= 15; meta++) {
                        Map<String, Object> variant = new LinkedHashMap<>();
                        variant.put("model", modelRef);
                        variants.put("meta=" + meta, variant);
                    }

                    blockstate.put("variants", variants);

                    try (FileWriter writer = new FileWriter(p.toFile())) {
                        writer.write(mapToJson(blockstate));
                    }
                } catch (Exception e) {
                    System.err.println("Error updating " + p + ": " + e.getMessage());
                }
            });

        System.out.println("Updated blockstates");
    }

    /**
     * Simple JSON serialization without external dependencies.
     */
    @SuppressWarnings("unchecked")
    private String mapToJson(Map<String, Object> map) {
        StringBuilder sb = new StringBuilder();
        sb.append("{\n");
        boolean first = true;
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            if (!first) sb.append(",\n");
            first = false;
            sb.append("  \"").append(entry.getKey()).append("\": ");
            Object value = entry.getValue();
            if (value instanceof Map) {
                sb.append(mapToJsonInline((Map<String, Object>) value));
            } else if (value instanceof String) {
                sb.append("\"").append(value).append("\"");
            } else {
                sb.append(value);
            }
        }
        sb.append("\n}");
        return sb.toString();
    }

    @SuppressWarnings("unchecked")
    private String mapToJsonInline(Map<String, Object> map) {
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        boolean first = true;
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            if (!first) sb.append(", ");
            first = false;
            sb.append("\"").append(entry.getKey()).append("\": ");
            Object value = entry.getValue();
            if (value instanceof Map) {
                sb.append(mapToJsonInline((Map<String, Object>) value));
            } else if (value instanceof String) {
                sb.append("\"").append(value).append("\"");
            } else {
                sb.append(value);
            }
        }
        sb.append("}");
        return sb.toString();
    }
}
