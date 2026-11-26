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
		return new InventoryState(dense, dark, fragments, 0, 1, 28 - used);
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
		InventoryState empty = new InventoryState(0, 0, 0, 0, 0, 28);
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
}
