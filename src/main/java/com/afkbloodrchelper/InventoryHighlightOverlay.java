package com.afkbloodrchelper;

import java.awt.Graphics2D;
import java.awt.Rectangle;
import javax.inject.Inject;
import net.runelite.api.widgets.WidgetItem;
import net.runelite.client.ui.overlay.WidgetItemOverlay;

/**
 * Highlights the next inventory item to click (chisel, dark essence block, blood essence).
 */
class InventoryHighlightOverlay extends WidgetItemOverlay
{
	private final AfkBloodRcHelperPlugin plugin;
	private final AfkBloodRcHelperConfig config;

	@Inject
	private InventoryHighlightOverlay(AfkBloodRcHelperPlugin plugin, AfkBloodRcHelperConfig config)
	{
		this.plugin = plugin;
		this.config = config;
		showOnInventory();
	}

	@Override
	public void renderItemOverlay(Graphics2D graphics, int itemId, WidgetItem widgetItem)
	{
		if (!plugin.isInArea() || !config.actionEnabled())
		{
			return;
		}
		if (config.actionOnlyWhenIdle() && !plugin.isIdle())
		{
			return;
		}
		if (itemId != plugin.getTarget().getItemId())
		{
			return;
		}

		Rectangle bounds = widgetItem.getCanvasBounds();
		graphics.setColor(plugin.currentHighlightColor());
		graphics.fill(bounds);
	}
}
