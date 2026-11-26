package com.afkbloodrchelper;

import com.google.common.collect.ImmutableSet;
import java.util.Set;
import net.runelite.api.coords.WorldArea;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.gameval.ItemID;
import net.runelite.api.gameval.ObjectID;
import net.runelite.api.gameval.VarbitID;

/**
 * IDs and locations used by the plugin.
 */
final class BloodRcIds
{
	private BloodRcIds()
	{
	}

	// Items
	static final int DENSE_ESSENCE_BLOCK = ItemID.ARCEUUS_ESSENCE_BLOCK;
	static final int DARK_ESSENCE_BLOCK = ItemID.ARCEUUS_ESSENCE_BLOCK_DARK;
	static final int DARK_ESSENCE_FRAGMENTS = 7938; // gameval name: BIGBLANKRUNE
	static final int CHISEL = ItemID.CHISEL;
	static final int BLOOD_ESSENCE_INACTIVE = ItemID.BLOOD_ESSENCE_INACTIVE;
	static final int BLOOD_ESSENCE_ACTIVE = ItemID.BLOOD_ESSENCE_ACTIVE;

	// Objects
	static final int RUNESTONE_NORTH = ObjectID.ARCEUUS_RUNESTONE_BASE_1;
	static final int RUNESTONE_SOUTH = ObjectID.ARCEUUS_RUNESTONE_BASE_2;
	static final int DARK_ALTAR = ObjectID.ARCHEUS_ALTAR_DARK;
	static final int BLOOD_ALTAR = ObjectID.ARCHEUS_ALTAR_BLOOD;
	static final int SHORTCUT_69 = 34741; // Rocks north of the runestones (to/from the Dark Altar)
	static final int SHORTCUT_73_TOP = ObjectID.ARCHEUUS_RUNESTONE_SHORTCUT_GREY_TOP; // Rocks west of the runestones
	static final int SHORTCUT_73_BOTTOM = ObjectID.ARCHEUUS_RUNESTONE_SHORTCUT_GREY_BOTTOM;

	// Varbits: 0 = dense (mineable), otherwise depleted
	static final int VARBIT_RUNESTONE_NORTH = VarbitID.ARCEUUS_RUNESTONE_1;
	static final int VARBIT_RUNESTONE_SOUTH = VarbitID.ARCEUUS_RUNESTONE_2;

	// Map regions making up the blood runecrafting area (runestones, Dark Altar, Blood Altar and the paths between)
	static final Set<Integer> REGIONS = ImmutableSet.of(6460, 6715, 6716, 6971, 6972);

	// Walk-to tiles used when the next object is not clickable on screen
	static final WorldArea WALK_DARK_TO_ROCKS = new WorldArea(1751, 3873, 5, 5, 0);
	static final WorldArea WALK_DARK_TO_BLOOD = new WorldArea(1720, 3854, 5, 5, 0);

	/**
	 * The runestone basin: east of the 73 shortcut (x 1742) and south of the 69 shortcut (y 3873).
	 */
	static boolean inMineBasin(WorldPoint p)
	{
		return p.getX() >= 1743 && p.getX() <= 1790 && p.getY() >= 3830 && p.getY() <= 3872;
	}

	/**
	 * West of the runestones on the Blood Altar side of the 73 shortcut (as opposed to up by the Dark Altar).
	 */
	static boolean onBloodAltarSide(WorldPoint p)
	{
		return p.getX() < 1743 && p.getY() < 3866;
	}
}
