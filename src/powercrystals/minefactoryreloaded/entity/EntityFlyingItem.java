package powercrystals.minefactoryreloaded.entity;

import com.google.common.base.Optional;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.world.World;

public class EntityFlyingItem extends EntitySafariNet {

	public int canBePickedUp = 0, pickupChance = 7;
	protected float damage = 2.0f;
	protected int knockbackStrength;

	public EntityFlyingItem(World world) {

		super(world);
	}

	public EntityFlyingItem(World world, double x, double y, double z, ItemStack stack) {

		super(world, x, y, z, stack);
	}

	public EntityFlyingItem(World world, EntityLivingBase owner, ItemStack stack) {

		super(world, owner, stack);
	}

	public void setDamage(float damage) {

		this.damage = damage;
	}

	public float getDamage() {

		return this.damage;
	}

	public void setKnockbackStrength(int p_70240_1_) {

		this.knockbackStrength = p_70240_1_;
	}

	@Override
	public boolean canBeAttackedWithItem() {

		return false;
	}

	@Override
	protected boolean onHitBlock(ItemStack storedEntity, RayTraceResult result) {

		if (canBePickedUp == 0 && (pickupChance > 0 && (pickupChance == 1 || worldObj.rand.nextInt(pickupChance) == 0))) {
			dropAsStack(this.getStoredEntity());
			return false;
		}
		return true;
	}

	@Override
	protected boolean onHitEntity(ItemStack storedEntity, RayTraceResult result) {

		DamageSource d = DamageSource.causeThrownDamage(this, getThrower() == null ? this : getThrower());
		if (result.entityHit.attackEntityFrom(d, damage)) {
			if (this.knockbackStrength > 0) {
				float f4 = MathHelper.sqrt_double(this.motionX * this.motionX + this.motionZ * this.motionZ);

				if (f4 > 0.0F) {
					result.entityHit.addVelocity(this.motionX * this.knockbackStrength * 0.6D /
							f4, 0.1D, this.motionZ * this.knockbackStrength * 0.6D / f4);
				}
			}
		}
		return true;
	}

	@Override
	protected void impact(double x, double y, double z, EnumFacing side) {
		Optional<ItemStack> entity = dataManager.get(STORED_ENTITY);
		ItemStack stack = entity.isPresent() ? entity.get() : null;

		float X = 0, Y = 0.14f, Z = 0;
		switch (side) {
		case UP:
			y += 1;
		case DOWN:
			x += 0.5;
			z += 0.5;
			if (side == EnumFacing.DOWN)
				Y = 0;
			break;
		case SOUTH:
			z += 1;
		case NORTH:
			x += 0.5;
			y += 0.5;
			Z = side == EnumFacing.SOUTH ? 1 : -1;
			break;
		case EAST:
			x += 1;
		case WEST:
			y += 0.5;
			z += 0.5;
			X = side == EnumFacing.EAST ? 1 : -1;
			break;
		}
		for (int j = 0; j < 8; ++j) {
			float f = (worldObj.rand.nextFloat() - 0.5f) * 0.37f;
			if (X != 0) f = Math.copySign(f, X);
			float f2 = (worldObj.rand.nextFloat() - 0.5f) * 0.37f;
			if (Z != 0) f2 = Math.copySign(f2, Z);
			if (stack == null) {
				worldObj.spawnParticle(EnumParticleTypes.SNOWBALL, x, y, z, f, Y, f2);
			} else {
				worldObj.spawnParticle(EnumParticleTypes.ITEM_CRACK, x, y, z, f, Y, f2, Item.getIdFromItem(stack.getItem()));
			}
		}
		if (!worldObj.isRemote)
			setDead();
	}

}
