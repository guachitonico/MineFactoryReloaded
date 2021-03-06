package powercrystals.minefactoryreloaded.tile.rednet;

import buildcraft.api.transport.IPipeTile.PipeType;

import cofh.asm.relauncher.Strippable;
import net.minecraft.network.play.server.SPacketUpdateTileEntity;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.NetworkManager;
import net.minecraft.util.EnumFacing;

import powercrystals.minefactoryreloaded.core.ArrayQueue;
import powercrystals.minefactoryreloaded.net.Packets;
import powercrystals.minefactoryreloaded.tile.base.TileEntityFactory;

public class TileEntityRedNetHistorian extends TileEntityFactory
{
	@SideOnly(Side.CLIENT)
	private ArrayQueue<Integer> _valuesClient;
	@SideOnly(Side.CLIENT)
	private int _currentValueClient;

	private int _currentSubnet = 0;
	private int[] _lastValues = new int[16];

	public TileEntityRedNetHistorian()
	{
		super(null);
	}

	@Override
	protected NBTTagCompound writePacketData(NBTTagCompound tag)
	{
		tag.setInteger("subnet", _currentSubnet);
		tag.setInteger("current", _lastValues[_currentSubnet]);

		return super.writePacketData(tag);
	}

	@Override
	protected void handlePacketData(NBTTagCompound tag)
	{
		super.handlePacketData(tag);
		_currentSubnet = tag.getInteger("subnet");
		_currentValueClient = tag.getInteger("current");
	}

	@Override
	public void onDataPacket(NetworkManager net, SPacketUpdateTileEntity pkt)
	{
		NBTTagCompound data = pkt.getNbtCompound();
		switch (pkt.getTileEntityType())
		{
		case 0:
			super.onDataPacket(net, pkt);
			break;
		case 1:
			_currentValueClient = data.getInteger("value");
			break;
		}
	}

	@Override
	public void validate()
	{
		if (!worldObj.isRemote)
		{
			setSelectedSubnet(_currentSubnet);
		}
		else
		{
			_valuesClient = new ArrayQueue<>(100);
			_currentValueClient = 0;
		}
	}

	@Override
	@SideOnly(Side.CLIENT)
	public void update()
	{
		if (worldObj.isRemote)
		{
			_valuesClient.pop();
			_valuesClient.push(_currentValueClient);
		}
	}

	@SideOnly(Side.CLIENT)
	public Integer[] getValues()
	{
		Integer[] values = new Integer[_valuesClient.size()];
		return _valuesClient.toArray(values);
	}

	@SideOnly(Side.CLIENT)
	public void setClientValue(int value)
	{
		_currentValueClient = value;
	}

	public void setSelectedSubnet(int newSubnet)
	{
		_currentSubnet = newSubnet;
		if (worldObj.isRemote)
		{
			_valuesClient.fill(null);
		}
		else
		{
			sendValue(_lastValues[_currentSubnet]);
		}
	}

	public void valuesChanged(int[] values)
	{
		for(int i = 0; i < 16; i++)
		{
			if(values[i] != _lastValues[i])
			{
				_lastValues[i] = values[i];
				if (i == _currentSubnet)
				{
					sendValue(values[i]);
				}
			}
		}
	}

	protected void sendValue(int value)
	{
		NBTTagCompound data = new NBTTagCompound();
		data.setInteger("value", value);
		Packets.sendToAllPlayersInRange(worldObj, pos, 50,
				new SPacketUpdateTileEntity(pos, 1, data));
	}

	public int getSelectedSubnet()
	{
		return _currentSubnet;
	}

	@Override
	public boolean canRotate()
	{
		return true;
	}

	@Override
	public String getDataType() {
		return "tile.mfr.rednet.panel.historian.name";
	}

	@Override
	public void readFromNBT(NBTTagCompound nbttagcompound)
	{
		super.readFromNBT(nbttagcompound);
		_currentSubnet = nbttagcompound.getInteger("subnet");
	}

	@Override
	public NBTTagCompound writeToNBT(NBTTagCompound nbttagcompound)
	{
		super.writeToNBT(nbttagcompound);
		nbttagcompound.setInteger("subnet", _currentSubnet);

		return nbttagcompound;
	}

	@Override
	@Strippable("buildcraft.api.transport.IPipeConnection")
	public ConnectOverride overridePipeConnection(PipeType type, EnumFacing with) {
		return ConnectOverride.DISCONNECT;
	}

    @Override
	@SideOnly(Side.CLIENT)
    public double getMaxRenderDistanceSquared()
    {
        return 4096.0D;
    }
}
