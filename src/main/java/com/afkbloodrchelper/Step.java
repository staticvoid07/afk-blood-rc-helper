package com.afkbloodrchelper;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * The concrete next action the player should take.
 */
@Getter
@RequiredArgsConstructor
public enum Step
{
	MINE_RUNESTONE("Mine the dense runestone until your inventory is full"),
	RUNESTONES_DEPLETED("Wait for a runestone to become dense"),
	CLIMB_TO_MINE_FROM_BLOOD("Climb the rocks back to the runestones"),
	CLIMB_TO_MINE_FROM_DARK("Climb down the rocks to the runestones"),
	CLIMB_TO_DARK_ALTAR("Climb the rocks to the Dark Altar"),
	VENERATE("Venerate at the Dark Altar"),
	CHISEL("Chisel your dark essence blocks"),
	CLIMB_TO_BLOOD_ALTAR("Climb the rocks towards the Blood Altar"),
	WALK_TO_BLOOD_ALTAR("Head to the Blood Altar"),
	STEP_CLOSER("Step closer to click the Blood Altar"),
	BIND("Bind at the Blood Altar"),
	ACTIVATE_ESSENCE("Activate your blood essence"),
	ENABLE_RUN("Turn on run");

	private final String text;
}
