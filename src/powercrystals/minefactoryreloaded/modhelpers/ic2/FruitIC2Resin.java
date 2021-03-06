package powercrystals.minefactoryreloaded.modhelpers.ic2;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import ic2.core.block.BlockRubWood;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import powercrystals.minefactoryreloaded.api.FertilizerType;
import powercrystals.minefactoryreloaded.api.IFactoryFertilizable;
import powercrystals.minefactoryreloaded.api.IFactoryFruit;
import powercrystals.minefactoryreloaded.api.ReplacementBlock;

public class FruitIC2Resin implements IFactoryFruit, IFactoryFertilizable
{
	private Block _rubberWood;
	private ItemStack _resin;
	private ReplacementBlock _repl;

	public FruitIC2Resin(ItemStack rubberWood, ItemStack resin)
	{
		this._rubberWood = ((ItemBlock)rubberWood.getItem()).getBlock();
		this._resin = resin;
		_repl = new ReplacementBlock(_rubberWood) {
			@Override
			protected int getMeta(World world, BlockPos pos, ItemStack stack)
			{
				return world.getBlockState(pos).getValue(BlockRubWood.stateProperty).getDry().ordinal();
			}
		};
	}

	@Override
	public Block getPlant()
	{
		return _rubberWood;
	}

	@Override
	public boolean canFertilize(World world, BlockPos pos, FertilizerType fertilizerType)
	{
		if (fertilizerType == FertilizerType.Grass)
			return false;
		IBlockState state = world.getBlockState(pos);
		Block block = state.getBlock();
		BlockRubWood.RubberWoodState woodState = state.getValue(BlockRubWood.stateProperty);
		return block.equals(_rubberWood) && !woodState.isPlain() && !woodState.wet;
	}

	@Override
	public boolean fertilize(World world, Random rand, BlockPos pos, FertilizerType fertilizerType)
	{
		IBlockState state = world.getBlockState(pos);
		return world.setBlockState(pos, state.withProperty(BlockRubWood.stateProperty, state.getValue(BlockRubWood.stateProperty).getWet()), 2);
	}

	@Override
	public boolean canBePicked(World world, BlockPos pos)
	{
		IBlockState state = world.getBlockState(pos);
		Block block = state.getBlock();
		return block.equals(_rubberWood) && state.getValue(BlockRubWood.stateProperty).wet;
	}

	@Override
	public boolean breakBlock()
	{
		return false;
	}

	@Override
	public ReplacementBlock getReplacementBlock(World world, BlockPos pos)
	{
		return _repl;
	}

	@Override
	public void prePick(World world, BlockPos pos)
	{

	}

	@Override
	public List<ItemStack> getDrops(World world, Random rand, BlockPos pos)
	{
		ArrayList<ItemStack> list = new ArrayList<ItemStack>();
		ItemStack a = _resin.copy();
		a.stackSize = 1 + rand.nextInt(3);
		list.add(a);
		return list;
	}

	@Override
	public void postPick(World world, BlockPos pos)
	{

	}

}
