package powercrystals.minefactoryreloaded.setup;

import net.minecraftforge.fluids.UniversalBucket;
import net.minecraftforge.fml.common.IFuelHandler;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import powercrystals.minefactoryreloaded.core.MFRUtil;

public class MineFactoryReloadedFuelHandler implements IFuelHandler
{
	@Override
	public int getBurnTime(ItemStack fuel)
	{
		if (fuel == null)
			return 0;
		Item item = fuel.getItem();
		if (item.equals(MFRThings.rubberWoodItem))
		{
			return 350;
		}
		if (item.equals(MFRThings.rubberLeavesItem))
		{
			return 4 * (fuel.getItemDamage() + 1);
		}
		else if (item.equals(MFRThings.rubberSaplingItem))
		{
			return 130;
		}
		else if (item.equals(MFRThings.sugarCharcoalItem))
		{
			return 400;
		}
		else if (item.equals(MFRThings.factoryDecorativeBrickItem))
		{
			if (fuel.getItemDamage() == 15)
				return 4000;
		}
		else if (item.equals(MFRThings.rawRubberItem))
		{
			return 30;
		}
		else if (item.equals(MFRThings.rubberBarItem))
		{
			return 90;
		}
		else if (item instanceof UniversalBucket && ItemStack.areItemStacksEqual(MFRUtil.getBucketFor(MFRThings.biofuel), fuel))
		{
			return 22500;
		}

		return 0;
	}
}
