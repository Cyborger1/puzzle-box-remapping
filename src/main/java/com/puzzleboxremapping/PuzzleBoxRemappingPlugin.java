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
	name = "Example"
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

	@Override
	protected void startUp() throws Exception
	{
	}

	@Override
	protected void shutDown() throws Exception
	{
	}

	@Subscribe
	public void onGameStateChanged(GameStateChanged gameStateChanged)
	{

	}

	@Provides
	PuzzleBoxRemappingConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(PuzzleBoxRemappingConfig.class);
	}

	@Subscribe
	private void onConfigChanged(ConfigChanged event)
	{
		if (event.getGroup().equals(PuzzleBoxRemappingConfig.CONFIG_GROUP)
			&& event.getKey().equals(PuzzleBoxRemappingConfig.INVERT_ARROW_KEYS_KEY))
		{
			clientThread.invokeLater(this::processPuzzleWidget);
		}
	}

	@Subscribe
	private void onScriptPostFired(ScriptPostFired event)
	{
		if (SCRIPT_IDS.contains(event.getScriptId()))
		{
			processPuzzleWidget();
		}
	}

	private void processPuzzleWidget()
	{
		Widget puzzleBox = client.getWidget(WidgetInfo.PUZZLE_BOX);
		if (puzzleBox == null)
		{
			return;
		}

		// Find the missing piece
		int missingIndex = -1;
		for (int i = 0; i < MAX_INDEX; i++)
		{
			Widget tile = puzzleBox.getChild(i);
			if (tile.isHidden())
			{
				missingIndex = i;
			}

			tile.setOnKeyListener((Object[]) null);
		}

		// If didn't find the missing piece somehow, quit.
		if (missingIndex == -1)
		{
			return;
		}

		AdjacentIndexes adj = getAdjacentIndexes(missingIndex);
		if (adj == null)
		{
			return;
		}

		addCallback(puzzleBox, adj.getAbove(), UP_ARROW_CODE);
		addCallback(puzzleBox, adj.getBelow(), DOWN_ARROW_CODE);
		addCallback(puzzleBox, adj.getLeft(), LEFT_ARROW_CODE);
		addCallback(puzzleBox, adj.getRight(), RIGHT_ARROW_CODE);
	}

	private void addCallback(Widget widget, int index, int keyCode)
	{
		if (widget == null || !isIndexValid(index))
		{
			return;
		}

		Widget tile = widget.getChild(index);
		if (tile == null)
		{
			return;
		}

		final int key;
		if (config.invertArrowKeys() && INVERSE_KEY_MAP.containsKey(keyCode))
		{
			key = INVERSE_KEY_MAP.get(keyCode);
		}
		else
		{
			key = keyCode;
		}

		final Object[] onOpListener = tile.getOnOpListener();
		tile.setOnKeyListener((JavaScriptCallback) e ->
		{
			System.out.println(e.getTypedKeyCode());
			if (e.getTypedKeyCode() == key) {
				System.out.println("Doing the script");
				clientThread.invokeLater(() -> client.runScript(onOpListener));
			}
		});
	}

	private static AdjacentIndexes getAdjacentIndexes(int index)
	{
		if (!isIndexValid(index))
		{
			return null;
		}

		AdjacentIndexes.AdjacentIndexesBuilder builder = AdjacentIndexes.builder();

		int v = index - DIMENSION_X;
		builder.above(isIndexValid(v) ? v : -1);

		v = index + DIMENSION_X;
		builder.below(isIndexValid(v) ? v : -1);

		builder.left(index % DIMENSION_X != 0 ? index - 1 : -1);

		v = index + 1;
		builder.right(v % DIMENSION_X != 0 ? v : -1);

		return builder.build();
	}

	private static boolean isIndexValid(int index)
	{
		return index > 0 && index < MAX_INDEX;
	}
}
