package com.puzzleboxremapping;

import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;

public class PuzzleBoxRemappingPluginTest
{
	public static void main(String[] args) throws Exception
	{
		ExternalPluginManager.loadBuiltin(PuzzleBoxRemappingPlugin.class);
		RuneLite.main(args);
	}
}