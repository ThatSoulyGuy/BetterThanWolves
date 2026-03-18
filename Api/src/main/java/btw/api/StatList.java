package btw.api;

import java.util.ArrayList;
import java.util.List;

public class StatList {
    public static List allStats = new ArrayList();
    public static List generalStats = new ArrayList();
    public static List itemStats = new ArrayList();
    public static List objectMineStats = new ArrayList();

    public static StatBase[] mineBlockStatArray = new StatBase[4096];
    public static StatBase[] objectUseStats = new StatBase[32000];
    public static StatBase[] objectBreakStats = new StatBase[32000];
    public static StatBase[] objectCraftStats = new StatBase[32000];

    public static StatBase startGameStat;
    public static StatBase createWorldStat;
    public static StatBase loadWorldStat;
    public static StatBase joinMultiplayerStat;
    public static StatBase leaveGameStat;
    public static StatBase minutesPlayedStat;
    public static StatBase distWalkedStat;
    public static StatBase distSwumStat;
    public static StatBase fallOneCmStat;
    public static StatBase climbOneCmStat;
    public static StatBase flyOneCmStat;
    public static StatBase diveOneCmStat;
    public static StatBase minecartOneCmStat;
    public static StatBase boatOneCmStat;
    public static StatBase pigOneCmStat;
    public static StatBase jumpStat;
    public static StatBase dropStat;
    public static StatBase damageDealtStat;
    public static StatBase damageTakenStat;
    public static StatBase deathsStat;
    public static StatBase mobKillsStat;
    public static StatBase playerKillsStat;
    public static StatBase fishCaughtStat;
}
