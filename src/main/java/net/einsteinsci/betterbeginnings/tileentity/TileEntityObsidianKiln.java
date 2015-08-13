package net.einsteinsci.betterbeginnings.tileentity;

import cpw.mods.fml.common.registry.GameRegistry;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.einsteinsci.betterbeginnings.blocks.BlockObsidianKiln;
import net.einsteinsci.betterbeginnings.register.recipe.KilnRecipes;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.inventory.ISidedInventory;
import net.minecraft.item.*;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.tileentity.TileEntity;

/**
 * Created by einsteinsci on 8/17/2014.
 */
public class TileEntityObsidianKiln extends TileEntity implements ISidedInventory
{
	public static final int smeltTime = 100;
	private static final int[] slotsTop = new int[] {0};
	private static final int[] slotsBottom = new int[] {2, 1};
	private static final int[] slotsSides = new int[] {1};
	public ItemStack[] kilnStacks = new ItemStack[3];

	public int kilnBurnTime;
	public int currentBurnTime;

	public int kilnCookTime;

	private String kilnName;

	public TileEntityObsidianKiln()
	{
		super();
	}

	public void furnaceName(String string)
	{
		kilnName = string;
	}

	@Override
	public void readFromNBT(NBTTagCompound tagCompound)
	{
		super.readFromNBT(tagCompound);

		// ItemStacks
		NBTTagList tagList = tagCompound.getTagList("Items", 10);
		kilnStacks = new ItemStack[getSizeInventory()];

		for (int i = 0; i < tagList.tagCount(); ++i)
		{
			NBTTagCompound itemTag = tagList.getCompoundTagAt(i);
			byte slot = itemTag.getByte("Slot");

			if (slot >= 0 && slot < kilnStacks.length)
			{
				kilnStacks[slot] = ItemStack.loadItemStackFromNBT(itemTag);
			}
		}

		// Burn Time & Cook Time
		kilnBurnTime = tagCompound.getShort("BurnTime");
		kilnCookTime = tagCompound.getShort("CookTime");
		currentBurnTime = getItemBurnTime(kilnStacks[1]);

		// stacked = tagCompound.getInteger("Stacked");

		if (tagCompound.hasKey("CustomName", 8))
		{
			kilnName = tagCompound.getString("CustomName");
		}
	}

	@Override
	public void writeToNBT(NBTTagCompound tagCompound)
	{
		super.writeToNBT(tagCompound);

		tagCompound.setShort("BurnTime", (short)kilnBurnTime);
		tagCompound.setShort("CookTime", (short)kilnCookTime);
		// tagCompound.setInteger("Stacked", stacked);
		NBTTagList tagList = new NBTTagList();

		for (int i = 0; i < kilnStacks.length; ++i)
		{
			if (kilnStacks[i] != null)
			{
				NBTTagCompound itemTag = new NBTTagCompound();
				kilnStacks[i].writeToNBT(itemTag);
				itemTag.setByte("Slot", (byte)i);
				tagList.appendTag(itemTag);
			}
		}

		tagCompound.setTag("Items", tagList);
		if (hasCustomInventoryName())
		{
			tagCompound.setString("CustomName", kilnName);
		}
	}

	@Override
	public void updateEntity()
	{
		boolean flag = kilnBurnTime > 0;
		boolean flag1 = false;

		if (kilnBurnTime > 0)
		{
			--kilnBurnTime;
		}

		if (!worldObj.isRemote)
		{
			if (kilnBurnTime == 0 && canSmelt())
			{
				currentBurnTime = kilnBurnTime = getItemBurnTime(kilnStacks[1]);

				if (kilnBurnTime > 0)
				{
					flag1 = true;
					if (kilnStacks[1] != null)
					{
						--kilnStacks[1].stackSize;

						if (kilnStacks[1].stackSize == 0)
						{
							kilnStacks[1] = kilnStacks[1].getItem().getContainerItem(kilnStacks[1]);
						}
					}
				}
			}

			if (isBurning() && canSmelt())
			{
				++kilnCookTime;
				if (kilnCookTime == smeltTime)
				{
					kilnCookTime = 0;
					smeltItem();
					flag1 = true;
				}
			}
			else
			{
				kilnCookTime = 0;
			}
		}

		if (flag != kilnBurnTime > 0)
		{
			flag1 = true;
			BlockObsidianKiln.updateBlockState(kilnBurnTime > 0, worldObj, xCoord, yCoord, zCoord);
		}

		if (flag1)
		{
			markDirty();
		}
	}

	private boolean canSmelt()
	{
		if (kilnStacks[0] == null)
		{
			return false;
		}
		else
		{
			ItemStack stack = KilnRecipes.smelting().getSmeltingResult(kilnStacks[0]);
			if (stack == null)
			{
				return false;
			}

			if (kilnStacks[2] == null)
			{
				return true;
			}
			if (!kilnStacks[2].isItemEqual(stack))
			{
				return false;
			}

			int result = kilnStacks[2].stackSize + stack.stackSize;
			return result <= getInventoryStackLimit() && result <= kilnStacks[2].getMaxStackSize();
		}
	}

	public boolean isBurning()
	{
		return kilnBurnTime > 0;
	}

	public void smeltItem()
	{
		if (canSmelt())
		{
			ItemStack itemStack = KilnRecipes.smelting().getSmeltingResult(kilnStacks[0]);

			if (kilnStacks[2] == null)
			{
				kilnStacks[2] = itemStack.copy();
			}
			else if (kilnStacks[2].getItem() == itemStack.getItem())
			{
				kilnStacks[2].stackSize += itemStack.stackSize;
			}

			--kilnStacks[0].stackSize;

			if (kilnStacks[0].stackSize <= 0)
			{
				kilnStacks[0] = null;
			}
		}
	}

	@Override
	public int getSizeInventory()
	{
		return kilnStacks.length;
	}

	public static int getItemBurnTime(ItemStack itemStack)
	{
		if (itemStack == null)
		{
			return 0;
		}
		else
		{
			Item item = itemStack.getItem();

			// Blaze Rods and Lava are valid fuel sources for an obsidian kiln.
			if (item == Items.blaze_rod)
			{
				return 1600;
			}
			if (item == Items.lava_bucket)
			{
				return 80000;
			}

			return TileEntityKiln.getItemBurnTime(itemStack);
		}
	}

	@Override
	public ItemStack getStackInSlot(int i)
	{
		return kilnStacks[i];
	}

	@Override
	public ItemStack decrStackSize(int slot, int amount)
	{
		if (kilnStacks[slot] != null)
		{
			ItemStack stack;
			if (kilnStacks[slot].stackSize <= amount)
			{
				stack = kilnStacks[slot];
				kilnStacks[slot] = null;
				return stack;
			}
			else
			{
				stack = kilnStacks[slot].splitStack(amount);

				if (kilnStacks[slot].stackSize == 0)
				{
					kilnStacks[slot] = null;
				}

				return stack;
			}
		}
		else
		{
			return null;
		}
	}

	@Override
	public ItemStack getStackInSlotOnClosing(int slot)
	{
		if (kilnStacks[slot] != null)
		{
			ItemStack stack = kilnStacks[slot];
			kilnStacks[slot] = null;
			return stack;
		}
		else
		{
			return null;
		}
	}

	@Override
	public void setInventorySlotContents(int slot, ItemStack stack)
	{
		kilnStacks[slot] = stack;

		if (stack != null && stack.stackSize > getInventoryStackLimit())
		{
			stack.stackSize = getInventoryStackLimit();
		}
	}

	@Override
	public String getInventoryName()
	{
		return hasCustomInventoryName() ? kilnName : "container.obsidianKiln";
	}

	@Override
	public boolean hasCustomInventoryName()
	{
		return kilnName != null && kilnName.length() > 0;
	}

	@Override
	public int getInventoryStackLimit()
	{
		return 64;
	}

	@Override
	public boolean isUseableByPlayer(EntityPlayer player)
	{
		if (worldObj.getTileEntity(xCoord, yCoord, zCoord) != this)
		{
			return false;
		}
		else
		{
			return player.getDistanceSq(xCoord + 0.5d, yCoord + 0.5d, zCoord + 0.5d) <= 64.0d;
		}
	}

	@Override
	public void openInventory()
	{

	}

	@Override
	public void closeInventory()
	{

	}

	@Override
	public boolean isItemValidForSlot(int slot, ItemStack stack)
	{
		return slot != 2 && (slot == 1 || isItemFuel(stack));
	}

	@SideOnly(Side.CLIENT)
	public int getCookProgressScaled(int progress)
	{
		return kilnCookTime * progress / smeltTime;
	}

	@SideOnly(Side.CLIENT)
	public int getBurnTimeRemainingScaled(int time)
	{
		if (currentBurnTime <= 0)
		{
			currentBurnTime = smeltTime;
		}

		return kilnBurnTime * time / currentBurnTime;
	}

	@Override
	public int[] getAccessibleSlotsFromSide(int side)
	{
		return side == 0 ? slotsBottom : side == 1 ? slotsTop : slotsSides;
	}

	@Override
	public boolean canInsertItem(int par1, ItemStack stack, int par3)
	{
		return isItemValidForSlot(par1, stack);
	}

	public static boolean isItemFuel(ItemStack itemStack)
	{
		return getItemBurnTime(itemStack) > 0;
	}

	@Override
	public boolean canExtractItem(int par1, ItemStack stack, int par3)
	{
		return par3 != 0 || par1 != 1 || stack.getItem() == Items.bucket;
	}
}

