package com.afkbloodrchelper;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import javax.inject.Inject;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPanel;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.components.LineComponent;
import net.runelite.client.ui.overlay.components.TitleComponent;

/**
 * Info panel with the current blood runecrafting stage.
 */
class InfoPanelOverlay extends OverlayPanel
{
	private static final Color TITLE_COLOR = new Color(70, 140, 255);

	private final AfkBloodRcHelperPlugin plugin;
	private final AfkBloodRcHelperConfig config;

	@Inject
	private InfoPanelOverlay(AfkBloodRcHelperPlugin plugin, AfkBloodRcHelperConfig config)
	{
		super(plugin);
		this.plugin = plugin;
		this.config = config;

		setPosition(OverlayPosition.TOP_LEFT);
		// Same layer as the full-screen tint. Within a layer, DYNAMIC overlays (the tint and
		// highlights) render before positioned ones, so the panel draws on top of the tint.
		setLayer(OverlayLayer.ALWAYS_ON_TOP);
		setPriority(PRIORITY_MED);
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		if (!plugin.isInArea() || !config.showInfoPanel())
		{
			return null;
		}
		buildPanel();
		return super.render(graphics);
	}

	private void buildPanel()
	{
		panelComponent.getChildren().clear();
		panelComponent.getChildren().add(TitleComponent.builder()
			.text("Blood Runecrafting")
			.color(TITLE_COLOR)
			.build());

		Stage stage = plugin.getStage();
		InventoryState inv = plugin.getInventory();
		if (stage == null || inv == null)
		{
			panelComponent.getChildren().add(LineComponent.builder()
				.left("Waiting for inventory...")
				.build());
			return;
		}

		panelComponent.getChildren().add(LineComponent.builder()
			.left("Stage:")
			.right(stage.getLabel())
			.rightColor(stage.getColor())
			.build());

		panelComponent.getChildren().add(LineComponent.builder()
			.left("Load:")
			.right(loadText(stage, inv))
			.build());

		// The fragment stack always reports quantity 1 (the real count isn't in the item), so
		// only show whether any are carried.
		panelComponent.getChildren().add(LineComponent.builder()
			.left("Fragments:")
			.right(inv.getFragments() > 0 ? "Carrying" : "None")
			.build());

		panelComponent.getChildren().add(LineComponent.builder()
			.left("Status:")
			.right(plugin.isIdle() ? "Idle" : "Active")
			.rightColor(plugin.isIdle() ? Color.ORANGE : Color.GREEN)
			.build());

		Step step = plugin.getStep();
		if (step != null)
		{
			panelComponent.getChildren().add(LineComponent.builder()
				.left(step.getText())
				.leftColor(Color.YELLOW)
				.build());
		}
	}

	/**
	 * Load 1 is chiseled into fragments at the Dark Altar, load 2 is taken to the Blood Altar.
	 */
	private String loadText(Stage stage, InventoryState inv)
	{
		boolean secondLoad;
		switch (stage)
		{
			case MINE:
			case VENERATE:
				secondLoad = inv.getFragments() > 0;
				break;
			case BIND:
				secondLoad = true;
				break;
			default:
				secondLoad = plugin.isCraftTrip();
				break;
		}
		return secondLoad ? "2 / 2 (craft)" : "1 / 2 (fragments)";
	}
}
