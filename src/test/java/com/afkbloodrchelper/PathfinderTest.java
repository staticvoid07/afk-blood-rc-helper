package com.afkbloodrchelper;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import java.util.Arrays;
import net.runelite.api.coords.WorldPoint;
import org.junit.Test;

public class PathfinderTest
{
	private static final int SIZE = 30;

	/**
	 * Synthetic scene: a plateau row at y=20 reachable in a few steps (where the player is),
	 * an unwalkable ridge for y 6..19, and low ground at y<=5 only reachable by a long detour.
	 */
	private static Pathfinder.DistanceMap scene()
	{
		int[] dist = new int[SIZE * SIZE];
		Arrays.fill(dist, -1);
		for (int x = 0; x < SIZE; x++)
		{
			dist[x * SIZE + 20] = Math.abs(x - 15);      // plateau, player at (15,20)
			for (int y = 0; y <= 5; y++)
			{
				dist[x * SIZE + y] = 80 + x + (5 - y);   // low ground, long walk around
			}
		}
		return new Pathfinder.DistanceMap(dist, SIZE, SIZE, 0, 0, 0);
	}

	@Test
	public void reachableTileIsItsOwnDestination()
	{
		assertEquals(new WorldPoint(12, 20, 0), Pathfinder.clickDestination(scene(), new WorldPoint(12, 20, 0)));
	}

	@Test
	public void ridgeTileNearPlateauWalksToPlateau()
	{
		// 3 tiles south of the plateau, 9+ from the low ground: nearest reachable is straight north.
		assertEquals(new WorldPoint(15, 20, 0), Pathfinder.clickDestination(scene(), new WorldPoint(15, 17, 0)));
	}

	@Test
	public void ridgeTileNearLowGroundWalksTheLongWay()
	{
		// Closer to the low ground: the game would pick it even though the walk is long.
		assertEquals(new WorldPoint(15, 5, 0), Pathfinder.clickDestination(scene(), new WorldPoint(15, 8, 0)));
	}

	@Test
	public void tieGoesToShorterWalk()
	{
		// (10,19) is 1 tile from both (10,20) on the plateau and (10,18) if that were reachable;
		// make (10,18) reachable but a longer walk, and the shorter walk must win.
		int[] dist = new int[SIZE * SIZE];
		Arrays.fill(dist, -1);
		dist[10 * SIZE + 20] = 3;
		dist[10 * SIZE + 18] = 40;
		Pathfinder.DistanceMap map = new Pathfinder.DistanceMap(dist, SIZE, SIZE, 0, 0, 0);
		assertEquals(new WorldPoint(10, 20, 0), Pathfinder.clickDestination(map, new WorldPoint(10, 19, 0)));
	}

	@Test
	public void nothingReachableNearbyReturnsNull()
	{
		int[] dist = new int[SIZE * SIZE];
		Arrays.fill(dist, -1);
		assertNull(Pathfinder.clickDestination(new Pathfinder.DistanceMap(dist, SIZE, SIZE, 0, 0, 0), new WorldPoint(15, 15, 0)));
	}
}
