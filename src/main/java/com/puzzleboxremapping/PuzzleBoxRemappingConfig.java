package com.puzzleboxremapping;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

@ConfigGroup(PuzzleBoxRemappingConfig.CONFIG_GROUP)
public interface PuzzleBoxRemappingConfig extends Config
{
	String CONFIG_GROUP = "puzzleboxremapping";
	String INVERT_ARROW_KEYS_KEY = "invertArrowKeys";
	@ConfigItem(
		keyName = INVERT_ARROW_KEYS_KEY,
		name = "Invert Arrow Keys",
		description = "Inverts arrow key presses.<br>If off, the blank space moves with the keys.<br>If on, the tiles move onto the blank space."
	)
	default boolean invertArrowKeys()
	{
		return false;
	}
}
