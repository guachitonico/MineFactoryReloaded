package powercrystals.minefactoryreloaded.tile.rednet;

import static powercrystals.minefactoryreloaded.block.transport.BlockRedNetCable.subSelection;
import static powercrystals.minefactoryreloaded.tile.base.TileEntityFactoryPowered.energyPerEU;
import static powercrystals.minefactoryreloaded.tile.rednet.RedstoneEnergyNetwork.TRANSFER_RATE;

import appeng.api.implementations.tiles.ICrankable;

import codechicken.lib.raytracer.IndexedCuboid6;
import codechicken.lib.vec.Vector3;
import cofh.api.energy.EnergyStorage;
import cofh.api.energy.IEnergyConnection;
import cofh.api.energy.IEnergyProvider;
import cofh.api.energy.IEnergyReceiver;
import cofh.api.energy.IEnergyTransport;
import cofh.asm.relauncher.Strippable;
import net.minecraft.network.play.server.SPacketUpdateTileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3i;
import net.minecraft.util.text.ITextComponent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.Arrays;
import java.util.List;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.NetworkManager;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;

import powercrystals.minefactoryreloaded.api.rednet.connectivity.RedNetConnectionType;
import powercrystals.minefactoryreloaded.core.IGridController;
import powercrystals.minefactoryreloaded.core.MFRUtil;
import powercrystals.minefactoryreloaded.net.Packets;

@Strippable({"appeng.api.implementations.tiles.ICrankable"})
public class TileEntityRedNetEnergy extends TileEntityRedNetCable implements
																	IEnergyTransport, ICrankable//, IEnergyInfo
{

	private byte[] sideMode = { 1, 1, 1, 1, 1, 1, 0 };
	private InterfaceType[] transportTypes = null;
	private IEnergyReceiver[] receiverCache = null;
	private IEnergyProvider[] providerCache = null;
	private boolean deadCache = false;
	private boolean readFromNBT = false;

	boolean isNode = false;
	int energyForGrid = 0;

	RedstoneEnergyNetwork _grid;

	public TileEntityRedNetEnergy() {

	}

	@Override
	public void validate() {

		super.validate();
		deadCache = true;
		receiverCache = null;
		providerCache = null;
		if (worldObj.isRemote)
			return;
		if (_grid == null) {
			incorporateTiles();
			if (_grid == null) {
				setGrid(new RedstoneEnergyNetwork(this));
			}
		}
		readFromNBT = true;
		reCache();

		//Packets.sendToAllPlayersWatching(this); //TODO likely just remove
	}

	@Override
	public void invalidate() {

		if (_grid != null) {
			removeFromGrid();
		}
		super.invalidate();
	}

	private void removeFromGrid() {

		_grid.removeConduit(this);
		_grid.storage.modifyEnergyStored(-energyForGrid);
		markForRegen();
		deadCache = true;
		_grid = null;
	}

	private void markForRegen() {

		int c = 0;
		for (int i = 6; i-- > 0;)
			if ((sideMode[i]) == 9)
				++c;
		if (c > 1)
			_grid.regenerate();
	}

	private void reCache() {

		if (deadCache) {
			for (EnumFacing dir : EnumFacing.VALUES)
				if (worldObj.isBlockLoaded(this.getPos().offset(dir)))
					addCache(MFRUtil.getTile(worldObj, this.getPos().offset(dir)));
			deadCache = false;
			RedstoneEnergyNetwork.HANDLER.addConduitForUpdate(this);
		}
	}

	private void incorporateTiles() {

		if (_grid == null) {
			for (EnumFacing dir : EnumFacing.VALUES) {
				if (readFromNBT && (sideMode[dir.getOpposite().ordinal()] & 1) == 0) continue;
				BlockPos offsetPos = pos.offset(dir);
				if (worldObj.isBlockLoaded(offsetPos)) {
					TileEntityRedNetEnergy pipe = MFRUtil.getTile(worldObj, pos.offset(dir), TileEntityRedNetEnergy.class);
					if (pipe != null) {
						if (pipe._grid != null && pipe.canInterface(this, dir)) {
							pipe._grid.addConduit(this);
						}
					}
				}
			}
		}
	}

	public boolean canInterface(TileEntityRedNetEnergy with, EnumFacing dir) {

		return (sideMode[dir.ordinal()] & 1) != 0;
	}

	@Override
	public void onNeighborTileChange(BlockPos neighborPos) {

		if (worldObj.isRemote | deadCache)
			return;
		TileEntity tile = worldObj.isBlockLoaded(neighborPos) ? worldObj.getTileEntity(neighborPos) : null;

		Vec3i diff = neighborPos.subtract(pos);
		addCache(tile, EnumFacing.getFacingFromVector(diff.getX(), diff.getY(), diff.getZ()));
	}

	private void addCache(TileEntity tile) {

		if (tile == null) return;
		Vec3i diff = tile.getPos().subtract(pos);
		addCache(tile, EnumFacing.getFacingFromVector(diff.getX(), diff.getY(), diff.getZ()));
	}

	private void addCache(TileEntity tile, EnumFacing side) {

		if (receiverCache != null)
			receiverCache[side.ordinal()] = null;
		if (providerCache != null)
			providerCache[side.ordinal()] = null;
		int lastMode = sideMode[side.ordinal()];
		sideMode[side.ordinal()] &= 1;
		if (tile instanceof TileEntityRedNetEnergy) {
			TileEntityRedNetEnergy cable = ((TileEntityRedNetEnergy) tile);
			sideMode[side.ordinal()] |= (4 << 1);
			if (cable.canInterface(this, side.getOpposite())) {
				if (_grid == null && cable._grid != null) {
					cable._grid.addConduit(this);
				}
				if (cable._grid == _grid || _grid.addConduit(cable)) {
					sideMode[side.ordinal()] |= 1; // always enable
				}
			} else {
				sideMode[side.ordinal()] &= ~1;
			}
		} else if (tile instanceof IEnergyConnection) {
			if (((IEnergyConnection) tile).canConnectEnergy(side)) {
				if (tile instanceof IEnergyTransport) {
					IEnergyTransport transport = (IEnergyTransport) tile;
					InterfaceType type = transport.getTransportState(side).getOpposite();
					if (type != InterfaceType.BALANCE) {
						createTransportTypes();
						transportTypes[side.ordinal()] = type;
					}
					sideMode[side.ordinal()] |= 2 << 1;
				} else {
					sideMode[side.ordinal()] |= 1 << 1;
					if (transportTypes != null) {
						transportTypes[side.ordinal()] = InterfaceType.BALANCE;
					}
				}
				if (tile instanceof IEnergyReceiver) {
					if (receiverCache == null) receiverCache = new IEnergyReceiver[6];
					receiverCache[side.ordinal()] = (IEnergyReceiver) tile;
				}
				if (tile instanceof IEnergyProvider) {
					if (providerCache == null) providerCache = new IEnergyProvider[6];
					providerCache[side.ordinal()] = (IEnergyProvider) tile;
				}
			}
		}
		if (!deadCache) {
			if (lastMode != sideMode[side.ordinal()]) {
				RedstoneEnergyNetwork.HANDLER.addConduitForUpdate(this);
				MFRUtil.notifyBlockUpdate(worldObj, pos);
			}
		}
	}

	private void createTransportTypes() {

		transportTypes = new InterfaceType[6];
		for (int i = transportTypes.length; i-- > 0; ) {
			transportTypes[i] = InterfaceType.BALANCE;
		}
	}

	@Override
	protected NBTTagCompound writePacketData(NBTTagCompound tag) {

		tag.setInteger("mode[2]", (sideMode[0] & 0xFF) | ((sideMode[1] & 0xFF) << 8) | ((sideMode[2] & 0xFF) << 16) |
				((sideMode[3] & 0xFF) << 24));
		tag.setInteger("mode[3]", (sideMode[4] & 0xFF) | ((sideMode[5] & 0xFF) << 8) | ((sideMode[6] & 0xFF) << 16));

		return super.writePacketData(tag);
	}

	@Override
	public SPacketUpdateTileEntity getUpdatePacket() {

		if (deadCache)
			return null;

		return super.getUpdatePacket();
	}

	@Override
	protected void handlePacketData(NBTTagCompound tag) {

		super.handlePacketData(tag);

		int mode = tag.getInteger("mode[2]");
		sideMode[0] = (byte) (mode & 0xFF);
		sideMode[1] = (byte) ((mode >> 8) & 0xFF);
		sideMode[2] = (byte) ((mode >> 16) & 0xFF);
		sideMode[3] = (byte) ((mode >> 24) & 0xFF);
		mode = tag.getInteger("mode[3]");
		sideMode[4] = (byte) (mode & 0xFF);
		sideMode[5] = (byte) ((mode >> 8) & 0xFF);
		sideMode[6] = (byte) ((mode >> 16) & 0xFF);
	}

	@SideOnly(Side.CLIENT)
	public void setModes(byte[] modes) {

		sideMode = modes;
	}

	// IEnergyHandler

	@Override
	public int receiveEnergy(EnumFacing from, int maxReceive, boolean simulate) {

		if (from == null) return 0;
		if ((sideMode[from.ordinal() ^ 1] & 1) != 0 & _grid != null)
			return _grid.storage.receiveEnergy(maxReceive, simulate);
		return 0;
	}

	@Override
	public int extractEnergy(EnumFacing from, int maxExtract, boolean simulate) {

		return 0;
	}

	@Override
	public boolean canConnectEnergy(EnumFacing from) {

		if (from == null) return false;
		return (sideMode[from.ordinal() ^ 1] & 1) != 0 & _grid != null;
	}

	@Override
	public int getEnergyStored(EnumFacing from) {

		if (from == null) return 0;
		if ((sideMode[from.ordinal() ^ 1] & 1) != 0 & _grid != null)
			return _grid.storage.getEnergyStored();
		return 0;
	}

	@Override
	public int getMaxEnergyStored(EnumFacing from) {

		if (from == null) return 0;
		if ((sideMode[from.ordinal() ^ 1] & 1) != 0 & _grid != null)
			return _grid.storage.getMaxEnergyStored();
		return 0;
	}

	@Override
	public InterfaceType getTransportState(EnumFacing from) {

		if (transportTypes != null) {
			return transportTypes[from.ordinal() ^ 1];
		}
		return InterfaceType.BALANCE;
	}

	@Override
	public boolean setTransportState(InterfaceType state, EnumFacing from) {

		if ((sideMode[from.ordinal() ^ 1] >> 1) == 1 || !isInterfacing(from)) {
			return false;
		}
		if (transportTypes == null) {
			createTransportTypes();
		}
		transportTypes[from.ordinal() ^ 1] = state;
		return true;
	}

	// ICrankable

	@Override
	public boolean canTurn() {

		return _grid.storage.getEnergyStored() < _grid.storage.getMaxEnergyStored();
	}

	@Override
	public void applyTurn() {

		_grid.storage.receiveEnergy(90, false);
	}

	@Override
	public boolean canCrankAttach(EnumFacing directionToCrank) {

		return true;
	}

	// internal

	public boolean isInterfacing(EnumFacing to) {

		int bSide = to.getOpposite().ordinal();
		int mode = sideMode[bSide] >> 1;
		return (sideMode[bSide] & 1) != 0 & mode != 0;
	}

	public int interfaceMode(EnumFacing to) {

		int bSide = to.getOpposite().ordinal();
		int mode = sideMode[bSide] >> 1;
		return (sideMode[bSide] & 1) != 0 ? mode : 0;
	}

	@Override
	public void readFromNBT(NBTTagCompound nbt) {

		super.readFromNBT(nbt);
		sideMode = nbt.getByteArray("SideMode");
		if (sideMode.length != 7)
			sideMode = new byte[] { 1, 1, 1, 1, 1, 1, 0 };
		energyForGrid = nbt.getInteger("Energy");
		readFromNBT = true;
	}

	@Override
	public NBTTagCompound writeToNBT(NBTTagCompound nbt) {

		super.writeToNBT(nbt);
		nbt.setByteArray("SideMode", sideMode);
		if (_grid != null) {
			if (isNode) {
				energyForGrid = _grid.getNodeShare(this);
			}
		}
		if (energyForGrid > 0)
			nbt.setInteger("Energy", energyForGrid);
		else
			energyForGrid = 0;
		return nbt;
	}

	void extract(EnumFacing side, EnergyStorage storage) {

		if (deadCache) return;
		int bSide = side.ordinal();
		if ((sideMode[bSide] & 1) != 0) {
			switch (sideMode[bSide] >> 1) {
			case 1: // IEnergyHandler
				if (providerCache != null) {
					IEnergyProvider handlerTile = providerCache[bSide];
					if (handlerTile != null) {
						int e = handlerTile.extractEnergy(side, TRANSFER_RATE, true);
						if (e > 0)
							handlerTile.extractEnergy(side, storage.receiveEnergy(e, false), false);
					}
				}
				break;
			case 2: {// IEnergyTransport
				InterfaceType state = getTransportState(side);
				if (providerCache != null && state == InterfaceType.RECEIVE) {
					IEnergyProvider handlerTile = providerCache[bSide];
					if (handlerTile != null) {
						int e = handlerTile.extractEnergy(side, TRANSFER_RATE, true);
						if (e > 0)
							handlerTile.extractEnergy(side, storage.receiveEnergy(e, false), false);
					}
				}
				break;
			}
			case 3: // IEnergyTile
				break;
			case 4: // TileEntityRednetCable
			case 0: // no mode
				// no-op
				break;
			}
		}
	}

	int transfer(EnumFacing side, int energy) {

		if (deadCache) return 0;
		int bSide = side.ordinal();
		if ((sideMode[bSide] & 1) != 0) {
			switch (sideMode[bSide] >> 1) {
			case 1: // IEnergyHandler
				if (receiverCache != null) {
					IEnergyReceiver handlerTile = receiverCache[bSide];
					if (handlerTile != null)
						return handlerTile.receiveEnergy(side, energy, false);
				}
				break;
			case 2: {// IEnergyTransport
				InterfaceType state = getTransportState(side);
				if (receiverCache != null && state != InterfaceType.RECEIVE) {
					IEnergyReceiver handlerTile = receiverCache[bSide];
					if (handlerTile != null && (state == InterfaceType.SEND || handlerTile.getEnergyStored(side) < _grid.storage.getEnergyStored()))
						return handlerTile.receiveEnergy(side, energy, false);
				}
				break;
			}
			case 3: // IEnergyTile
				break;
			case 4: // TileEntityRednetCable
			case 0: // no mode
				// no-op
				break;
			}
		}
		return 0;
	}

	void setGrid(RedstoneEnergyNetwork newGrid) {

		_grid = newGrid;
	}

	@Override
	public void updateInternalTypes(IGridController grid) {

		super.updateInternalTypes(grid);
		if (deadCache) return;
		if (grid != RedstoneEnergyNetwork.HANDLER) return;
		isNode = false;
		for (int i = 0; i < 6; i++) {
			int mode = sideMode[i] >> 1;
			if (((sideMode[i] & 1) != 0) & (mode != 0) & (mode != 4)) {
				isNode = true;
			}
		}
		if (_grid != null)
			_grid.addConduit(this);
		Packets.sendToAllPlayersWatching(this);
	}

	@Override
	public boolean onPartHit(EntityPlayer player, EnumFacing side, int subHit) {

		if (subHit >= (2 + 6 * 4) && subHit < (2 + 6 * 6)) {
			if (MFRUtil.isHoldingUsableTool(player, pos)) {
				if (!player.worldObj.isRemote) {
					int dir = side.getOpposite().ordinal();
					if (sideMode[dir] == 9) {
						removeFromGrid();
					}
					sideMode[dir] ^= 1;
					markDirty();
					if (sideMode[dir] == 8) {
						deadCache = true;
						reCache();
						if (_grid == null)
							setGrid(new RedstoneEnergyNetwork(this));
					}
					RedstoneEnergyNetwork.HANDLER.addConduitForUpdate(this);
					Packets.sendToAllPlayersWatching(this);
					MFRUtil.notifyBlockUpdate(worldObj, pos);
				}
				return true;
			}
		}
		return false;
	}

	@Override
	public void addTraceableCuboids(List<IndexedCuboid6> list, boolean forTrace, boolean hasTool) {

		Vector3 offset = Vector3.fromBlockPos(pos);

		IndexedCuboid6 main = new IndexedCuboid6(0, subSelection[1]); // main body
		list.add(main);

		EnumFacing[] side = EnumFacing.VALUES;
		for (int i = side.length; i-- > 0;) {
			RedNetConnectionType c = getConnectionState(side[i], true);
			int mode = sideMode[EnumFacing.VALUES[i].getOpposite().ordinal()];
			boolean iface = mode > 1 & ((mode & 1) == 1 | hasTool);
			int o = 2 + i, k = o;
			l: if (c.isConnected) {
				if (c.isPlate)
					o += 6;
				else if (c.isCable)
					if (c.isAllSubnets) {
						if (!iface | !hasTool) {
							o = 2 + 6 * 3 + i;
						}
						list.add((IndexedCuboid6) new IndexedCuboid6(hasTool ? k + 6 * 6 : 1, subSelection[o]).add(offset)); // cable part or connection point
						break l;
					}
				k = 2 + 6 * 3 + i;
				if (iface)
					k += forTrace ? 6 : 12;
				// cable part or energy selection box
				list.add((IndexedCuboid6) new IndexedCuboid6(iface & hasTool ? k : 1, subSelection[k]).add(offset));
				list.add((IndexedCuboid6) new IndexedCuboid6(o, subSelection[o]).add(offset)); // connection point
				o = 2 + 6 * 2 + i;
				if (c.isSingleSubnet) // color band
					list.add((IndexedCuboid6) new IndexedCuboid6(o, subSelection[o]).add(offset));
				iface = false;
			} else if (forTrace & hasTool & _cableMode[6] != 1 && (c = getConnectionState(side[i], false)).isConnected) { // cable-only
				if (c.isAllSubnets & c.isCable) {
					if (!iface) {
						k += 6 * 3;
					}
					o += 6 * 6;
				}
				list.add((IndexedCuboid6) new IndexedCuboid6(o, subSelection[k]).add(offset)); // connection point (raytrace)
			}
			if (iface) {
				o = 2 + 6 * 5 + i;
				list.add((IndexedCuboid6) new IndexedCuboid6(hasTool ? o : 1, subSelection[o]).add(offset));
			}
		}
		main.add(offset);
	}

	@Override
	public void getTileInfo(List<ITextComponent> info, EnumFacing side, EntityPlayer player, boolean debug) {

		info.add(text("-Redstone-"));
		super.getTileInfo(info, side, player, debug && player.isSneaking());
		info.add(text("-Energy-"));
		if (_grid != null) {/* TODO: advanced monitoring
							if (isNode) {
							info.add("Throughput All: " + _grid.distribution);
							info.add("Throughput Side: " + _grid.distributionSide);
							} else//*/
			if (!debug) {
				float sat = 0;
				if (_grid.getNodeCount() != 0)
					sat = (float) (Math.ceil(_grid.storage.getEnergyStored() /
							(float) _grid.storage.getMaxEnergyStored() * 1000f) / 10f);
				info.add(text("Saturation: " + sat));
			}
		} else if (!debug)
			info.add(text("Null Grid"));
		if (debug) {
			if (_grid != null) {
				info.add(text("Grid:" + _grid));
				info.add(text("Conduits: " + _grid.getConduitCount() + ", Nodes: " + _grid.getNodeCount()));
				info.add(text("Grid Max: " + _grid.storage.getMaxEnergyStored() + ", Grid Cur: " +
						_grid.storage.getEnergyStored()));
				info.add(text("Caches: (RF):({" + Arrays.toString(receiverCache) + "," +
						Arrays.toString(providerCache) + "})"));
			} else {
				info.add(text("Null Grid"));
			}
			info.add(text("SideType: " + Arrays.toString(sideMode)));
			info.add(text("Node: " + isNode + ", Energy: " + energyForGrid));
			return;
		}
	}
}
