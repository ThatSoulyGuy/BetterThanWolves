package btw.modern;

// 1.5.2 vanilla/server VillageAgressor.java — verbatim port; internal bookkeeping for
// Village.addOrRenewAgressor / findNearestVillageAggressor / removeDeadAndOldAgressors
class VillageAgressor
{
    public EntityLiving agressor;
    public int agressionTime;

    final Village villageObj;

    VillageAgressor(Village par1Village, EntityLiving par2EntityLiving, int par3)
    {
        this.villageObj = par1Village;
        this.agressor = par2EntityLiving;
        this.agressionTime = par3;
    }
}
