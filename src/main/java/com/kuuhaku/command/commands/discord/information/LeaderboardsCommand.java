/*
 * This file is part of Shiro J Bot.
 * Copyright (C) 2021  Yago Gimenez (KuuHaKu)
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

package com.kuuhaku.command.commands.discord.information;

import com.kuuhaku.Main;
import com.kuuhaku.command.Category;
import com.kuuhaku.command.Executable;
import com.kuuhaku.command.commands.PreparedCommand;
import com.kuuhaku.command.commands.discord.fun.GuessTheCardsCommand;
import com.kuuhaku.command.commands.discord.fun.GuessTheNumberCommand;
import com.kuuhaku.command.commands.discord.fun.JankenponCommand;
import com.kuuhaku.controller.postgresql.LeaderboardsDAO;
import com.kuuhaku.model.annotations.Command;
import com.kuuhaku.model.annotations.Requires;
import com.kuuhaku.model.common.ColorlessEmbedBuilder;
import com.kuuhaku.model.persistent.Leaderboards;
import com.kuuhaku.utils.XStringBuilder;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.*;

import java.util.List;

@Command(
		name = "placares",
		aliases = {"lb", "leaderboards"},
		category = Category.INFO
)
@Requires({
		Permission.MESSAGE_MANAGE,
		Permission.MESSAGE_EMBED_LINKS,
		Permission.MESSAGE_ADD_REACTION
})
public class LeaderboardsCommand implements Executable {

	@Override
	public void execute(User author, Member member, String command, String argsAsText, String[] args, Message message, TextChannel channel, Guild guild, String prefix) {
		if (args.length < 1) {
			channel.sendMessage("❌ | Você precisa informar um minigame.").queue();
			return;
		}

		PreparedCommand cmd = Main.getCommandManager().getCommand(args[0]);
		if (cmd == null) {
			channel.sendMessage("❌ | Esse minigame não existe.").queue();
			return;
		}

		sendLeaderboards(cmd,
				switch (cmd.getCommand().getClass().getSimpleName()) {
					case "FaceoffCommand" -> LeaderboardsDAO.getFaceoffLeaderboards();
					case "SlotsCommand" -> LeaderboardsDAO.getSlotsLeaderboards();
					case "GuessTheCardsCommand" -> LeaderboardsDAO.getCommonLeaderboards(GuessTheCardsCommand.class);
					case "GuessTheNumberCommand" -> LeaderboardsDAO.getCommonLeaderboards(GuessTheNumberCommand.class);
					case "JankenponCommand" -> LeaderboardsDAO.getCommonLeaderboards(JankenponCommand.class);
					default -> null;
				}, channel);
	}

	private void sendLeaderboards(PreparedCommand cmd, List<Leaderboards> lb, TextChannel channel) {
		if (lb == null) {
			channel.sendMessage("❌ | Esse comando não é um minigame ou não possui placares.").queue();
			return;
		}

		XStringBuilder sb = new XStringBuilder();
		for (int i = 0; i < lb.size(); i++) {
			Leaderboards l = lb.get(i);
			String unit = switch (cmd.getCommand().getClass().getSimpleName()) {
				case "FaceoffCommand" -> "ms";
				case "SlotsCommand" -> "crédito" + (l.getScore() == 1 ? "" : "s");
				case "GuessTheCardsCommand", "JankenponCommand", "GuessTheNumberCommand" -> "ponto" + (l.getScore() == 1 ? "" : "s");
				default -> "";
			};

			if (i == 0)
				sb.appendNewLine("**%dº - %s (%d %s)**".formatted(i + 1, l.getUsr(), l.getScore(), unit));
			else
				sb.appendNewLine("%dº - %s (%d %s)".formatted(i + 1, l.getUsr(), l.getScore(), unit));
		}

		EmbedBuilder eb = new ColorlessEmbedBuilder()
				.setTitle("10 melhores jogadores de " + switch (cmd.getCommand().getClass().getSimpleName()) {
					case "FaceoffCommand" -> "confronto";
					case "SlotsCommand" -> "slots";
					case "GuessTheCardsCommand" -> "adivinhe as cartas";
					case "GuessTheNumberCommand" -> "adivinhe o número";
					case "JankenponCommand" -> "pedra-papel-tesoura";
					default -> "";
				})
				.setDescription(sb.toString());

		channel.sendMessage(eb.build()).queue();
	}
}
