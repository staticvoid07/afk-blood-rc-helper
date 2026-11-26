package com.afkbloodrchelper;

import lombok.Value;
import net.runelite.api.TileObject;
import net.runelite.api.coords.WorldArea;

/**
 * What the overlays should highlight.
 * <p>
 * For objects, the object itself is flashed whenever it is clickable on screen.
 * If it isn't, the optional fallback area of tiles is highlighted instead so the
 * player knows which way to walk.
 */
@Value
class Target
{
	static final Target NONE = new Target(null, null, -1, false);
	static final Target RUN_ORB = new Target(null, null, -1, true);

	TileObject object;
	WorldArea fallbackArea;
	int itemId;
	boolean runOrb;

	static Target object(TileObject object, WorldArea fallbackArea)
	{
		return new Target(object, fallbackArea, -1, false);
	}

	static Target item(int itemId)
	{
		return new Target(null, null, itemId, false);
	}
}
