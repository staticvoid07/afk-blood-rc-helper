package com.afkbloodrchelper;

import com.google.inject.Provides;
import java.awt.Color;
import java.util.Collections;
import java.util.List;
import java.util.function.Predicate;
import javax.inject.Inject;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.GameObject;
import net.runelite.api.GameState;
import net.runelite.api.ItemContainer;
import net.runelite.api.Player;
import net.runelite.api.Scene;
import net.runelite.api.Tile;
import net.runelite.api.TileObject;
import net.runelite.api.coords.WorldArea;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.ClientTick;
import net.runelite.api.events.DecorativeObjectDespawned;
import net.runelite.api.events.DecorativeObjectSpawned;
import net.runelite.api.events.GameObjectDespawned;
import net.runelite.api.events.GameObjectSpawned;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.GroundObjectDespawned;
import net.runelite.api.events.GroundObjectSpawned;
import net.runelite.api.events.WallObjectDespawned;
import net.runelite.api.events.WallObjectSpawned;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.gameval.InventoryID;
import net.runelite.api.gameval.VarPlayerID;
import net.runelite.api.widgets.Widget;
import net.runelite.client.Notifier;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.RuneScapeProfileChanged;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.OverlayManager;

@Slf4j
@PluginDescriptor(
	name = "AFK Blood RC Helper",
	description = "Contextual highlights, screen tint and a stage panel for blood runecrafting",
	tags = {"runecrafting", "rc", "blood", "afk", "skilling", "arceuus"}
)
public class AfkBloodRcHelperPlugin extends Plugin
{
	private static final String KEY_CRAFT_TRIP = "craftTrip";
	private static final String KEY_FIRST_BIND_DONE = "firstBindDone";
	private static final int ANIMATION_IDLE = -1;

	/**
	 * How long an inventory change counts as activity. Covers short gaps without an animation,
	 * e.g. the first chisel (no animation) before automatic chiseling of the rest starts.
	 */
	private static final long INVENTORY_ACTIVITY_MILLIS = 1200;

	@Inject
	private Client client;

	@Inject
	private ClientThread clientThread;

	@Inject
	private AfkBloodRcHelperConfig config;

	@Inject
	private ConfigManager configManager;

	@Inject
	private OverlayManager overlayManager;

	@Inject
	private Notifier notifier;

	@Inject
	private HighlightOverlay highlightOverlay;

	@Inject
	private InventoryHighlightOverlay inventoryHighlightOverlay;

	@Inject
	private TintOverlay tintOverlay;

	@Inject
	private InfoPanelOverlay infoPanelOverlay;

	@Inject
	private PathOverlay pathOverlay;

	@Inject
	private PathMinimapOverlay pathMinimapOverlay;

	// Tracked scene objects. These are tracked regardless of where the player is, so they
	// are already known when the player logs in (or loads a new scene) inside the area.
	private TileObject runestoneNorth;
	private TileObject runestoneSouth;
	private TileObject darkAltar;
	private TileObject bloodAltar;
	private TileObject shortcut69;
	private TileObject shortcut73Top;
	private TileObject shortcut73Bottom;

	@Getter(AccessLevel.PACKAGE)
	private boolean inArea;

	private boolean idle = true;

	/**
	 * Set right after venerating, binding or chiseling the last block so the next step is highlighted (and the idle
	 * tint shown) immediately, without waiting for the animation to end or the idle delay.
	 * Cleared once the player acts: moves, changes the inventory (chisel, bind, venerate), or the
	 * stage changes. Animations aren't used, since the previous action's animation is often still
	 * playing when the prompt appears.
	 */
	private boolean promptImmediately;

	/** True once a notification was sent for the current "action needed" stretch; reset when busy again. */
	private boolean notifiedForAction;
	private WorldPoint promptLocation;
	private InventoryState promptInventory;
	private Stage promptStage;

	private long lastActiveMillis;
	private volatile long lastInventoryChangeMillis;

	@Getter(AccessLevel.PACKAGE)
	private boolean levelUpOpen;

	@Getter(AccessLevel.PACKAGE)
	private Stage stage;

	@Getter(AccessLevel.PACKAGE)
	private Step step;

	@Getter(AccessLevel.PACKAGE)
	private Target target = Target.NONE;

	/**
	 * Walking path to the current target, recomputed every tick.
	 */
	@Getter(AccessLevel.PACKAGE)
	private List<WorldPoint> path = Collections.emptyList();
	private WorldPoint pathFrom;
	private Target pathTarget;

	/**
	 * Tiles to highlight for the Step Closer step (walkable or not): clicking any of them walks the
	 * player a short way to a tile within reach of the Blood Altar. Recomputed when the player moves.
	 */
	@Getter(AccessLevel.PACKAGE)
	private List<WorldPoint> stepCloserTiles = Collections.emptyList();
	private WorldPoint stepCloserFrom;

	@Getter(AccessLevel.PACKAGE)
	private InventoryState inventory;

	/**
	 * Inventory from the previous tick, used to detect venerating. Null right after
	 * logging in / hopping so that the (possibly not yet loaded) login inventory is
	 * never mistaken for a change made by the player.
	 */
	private InventoryState previousInventory;

	/**
	 * Which load of the loop the player is on. Persisted per account so it survives
	 * relogs and client restarts.
	 */
	private TripState tripState = new TripState();

	@Override
	protected void startUp()
	{
		overlayManager.add(tintOverlay);
		overlayManager.add(highlightOverlay);
		overlayManager.add(inventoryHighlightOverlay);
		overlayManager.add(infoPanelOverlay);
		overlayManager.add(pathOverlay);
		overlayManager.add(pathMinimapOverlay);

		clientThread.invokeLater(() ->
		{
			if (client.getGameState() == GameState.LOGGED_IN)
			{
				loadTripState();
				scanScene();
			}
		});
	}

	@Override
	protected void shutDown()
	{
		overlayManager.remove(tintOverlay);
		overlayManager.remove(highlightOverlay);
		overlayManager.remove(inventoryHighlightOverlay);
		overlayManager.remove(infoPanelOverlay);
		overlayManager.remove(pathOverlay);
		overlayManager.remove(pathMinimapOverlay);
		clearObjects();
		resetSessionState();
	}

	@Provides
	AfkBloodRcHelperConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(AfkBloodRcHelperConfig.class);
	}

	// ------------------------------------------------------------------------------------
	// Session / login handling
	// ------------------------------------------------------------------------------------

	@Subscribe
	public void onGameStateChanged(GameStateChanged event)
	{
		switch (event.getGameState())
		{
			case LOADING:
				// The whole scene is reloaded; every object will spawn again afterwards.
				clearObjects();
				break;
			case LOGIN_SCREEN:
			case HOPPING:
			case CONNECTION_LOST:
				clearObjects();
				resetSessionState();
				break;
			case LOGGED_IN:
				loadTripState();
				break;
		}
	}

	@Subscribe
	public void onRuneScapeProfileChanged(RuneScapeProfileChanged event)
	{
		loadTripState();
	}

	private void resetSessionState()
	{
		inArea = false;
		idle = true;
		stage = null;
		step = null;
		target = Target.NONE;
		inventory = null;
		previousInventory = null;
		clearPrompt();
		notifiedForAction = false;
		path = Collections.emptyList();
		stepCloserTiles = Collections.emptyList();
		stepCloserFrom = null;
		pathFrom = null;
		pathTarget = null;
	}

	private void loadTripState()
	{
		tripState = new TripState(readProfileFlag(KEY_CRAFT_TRIP), readProfileFlag(KEY_FIRST_BIND_DONE));
	}

	private boolean readProfileFlag(String key)
	{
		return Boolean.TRUE.equals(configManager.getRSProfileConfiguration(AfkBloodRcHelperConfig.GROUP, key, Boolean.class));
	}

	private void saveTripState(TripState before)
	{
		if (before.isCraftTrip() != tripState.isCraftTrip())
		{
			configManager.setRSProfileConfiguration(AfkBloodRcHelperConfig.GROUP, KEY_CRAFT_TRIP, tripState.isCraftTrip());
		}
		if (before.isFirstBindDone() != tripState.isFirstBindDone())
		{
			configManager.setRSProfileConfiguration(AfkBloodRcHelperConfig.GROUP, KEY_FIRST_BIND_DONE, tripState.isFirstBindDone());
		}
	}

	boolean isCraftTrip()
	{
		return tripState.isCraftTrip();
	}

	// ------------------------------------------------------------------------------------
	// Object tracking
	// ------------------------------------------------------------------------------------

	@Subscribe
	public void onGameObjectSpawned(GameObjectSpawned event)
	{
		onTileObject(null, event.getGameObject());
	}

	@Subscribe
	public void onGameObjectDespawned(GameObjectDespawned event)
	{
		onTileObject(event.getGameObject(), null);
	}

	@Subscribe
	public void onGroundObjectSpawned(GroundObjectSpawned event)
	{
		onTileObject(null, event.getGroundObject());
	}

	@Subscribe
	public void onGroundObjectDespawned(GroundObjectDespawned event)
	{
		onTileObject(event.getGroundObject(), null);
	}

	@Subscribe
	public void onWallObjectSpawned(WallObjectSpawned event)
	{
		onTileObject(null, event.getWallObject());
	}

	@Subscribe
	public void onWallObjectDespawned(WallObjectDespawned event)
	{
		onTileObject(event.getWallObject(), null);
	}

	@Subscribe
	public void onDecorativeObjectSpawned(DecorativeObjectSpawned event)
	{
		onTileObject(null, event.getDecorativeObject());
	}

	@Subscribe
	public void onDecorativeObjectDespawned(DecorativeObjectDespawned event)
	{
		onTileObject(event.getDecorativeObject(), null);
	}

	private void onTileObject(TileObject oldObject, TileObject newObject)
	{
		TileObject object = newObject != null ? newObject : oldObject;
		if (object == null)
		{
			return;
		}

		switch (object.getId())
		{
			case BloodRcIds.RUNESTONE_NORTH:
				runestoneNorth = replace(runestoneNorth, oldObject, newObject);
				break;
			case BloodRcIds.RUNESTONE_SOUTH:
				runestoneSouth = replace(runestoneSouth, oldObject, newObject);
				break;
			case BloodRcIds.DARK_ALTAR:
				darkAltar = replace(darkAltar, oldObject, newObject);
				break;
			case BloodRcIds.BLOOD_ALTAR:
				bloodAltar = replace(bloodAltar, oldObject, newObject);
				break;
			case BloodRcIds.SHORTCUT_69:
				shortcut69 = replace(shortcut69, oldObject, newObject);
				break;
			case BloodRcIds.SHORTCUT_73_TOP:
				shortcut73Top = replace(shortcut73Top, oldObject, newObject);
				break;
			case BloodRcIds.SHORTCUT_73_BOTTOM:
				shortcut73Bottom = replace(shortcut73Bottom, oldObject, newObject);
				break;
		}
	}

	/**
	 * Only clear a tracked object when the despawning object is the one being tracked.
	 */
	private static TileObject replace(TileObject current, TileObject oldObject, TileObject newObject)
	{
		if (newObject != null)
		{
			return newObject;
		}
		return current == oldObject ? null : current;
	}

	private void clearObjects()
	{
		runestoneNorth = null;
		runestoneSouth = null;
		darkAltar = null;
		bloodAltar = null;
		shortcut69 = null;
		shortcut73Top = null;
		shortcut73Bottom = null;
	}

	/**
	 * Spawn events are not fired for objects that already exist when the plugin is turned
	 * on while logged in, so look for them in the loaded scene.
	 */
	private void scanScene()
	{
		Scene scene = client.getTopLevelWorldView().getScene();
		Tile[][][] tiles = scene.getTiles();
		int plane = client.getTopLevelWorldView().getPlane();
		if (tiles == null || plane < 0 || plane >= tiles.length)
		{
			return;
		}

		for (Tile[] column : tiles[plane])
		{
			for (Tile tile : column)
			{
				if (tile == null)
				{
					continue;
				}
				GameObject[] gameObjects = tile.getGameObjects();
				if (gameObjects != null)
				{
					for (GameObject gameObject : gameObjects)
					{
						if (gameObject != null)
						{
							onTileObject(null, gameObject);
						}
					}
				}
				if (tile.getGroundObject() != null)
				{
					onTileObject(null, tile.getGroundObject());
				}
				if (tile.getWallObject() != null)
				{
					onTileObject(null, tile.getWallObject());
				}
				if (tile.getDecorativeObject() != null)
				{
					onTileObject(null, tile.getDecorativeObject());
				}
			}
		}
	}

	// ------------------------------------------------------------------------------------
	// Idle detection
	// ------------------------------------------------------------------------------------

	@Subscribe
	public void onClientTick(ClientTick tick)
	{
		Player player = client.getLocalPlayer();
		if (!inArea || player == null)
		{
			return;
		}

		long now = System.currentTimeMillis();
		boolean active = player.getAnimation() != ANIMATION_IDLE
			|| player.getPoseAnimation() != player.getIdlePoseAnimation()
			|| now - lastInventoryChangeMillis < INVENTORY_ACTIVITY_MILLIS;

		if (active)
		{
			lastActiveMillis = now;
			idle = false;
		}
		else
		{
			idle = now - lastActiveMillis >= config.idleDelay();
		}
	}

	// ------------------------------------------------------------------------------------
	// Stage logic
	// ------------------------------------------------------------------------------------

	@Subscribe
	public void onGameTick(GameTick event)
	{
		Player player = client.getLocalPlayer();
		if (player == null)
		{
			return;
		}

		WorldPoint location = player.getWorldLocation();
		inArea = BloodRcIds.REGIONS.contains(location.getRegionID());
		if (!inArea)
		{
			stage = null;
			step = null;
			target = Target.NONE;
			previousInventory = null;
			path = Collections.emptyList();
			return;
		}

		levelUpOpen = client.getWidget(InterfaceID.LevelupDisplay.UNIVERSE) != null;

		ItemContainer container = client.getItemContainer(InventoryID.INV);
		if (container == null)
		{
			// Inventory not loaded yet (e.g. right after logging in). Don't guess.
			stage = null;
			step = null;
			target = Target.NONE;
			return;
		}

		InventoryState current = InventoryState.read(container);
		boolean inventoryChanged = previousInventory != null && !current.equals(previousInventory);
		if (inventoryChanged)
		{
			lastInventoryChangeMillis = System.currentTimeMillis();
		}
		// Venerating, binding or running out of blocks to chisel: show the next step right away.
		boolean promptNow = TripState.venerated(previousInventory, current)
			|| TripState.bound(previousInventory, current)
			|| TripState.finishedChiseling(previousInventory, current);
		TripState before = new TripState(tripState.isCraftTrip(), tripState.isFirstBindDone());
		tripState.update(previousInventory, current);
		saveTripState(before);
		previousInventory = current;
		inventory = current;

		decideNextAction(location, current, promptNow);

		notifyIfActionNeeded();
		updateStepCloserTiles(location);
		if (config.pathDisplay() == PathDisplay.OFF)
		{
			path = Collections.emptyList();
			pathFrom = null;
		}
		else if (!location.equals(pathFrom) || !target.equals(pathTarget))
		{
			// Only re-path when the player or the destination changed.
			path = findPath(location);
			pathFrom = location;
			pathTarget = target;
		}
	}

	private void decideNextAction(WorldPoint location, InventoryState inv, boolean promptNow)
	{
		stage = tripState.stage(inv);
		routeStage(location, inv);

		if (promptNow)
		{
			// Don't wait for the venerate/bind animation to finish or the idle delay.
			promptImmediately = true;
			promptLocation = location;
			promptInventory = inv;
			promptStage = stage;
		}
		else if (promptImmediately
			&& (stage != promptStage || !location.equals(promptLocation) || !inv.equals(promptInventory)))
		{
			clearPrompt();
		}

		boolean runEnabled = client.getVarpValue(VarPlayerID.OPTION_RUN) == 1;
		if (config.suggestRun() && !runEnabled && client.getEnergy() > 50 * 100
			&& client.getWidget(InterfaceID.Orbs.RUNBUTTON) != null)
		{
			step = Step.ENABLE_RUN;
			target = Target.RUN_ORB;
		}
		else if (config.remindBloodEssence() && inv.getInactiveEssence() > 0 && inv.getActiveEssence() == 0)
		{
			step = Step.ACTIVATE_ESSENCE;
			target = Target.item(BloodRcIds.BLOOD_ESSENCE_INACTIVE);
		}
	}

	private void routeStage(WorldPoint location, InventoryState inv)
	{
		boolean inBasin = BloodRcIds.inMineBasin(location);
		switch (stage)
		{
			case MINE:
				if (inBasin)
				{
					TileObject runestone = chooseRunestone(location);
					step = runestone == null ? Step.RUNESTONES_DEPLETED : Step.MINE_RUNESTONE;
					target = Target.object(runestone, null);
				}
				else if (BloodRcIds.onBloodAltarSide(location))
				{
					step = Step.CLIMB_TO_MINE_FROM_BLOOD;
					target = Target.object(shortcut73(), null);
				}
				else
				{
					step = Step.CLIMB_TO_MINE_FROM_DARK;
					target = Target.object(shortcut69, BloodRcIds.WALK_DARK_TO_ROCKS);
				}
				break;
			case VENERATE:
				if (inBasin)
				{
					step = Step.CLIMB_TO_DARK_ALTAR;
					target = Target.object(shortcut69, null);
				}
				else
				{
					step = Step.VENERATE;
					target = Target.object(darkAltar, null);
				}
				break;
			case CHISEL:
				step = Step.CHISEL;
				target = Target.item(nextChiselItem());
				break;
			case BIND:
				if (inBasin)
				{
					step = Step.CLIMB_TO_BLOOD_ALTAR;
					target = Target.object(shortcut73(), null);
				}
				else
				{
					routeToBloodAltar(location);
				}
				break;
		}
	}

	private void routeToBloodAltar(WorldPoint location)
	{
		boolean inReach = BloodRcIds.distanceToBloodAltar(location) <= BloodRcIds.BLOOD_ALTAR_REACH;
		if (bloodAltar != null && inReach)
		{
			// Close enough to click: the altar flashes as soon as it's on screen.
			step = Step.BIND;
			target = Target.object(bloodAltar, isNorthOf(location, BloodRcIds.WALK_DARK_TO_BLOOD) ? BloodRcIds.WALK_DARK_TO_BLOOD : null);
		}
		else if (config.stepCloserToBloodAltar() && bloodAltar != null
			&& location.distanceTo(BloodRcIds.DARK_APPROACH) <= 8)
		{
			// Altar is loaded but just out of reach from the Dark Altar: a few steps south fixes that.
			step = Step.STEP_CLOSER;
			target = Target.object(null, BloodRcIds.STEP_CLOSER_TO_BLOOD);
		}
		else
		{
			// Out of reach (clicking it wouldn't work) or not loaded: walk.
			step = Step.WALK_TO_BLOOD_ALTAR;
			target = Target.object(null, BloodRcIds.WALK_DARK_TO_BLOOD);
		}
	}

	private void notifyIfActionNeeded()
	{
		boolean actionNeeded = step != null && target != Target.NONE && isIdle() && !levelUpOpen;
		if (!actionNeeded)
		{
			notifiedForAction = false;
			return;
		}
		if (!notifiedForAction)
		{
			notifiedForAction = true;
			if (config.notifyAction().isEnabled())
			{
				notifier.notify(config.notifyAction(), "Blood RC: " + step.getText());
			}
		}
	}

	private void updateStepCloserTiles(WorldPoint location)
	{
		if (step != Step.STEP_CLOSER)
		{
			stepCloserTiles = Collections.emptyList();
			stepCloserFrom = null;
			return;
		}
		if (location.equals(stepCloserFrom))
		{
			return;
		}
		stepCloserFrom = location;

		Pathfinder.DistanceMap map = Pathfinder.distances(client.getTopLevelWorldView(), location);
		List<WorldPoint> tiles = new java.util.ArrayList<>();
		for (WorldPoint candidate : BloodRcIds.STEP_CLOSER_CANDIDATES.toWorldPointList())
		{
			WorldPoint destination = Pathfinder.clickDestination(map, candidate);
			if (destination != null
				&& BloodRcIds.distanceToBloodAltar(destination) <= BloodRcIds.BLOOD_ALTAR_REACH
				&& map.get(destination) <= BloodRcIds.STEP_CLOSER_MAX_WALK)
			{
				tiles.add(candidate);
			}
		}

		// To be safe, leave out the southmost row of what would be shown.
		int southmost = tiles.stream().mapToInt(WorldPoint::getY).min().orElse(Integer.MIN_VALUE);
		tiles.removeIf(p -> p.getY() == southmost);
		stepCloserTiles = tiles;
	}

	// ------------------------------------------------------------------------------------
	// Path
	// ------------------------------------------------------------------------------------

	private List<WorldPoint> findPath(WorldPoint location)
	{
		TileObject object = target.getObject();
		WorldArea area = target.getFallbackArea();
		if (object == null && area == null)
		{
			return Collections.emptyList();
		}

		if (object != null)
		{
			List<WorldPoint> toObject = Pathfinder.find(client.getTopLevelWorldView(), location, nextTo(object));
			if (!toObject.isEmpty())
			{
				return toObject;
			}
		}
		if (area != null)
		{
			List<WorldPoint> toArea = Pathfinder.find(client.getTopLevelWorldView(), location, area::contains);
			if (!toArea.isEmpty())
			{
				return toArea;
			}
		}
		if (step == Step.WALK_TO_BLOOD_ALTAR)
		{
			// The walk loops west around the ridge and can leave the loaded scene; use the
			// recorded route from the nearest waypoint onwards.
			return recordedBloodApproach(location);
		}
		return Collections.emptyList();
	}

	/**
	 * Goal: any tile next to (but not on) the object's footprint.
	 */
	private Predicate<WorldPoint> nextTo(TileObject object)
	{
		int minX, minY, maxX, maxY;
		int baseX = client.getTopLevelWorldView().getBaseX();
		int baseY = client.getTopLevelWorldView().getBaseY();
		if (object instanceof GameObject)
		{
			GameObject gameObject = (GameObject) object;
			minX = baseX + gameObject.getSceneMinLocation().getX();
			minY = baseY + gameObject.getSceneMinLocation().getY();
			maxX = baseX + gameObject.getSceneMaxLocation().getX();
			maxY = baseY + gameObject.getSceneMaxLocation().getY();
		}
		else
		{
			WorldPoint p = object.getWorldLocation();
			minX = maxX = p.getX();
			minY = maxY = p.getY();
		}
		final int x1 = minX, y1 = minY, x2 = maxX, y2 = maxY;
		return p ->
		{
			boolean inside = p.getX() >= x1 && p.getX() <= x2 && p.getY() >= y1 && p.getY() <= y2;
			return !inside && p.getX() >= x1 - 1 && p.getX() <= x2 + 1 && p.getY() >= y1 - 1 && p.getY() <= y2 + 1;
		};
	}

	private static List<WorldPoint> recordedBloodApproach(WorldPoint location)
	{
		List<WorldPoint> route = BloodRcIds.BLOOD_APPROACH;
		int nearest = 0;
		for (int i = 1; i < route.size(); i++)
		{
			if (location.distanceTo(route.get(i)) < location.distanceTo(route.get(nearest)))
			{
				nearest = i;
			}
		}
		List<WorldPoint> path = new java.util.ArrayList<>();
		path.add(location);
		path.addAll(route.subList(nearest, route.size()));
		return path;
	}

	private static boolean isNorthOf(WorldPoint location, WorldArea area)
	{
		return location.getY() >= area.getY() + area.getHeight();
	}

	private void clearPrompt()
	{
		promptImmediately = false;
		promptLocation = null;
		promptInventory = null;
		promptStage = null;
	}

	boolean isIdle()
	{
		return idle || promptImmediately;
	}

	private TileObject shortcut73()
	{
		return shortcut73Top != null ? shortcut73Top : shortcut73Bottom;
	}

	private TileObject chooseRunestone(WorldPoint location)
	{
		boolean northDense = runestoneNorth != null && client.getVarbitValue(BloodRcIds.VARBIT_RUNESTONE_NORTH) == 0;
		boolean southDense = runestoneSouth != null && client.getVarbitValue(BloodRcIds.VARBIT_RUNESTONE_SOUTH) == 0;

		if (northDense && southDense)
		{
			int north = location.distanceTo(runestoneNorth.getWorldLocation());
			int south = location.distanceTo(runestoneSouth.getWorldLocation());
			return north <= south ? runestoneNorth : runestoneSouth;
		}
		if (northDense)
		{
			return runestoneNorth;
		}
		if (southDense)
		{
			return runestoneSouth;
		}
		return null;
	}

	private int nextChiselItem()
	{
		if (client.isWidgetSelected())
		{
			Widget selected = client.getSelectedWidget();
			if (selected != null && selected.getItemId() == BloodRcIds.CHISEL)
			{
				return BloodRcIds.DARK_ESSENCE_BLOCK;
			}
		}
		return BloodRcIds.CHISEL;
	}

	// ------------------------------------------------------------------------------------
	// Colors
	// ------------------------------------------------------------------------------------

	Color currentHighlightColor()
	{
		return flash(config.actionFlash(), config.actionColor(), config.actionColor2(),
			config.actionColor1Time(), config.actionColor2Time());
	}

	Color currentIdleTintColor()
	{
		return flash(config.idleTintFlash(), config.idleTintColor(), config.idleTintColor2(),
			config.idleTintColor1Time(), config.idleTintColor2Time());
	}

	private static Color flash(boolean enabled, Color color1, Color color2, int time1, int time2)
	{
		if (enabled)
		{
			long cycle = Math.max(1, (long) time1 + time2);
			if (System.currentTimeMillis() % cycle >= time1)
			{
				return color2;
			}
		}
		return color1;
	}
}
