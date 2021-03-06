package powercrystals.minefactoryreloaded.item.tool;

import java.util.Map;

import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.EnumAction;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.RayTraceResult.Type;
import net.minecraft.world.World;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidTankInfo;
import net.minecraftforge.fluids.IFluidHandler;

import powercrystals.minefactoryreloaded.MFRRegistry;
import powercrystals.minefactoryreloaded.api.ILiquidDrinkHandler;
import powercrystals.minefactoryreloaded.item.base.ItemFactoryTool;

public class ItemStraw extends ItemFactoryTool {

	@Override
	public ItemStack onItemUseFinish(ItemStack stack, World world, EntityLivingBase entity) {

		if (!world.isRemote || !(entity instanceof EntityPlayer)) {
			EntityPlayer player = (EntityPlayer) entity;
			RayTraceResult result = rayTrace(world, player, true);
			Map<String, ILiquidDrinkHandler> map = MFRRegistry.getLiquidDrinkHandlers();
			if (result != null && result.typeOfHit == Type.BLOCK) {
				BlockPos pos = result.getBlockPos();
				IBlockState state = world.getBlockState(pos);
				Block block = state.getBlock();
				Fluid fluid = FluidRegistry.lookupFluidForBlock(block);
				if (fluid != null && map.containsKey(fluid.getName())) {
					map.get(fluid.getName()).onDrink(player);
					world.setBlockToAir(pos);
				} else if (block.hasTileEntity(state)) {
					TileEntity tile = world.getTileEntity(pos);
					if (tile instanceof IFluidHandler) {
						IFluidHandler handler = (IFluidHandler) tile;
						FluidTankInfo[] info = handler.getTankInfo(result.sideHit);
						for (int i = info.length; i-- > 0;) {
							FluidStack fstack = info[i].fluid;
							if (fstack != null) {
								fluid = fstack.getFluid();
								if (fluid != null && map.containsKey(fluid.getName()) && fstack.amount >= 1000) {
									fstack = fstack.copy();
									fstack.amount = 1000;
									FluidStack r = handler.drain(result.sideHit, fstack.copy(), false);
									if (r != null && r.amount >= 1000) {
										map.get(fluid.getName()).onDrink(player);
										handler.drain(result.sideHit, fstack, true);
										break;
									}
								}
							}
						}
					}
				}
			}
		}

		return stack;
	}

	@Override
	public int getMaxItemUseDuration(ItemStack stack) {

		return 32;
	}

	@Override
	public EnumAction getItemUseAction(ItemStack stack) {

		return EnumAction.DRINK;
	}

	@Override
	public ActionResult<ItemStack> onItemRightClick(ItemStack stack, World world, EntityPlayer player, EnumHand hand) {

		RayTraceResult result = rayTrace(world, player, true);
		Map<String, ?> map = MFRRegistry.getLiquidDrinkHandlers();
		if (result != null && result.typeOfHit == Type.BLOCK) {
			BlockPos pos = result.getBlockPos();
			IBlockState state = world.getBlockState(pos);
			Block block = state.getBlock();
			Fluid fluid = FluidRegistry.lookupFluidForBlock(block);
			if (fluid != null && map.containsKey(fluid.getName())) {
				player.setActiveHand(hand);
			} else if (block.hasTileEntity(state)) {
				TileEntity tile = world.getTileEntity(pos);
				if (tile instanceof IFluidHandler) {
					IFluidHandler handler = (IFluidHandler) tile;
					FluidTankInfo[] info = handler.getTankInfo(result.sideHit);
					for (int i = info.length; i-- > 0;) {
						FluidStack fstack = info[i].fluid;
						if (fstack != null) {
							fluid = fstack.getFluid();
							if (fluid != null && map.containsKey(fluid.getName()) && fstack.amount >= 1000) {
								FluidStack r = handler.drain(result.sideHit, fstack, false);
								if (r != null && r.amount >= 1000) {
									player.setActiveHand(hand);
									break;
								}
							}
						}
					}
				}
			}
		}
		return new ActionResult<ItemStack>(EnumActionResult.SUCCESS, stack);
	}

}
