package powercrystals.minefactoryreloaded.processing;

import powercrystals.minefactoryreloaded.MineFactoryReloadedCore;

public class TileEntityOilFabricator extends TileEntityLiquidFabricator
{
	public TileEntityOilFabricator()
	{
		super(MineFactoryReloadedCore.oilLiquidId, 200, 20);
	}

	@Override
	public String getInvName()
	{
		return "Oil Fabricator";
	}

}
