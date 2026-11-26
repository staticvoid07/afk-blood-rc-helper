package com.afkbloodrchelper;

import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import javax.inject.Inject;
import net.runelite.api.Client;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;

/**
 * Solace watermark fixed to the center of the client, used when "Center Watermark" is on.
 */
class CenteredWatermarkOverlay extends Overlay
{
	private final Client client;
	private final AfkBloodRcHelperPlugin plugin;
	private final AfkBloodRcHelperConfig config;
	private final SolaceLogo logo;

	@Inject
	private CenteredWatermarkOverlay(Client client, AfkBloodRcHelperPlugin plugin, AfkBloodRcHelperConfig config, SolaceLogo logo)
	{
		this.client = client;
		this.plugin = plugin;
		this.config = config;
		this.logo = logo;

		setPosition(OverlayPosition.DYNAMIC);
		setLayer(OverlayLayer.ALWAYS_ON_TOP);
		// Dynamic overlays render in ascending priority: above the tint (low), below the highlights (high).
		setPriority(PRIORITY_MED);
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		if (!plugin.isInArea() || !config.centerWatermark())
		{
			return null;
		}
		BufferedImage image = logo.image();
		int x = (client.getCanvasWidth() - image.getWidth()) / 2;
		int y = (client.getCanvasHeight() - image.getHeight()) / 2;
		logo.draw(graphics, x, y);
		return null;
	}
}
