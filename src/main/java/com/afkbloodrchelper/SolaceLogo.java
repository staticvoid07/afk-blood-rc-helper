package com.afkbloodrchelper;

import java.awt.AlphaComposite;
import java.awt.Composite;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.client.util.ImageUtil;

/**
 * The Solace logo, scaled to the configured size and drawn at a fixed transparency.
 */
@Singleton
class SolaceLogo
{
	private static final float ALPHA = 100f / 255f;
	private static final int MIN_SIZE = 50;

	private final AfkBloodRcHelperConfig config;
	private final BufferedImage logo;

	private BufferedImage scaled;
	private int scaledSize = -1;

	@Inject
	private SolaceLogo(AfkBloodRcHelperConfig config)
	{
		this.config = config;
		this.logo = ImageUtil.loadImageResource(AfkBloodRcHelperPlugin.class, "solace_logo.png");
	}

	/**
	 * @return the logo scaled so its longest side is the configured size (at least 50px)
	 */
	BufferedImage image()
	{
		int size = Math.max(MIN_SIZE, config.watermarkSize());
		if (scaled == null || scaledSize != size)
		{
			scaled = ImageUtil.resizeImage(logo, size, size, true);
			scaledSize = size;
		}
		return scaled;
	}

	void draw(Graphics2D graphics, int x, int y)
	{
		Composite original = graphics.getComposite();
		graphics.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, ALPHA));
		graphics.drawImage(image(), x, y, null);
		graphics.setComposite(original);
	}
}
