package powercrystals.minefactoryreloaded.block;

import static powercrystals.minefactoryreloaded.item.base.ItemMulti.getName;

import java.util.List;

import net.minecraft.block.Block;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;

import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import powercrystals.minefactoryreloaded.core.MFRUtil;

public class ItemBlockFactory extends ItemBlock
{
	protected String[] _names = {null};
	protected int metaOffset = 0;

	public ItemBlockFactory(Block block)
	{
		super(block);
		setMaxDamage(0);
	}

	public ItemBlockFactory(Block block, String[] names)
	{
		this(block);
		setNames(names);
	}

/* TODO doesn't seem to be used - remove
	public ItemBlockFactory(Block p_i45328_1_, Integer metaOffset, String[] names)
	{
		this(p_i45328_1_, names);
		this.metaOffset = metaOffset.intValue();
	}
*/
	public ItemBlockFactory(Block block, Integer metaOffset)
	{
		this(block);
		this.metaOffset = metaOffset.intValue();
	}

	protected void setNames(String[] names)
	{
		_names = names;
		setHasSubtypes(true);
	}

	protected String name(ItemStack stack)
	{
		return _names[Math.min(stack.getItemDamage(), _names.length - 1)];
	}

	@Override
	public int getMetadata(int meta)
	{
		return (metaOffset + meta) & 15;
	}

	@Override
	public String getUnlocalizedName(ItemStack stack)
	{
		return getName(getUnlocalizedName(), name(stack));
	}

	@Override
	@SideOnly(Side.CLIENT)
	public void addInformation(ItemStack stack, EntityPlayer player, List<String> info, boolean adv)
	{
		String str = getName("tip.info" + getUnlocalizedName().substring(4), name(stack));
		str = MFRUtil.localize(str, true, null);
		if (str != null)
			info.add(str);
	}

	public void getSubItems(Item itemId, List<ItemStack> subTypes)
	{
		for(int i = 0; i < _names.length; i++)
		{
			subTypes.add(new ItemStack(itemId, 1, i));
		}
	}

	@Override
	public void getSubItems(Item item, CreativeTabs creativeTab, List<ItemStack> subTypes)
	{
		getSubItems(item, subTypes);
	}
}
