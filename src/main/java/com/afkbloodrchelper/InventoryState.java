package com.afkbloodrchelper;

import lombok.Value;
import net.runelite.api.Item;
import net.runelite.api.ItemContainer;

/**
 * Immutable snapshot of the inventory contents relevant to blood runecrafting.
 */
@Value
class InventoryState
{
	static final int INVENTORY_SIZE = 28;

	int dense;
	int dark;
	int fragments;
	int inactiveEssence;
	int activeEssence;
	int freeSlots;
	int bloodRunes;

	static InventoryState read(ItemContainer container)
	{
		return read(container.getItems());
	}

	static InventoryState read(Item[] items)
	{
		int dense = 0, dark = 0, fragments = 0, inactive = 0, active = 0, used = 0, bloodRunes = 0;

		// NOTE: the item array can be shorter than 28 (e.g. right after logging in when
		// the trailing slots are empty), so free slots are counted from used slots.
		for (Item item : items)
		{
			if (item.getId() == -1 || item.getQuantity() <= 0)
			{
				continue;
			}
			used++;
			switch (item.getId())
			{
				case BloodRcIds.DENSE_ESSENCE_BLOCK:
					dense++;
					break;
				case BloodRcIds.DARK_ESSENCE_BLOCK:
					dark++;
					break;
				case BloodRcIds.DARK_ESSENCE_FRAGMENTS:
					// NOTE: always quantity 1 in game; treat as "has fragments", not a count.
					fragments += item.getQuantity();
					break;
				case BloodRcIds.BLOOD_ESSENCE_INACTIVE:
					inactive++;
					break;
				case BloodRcIds.BLOOD_ESSENCE_ACTIVE:
					active++;
					break;
				case BloodRcIds.BLOOD_RUNE:
					bloodRunes += item.getQuantity();
					break;
			}
		}
		return new InventoryState(dense, dark, fragments, inactive, active, Math.max(0, INVENTORY_SIZE - used), bloodRunes);
	}
}
