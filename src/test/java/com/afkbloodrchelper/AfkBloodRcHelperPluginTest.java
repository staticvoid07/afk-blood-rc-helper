package com.afkbloodrchelper;

import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;

public class AfkBloodRcHelperPluginTest
{
	public static void main(String[] args) throws Exception
	{
		ExternalPluginManager.loadBuiltin(AfkBloodRcHelperPlugin.class);
		RuneLite.main(args);
	}
}
