package powercrystals.minefactoryreloaded.item.base;

import cofh.core.item.ItemArmorCore;
import net.minecraft.init.SoundEvents;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.Locale;

import net.minecraft.item.Item;
import net.minecraft.item.ItemArmor;
import net.minecraftforge.common.util.EnumHelper;

import powercrystals.minefactoryreloaded.MFRRegistry;
import powercrystals.minefactoryreloaded.MineFactoryReloadedCore;

public class ItemFactoryArmor extends ItemArmorCore {

	public static final ItemArmor.ArmorMaterial PLASTIC_ARMOR = EnumHelper.addArmorMaterial("mfr:plastic", "plastic", 3, new int[] { 1, 2, 2, 1 }, 7, SoundEvents.ITEM_ARMOR_EQUIP_LEATHER, 0);
	public static final ItemArmor.ArmorMaterial GLASS_ARMOR = EnumHelper.addArmorMaterial("mfr:glass", "glass", 3, new int[] { 0, 0, 0, 0 }, 0, SoundEvents.ITEM_ARMOR_EQUIP_DIAMOND, 0);

	private static final String getName(ItemArmor.ArmorMaterial mat) {

		String r = mat.name().toLowerCase(Locale.US);
		int i = r.indexOf(':') + 1;
		return i > 0 ? r.substring(i, r.length()) : r;
	}

	public ItemFactoryArmor(ItemArmor.ArmorMaterial mat, EntityEquipmentSlot type) {

		super(mat, type);
		setMaxStackSize(1);
		String prefix = MineFactoryReloadedCore.armorTextureFolder + getName(mat);
		setArmorTextures(new String[] { prefix + "_layer_1.png", prefix + "_layer_2.png" });
	}

	@Override
	public Item setUnlocalizedName(String name) {

		super.setUnlocalizedName(name);
		MFRRegistry.registerItem(this, getUnlocalizedName());
		return this;
	}

/*
	@SideOnly(Side.CLIENT)
	@Override
	public void registerIcons(IIconRegister par1IconRegister) {

		this.itemIcon = par1IconRegister.registerIcon("minefactoryreloaded:" + getUnlocalizedName());
	}
*/

	@Override
	public String toString() {

		StringBuilder b = new StringBuilder(getClass().getName());
		b.append('@').append(System.identityHashCode(this)).append('{');
		b.append("l:").append(getUnlocalizedName());
		b.append('}');
		return b.toString();
	}

}
