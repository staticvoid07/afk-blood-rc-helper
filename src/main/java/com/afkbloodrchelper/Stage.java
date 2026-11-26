package com.afkbloodrchelper;

import java.awt.Color;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * High level stage of the blood runecrafting loop, shown in the info panel.
 */
@Getter
@RequiredArgsConstructor
public enum Stage
{
	MINE("Mining", new Color(190, 190, 190)),
	VENERATE("Venerating", new Color(170, 110, 255)),
	CHISEL("Chiseling", new Color(255, 170, 60)),
	BIND("Binding", new Color(230, 40, 40));

	private final String label;
	private final Color color;
}
