package powercrystals.minefactoryreloaded.block;

import cofh.lib.util.helpers.ItemHelper;
import net.minecraft.block.BlockHorizontal;
import net.minecraft.block.properties.PropertyDirection;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraft.util.EnumFacing;

import powercrystals.minefactoryreloaded.MineFactoryReloadedCore;
import powercrystals.minefactoryreloaded.api.rednet.IRedNetInfo;
import powercrystals.minefactoryreloaded.api.rednet.IRedNetOmniNode;
import powercrystals.minefactoryreloaded.api.rednet.connectivity.RedNetConnectionType;
import powercrystals.minefactoryreloaded.core.MFRUtil;
import powercrystals.minefactoryreloaded.item.ItemLogicUpgradeCard;
import powercrystals.minefactoryreloaded.item.tool.ItemRedNetMemoryCard;
import powercrystals.minefactoryreloaded.item.tool.ItemRedNetMeter;
import powercrystals.minefactoryreloaded.tile.rednet.TileEntityRedNetLogic;

import java.util.List;

public class BlockRedNetLogic extends BlockFactory implements IRedNetOmniNode, IRedNetInfo {

	public static final PropertyDirection FACING = BlockHorizontal.FACING;

	private int[] _sideRemap = new int[] { 3, 1, 2, 0 };

	public BlockRedNetLogic() {

		super(0.8F);
		setUnlocalizedName("mfr.rednet.logic");
	}

	@Override
	public IBlockState onBlockPlaced(World worldIn, BlockPos pos, EnumFacing facing, float hitX, float hitY, float hitZ, int meta, EntityLivingBase placer) {

		return getDefaultState().withProperty(FACING, placer.getHorizontalFacing().getOpposite());
	}

	@Override
	public boolean hasTileEntity() {
		return true;
	}

	@Override
	public TileEntity createTileEntity(World world, IBlockState state) {
		return new TileEntityRedNetLogic();
	}

	@Override
	public int damageDropped(IBlockState state) {

		return 0;
	}

	@Override
	public RedNetConnectionType getConnectionType(World world, BlockPos pos, EnumFacing side) {

		TileEntityRedNetLogic logic = (TileEntityRedNetLogic) world.getTileEntity(pos);
		if (logic != null && side.getAxis().isHorizontal()) {
			if (world.getBlockState(pos).getValue(FACING) == side) {
				return RedNetConnectionType.None;
			}
		}
		return RedNetConnectionType.CableAll;
	}

	@Override
	public int getOutputValue(World world, BlockPos pos, EnumFacing side, int subnet) {

		TileEntityRedNetLogic logic = (TileEntityRedNetLogic) world.getTileEntity(pos);
		if (logic != null) {
			return logic.getOutputValue(side, subnet);
		} else {
			return 0;
		}
	}

	@Override
	public int[] getOutputValues(World world, BlockPos pos, EnumFacing side) {

		TileEntityRedNetLogic logic = (TileEntityRedNetLogic) world.getTileEntity(pos);
		if (logic != null) {
			return logic.getOutputValues(side);
		} else {
			return new int[16];
		}
	}

	@Override
	public void onInputsChanged(World world, BlockPos pos, EnumFacing side, int[] inputValues) {

		TileEntityRedNetLogic logic = (TileEntityRedNetLogic) world.getTileEntity(pos);
		if (logic != null) {
			logic.onInputsChanged(side, inputValues);
		}
	}

	@Override
	public void onInputChanged(World world, BlockPos pos, EnumFacing side, int inputValue) {

	}

	@Override
	public boolean activated(World world, BlockPos pos, EntityPlayer player, EnumFacing side, EnumHand hand, ItemStack heldItem) {

		if (MFRUtil.isHoldingUsableTool(player, pos)) {
			if (rotateBlock(world, pos, side)) {
				MFRUtil.usedWrench(player, pos);
				return true;
			}
		}

		if (MFRUtil.isHolding(player, ItemLogicUpgradeCard.class)) {
			TileEntityRedNetLogic logic = (TileEntityRedNetLogic) world.getTileEntity(pos);
			if (logic != null) {
				if (logic.insertUpgrade(heldItem.getItemDamage() + 1)) {
					if (!player.capabilities.isCreativeMode) {
						ItemHelper.consumeItem(heldItem, player);
					}
					return true;
				}
			}
			return false;
		} else if (!MFRUtil.isHolding(player, ItemRedNetMeter.class) && !MFRUtil.isHolding(player, ItemRedNetMemoryCard.class)) {
			if (!world.isRemote) {
				player.openGui(MineFactoryReloadedCore.instance(), 0, world, pos.getX(), pos.getY(), pos.getZ());
			}
			return true;
		}
		return false;
	}

/*
	@Override
	@SideOnly(Side.CLIENT)
	public void registerBlockIcons(IIconRegister ir) {

		blockIcon = ir.registerIcon("minefactoryreloaded:" + getUnlocalizedName());
		RedNetLogicRenderer.updateUVT(blockIcon);
	}
*/

	@Override
	public boolean isOpaqueCube(IBlockState state) {

		return false;
	}

	@Override
	public boolean isSideSolid(IBlockState state, IBlockAccess world, BlockPos pos, EnumFacing side) {

		return !side.getAxis().isHorizontal() || side != state.getValue(FACING);
	}

	@Override
	public void getRedNetInfo(IBlockAccess world, BlockPos pos, EnumFacing side,
			EntityPlayer player, List<ITextComponent> info) {

		TileEntity te = world.getTileEntity(pos);
		if (te instanceof TileEntityRedNetLogic) {
			int value;
			int foundNonZero = 0;
			for (int i = 0; i < ((TileEntityRedNetLogic) te).getBufferLength(13); i++) {
				value = ((TileEntityRedNetLogic) te).getVariableValue(i);

				if (value != 0) {
					info.add(new TextComponentTranslation("chat.info.mfr.rednet.meter.varprefix")
							.appendText(" " + i + ": " + value));
					++foundNonZero;
				}
			}

			if (foundNonZero == 0) {
				info.add(new TextComponentTranslation("chat.info.mfr.rednet.meter.var.allzero"));
			} else if (foundNonZero < 16) {
				info.add(new TextComponentTranslation("chat.info.mfr.rednet.meter.var.restzero"));
			}
		}
	}

}
