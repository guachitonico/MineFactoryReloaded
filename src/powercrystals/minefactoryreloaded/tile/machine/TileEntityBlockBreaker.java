package powercrystals.minefactoryreloaded.tile.machine;

import net.minecraft.block.state.IBlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.List;

import net.minecraft.block.Block;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

import powercrystals.minefactoryreloaded.gui.client.GuiFactoryInventory;
import powercrystals.minefactoryreloaded.gui.client.GuiFactoryPowered;
import powercrystals.minefactoryreloaded.gui.container.ContainerFactoryPowered;
import powercrystals.minefactoryreloaded.setup.MFRConfig;
import powercrystals.minefactoryreloaded.setup.Machine;
import powercrystals.minefactoryreloaded.tile.base.TileEntityFactoryPowered;

public class TileEntityBlockBreaker extends TileEntityFactoryPowered
{
	protected BlockPos breakPos;
	public TileEntityBlockBreaker()
	{
		super(Machine.BlockBreaker);
		setManageSolids(true);
		setCanRotate(true);
	}

	@Override
	protected void onRotate()
	{
		breakPos = pos.offset(getDirectionFacing());
		super.onRotate();
	}

	@Override
	public void onNeighborBlockChange()
	{
		if (breakPos != null && !worldObj.isAirBlock(breakPos))
			setIdleTicks(0);
	}

	@Override
	public boolean activateMachine()
	{
		World worldObj = this.worldObj;
		IBlockState state = worldObj.getBlockState(breakPos);
		Block block = state.getBlock();

		if (!block.isAir(state, worldObj, breakPos) &&
				!state.getMaterial().isLiquid() &&
				state.getBlockHardness(worldObj, breakPos) >= 0)
		{
			List<ItemStack> drops = block.getDrops(worldObj, breakPos, state, 0);
			if (worldObj.setBlockToAir(breakPos))
			{
				doDrop(drops);
				if (MFRConfig.playSounds.getBoolean(true))
					worldObj.playEvent(null, 2001, breakPos, Block.getStateId(state));
			}
			return true;
		}
		setIdleTicks(getIdleTicksMax());
		return false;
	}

	@Override
	@SideOnly(Side.CLIENT)
	public GuiFactoryInventory getGui(InventoryPlayer inventoryPlayer)
	{
		return new GuiFactoryPowered(getContainer(inventoryPlayer), this);
	}

	@Override
	public ContainerFactoryPowered getContainer(InventoryPlayer inventoryPlayer)
	{
		return new ContainerFactoryPowered(this, inventoryPlayer);
	}

	@Override
	public int getWorkMax()
	{
		return 1;
	}

	@Override
	public int getIdleTicksMax()
	{
		return 60;
	}

	@Override
	public int getSizeInventory()
	{
		return 0;
	}
}
