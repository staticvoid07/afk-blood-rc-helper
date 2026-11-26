package com.afkbloodrchelper;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Tracks where the player is in the two-load blood runecrafting loop and decides the stage.
 * <p>
 * Load 1: mine, venerate, chisel everything into fragments.
 * Load 2: mine, venerate (while holding fragments), bind at the Blood Altar, chisel the
 * dark blocks, bind again.
 * <p>
 * The stage is decided purely from the inventory plus these two flags, never from where the
 * player is standing. The flags are persisted by the plugin so they survive relogs.
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
class TripState
{
	/**
	 * The current load was venerated while already holding fragments, so it goes to the Blood Altar.
	 */
	private boolean craftTrip;

	/**
	 * The fragments carried to the Blood Altar have been bound; chisel the remaining blocks next.
	 */
	private boolean firstBindDone;

	/**
	 * @param previous inventory from the previous tick, or null when unknown (e.g. right after login)
	 * @param current  inventory this tick
	 */
	void update(InventoryState previous, InventoryState current)
	{
		if (previous != null)
		{
			if (venerated(previous, current))
			{
				craftTrip = previous.getFragments() > 0;
				firstBindDone = false;
			}

			// Reset only on the transition from holding essence to holding none (everything
			// bound or banked). An inventory that hasn't loaded yet after login (empty, or
			// empty for several ticks) never wipes the persisted flags. Stale flags are harmless:
			// with no dark blocks or fragments the stage is always MINE, and the next
			// veneration sets them again.
			boolean hadEssence = previous.getDark() > 0 || previous.getFragments() > 0;
			if (hadEssence && current.getDark() == 0 && current.getFragments() == 0)
			{
				craftTrip = false;
				firstBindDone = false;
			}
		}

		if (current.getDense() > 0 && current.getFragments() == 0)
		{
			// Mining without any fragments can only be the first load, whatever was saved before.
			craftTrip = false;
			firstBindDone = false;
		}

		if (craftTrip && current.getDark() > 0 && current.getFragments() == 0)
		{
			firstBindDone = true;
		}
	}

	/**
	 * Dense essence was just turned into dark essence at the Dark Altar.
	 */
	static boolean venerated(InventoryState previous, InventoryState current)
	{
		return previous != null
			&& previous.getDense() > 0
			&& current.getDense() < previous.getDense()
			&& current.getDark() > previous.getDark();
	}

	/**
	 * The last dark essence block was just chiseled into fragments. Only the blocks are checked,
	 * in case the fragment stack updates on a different tick.
	 */
	static boolean finishedChiseling(InventoryState previous, InventoryState current)
	{
		return previous != null
			&& previous.getDark() > 0
			&& current.getDark() == 0
			&& current.getDense() == 0;
	}

	/**
	 * Fragments were just bound into blood runes at the Blood Altar (either bind of the trip).
	 * Only the fragments are checked, in case the rune stack updates on a different tick.
	 * Decided from the inventory alone, so it works even if the trip flags were lost.
	 */
	static boolean bound(InventoryState previous, InventoryState current)
	{
		return previous != null
			&& previous.getFragments() > 0
			&& current.getFragments() < previous.getFragments();
	}

	Stage stage(InventoryState inv)
	{
		if (inv.getDense() > 0)
		{
			// Keep mining until the inventory is full, wherever the player is standing.
			return inv.getFreeSlots() > 0 ? Stage.MINE : Stage.VENERATE;
		}
		if (inv.getDark() > 0)
		{
			return craftTrip && inv.getFragments() > 0 && !firstBindDone ? Stage.BIND : Stage.CHISEL;
		}
		if (inv.getFragments() > 0 && craftTrip)
		{
			// Remaining blocks were chiseled at the Blood Altar; bind them no matter how far
			// the player has wandered from the altar.
			return Stage.BIND;
		}
		return Stage.MINE;
	}
}
