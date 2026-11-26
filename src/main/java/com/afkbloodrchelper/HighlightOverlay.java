package com.afkbloodrchelper;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.geom.Area;
import java.util.List;
import javax.inject.Inject;
import net.runelite.api.Client;
import net.runelite.api.Perspective;
import net.runelite.api.TileObject;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldArea;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.widgets.Widget;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.OverlayUtil;

/**
 * Flashes the next object to click (or walk-to tiles when it isn't clickable) and the run orb.
 * Drawn on top of everything, above the screen tint.
 */
class HighlightOverlay extends Overlay
{
	private final Client client;
	private final AfkBloodRcHelperPlugin plugin;
	private final AfkBloodRcHelperConfig config;

	@Inject
	private HighlightOverlay(Client client, AfkBloodRcHelperPlugin plugin, AfkBloodRcHelperConfig config)
	{
		this.client = client;
		this.plugin = plugin;
		this.config = config;

		setPosition(OverlayPosition.DYNAMIC);
		setLayer(OverlayLayer.ALWAYS_ON_TOP);
		// Dynamic overlays render in ascending priority, so this draws after the (low priority) tint.
		setPriority(PRIORITY_HIGH);
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		if (!plugin.isInArea() || !config.actionEnabled())
		{
			return null;
		}
		if (config.actionOnlyWhenIdle() && !plugin.isIdle())
		{
			return null;
		}

		Target target = plugin.getTarget();
		Color color = plugin.currentHighlightColor();

		if (target.isRunOrb())
		{
			Widget runOrb = client.getWidget(InterfaceID.Orbs.RUNBUTTON);
			if (runOrb != null && !runOrb.isHidden())
			{
				OverlayUtil.renderPolygon(graphics, runOrb.getBounds(), color, color, new BasicStroke());
			}
			return null;
		}

		TileObject object = target.getObject();
		Shape clickbox = object != null ? object.getClickbox() : null;
		if (clickbox != null && isOnScreen(clickbox))
		{
			OverlayUtil.renderHoverableArea(graphics, clickbox, client.getMouseCanvasPosition(), color, color, color);
		}
		else if (target.getFallbackArea() != null)
		{
			List<WorldPoint> stepCloserTiles = plugin.getStepCloserTiles();
			if (target.getFallbackArea() == BloodRcIds.STEP_CLOSER_TO_BLOOD && !stepCloserTiles.isEmpty())
			{
				// Step Closer: every nearby tile (walkable or not) that walks the player into reach.
				for (WorldPoint point : stepCloserTiles)
				{
					highlightTile(graphics, point, color);
				}
			}
			else
			{
				highlightTiles(graphics, target.getFallbackArea(), color);
			}
		}
		return null;
	}

	/**
	 * An object counts as clickable when some part of its clickbox is inside the game viewport.
	 */
	private boolean isOnScreen(Shape clickbox)
	{
		Rectangle viewport = new Rectangle(
			client.getViewportXOffset(), client.getViewportYOffset(),
			client.getViewportWidth(), client.getViewportHeight());
		if (!clickbox.getBounds().intersects(viewport))
		{
			return false;
		}
		Area visible = new Area(clickbox);
		visible.intersect(new Area(viewport));
		return !visible.isEmpty();
	}

	private void highlightTiles(Graphics2D graphics, WorldArea area, Color color)
	{
		for (WorldPoint point : area.toWorldPointList())
		{
			if (Pathfinder.isWalkable(client.getTopLevelWorldView(), point))
			{
				highlightTile(graphics, point, color);
			}
		}
	}

	private void highlightTile(Graphics2D graphics, WorldPoint point, Color color)
	{
		LocalPoint local = LocalPoint.fromWorld(client.getTopLevelWorldView(), point);
		if (local == null)
		{
			return;
		}
		Polygon poly = Perspective.getCanvasTilePoly(client, local);
		if (poly != null)
		{
			OverlayUtil.renderPolygon(graphics, poly, color, color, new BasicStroke(1));
		}
	}
}
