package powercrystals.minefactoryreloaded.block;

import net.minecraft.block.BlockLog;
import net.minecraft.block.properties.PropertyBool;
import net.minecraft.block.properties.PropertyEnum;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.Rotation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import powercrystals.minefactoryreloaded.api.rednet.connectivity.IRedNetDecorative;
import powercrystals.minefactoryreloaded.gui.MFRCreativeTab;
import powercrystals.minefactoryreloaded.setup.MFRThings;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class BlockRubberWood extends BlockLog implements IRedNetDecorative
{
	public static final PropertyBool RUBBER_FILLED = PropertyBool.create("rubber");

	public BlockRubberWood()
	{
		setUnlocalizedName("mfr.rubberwood.log");
		setCreativeTab(MFRCreativeTab.tab);
		setHarvestLevel("axe", 0);
		setDefaultState(blockState.getBaseState().withProperty(RUBBER_FILLED, true).withProperty(LOG_AXIS, EnumAxis.Y));
	}

	@Override
	protected BlockStateContainer createBlockState() {
		return new BlockStateContainer(this, LOG_AXIS, RUBBER_FILLED);
	}

	@Override
	public IBlockState getStateFromMeta(int meta) {

		IBlockState state = getDefaultState().withProperty(RUBBER_FILLED, (meta & 3) == 1);
		
		switch (meta & 12)
		{
			case 0:
				state = state.withProperty(LOG_AXIS, BlockLog.EnumAxis.Y);
				break;
			case 4:
				state = state.withProperty(LOG_AXIS, BlockLog.EnumAxis.X);
				break;
			case 8:
				state = state.withProperty(LOG_AXIS, BlockLog.EnumAxis.Z);
				break;
			default:
				state = state.withProperty(LOG_AXIS, BlockLog.EnumAxis.NONE);
		}
		
		return state;
	}

	@Override
	public int getMetaFromState(IBlockState state) {

		int meta = state.getValue(RUBBER_FILLED) ? 1 : 0;
		switch (state.getValue(LOG_AXIS))
		{
			case X:
				meta |= 4;
				break;
			case Z:
				meta |= 8;
				break;
			case NONE:
				meta |= 12;
		}
		
		return meta;
	}

	@Override
	public ArrayList<ItemStack> getDrops(IBlockAccess world, BlockPos pos, IBlockState state, int fortune)
	{
		ArrayList<ItemStack> drops = new ArrayList<ItemStack>();

		drops.add(new ItemStack(this, 1, 0));
		if(state.getValue(RUBBER_FILLED)) {
			Random rand = world instanceof World ? ((World)world).rand : RANDOM;
			drops.add(new ItemStack(MFRThings.rawRubberItem,
					fortune <= 0 ? 1 : 1 + rand.nextInt(fortune)));
		}

		return drops;
	}

	@Override
	public int getFireSpreadSpeed(IBlockAccess world, BlockPos pos, EnumFacing face)
	{
		return super.getFireSpreadSpeed(world, pos, face) * (world.getBlockState(pos).getValue(RUBBER_FILLED) ? 2 : 1);
	}

	@Override
	public int getFlammability(IBlockAccess world, BlockPos pos, EnumFacing face)
	{
		return super.getFlammability(world, pos, face) * (world.getBlockState(pos).getValue(RUBBER_FILLED) ? 2 : 1);
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	@SideOnly(Side.CLIENT)
	@Override
	public void getSubBlocks(Item blockId, CreativeTabs tab, List subBlocks)
	{
		subBlocks.add(new ItemStack(blockId, 1, 0));
		subBlocks.add(new ItemStack(blockId, 1, 1));
	}

	@Override
	protected boolean canSilkHarvest()
	{
		return false;
	}
}
