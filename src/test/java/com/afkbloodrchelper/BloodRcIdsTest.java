package com.afkbloodrchelper;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import net.runelite.api.coords.WorldPoint;
import org.junit.Test;

public class BloodRcIdsTest
{
	@Test
	public void darkApproachIsExactlyInReach()
	{
		assertEquals(BloodRcIds.BLOOD_ALTAR_REACH, BloodRcIds.distanceToBloodAltar(BloodRcIds.DARK_APPROACH));
	}

	@Test
	public void standingAtDarkAltarIsOutOfReach()
	{
		// Tile just south of the Dark Altar (1718, 3882)
		assertTrue(BloodRcIds.distanceToBloodAltar(new WorldPoint(1718, 3880, 0)) > BloodRcIds.BLOOD_ALTAR_REACH);
	}

	@Test
	public void everyStepCloserTileIsInReach()
	{
		for (WorldPoint p : BloodRcIds.STEP_CLOSER_TO_BLOOD.toWorldPointList())
		{
			assertTrue(p + " out of reach", BloodRcIds.distanceToBloodAltar(p) <= BloodRcIds.BLOOD_ALTAR_REACH);
		}
	}

	@Test
	public void shortcutLandingsAreOnTheRightSide()
	{
		// 69 climb: (1761,3872) mine side, (1761,3874) Dark Altar side. 73 climb lands at (1752,3854).
		assertTrue(BloodRcIds.inMineBasin(new WorldPoint(1761, 3872, 0)));
		assertFalse(BloodRcIds.inMineBasin(new WorldPoint(1761, 3874, 0)));
		assertTrue(BloodRcIds.inMineBasin(new WorldPoint(1752, 3854, 0)));
		assertTrue(BloodRcIds.onBloodAltarSide(new WorldPoint(1742, 3854, 0)));
	}
}
