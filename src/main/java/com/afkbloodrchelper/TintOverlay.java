package com.afkbloodrchelper;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import javax.inject.Inject;
import net.runelite.api.Client;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;

/**
 * Tints the entire client canvas, including the inventory, chatbox and minimap.
 * <p>
 * The old plugin drew this after the inventory widget layer, so anything rendered later
 * (the side panel around the inventory, tabs, etc.) was left untinted. Drawing it on the
 * always-on-top layer covers the whole screen.
 */
class TintOverlay extends Overlay
{
	private final Client client;
	private final AfkBloodRcHelperPlugin plugin;
	private final AfkBloodRcHelperConfig config;

	@Inject
	private TintOverlay(Client client, AfkBloodRcHelperPlugin plugin, AfkBloodRcHelperConfig config)
	{
		this.client = client;
		this.plugin = plugin;
		this.config = config;

		setPosition(OverlayPosition.DYNAMIC);
		setLayer(OverlayLayer.ALWAYS_ON_TOP);
		setPriority(PRIORITY_LOW);
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		if (!plugin.isInArea() || plugin.isLevelUpOpen())
		{
			return null;
		}

		if (plugin.isIdle())
		{
			if (config.idleTint())
			{
				tint(graphics, plugin.currentIdleTintColor());
			}
		}
		else if (config.activeTint())
		{
			tint(graphics, config.activeTintColor());
		}
		return null;
	}

	private void tint(Graphics2D graphics, Color color)
	{
		graphics.setColor(color);
		graphics.fillRect(0, 0, client.getCanvasWidth(), client.getCanvasHeight());
	}
}
