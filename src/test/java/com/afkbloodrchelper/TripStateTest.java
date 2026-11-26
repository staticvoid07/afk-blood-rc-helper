package com.afkbloodrchelper;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import net.runelite.api.Item;
import net.runelite.api.ItemContainer;
import org.junit.Test;

public class TripStateTest
{
	// Inventory: chisel + blood essence take 2 slots, leaving 26 for essence.
	private static InventoryState inv(int dense, int dark, int fragments)
	{
		int used = 2 + dense + dark + (fragments > 0 ? 1 : 0);
		return new InventoryState(dense, dark, fragments, 0, 1, 28 - used, 0);
	}

	private final TripState trip = new TripState();
	private InventoryState previous;

	private Stage tick(InventoryState current)
	{
		trip.update(previous, current);
		previous = current;
		return trip.stage(current);
	}

	@Test
	public void fullLoop()
	{
		// Load 1
		assertEquals(Stage.MINE, tick(inv(0, 0, 0)));
		assertEquals(Stage.MINE, tick(inv(13, 0, 0)));
		assertEquals(Stage.VENERATE, tick(inv(26, 0, 0)));
		assertEquals(Stage.CHISEL, tick(inv(0, 26, 0)));
		assertEquals(Stage.CHISEL, tick(inv(0, 13, 52)));
		assertEquals(Stage.MINE, tick(inv(0, 0, 104)));

		// Load 2
		assertEquals(Stage.MINE, tick(inv(10, 0, 104)));
		assertEquals(Stage.VENERATE, tick(inv(25, 0, 104)));
		assertEquals(Stage.BIND, tick(inv(0, 25, 104)));
		assertEquals(Stage.CHISEL, tick(inv(0, 25, 0)));
		assertEquals(Stage.CHISEL, tick(inv(0, 10, 60)));
		assertEquals(Stage.BIND, tick(inv(0, 0, 100)));
		assertEquals(Stage.MINE, tick(inv(0, 0, 0)));
		assertFalse(trip.isCraftTrip());
	}

	@Test
	public void neverLeavesMiningBeforeInventoryIsFull()
	{
		tick(inv(0, 0, 104));
		for (int dense = 1; dense < 25; dense++)
		{
			assertEquals(Stage.MINE, tick(inv(dense, 0, 104)));
		}
		assertEquals(Stage.VENERATE, tick(inv(25, 0, 104)));
	}

	@Test
	public void keepsBindingAfterChiselingAtBloodAltar()
	{
		// Old plugin sent the player back to the runestones when standing >15 tiles from the altar here.
		tick(inv(25, 0, 104));
		tick(inv(0, 25, 104));
		tick(inv(0, 25, 0));
		assertEquals(Stage.BIND, tick(inv(0, 0, 100)));
		assertEquals(Stage.BIND, tick(inv(0, 0, 100)));
	}

	@Test
	public void relogMidCraftTripKeepsState()
	{
		tick(inv(25, 0, 104));
		assertEquals(Stage.BIND, tick(inv(0, 25, 104)));

		// Relog: flags come back from the RS profile, the previous inventory is unknown, and
		// the inventory may read as empty for a few ticks before it loads.
		TripState afterLogin = new TripState(trip.isCraftTrip(), trip.isFirstBindDone());
		InventoryState empty = new InventoryState(0, 0, 0, 0, 0, 28, 0);
		InventoryState loaded = inv(0, 25, 104);
		afterLogin.update(null, empty);
		afterLogin.update(empty, empty);
		afterLogin.update(empty, loaded);
		assertTrue(afterLogin.isCraftTrip());
		assertEquals(Stage.BIND, afterLogin.stage(loaded));
	}

	@Test
	public void staleCraftFlagDoesNotAffectFreshLoad()
	{
		TripState stale = new TripState(true, false);
		InventoryState empty = inv(0, 0, 0);
		stale.update(null, empty);
		assertEquals(Stage.MINE, stale.stage(empty));
		stale.update(empty, inv(26, 0, 0));
		stale.update(inv(26, 0, 0), inv(0, 26, 0));
		assertFalse(stale.isCraftTrip());
		assertEquals(Stage.CHISEL, stale.stage(inv(0, 26, 0)));
	}

	@Test
	public void relogAfterFirstBindStillChisels()
	{
		TripState afterLogin = new TripState(true, false);
		InventoryState loaded = inv(0, 25, 0);
		afterLogin.update(null, loaded);
		assertEquals(Stage.CHISEL, afterLogin.stage(loaded));
		assertTrue(afterLogin.isFirstBindDone());
	}

	@Test
	public void shortItemArrayAfterLoginIsNotFull()
	{
		// Old plugin treated a short item array with no -1 slots as a full inventory.
		Item[] items = new Item[12];
		for (int i = 0; i < items.length; i++)
		{
			items[i] = new Item(BloodRcIds.DENSE_ESSENCE_BLOCK, 1);
		}
		ItemContainer container = mock(ItemContainer.class);
		when(container.getItems()).thenReturn(items);

		InventoryState state = InventoryState.read(container);
		assertEquals(12, state.getDense());
		assertEquals(16, state.getFreeSlots());
		assertEquals(Stage.MINE, new TripState().stage(state));
	}

	private static InventoryState withRunes(int dark, int fragments, int runes)
	{
		return new InventoryState(0, dark, fragments, 0, 1, 26 - dark - (fragments > 0 ? 1 : 0), runes);
	}

	@Test
	public void boundDetectsBothBinds()
	{
		// First bind: fragments used, dark blocks remain (chisel next).
		assertTrue(TripState.bound(withRunes(25, 104, 0), withRunes(25, 0, 100)));
		// Final bind: nothing left (73 rocks next).
		assertTrue(TripState.bound(withRunes(0, 100, 100), withRunes(0, 0, 200)));
		// Works without any trip flags (e.g. started mid-trip) since it only looks at the inventory.
		assertTrue(TripState.bound(withRunes(0, 5, 0), withRunes(0, 0, 5)));
		// Chiseling adds fragments, no runes.
		assertFalse(TripState.bound(withRunes(25, 0, 100), withRunes(24, 4, 100)));
		// Runes arriving a tick after the fragments are used still counts.
		assertTrue(TripState.bound(withRunes(0, 100, 100), withRunes(0, 0, 100)));
		// Unknown previous inventory (just logged in).
		assertFalse(TripState.bound(null, withRunes(0, 0, 200)));
	}

	@Test
	public void veneratedDetectsBothLoads()
	{
		assertTrue(TripState.venerated(inv(26, 0, 0), inv(0, 26, 0)));
		assertTrue(TripState.venerated(inv(25, 0, 104), inv(0, 25, 104)));
		assertFalse(TripState.venerated(inv(25, 0, 104), inv(26, 0, 104)));
		assertFalse(TripState.venerated(inv(0, 26, 0), inv(0, 25, 4)));
		assertFalse(TripState.venerated(null, inv(0, 26, 0)));
	}

	@Test
	public void finishedChiselingOnlyOnLastBlock()
	{
		// Last block chiseled (load 1 at the Dark Altar, or load 2 at the Blood Altar).
		assertTrue(TripState.finishedChiseling(inv(0, 1, 100), inv(0, 0, 104)));
		// Still blocks left.
		assertFalse(TripState.finishedChiseling(inv(0, 10, 60), inv(0, 9, 64)));
		// Binding with no blocks: no dark blocks before either.
		assertFalse(TripState.finishedChiseling(inv(0, 0, 100), inv(0, 0, 0)));
		// Fragment stack updating a tick later still counts.
		assertTrue(TripState.finishedChiseling(inv(0, 1, 100), inv(0, 0, 100)));
		// Unknown previous inventory (just logged in).
		assertFalse(TripState.finishedChiseling(null, inv(0, 0, 104)));
	}

	@Test
	public void firstLoadChiselingNeverLooksLikeSecondLap()
	{
		// Even with a stale "craft trip" flag saved from an earlier session...
		TripState stale = new TripState(true, true);
		InventoryState mining = inv(10, 0, 0);
		stale.update(null, mining);
		assertFalse(stale.isCraftTrip());

		InventoryState full = inv(26, 0, 0);
		stale.update(mining, full);
		InventoryState venerated = inv(0, 26, 0);
		stale.update(full, venerated);
		assertEquals(Stage.CHISEL, stale.stage(venerated));

		// ...every chisel of load 1 stays CHISEL, never BIND.
		InventoryState previous = venerated;
		for (int dark = 25; dark >= 1; dark--)
		{
			InventoryState current = inv(0, dark, (26 - dark) * 4);
			stale.update(previous, current);
			assertEquals("dark=" + dark, Stage.CHISEL, stale.stage(current));
			previous = current;
		}
	}
}
