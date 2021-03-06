package cd4017be.lib.Gui;

import net.minecraft.item.ItemStack;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.SlotItemHandler;

/**
 *
 * @author CD4017BE
 */
public class SlotItemType extends SlotItemHandler {

	private final ItemStack[] allowed;

	public SlotItemType(IItemHandler inv, int id, int x, int y, ItemStack... allowed) {
		super(inv, id, x, y);
		this.allowed = allowed;
	}

	@Override
	public boolean isItemValid(ItemStack item) {
		if (!item.isEmpty()) 
			for (ItemStack comp : allowed)
				if (item.getItem() == comp.getItem() && !(item.getHasSubtypes() && item.getItemDamage() != comp.getItemDamage())) return true;
		return false;
	}

	@Override
	public int getItemStackLimit(ItemStack item) {
		for (ItemStack comp : allowed)
			if (item.getItem() == comp.getItem() && !(item.getHasSubtypes() && item.getItemDamage() != comp.getItemDamage()))
				return comp.getCount();
		return 0;
	}

}
