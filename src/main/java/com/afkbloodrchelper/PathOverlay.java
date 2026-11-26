package com.afkbloodrchelper;

import java.awt.BasicStroke;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.util.List;
import javax.inject.Inject;
import net.runelite.api.Client;
import net.runelite.api.Perspective;
import net.runelite.api.Point;
import net.runelite.api.WorldView;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;

/**
 * Draws the walking path to the next destination on the floor.
 */
class PathOverlay extends Overlay
{
	private final Client client;
	private final AfkBloodRcHelperPlugin plugin;
	private final AfkBloodRcHelperConfig config;

	@Inject
	private PathOverlay(Client client, AfkBloodRcHelperPlugin plugin, AfkBloodRcHelperConfig config)
	{
		this.client = client;
		this.plugin = plugin;
		this.config = config;

		setPosition(OverlayPosition.DYNAMIC);
		setLayer(OverlayLayer.ABOVE_SCENE);
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		List<WorldPoint> path = plugin.getPath();
		if (!plugin.isInArea() || !config.pathDisplay().floor() || path.size() < 2)
		{
			return null;
		}

		WorldView worldView = client.getTopLevelWorldView();
		graphics.setColor(config.pathColor());
		graphics.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

		Point previous = null;
		for (WorldPoint point : path)
		{
			LocalPoint local = LocalPoint.fromWorld(worldView, point);
			Point current = local == null ? null : Perspective.localToCanvas(client, local, worldView.getPlane());
			if (previous != null && current != null)
			{
				graphics.drawLine(previous.getX(), previous.getY(), current.getX(), current.getY());
			}
			previous = current;
		}
		return null;
	}
}
