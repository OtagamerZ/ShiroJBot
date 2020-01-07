package com.kuuhaku.handlers.games.rpg.dialogs;

import java.util.Random;

public class CombatDialog {
	public static String dialog() {
		final String[] dialogs = {
			"%actor% está pensando...",
				"É a vez de %actor%...",
				"%actor% está montando sua estratégia...",
				"%actor% está se preparando..."
		};

		return dialogs[new Random().nextInt(dialogs.length - 1)];
	}
}
