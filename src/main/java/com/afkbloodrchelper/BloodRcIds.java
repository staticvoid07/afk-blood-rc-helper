package com.afkbloodrchelper;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import java.util.List;
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
	static final int BLOOD_RUNE = ItemID.BLOODRUNE;

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

	// Locations below are from Jam's Arceuus Runecrafting (JamsRepos/zeah-rc-helper, BSD 2-Clause),
	// captured in game by its author.

	/**
	 * Four tiles south of the Dark Altar: the nearest spot to it from which the Blood Altar can be clicked.
	 */
	static final WorldPoint DARK_APPROACH = new WorldPoint(1718, 3878, 0);

	/**
	 * "Step Closer to Blood Altar" tiles: a block around {@link #DARK_APPROACH}, every tile of which
	 * is still within reach of the Blood Altar (y <= 3878), so there is more to click when zoomed out.
	 * Only walkable tiles are highlighted.
	 */
	static final WorldArea STEP_CLOSER_TO_BLOOD = new WorldArea(1715, 3876, 7, 3, 0);

	/**
	 * Extra click tiles for "Step Closer": tiles around and south of {@link #STEP_CLOSER_TO_BLOOD}
	 * (mostly the unwalkable ridge) are also highlighted when clicking them would make the player
	 * walk a short way to a tile within reach of the Blood Altar.
	 */
	static final WorldArea STEP_CLOSER_CANDIDATES = new WorldArea(1711, 3866, 15, 13, 0);
	static final int STEP_CLOSER_MAX_WALK = 12;

	/** Blood Altar 4x4 footprint (south-west tile) and the furthest distance it can be clicked from. */
	static final int BLOOD_ALTAR_SW_X = 1716;
	static final int BLOOD_ALTAR_SW_Y = 3829;
	static final int BLOOD_ALTAR_SIZE = 4;
	static final int BLOOD_ALTAR_REACH = 46;

	/** Dark Altar to Blood Altar walking route (west around the ridge, then south-east). */
	static final List<WorldPoint> BLOOD_APPROACH = ImmutableList.of(
		new WorldPoint(1701, 3880, 0),
		new WorldPoint(1674, 3879, 0),
		new WorldPoint(1659, 3881, 0),
		new WorldPoint(1661, 3863, 0),
		new WorldPoint(1675, 3859, 0),
		new WorldPoint(1731, 3856, 0),
		new WorldPoint(1735, 3849, 0),
		new WorldPoint(1735, 3828, 0),
		new WorldPoint(1728, 3826, 0),
		new WorldPoint(1717, 3826, 0)
	);

	/**
	 * Chebyshev distance from a tile to the nearest tile of the Blood Altar.
	 */
	static int distanceToBloodAltar(WorldPoint p)
	{
		int max = BLOOD_ALTAR_SIZE - 1;
		int dx = Math.max(Math.max(BLOOD_ALTAR_SW_X - p.getX(), p.getX() - (BLOOD_ALTAR_SW_X + max)), 0);
		int dy = Math.max(Math.max(BLOOD_ALTAR_SW_Y - p.getY(), p.getY() - (BLOOD_ALTAR_SW_Y + max)), 0);
		return Math.max(dx, dy);
	}


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
