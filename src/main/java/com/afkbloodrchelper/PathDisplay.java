package com.afkbloodrchelper;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public enum PathDisplay
{
	FLOOR_AND_MINIMAP("Floor & minimap"),
	FLOOR("Floor only"),
	MINIMAP("Minimap only"),
	OFF("Off");

	private final String name;

	boolean floor()
	{
		return this == FLOOR_AND_MINIMAP || this == FLOOR;
	}

	boolean minimap()
	{
		return this == FLOOR_AND_MINIMAP || this == MINIMAP;
	}

	@Override
	public String toString()
	{
		return name;
	}
}
