package powercrystals.minefactoryreloaded.core;

import buildcraft.api.transport.IPipeTile;

import cofh.api.tileentity.IItemDuct;
import cofh.asm.relauncher.Strippable;
import cofh.lib.inventory.IInventoryManager;
import cofh.lib.inventory.InventoryManager;
import cofh.lib.util.helpers.ItemHelper;
import net.minecraft.block.state.IBlockState;
import net.minecraft.item.Item;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;

import net.minecraft.block.Block;
import net.minecraft.block.BlockChest;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityChest;
import net.minecraft.world.World;
import net.minecraft.util.EnumFacing;

public abstract class UtilInventory
{
	/**
	 * Searches from position x, y, z, checking for TE-compatible pipes in all directions.
	 *
	 * @return Map<EnumFacing, IItemDuct> specifying all found pipes and their directions.
	 */
	public static Map<EnumFacing, IItemDuct> findConduits(World world, BlockPos pos)
	{
	return findConduits(world, pos, EnumFacing.VALUES);
	}

	/**
	 * Searches from position x, y, z, checking for TE-compatible pipes in each directiontocheck.
	 *
	 * @return Map<EnumFacing, IItemDuct> specifying all found pipes and their directions.
	 */
	public static Map<EnumFacing, IItemDuct> findConduits(World world, BlockPos pos,
			EnumFacing[] directionstocheck)
	{
		Map<EnumFacing, IItemDuct> pipes = new LinkedHashMap<EnumFacing, IItemDuct>();
		for (EnumFacing direction : directionstocheck)
		{
			TileEntity te = world.getTileEntity(pos.offset(direction));
			if (te instanceof IItemDuct)
			{
				pipes.put(direction, (IItemDuct) te);
			}
		}
		return pipes;
	}

	/**
	 * Searches from position x, y, z, checking for BC-compatible pipes in all directions.
	 *
	 * @return Map<EnumFacing, IPipeTile> specifying all found pipes and their directions.
	 */
	@Strippable(pipeClass)
	public static Map<EnumFacing, IPipeTile> findPipes(World world, BlockPos pos)
	{
		return findPipes(world, pos, EnumFacing.VALUES);
	}

	/**
	 * Searches from position x, y, z, checking for BC-compatible pipes in each directiontocheck.
	 *
	 * @return Map<EnumFacing, IPipeTile> specifying all found pipes and their directions.
	 */
	@Strippable(pipeClass)
	public static Map<EnumFacing, IPipeTile> findPipes(World world, BlockPos pos,
			EnumFacing[] directionstocheck)
	{
		Map<EnumFacing, IPipeTile> pipes = new LinkedHashMap<EnumFacing, IPipeTile>();
		for (EnumFacing direction : directionstocheck)
		{
			TileEntity te = world.getTileEntity(pos.offset(direction));
			if (te instanceof IPipeTile)
			{
				pipes.put(direction, (IPipeTile) te);
			}
		}
		return pipes;
	}

	/**
	 * Searches from position x, y, z, checking for inventories in all directions.
	 *
	 * @return Map<EnumFacing, IInventory> specifying all found inventories and their directions.
	 */
	public static Map<EnumFacing, IInventory> findChests(World world, BlockPos pos)
	{
		return findChests(world, pos, EnumFacing.VALUES);
	}

	/**
	 * Searches from position x, y, z, checking for inventories in each directiontocheck.
	 *
	 * @return Map<EnumFacing, IInventory> specifying all found inventories and their directions.
	 */
	public static Map<EnumFacing, IInventory> findChests(World world, BlockPos pos,
			EnumFacing[] directionstocheck)
	{
		Map<EnumFacing, IInventory> chests = new LinkedHashMap<EnumFacing, IInventory>();
		for (EnumFacing direction : directionstocheck)
		{
			BlockPos chestPos = pos.offset(direction);
			TileEntity te = world.getTileEntity(chestPos);
			if (te != null && te instanceof IInventory)
			{
				chests.put(direction, checkForDoubleChest(world, te, chestPos));
			}
		}
		return chests;
	}

	private static IInventory checkForDoubleChest(World world, TileEntity te, BlockPos chestloc)
	{
		Block block = world.getBlockState(chestloc).getBlock();
		if (block instanceof BlockChest && te instanceof TileEntityChest) {
			return ((BlockChest) block).getContainer(world, chestloc, true);
		}
		return ((IInventory)te);
	}

	/**
	 * Drops an ItemStack, checking all directions for pipes > chests. DOESN'T drop items into the world.
	 * Example of this behavior: Cargo dropoff rail, item collector.
	 *
	 * @return The remainder of the ItemStack. Whatever -wasn't- successfully dropped.
	 */
	public static ItemStack dropStack(TileEntity from, ItemStack stack)
	{
		return dropStack(from.getWorld(), from.getPos(),
				stack, EnumFacing.VALUES, null);
	}

	/**
	 * Drops an ItemStack, checking all directions for pipes > chests. Drops items into the world.
	 * Example of this behavior: Harvesters, sludge boilers, etc.
	 *
	 * @param airdropdirection
	 *            the direction that the stack may be dropped into air.
	 * @return The remainder of the ItemStack. Whatever -wasn't- successfully dropped.
	 */
	public static ItemStack dropStack(TileEntity from, ItemStack stack, EnumFacing airdropdirection)
	{
		return dropStack(from.getWorld(), from.getPos(),
				stack, EnumFacing.VALUES, airdropdirection);
	}

	/**
	 * Drops an ItemStack, into chests > pipes > the world, but only in a single direction.
	 * Example of this behavior: Item Router, Ejector
	 *
	 * @param dropdirection
	 *            a -single- direction in which to check for pipes/chests
	 * @param airdropdirection
	 *            the direction that the stack may be dropped into air.
	 * @return The remainder of the ItemStack. Whatever -wasn't- successfully dropped.
	 */
	public static ItemStack dropStack(TileEntity from, ItemStack stack, EnumFacing dropdirection,
			EnumFacing airdropdirection)
	{
		EnumFacing[] dropdirections = { dropdirection };
		return dropStack(from.getWorld(), from.getPos(),
				stack, dropdirections, airdropdirection);
	}

	/**
	 * Drops an ItemStack, checks pipes > chests > world in that order.
	 *
	 * @param from
	 *            the TileEntity doing the dropping
	 * @param stack
	 *            the ItemStack being dropped
	 * @param dropdirections
	 *            directions in which stack may be dropped into chests or pipes
	 * @param airdropdirection
	 *            the direction that the stack may be dropped into air.
	 *            null or other invalid directions indicate that stack shouldn't be
	 *            dropped into the world.
	 * @return The remainder of the ItemStack. Whatever -wasn't- successfully dropped.
	 */
	public static ItemStack dropStack(TileEntity from, ItemStack stack, EnumFacing[] dropdirections,
			EnumFacing airdropdirection)
	{
		return dropStack(from.getWorld(), from.getPos(),
				stack, dropdirections, airdropdirection);
	}

	/**
	 * Drops an ItemStack, checks pipes > chests > world in that order. It generally shouldn't be necessary to call this explicitly.
	 *
	 * @param world
	 *            the worldObj
	 * @param pos
	 *            the BlockPos to drop from
	 * @param stack
	 *            the ItemStack being dropped
	 * @param dropdirections
	 *            directions in which stack may be dropped into chests or pipes
	 * @param airdropdirection
	 *            the direction that the stack may be dropped into air.
	 *             null or other invalid directions indicate that stack shouldn't be
	 *            dropped into the world.
	 * @return The remainder of the ItemStack. Whatever -wasn't- successfully dropped.
	 */
	public static ItemStack dropStack(World world, BlockPos pos, ItemStack stack,
			EnumFacing[] dropdirections, EnumFacing airdropdirection)
	{
		// (0) Sanity check. Don't bother dropping if there's nothing to drop, and never try to drop items on the client.
		if (world.isRemote | stack == null || stack.stackSize == 0 || stack.getItem() == null)
			return null;

		stack = stack.copy();
		// (0.5) Try to put stack in conduits that are in valid directions
		for (Entry<EnumFacing, IItemDuct> pipe : findConduits(world, pos, dropdirections).entrySet())
		{
			EnumFacing from = pipe.getKey().getOpposite();
			stack = pipe.getValue().insertItem(from, stack);
			if (stack == null || stack.stackSize <= 0)
			{
				return null;
			}
		}
		// (1) Try to put stack in pipes that are in valid directions
		if (handlePipeTiles) {
			stack = handleIPipeTile(world, pos, dropdirections, stack);
			if (stack == null || stack.stackSize <= 0)
			{
				return null;
			}
		}
		// (2) Try to put stack in chests that are in valid directions
		for (Entry<EnumFacing, IInventory> chest : findChests(world, pos, dropdirections).entrySet())
		{
			IInventoryManager manager = InventoryManager.create(chest.getValue(), chest.getKey().getOpposite());
			stack = manager.addItem(stack);
			if (stack == null || stack.stackSize <= 0)
			{
				return null;
			}
		}
		// (3) Having failed to put it in a chest or a pipe, drop it in the air if airdropdirection is a valid direction.
		if (MFRUtil.VALID_DIRECTIONS.contains(airdropdirection) && isAirDrop(world, pos.offset(airdropdirection)))
		{
			dropStackInAir(world, pos, stack, airdropdirection);
			return null;
		}
		// (4) Is the stack still here? :( Better give it back.
		return stack;
	}

	public static boolean isAirDrop(World world, BlockPos pos)
	{
		IBlockState state = world.getBlockState(pos);
		Block block = state.getBlock();
		if (world.isAirBlock(pos))
			return true;
		return block.getCollisionBoundingBox(state, world, pos) == null;
	}

	@SuppressWarnings("deprecation")
	private static ItemStack handleIPipeTile(World world, BlockPos pos, EnumFacing[] dropdirections, ItemStack stack)
	{
		for (Entry<EnumFacing, IPipeTile> pipe : findPipes(world, pos, dropdirections).entrySet())
		{
			EnumFacing from = pipe.getKey().getOpposite();
			if (pipe.getValue().isPipeConnected(from))
			{
				ItemStack returnedStack = pipe.getValue().injectItem(stack.copy(), false, from, null, 0);
				if (returnedStack == null || returnedStack.stackSize < stack.stackSize)
				{
					stack = pipe.getValue().injectItem(stack.copy(), true, from, null, 0);
					if (stack != null && stack.stackSize <= 0)
					{
						return null;
					}
				}
			}
		}
		return stack;
	}

	public static void dropStackInAir(World world, BlockPos pos, ItemStack stack) {
		dropStackInAir(world, pos, stack, null);
	}

	public static void dropStackInAir(World world, BlockPos pos, ItemStack stack, int delay) {
		dropStackInAir(world, pos, stack, delay, null);
	}

	public static void dropStackInAir(World world, BlockPos pos, ItemStack stack, EnumFacing towards) {
		dropStackInAir(world, pos, stack, 20, towards);
	}

	public static void dropStackInAir(World world, Entity entity, ItemStack stack) {
		dropStackInAir(world, entity, stack, null);
	}

	public static void dropStackInAir(World world, Entity entity, ItemStack stack, int delay) {
		dropStackInAir(world, entity, stack, delay, null);
	}

	public static void dropStackInAir(World world, Entity entity, ItemStack stack, EnumFacing towards) {
		dropStackInAir(world, entity, stack, 20, towards);
	}

	public static void dropStackInAir(World world, Entity entity, ItemStack stack, int delay, EnumFacing towards) {
		dropStackInAir(world, entity.getPosition(), stack, delay, towards);
	}

	public static void dropStackInAir(World world, BlockPos pos, ItemStack stack,
			int delay, EnumFacing towards)
	{
		if (stack == null) return;

		double dropOffsetX = 0.0F;
		double dropOffsetY = 0.0F;
		double dropOffsetZ = 0.0F;

		if (towards == null) {
			float f = 0.3F;
			dropOffsetX = world.rand.nextFloat() * f + (1.0D - f) * 0.5D;
			dropOffsetY = world.rand.nextFloat() * f + (1.0D - f) * 0.5D;
			dropOffsetZ = world.rand.nextFloat() * f + (1.0D - f) * 0.5D;
		} else {
			switch (towards)
			{
				case UP:
					dropOffsetX = 0.5F;
					dropOffsetY = 1.5F;
					dropOffsetZ = 0.5F;
					break;
				case DOWN:
					dropOffsetX = 0.5F;
					dropOffsetY = -0.75F;
					dropOffsetZ = 0.5F;
					break;
				case NORTH:
					dropOffsetX = 0.5F;
					dropOffsetY = 0.5F;
					dropOffsetZ = -0.5F;
					break;
				case SOUTH:
					dropOffsetX = 0.5F;
					dropOffsetY = 0.5F;
					dropOffsetZ = 1.5F;
					break;
				case EAST:
					dropOffsetX = 1.5F;
					dropOffsetY = 0.5F;
					dropOffsetZ = 0.5F;
					break;
				case WEST:
					dropOffsetX = -0.5F;
					dropOffsetY = 0.5F;
					dropOffsetZ = 0.5F;
					break;
			}
		}


		EntityItem entityitem = new EntityItem(world, pos.getX() + dropOffsetX, pos.getY() + dropOffsetY, pos.getZ() + dropOffsetZ, stack.copy());
		if (towards != null) {
			entityitem.motionX = 0.0D;
			if (towards != EnumFacing.DOWN)
				entityitem.motionY = 0.3D;
			entityitem.motionZ = 0.0D;
		}
		entityitem.setPickupDelay(delay);
		world.spawnEntityInWorld(entityitem);
	}

	public static ItemStack consumeItem(ItemStack stack, EntityPlayer player)
	{
		return ItemHelper.consumeItem(stack, player);
	}

	public static void mergeStacks(ItemStack to, ItemStack from)
	{
		if (!stacksEqual(to, from))
			return;

		int amountToCopy = Math.min(to.getMaxStackSize() - to.stackSize, from.stackSize);
		to.stackSize += amountToCopy;
		from.stackSize -= amountToCopy;
	}

	public static boolean stacksEqual(ItemStack s1, ItemStack s2)
	{
		return stacksEqual(s1, s2, true);
	}

	public static boolean stacksEqual(ItemStack s1, ItemStack s2, boolean nbtSensitive)
	{
		if (s1 == null | s2 == null) return false;
		if (!s1.isItemEqual(s2)) return false;
		if (!nbtSensitive) return true;

		if (s1.getTagCompound() == s2.getTagCompound()) return true;
		if (s1.getTagCompound() == null || s2.getTagCompound() == null) return false;
		return s1.getTagCompound().equals(s2.getTagCompound());
	}

	public static boolean stackIsItem(ItemStack stack, Item item) {

		return stack != null && stack.getItem() == item;
	}

	private static boolean handlePipeTiles = false;
	private static final String pipeClass = "buildcraft.api.transport.IPipeTile";
	static {
		try {
			Class.forName(pipeClass);
			handlePipeTiles = true;
		} catch(Throwable _) {}
	}

	public static boolean playerHasItem(EntityPlayer player, Item item) {
		for (int i = 0; i < player.inventory.getSizeInventory(); ++i)
		{
			if (stackIsItem(player.inventory.getStackInSlot(i), item))
				return true;
		}
		return false;
	}

	public static ItemStack findItem(EntityPlayer player, Item item)
	{
		if (stackIsItem(player.getHeldItem(EnumHand.OFF_HAND), item))
		{
			return player.getHeldItem(EnumHand.OFF_HAND);
		}
		else if (stackIsItem(player.getHeldItem(EnumHand.MAIN_HAND), item))
		{
			return player.getHeldItem(EnumHand.MAIN_HAND);
		}
		else
		{
			for (int i = 0; i < player.inventory.getSizeInventory(); ++i)
			{
				ItemStack itemstack = player.inventory.getStackInSlot(i);

				if (stackIsItem(itemstack, item))
				{
					return itemstack;
				}
			}

			return null;
		}
	}
}
