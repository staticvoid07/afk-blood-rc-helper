package com.afkbloodrchelper;

import java.awt.Color;
import net.runelite.client.config.Alpha;
import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.ConfigSection;
import net.runelite.client.config.Range;
import net.runelite.client.config.Units;

@ConfigGroup(AfkBloodRcHelperConfig.GROUP)
public interface AfkBloodRcHelperConfig extends Config
{
	String GROUP = "afkbloodrchelper";

	@ConfigSection(
		name = "Solace Overlay",
		description = "Watermark and info panel shown while in the blood runecrafting area.",
		position = 0
	)
	String sectionSolace = "solace";

	@ConfigSection(
		name = "Highlight Next Action",
		description = "Highlight the next thing to click.",
		position = 10
	)
	String sectionAction = "action";

	@ConfigSection(
		name = "Idle Screen Tint",
		description = "Tint the whole screen when idle.",
		position = 20
	)
	String sectionIdleTint = "idleTint";

	@ConfigSection(
		name = "Active Screen Tint",
		description = "Tint the whole screen when active.",
		position = 30,
		closedByDefault = true
	)
	String sectionActiveTint = "activeTint";

	// ---- General ----

	@ConfigItem(
		keyName = "suggestRun",
		name = "Suggest Enabling Run",
		description = "Highlight the run orb when run is off and energy is over 50%.",
		position = 1
	)
	default boolean suggestRun()
	{
		return true;
	}

	@ConfigItem(
		keyName = "remindBloodEssence",
		name = "Remind Blood Essence",
		description = "Highlight an inactive blood essence when you aren't carrying an active one.",
		position = 2
	)
	default boolean remindBloodEssence()
	{
		return true;
	}

	@Range(min = 0, max = 5000)
	@Units(Units.MILLISECONDS)
	@ConfigItem(
		keyName = "idleDelay",
		name = "Idle Delay",
		description = "How long the player must stand still before being considered idle.",
		position = 3
	)
	default int idleDelay()
	{
		return 600;
	}

	// ---- Solace overlay ----

	@Range(min = 50, max = 256)
	@Units(Units.PIXELS)
	@ConfigItem(
		keyName = "watermarkSize",
		name = "Watermark Size",
		description = "Size of the Solace logo (longest side).",
		section = sectionSolace,
		position = 1
	)
	default int watermarkSize()
	{
		return 128;
	}

	@ConfigItem(
		keyName = "showInfoPanel",
		name = "Show Info Panel",
		description = "Show the current blood runecrafting stage below the watermark.",
		section = sectionSolace,
		position = 2
	)
	default boolean showInfoPanel()
	{
		return true;
	}

	// ---- Highlight ----

	@ConfigItem(
		keyName = "actionEnabled",
		name = "Enabled",
		description = "Highlight the next thing to click.",
		section = sectionAction,
		position = 1
	)
	default boolean actionEnabled()
	{
		return true;
	}

	@ConfigItem(
		keyName = "actionOnlyWhenIdle",
		name = "Only When Idle",
		description = "Only show the highlight while the player is idle.",
		section = sectionAction,
		position = 2
	)
	default boolean actionOnlyWhenIdle()
	{
		return true;
	}

	@Alpha
	@ConfigItem(
		keyName = "actionColor",
		name = "Color",
		description = "Highlight color.",
		section = sectionAction,
		position = 3
	)
	default Color actionColor()
	{
		return new Color(255, 255, 0, 128);
	}

	@ConfigItem(
		keyName = "actionFlash",
		name = "Flashing",
		description = "Flash between two colors.",
		section = sectionAction,
		position = 4
	)
	default boolean actionFlash()
	{
		return true;
	}

	@Alpha
	@ConfigItem(
		keyName = "actionColor2",
		name = "Color 2",
		description = "Second color used when flashing.",
		section = sectionAction,
		position = 5
	)
	default Color actionColor2()
	{
		return new Color(0, 255, 255, 128);
	}

	@Range(min = 1)
	@Units(Units.MILLISECONDS)
	@ConfigItem(
		keyName = "actionColor1Time",
		name = "Color 1 Time",
		description = "Milliseconds to show color 1 when flashing.",
		section = sectionAction,
		position = 6
	)
	default int actionColor1Time()
	{
		return 500;
	}

	@Range(min = 1)
	@Units(Units.MILLISECONDS)
	@ConfigItem(
		keyName = "actionColor2Time",
		name = "Color 2 Time",
		description = "Milliseconds to show color 2 when flashing.",
		section = sectionAction,
		position = 7
	)
	default int actionColor2Time()
	{
		return 500;
	}

	// ---- Idle tint ----

	@ConfigItem(
		keyName = "idleTint",
		name = "Enabled",
		description = "Tint the whole screen when idle.",
		section = sectionIdleTint,
		position = 1
	)
	default boolean idleTint()
	{
		return true;
	}

	@Alpha
	@ConfigItem(
		keyName = "idleTintColor",
		name = "Color",
		description = "Tint color.",
		section = sectionIdleTint,
		position = 2
	)
	default Color idleTintColor()
	{
		return new Color(255, 175, 175, 64);
	}

	@ConfigItem(
		keyName = "idleTintFlash",
		name = "Flashing",
		description = "Flash between two colors.",
		section = sectionIdleTint,
		position = 3
	)
	default boolean idleTintFlash()
	{
		return true;
	}

	@Alpha
	@ConfigItem(
		keyName = "idleTintColor2",
		name = "Color 2",
		description = "Second color used when flashing.",
		section = sectionIdleTint,
		position = 4
	)
	default Color idleTintColor2()
	{
		return new Color(0, 0, 0, 64);
	}

	@Range(min = 1)
	@Units(Units.MILLISECONDS)
	@ConfigItem(
		keyName = "idleTintColor1Time",
		name = "Color 1 Time",
		description = "Milliseconds to show color 1 when flashing.",
		section = sectionIdleTint,
		position = 5
	)
	default int idleTintColor1Time()
	{
		return 600;
	}

	@Range(min = 1)
	@Units(Units.MILLISECONDS)
	@ConfigItem(
		keyName = "idleTintColor2Time",
		name = "Color 2 Time",
		description = "Milliseconds to show color 2 when flashing.",
		section = sectionIdleTint,
		position = 6
	)
	default int idleTintColor2Time()
	{
		return 600;
	}

	// ---- Active tint ----

	@ConfigItem(
		keyName = "activeTint",
		name = "Enabled",
		description = "Tint the whole screen when active.",
		section = sectionActiveTint,
		position = 1
	)
	default boolean activeTint()
	{
		return false;
	}

	@Alpha
	@ConfigItem(
		keyName = "activeTintColor",
		name = "Color",
		description = "Tint color.",
		section = sectionActiveTint,
		position = 2
	)
	default Color activeTintColor()
	{
		return new Color(0, 255, 0, 64);
	}
}
