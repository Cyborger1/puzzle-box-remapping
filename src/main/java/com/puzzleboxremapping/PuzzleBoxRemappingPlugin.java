package com.puzzleboxremapping;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.inject.Provides;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.ScriptPostFired;
import net.runelite.api.widgets.JavaScriptCallback;
import net.runelite.api.widgets.Widget;
import net.runelite.api.widgets.WidgetInfo;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;

@Slf4j
@PluginDescriptor(
	name = "Puzzle Box Remapping"
)
public class PuzzleBoxRemappingPlugin extends Plugin
{
	private static final int DIMENSION_X = 5;
	private static final int DIMENSION_Y = 5;
	private static final int MAX_INDEX = DIMENSION_X * DIMENSION_Y;

	private static final ImmutableSet<Integer> SCRIPT_IDS = ImmutableSet.of(689, 691);

	private static final int LEFT_ARROW_CODE = 96;
	private static final int UP_ARROW_CODE = 98;
	private static final int RIGHT_ARROW_CODE = 97;
	private static final int DOWN_ARROW_CODE = 99;

	private static final ImmutableMap<Integer, Integer> INVERSE_KEY_MAP =
		ImmutableMap.of(
			LEFT_ARROW_CODE, RIGHT_ARROW_CODE,
			RIGHT_ARROW_CODE, LEFT_ARROW_CODE,
			UP_ARROW_CODE, DOWN_ARROW_CODE,
			DOWN_ARROW_CODE, UP_ARROW_CODE
		);

	@Inject
	private Client client;

	@Inject
	private ClientThread clientThread;

	@Inject
	private PuzzleBoxRemappingConfig config;

	@Provides
	PuzzleBoxRemappingConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(PuzzleBoxRemappingConfig.class);
	}

	@Override
	protected void startUp() throws Exception
	{
		clientThread.invokeLater(() ->
		{
			setOnKeyListeners(true);
			setOnKeyListeners(false);
		});
	}

	@Override
	protected void shutDown() throws Exception
	{
		clientThread.invokeLater(() -> setOnKeyListeners(true));
	}

	@Subscribe
	private void onConfigChanged(ConfigChanged event)
	{
		/*
		if (event.getGroup().equals(PuzzleBoxRemappingConfig.CONFIG_GROUP)
			&& event.getKey().equals(PuzzleBoxRemappingConfig.INVERT_ARROW_KEYS_KEY))
		{
			clientThread.invokeLater(() ->
			{
				setOnKeyListeners(true);
				setOnKeyListeners(false);
			});
		}
		*/
	}

	@Subscribe
	private void onScriptPostFired(ScriptPostFired event)
	{
		if (event.getScriptId() == 690)
		{
			setOnKeyListeners(false);
		}
	}

	private void setOnKeyListeners(boolean clearListeners)
	{
		Widget puzzleBox = client.getWidget(WidgetInfo.PUZZLE_BOX);
		if (puzzleBox == null)
		{
			return;
		}

		for (int i = 0; i < MAX_INDEX; i++)
		{
			Widget tile = puzzleBox.getChild(i);
			if (clearListeners)
			{
				tile.setOnKeyListener((Object[]) null);
			}
			else
			{
				final int index = i;
				final Object[] onOpListener = tile.getOnOpListener();
				// Op index needs to be 1
				onOpListener[1] = 1;
				// Component ID of the puzzle box
				onOpListener[2] = puzzleBox.getId();
				// Current child id
				onOpListener[3] = index;
				tile.setOnKeyListener((JavaScriptCallback) e ->
				{
					if (e.getTypedKeyCode() == getKeyCode(puzzleBox, index))
					{
						clientThread.invokeLater(() -> client.runScript(onOpListener));
					}
				});
			}
		}
	}

	private static int getKeyCode(Widget puzzle, int index)
	{
		if (!isIndexValid(index))
		{
			return -1;
		}

		// Hidden is above
		int v = index - DIMENSION_X;
		if (isIndexValid(v) && puzzle.getChild(v).isHidden())
		{
			return DOWN_ARROW_CODE;
		}

		// Below
		v = index + DIMENSION_X;
		if (isIndexValid(v) && puzzle.getChild(v).isHidden())
		{
			return UP_ARROW_CODE;
		}

		// On the left
		if (index % DIMENSION_X != 0 && puzzle.getChild(index - 1).isHidden())
		{
			return RIGHT_ARROW_CODE;
		}

		// On the right
		v = index + 1;
		if (v % DIMENSION_X != 0 && puzzle.getChild(v).isHidden())
		{
			return LEFT_ARROW_CODE;
		}

		return -1;
	}

	private static int getKeyCode(Widget puzzle, int index, boolean invert)
	{
		int key = getKeyCode(puzzle, index);
		if (key >= 0 && invert && INVERSE_KEY_MAP.containsKey(key))
		{
			key = INVERSE_KEY_MAP.get(key);
		}
		return key;
	}

	private static boolean isIndexValid(int index)
	{
		return index >= 0 && index < MAX_INDEX;
	}
}
