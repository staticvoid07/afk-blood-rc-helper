package com.afkbloodrchelper;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.image.BufferedImage;
import java.text.NumberFormat;
import javax.inject.Inject;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.components.ComponentConstants;
import net.runelite.client.ui.overlay.components.LineComponent;
import net.runelite.client.ui.overlay.components.PanelComponent;
import net.runelite.client.ui.overlay.components.TitleComponent;
import net.runelite.client.util.ImageUtil;

/**
 * Solace logo watermark with the stage info panel directly below it.
 */
class SolaceOverlay extends Overlay
{
	private static final int GAP = 4;
	private static final float WATERMARK_ALPHA = 100f / 255f;
	private static final int MIN_WATERMARK_SIZE = 50;
	private static final Color SOLACE_BLUE = new Color(70, 140, 255);

	private final AfkBloodRcHelperPlugin plugin;
	private final AfkBloodRcHelperConfig config;
	private final PanelComponent panel = new PanelComponent();
	private final BufferedImage logo;

	private BufferedImage scaledLogo;
	private int scaledLogoSize = -1;

	@Inject
	private SolaceOverlay(AfkBloodRcHelperPlugin plugin, AfkBloodRcHelperConfig config)
	{
		this.plugin = plugin;
		this.config = config;
		this.logo = ImageUtil.loadImageResource(AfkBloodRcHelperPlugin.class, "solace_logo.png");

		setPosition(OverlayPosition.TOP_LEFT);
		// Same layer as the full-screen tint. Within a layer, DYNAMIC overlays (the tint and
		// highlights) render before positioned ones, so the logo and panel draw on top of the tint.
		setLayer(OverlayLayer.ALWAYS_ON_TOP);
		setPriority(PRIORITY_LOW);
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		if (!plugin.isInArea())
		{
			return null;
		}

		// The watermark is always shown in the area and can't be turned off.
		BufferedImage image = getScaledLogo();
		boolean showPanel = config.showInfoPanel();
		int width = showPanel ? Math.max(image.getWidth(), ComponentConstants.STANDARD_WIDTH) : image.getWidth();

		Composite original = graphics.getComposite();
		graphics.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, WATERMARK_ALPHA));
		graphics.drawImage(image, (width - image.getWidth()) / 2, 0, null);
		graphics.setComposite(original);
		int height = image.getHeight();

		if (showPanel)
		{
			height += GAP;
			buildPanel();
			panel.setPreferredLocation(new Point(0, height));
			panel.setPreferredSize(new Dimension(width, 0));
			Dimension panelSize = panel.render(graphics);
			if (panelSize != null)
			{
				height += panelSize.height;
				width = Math.max(width, panelSize.width);
			}
		}

		return new Dimension(width, height);
	}

	private void buildPanel()
	{
		panel.getChildren().clear();
		panel.getChildren().add(TitleComponent.builder()
			.text("Blood Runecrafting")
			.color(SOLACE_BLUE)
			.build());

		Stage stage = plugin.getStage();
		InventoryState inv = plugin.getInventory();
		if (stage == null || inv == null)
		{
			panel.getChildren().add(LineComponent.builder()
				.left("Waiting for inventory...")
				.build());
			return;
		}

		panel.getChildren().add(LineComponent.builder()
			.left("Stage:")
			.right(stage.getLabel())
			.rightColor(stage.getColor())
			.build());

		panel.getChildren().add(LineComponent.builder()
			.left("Load:")
			.right(loadText(stage, inv))
			.build());

		panel.getChildren().add(LineComponent.builder()
			.left("Fragments:")
			.right(NumberFormat.getIntegerInstance().format(inv.getFragments()))
			.build());

		panel.getChildren().add(LineComponent.builder()
			.left("Status:")
			.right(plugin.isIdle() ? "Idle" : "Active")
			.rightColor(plugin.isIdle() ? Color.ORANGE : Color.GREEN)
			.build());

		Step step = plugin.getStep();
		if (step != null)
		{
			panel.getChildren().add(LineComponent.builder()
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

	private BufferedImage getScaledLogo()
	{
		int size = Math.max(MIN_WATERMARK_SIZE, config.watermarkSize());
		if (scaledLogo == null || scaledLogoSize != size)
		{
			scaledLogo = ImageUtil.resizeImage(logo, size, size, true);
			scaledLogoSize = size;
		}
		return scaledLogo;
	}
}
