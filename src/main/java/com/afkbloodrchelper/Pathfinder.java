package com.afkbloodrchelper;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Predicate;
import net.runelite.api.CollisionData;
import net.runelite.api.CollisionDataFlag;
import net.runelite.api.WorldView;
import net.runelite.api.coords.WorldArea;
import net.runelite.api.coords.WorldPoint;

/**
 * Breadth-first walking path over the loaded scene's collision data. Every route this plugin
 * draws ends at the next shortcut or altar on the player's side, so no transports are needed.
 */
final class Pathfinder
{
	// Same neighbour order the game uses: W, E, S, N, SW, SE, NW, NE
	private static final int[] DX = {-1, 1, 0, 0, -1, 1, -1, 1};
	private static final int[] DY = {0, 0, -1, 1, -1, -1, 1, 1};

	private static final int BLOCKED = CollisionDataFlag.BLOCK_MOVEMENT_FULL;
	private static final int ALTERNATIVE_RANGE = 10;

	private Pathfinder()
	{
	}

	/**
	 * @return tiles from start (inclusive) to the first tile matching the goal, or an empty
	 * list when no goal tile is reachable inside the loaded scene
	 */
	static List<WorldPoint> find(WorldView worldView, WorldPoint start, Predicate<WorldPoint> goal)
	{
		int sizeX = worldView.getSizeX();
		int sizeY = worldView.getSizeY();
		int baseX = worldView.getBaseX();
		int baseY = worldView.getBaseY();
		int plane = start.getPlane();

		int sx = start.getX() - baseX;
		int sy = start.getY() - baseY;
		if (sx < 0 || sy < 0 || sx >= sizeX || sy >= sizeY)
		{
			return Collections.emptyList();
		}
		if (goal.test(start))
		{
			return Collections.singletonList(start);
		}

		int[] parent = new int[sizeX * sizeY];
		java.util.Arrays.fill(parent, -1);
		int[] queue = new int[sizeX * sizeY];
		int head = 0, tail = 0;
		int startIndex = sx * sizeY + sy;
		parent[startIndex] = startIndex;
		queue[tail++] = startIndex;

		while (head < tail)
		{
			int index = queue[head++];
			int x = index / sizeY;
			int y = index % sizeY;
			WorldArea tile = new WorldArea(baseX + x, baseY + y, 1, 1, plane);

			for (int d = 0; d < DX.length; d++)
			{
				int nx = x + DX[d];
				int ny = y + DY[d];
				if (nx < 0 || ny < 0 || nx >= sizeX || ny >= sizeY)
				{
					continue;
				}
				int next = nx * sizeY + ny;
				if (parent[next] != -1 || !tile.canTravelInDirection(worldView, DX[d], DY[d]))
				{
					continue;
				}
				parent[next] = index;

				WorldPoint point = new WorldPoint(baseX + nx, baseY + ny, plane);
				if (goal.test(point))
				{
					return backtrack(parent, next, startIndex, sizeY, baseX, baseY, plane);
				}
				queue[tail++] = next;
			}
		}
		return Collections.emptyList();
	}

	/**
	 * Walking distance (in steps) from start to every reachable tile of the loaded scene.
	 */
	static DistanceMap distances(WorldView worldView, WorldPoint start)
	{
		int sizeX = worldView.getSizeX();
		int sizeY = worldView.getSizeY();
		int baseX = worldView.getBaseX();
		int baseY = worldView.getBaseY();
		int plane = start.getPlane();

		int[] dist = new int[sizeX * sizeY];
		java.util.Arrays.fill(dist, -1);
		int sx = start.getX() - baseX;
		int sy = start.getY() - baseY;
		if (sx < 0 || sy < 0 || sx >= sizeX || sy >= sizeY)
		{
			return new DistanceMap(dist, sizeX, sizeY, baseX, baseY, plane);
		}

		int[] queue = new int[sizeX * sizeY];
		int head = 0, tail = 0;
		int startIndex = sx * sizeY + sy;
		dist[startIndex] = 0;
		queue[tail++] = startIndex;
		while (head < tail)
		{
			int index = queue[head++];
			int x = index / sizeY;
			int y = index % sizeY;
			WorldArea tile = new WorldArea(baseX + x, baseY + y, 1, 1, plane);
			for (int d = 0; d < DX.length; d++)
			{
				int nx = x + DX[d];
				int ny = y + DY[d];
				if (nx < 0 || ny < 0 || nx >= sizeX || ny >= sizeY)
				{
					continue;
				}
				int next = nx * sizeY + ny;
				if (dist[next] != -1 || !tile.canTravelInDirection(worldView, DX[d], DY[d]))
				{
					continue;
				}
				dist[next] = dist[index] + 1;
				queue[tail++] = next;
			}
		}
		return new DistanceMap(dist, sizeX, sizeY, baseX, baseY, plane);
	}

	/**
	 * Where the player ends up when clicking a tile, following the game's rule for unreachable
	 * destinations: the reachable tile within 10 tiles of the click that is closest to it
	 * (squared distance), ties broken by the shorter walk.
	 *
	 * @return the tile walked to, or null if nothing is reachable near the click
	 */
	static WorldPoint clickDestination(DistanceMap map, WorldPoint clicked)
	{
		if (map.get(clicked) >= 0)
		{
			return clicked;
		}
		WorldPoint best = null;
		int bestCost = Integer.MAX_VALUE;
		int bestDistance = Integer.MAX_VALUE;
		for (int dx = -ALTERNATIVE_RANGE; dx <= ALTERNATIVE_RANGE; dx++)
		{
			for (int dy = -ALTERNATIVE_RANGE; dy <= ALTERNATIVE_RANGE; dy++)
			{
				WorldPoint p = clicked.dx(dx).dy(dy);
				int distance = map.get(p);
				if (distance < 0)
				{
					continue;
				}
				int cost = dx * dx + dy * dy;
				if (cost < bestCost || (cost == bestCost && distance < bestDistance))
				{
					best = p;
					bestCost = cost;
					bestDistance = distance;
				}
			}
		}
		return best;
	}

	static final class DistanceMap
	{
		private final int[] dist;
		private final int sizeX;
		private final int sizeY;
		private final int baseX;
		private final int baseY;
		private final int plane;

		DistanceMap(int[] dist, int sizeX, int sizeY, int baseX, int baseY, int plane)
		{
			this.dist = dist;
			this.sizeX = sizeX;
			this.sizeY = sizeY;
			this.baseX = baseX;
			this.baseY = baseY;
			this.plane = plane;
		}

		/**
		 * @return steps to walk to the tile, or -1 if it can't be reached
		 */
		int get(WorldPoint p)
		{
			int x = p.getX() - baseX;
			int y = p.getY() - baseY;
			if (p.getPlane() != plane || x < 0 || y < 0 || x >= sizeX || y >= sizeY)
			{
				return -1;
			}
			return dist[x * sizeY + y];
		}
	}

	/**
	 * Whether a tile can be stood on (no full movement block), for highlighting walk-to tiles.
	 */
	static boolean isWalkable(WorldView worldView, WorldPoint point)
	{
		CollisionData[] maps = worldView.getCollisionMaps();
		int plane = point.getPlane();
		if (maps == null || plane < 0 || plane >= maps.length || maps[plane] == null)
		{
			return true; // unknown; don't hide anything
		}
		int x = point.getX() - worldView.getBaseX();
		int y = point.getY() - worldView.getBaseY();
		int[][] flags = maps[plane].getFlags();
		if (x < 0 || y < 0 || x >= flags.length || y >= flags[x].length)
		{
			return false;
		}
		return (flags[x][y] & BLOCKED) == 0;
	}

	private static List<WorldPoint> backtrack(int[] parent, int end, int start, int sizeY, int baseX, int baseY, int plane)
	{
		List<WorldPoint> path = new ArrayList<>();
		int index = end;
		while (true)
		{
			path.add(new WorldPoint(baseX + index / sizeY, baseY + index % sizeY, plane));
			if (index == start)
			{
				break;
			}
			index = parent[index];
		}
		Collections.reverse(path);
		return path;
	}
}
