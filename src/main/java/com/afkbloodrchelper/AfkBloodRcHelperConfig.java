package com.afkbloodrchelper;

import java.awt.Color;
import net.runelite.client.config.Alpha;
import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.ConfigSection;
import net.runelite.client.config.Notification;
import net.runelite.client.config.Range;
import net.runelite.client.config.Units;

@ConfigGroup(AfkBloodRcHelperConfig.GROUP)
public interface AfkBloodRcHelperConfig extends Config
{
	String GROUP = "afkbloodrchelper";

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
		keyName = "showInfoPanel",
		name = "Show Info Panel",
		description = "Show the info panel with the current blood runecrafting stage.",
		position = 0
	)
	default boolean showInfoPanel()
	{
		return true;
	}

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

	@ConfigItem(
		keyName = "stepCloserToBloodAltar",
		name = "Step Closer to Blood Altar",
		description = "After venerating the second load, step a few tiles south of the Dark Altar and click the Blood Altar from there instead of walking.",
		position = 3
	)
	default boolean stepCloserToBloodAltar()
	{
		return false;
	}

	@ConfigItem(
		keyName = "notifyAction",
		name = "Notify When Action Needed",
		description = "Send a notification when you need to do something (once each time you go from busy to needing to act).",
		position = 5
	)
	default Notification notifyAction()
	{
		return Notification.OFF;
	}

	@Range(min = 0, max = 5000)
	@Units(Units.MILLISECONDS)
	@ConfigItem(
		keyName = "idleDelay",
		name = "Idle Delay",
		description = "How long the player must stand still before being considered idle.",
		position = 4
	)
	default int idleDelay()
	{
		return 600;
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

	@ConfigItem(
		keyName = "pathDisplay",
		name = "Path Display",
		description = "Where to draw the walking path to the next destination.",
		section = sectionAction,
		position = 3
	)
	default PathDisplay pathDisplay()
	{
		return PathDisplay.FLOOR_AND_MINIMAP;
	}

	@Alpha
	@ConfigItem(
		keyName = "pathColor",
		name = "Path Color",
		description = "Color of the path line.",
		section = sectionAction,
		position = 4
	)
	default Color pathColor()
	{
		return new Color(0, 200, 255, 200);
	}

	@Alpha
	@ConfigItem(
		keyName = "actionColor",
		name = "Color",
		description = "Highlight color.",
		section = sectionAction,
		position = 5
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
		position = 6
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
		position = 7
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
		position = 8
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
		position = 9
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
