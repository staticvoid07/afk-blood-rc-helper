package com.afkbloodrchelper;

import com.google.inject.Provides;
import java.awt.Color;
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
	tags = {"runecrafting", "rc", "blood", "afk", "skilling", "arceuus", "solace"}
)
public class AfkBloodRcHelperPlugin extends Plugin
{
	private static final String KEY_CRAFT_TRIP = "craftTrip";
	private static final String KEY_FIRST_BIND_DONE = "firstBindDone";
	private static final int ANIMATION_IDLE = -1;

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
	private HighlightOverlay highlightOverlay;

	@Inject
	private InventoryHighlightOverlay inventoryHighlightOverlay;

	@Inject
	private TintOverlay tintOverlay;

	@Inject
	private SolaceOverlay solaceOverlay;

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

	@Getter(AccessLevel.PACKAGE)
	private boolean idle = true;

	private long lastActiveMillis;

	@Getter(AccessLevel.PACKAGE)
	private boolean levelUpOpen;

	@Getter(AccessLevel.PACKAGE)
	private Stage stage;

	@Getter(AccessLevel.PACKAGE)
	private Step step;

	@Getter(AccessLevel.PACKAGE)
	private Target target = Target.NONE;

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
		overlayManager.add(solaceOverlay);

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
		overlayManager.remove(solaceOverlay);
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

		boolean active = player.getAnimation() != ANIMATION_IDLE
			|| player.getPoseAnimation() != player.getIdlePoseAnimation();

		long now = System.currentTimeMillis();
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
		TripState before = new TripState(tripState.isCraftTrip(), tripState.isFirstBindDone());
		tripState.update(previousInventory, current);
		saveTripState(before);
		previousInventory = current;
		inventory = current;

		decideNextAction(location, current);
	}

	private void decideNextAction(WorldPoint location, InventoryState inv)
	{
		stage = tripState.stage(inv);
		routeStage(location, inv);

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
					// Still north of the walk tiles means still on the way from the Dark Altar.
					boolean walking = location.getY() >= BloodRcIds.WALK_DARK_TO_BLOOD.getY() + BloodRcIds.WALK_DARK_TO_BLOOD.getHeight();
					step = walking ? Step.WALK_TO_BLOOD_ALTAR : Step.BIND;
					target = Target.object(bloodAltar, walking ? BloodRcIds.WALK_DARK_TO_BLOOD : null);
				}
				break;
		}
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
