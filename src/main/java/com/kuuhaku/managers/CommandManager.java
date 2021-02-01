/*
 * This file is part of Shiro J Bot.
 * Copyright (C) 2020  Yago Gimenez (KuuHaKu)
 *
 * Shiro J Bot is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Shiro J Bot is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Shiro J Bot.  If not, see <https://www.gnu.org/licenses/>
 */

package com.kuuhaku.managers;

import com.kuuhaku.command.Category;
import com.kuuhaku.command.Executable;
import com.kuuhaku.command.commands.PreparedCommand;
import com.kuuhaku.command.commands.discord.fun.*;
import com.kuuhaku.model.annotations.Command;
import com.kuuhaku.model.annotations.Requires;
import com.kuuhaku.utils.Helper;
import org.reflections.Reflections;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import static com.kuuhaku.command.Category.FUN;

public class CommandManager {

	private static final String REQ_MENTION = "req_mention";
	private static final String REQ_MESSAGE = "req_text";
	private static final String REQ_NAME = "req_name";
	private static final String REQ_SERVER_ID = "req_server-id";
	private static final String REQ_MENTION_REASON = "req_mention-id-reason";
	private static final String REQ_TEXT = "req_text";
	private static final String REQ_LINK = "req_link";
	private static final String REQ_QUESTION = "req_question";
	private static final String REQ_TWO_OPTIONS = "req_two-options";
	private static final String REQ_ID = "req_id";
	private static final String REQ_KEY_FILE = "req_key-file";
	private static final String REQ_CHANNEL = "req_channel";
	private static final String REQ_MENTION_BET = "req_mention-bet";
	private static final String REQ_COLOR = "req_color";
	private static final String REQ_ITEM = "req_item";
	private final HashMap<Class<? extends Executable>, Argument> commands = new HashMap<>() {
		{
			/*
			put(MatchStatsCommand.class, new Argument(
					"dadosdapartida", new String[]{"matchstats", "mstats", "estatisticas"}, REQ_ID, "cmd_match-stats", INFO, true
			));

			put(TranslateCommand.class, new Argument(
					"traduzir", new String[]{"translate", "traduza", "trad"}, "req_from-to-text", "cmd_translate", MISC, false
			));

			put(ChessCommand.class, new Argument(
					"xadrez", new String[]{"chess"}, REQ_MENTION, "cmd_chess", FUN, true
			));

			put(DisboardCommand.class, new Arguments(
					"disboard", new String[]{"exmap", "mapa"}, "cmd_disboard", EXCEED, false
			));
			*/

			//FUN
			put(SadButTrueCommand.class, new Argument(
					"tristemasverdade", new String[]{"tmv", "sadbuttrue", "sbt"}, "req_truth", "cmd_sad-but-true", FUN, false
			));
			put(HardDecisionCommand.class, new Argument(
					"doisbotoes", new String[]{"tb", "twobuttons", "buttons"}, REQ_TWO_OPTIONS, "cmd_two-buttons", FUN, false
			));
			put(ExpandingBrainCommand.class, new Argument(
					"menteexpandida", new String[]{"eb", "expandingbrain", "brain"}, "req_four-options", "cmd_expanded-brain", FUN, false
			));
			put(JojoCommand.class, new Argument(
					"jojo", new String[]{"jj", "kickhim", "chutaele"}, "req_two-mentions-message", "cmd_jojo", FUN, false
			));
			put(RPSCommand.class, new Argument(
					"jankenpon", new String[]{"ppt", "rps", "jokenpo", "janken"}, "req_jakenpon", "cmd_jankenpon", FUN, false
			));
			put(ShipCommand.class, new Argument(
					"ship", new String[]{"shippar"}, "req_two-mentions", "cmd_ship", FUN, false
			));
			put(MarryCommand.class, new Argument(
					"casar", new String[]{"declarar", "marry"}, REQ_MENTION, "cmd_marry", FUN, false
			));
			put(StonksCommand.class, new Argument(
					"stonks", new String[]{"stks"}, REQ_TEXT, "cmd_stonks", FUN, false
			));
			put(NotStonksCommand.class, new Argument(
					"notstonks", new String[]{"notstks", "stinks"}, REQ_TEXT, "cmd_stinks", FUN, false
			));
			put(GuessIllDieCommand.class, new Argument(
					"guessilldie", new String[]{"gid", "achoquevoumorrer", "meh"}, REQ_TEXT, "cmd_guess-ill-die", FUN, false
			));
			put(PatheticCommand.class, new Argument(
					"patetico", new String[]{"pathetic"}, REQ_TEXT, "cmd_pathetic", FUN, false
			));
			put(DrakeCommand.class, new Argument(
					"drake", new String[]{"drk"}, REQ_TWO_OPTIONS, "cmd_drake", FUN, false
			));
			put(SpiderManCommand.class, new Argument(
					"homemaranha", new String[]{"spiderman", "spoda", "miranha"}, REQ_TEXT, "cmd_spider-man", FUN, false
			));
			put(TomCruiseCommand.class, new Argument(
					"tomcruise", new String[]{"vainessa", "iludido", "noyoullnot"}, REQ_TEXT, "cmd_tom-cruise", FUN, false
			));
			put(PixelCanvasCommand.class, new Argument(
					"canvas", new String[]{"pixel", "pixelcanvas"}, "req_x-y-color", "cmd_canvas", FUN, false
			));
			put(PixelChunkCommand.class, new Argument(
					"chunk", new String[]{"zone", "pixelchunk"}, "req_zone-x-y-color", "cmd_canvas-chunk", FUN, false
			));
			put(DivorceCommand.class, new Argument(
					"divorciar", new String[]{"separar", "divorce"}, "cmd_divorce", FUN, false
			));
			put(SlotsCommand.class, new Argument(
					"slots", new String[]{"roleta"}, "req_bet", "cmd_slots", FUN, false
			));
			put(QuizCommand.class, new Argument(
					"quiz", new String[]{"qna", "per"}, "req_difficulty", "cmd_quiz", FUN, true
			));
			put(GuessTheNumberCommand.class, new Argument(
					"adivinheonumero", new String[]{"aon", "guessthenumber", "gtn"}, "cmd_guess-the-number", FUN, true
			));
		}
	};

	public Set<PreparedCommand> getCommands() {
		Reflections refl = new Reflections(this.getClass().getPackageName());
		Set<Class<?>> cmds = refl.getTypesAnnotatedWith(Command.class);
		Set<PreparedCommand> commands = new HashSet<>();

		for (Class<?> cmd : cmds) {
			Command params = cmd.getDeclaredAnnotation(Command.class);
			extractCommand(commands, cmd, params);
		}

		return commands;
	}

	public Set<PreparedCommand> getCommands(Category category) {
		Reflections refl = new Reflections(this.getClass().getPackageName());
		Set<Class<?>> cmds = refl.getTypesAnnotatedWith(Command.class);
		Set<PreparedCommand> commands = new HashSet<>();

		for (Class<?> cmd : cmds) {
			Command params = cmd.getDeclaredAnnotation(Command.class);
			if (params.category() == category) {
				extractCommand(commands, cmd, params);
			}
		}

		return commands;
	}

	public PreparedCommand getCommand(String name) {
		Reflections refl = new Reflections(this.getClass().getPackageName());
		Set<Class<?>> cmds = refl.getTypesAnnotatedWith(Command.class);

		for (Class<?> cmd : cmds) {
			Command params = cmd.getDeclaredAnnotation(Command.class);
			if (name.equalsIgnoreCase(params.name()) || Helper.equalsAny(name, params.aliases())) {
				Requires req = cmd.getDeclaredAnnotation(Requires.class);
				return new PreparedCommand(
						params.name(),
						params.aliases(),
						params.usage(),
						"cmd_" + cmd.getSimpleName()
								.replaceAll("Command|Reaction", "")
								.replaceAll("[a-z](?=[A-Z])", "$0-")
								.toLowerCase(),
						params.category(),
						req == null ? null : req.value()
				);
			}
		}

		return null;
	}

	private void extractCommand(Set<PreparedCommand> commands, Class<?> cmd, Command params) {
		Requires req = cmd.getDeclaredAnnotation(Requires.class);
		commands.add(new PreparedCommand(
				params.name(),
				params.aliases(),
				params.usage(),
				"cmd_" + cmd.getSimpleName()
						.replaceAll("Command|Reaction", "")
						.replaceAll("[a-z](?=[A-Z])", "$0-")
						.toLowerCase(),
				params.category(),
				req == null ? null : req.value()
		));
	}
}
