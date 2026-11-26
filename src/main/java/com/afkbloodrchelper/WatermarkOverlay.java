package com.afkbloodrchelper;

import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import javax.inject.Inject;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;

/**
 * Movable Solace watermark (alt-drag to reposition). Always shown in the area unless the
 * centered watermark is enabled instead.
 */
class WatermarkOverlay extends Overlay
{
	private final AfkBloodRcHelperPlugin plugin;
	private final AfkBloodRcHelperConfig config;
	private final SolaceLogo logo;

	@Inject
	private WatermarkOverlay(AfkBloodRcHelperPlugin plugin, AfkBloodRcHelperConfig config, SolaceLogo logo)
	{
		this.plugin = plugin;
		this.config = config;
		this.logo = logo;

		setPosition(OverlayPosition.TOP_LEFT);
		// Same layer as the full-screen tint. Within a layer, DYNAMIC overlays (the tint and
		// highlights) render before positioned ones, so this draws on top of the tint.
		setLayer(OverlayLayer.ALWAYS_ON_TOP);
		// Above the info panel when both sit in the same corner.
		setPriority(PRIORITY_HIGH);
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		if (!plugin.isInArea() || config.centerWatermark())
		{
			return null;
		}
		BufferedImage image = logo.image();
		logo.draw(graphics, 0, 0);
		return new Dimension(image.getWidth(), image.getHeight());
	}
}
