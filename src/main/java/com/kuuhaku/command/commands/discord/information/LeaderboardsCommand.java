/*
 * This file is part of Shiro J Bot.
 * Copyright (C) 2019-2021  Yago Gimenez (KuuHaKu)
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
import com.kuuhaku.model.annotations.Command;
import com.kuuhaku.model.annotations.Requires;
import com.kuuhaku.model.common.ColorlessEmbedBuilder;
import com.kuuhaku.model.persistent.Leaderboards;
import com.kuuhaku.utils.XStringBuilder;
import com.kuuhaku.utils.helpers.StringHelper;
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
		Permission.MESSAGE_EMBED_LINKS,
		Permission.MESSAGE_ADD_REACTION
})
public class LeaderboardsCommand implements Executable {

	@Override
	public void execute(User author, Member member, String argsAsText, String[] args, Message message, TextChannel channel, Guild guild, String prefix) {
		if (args.length < 1) {
			channel.sendMessage("❌ | Você precisa informar um minigame.").queue();
			return;
		}

		PreparedCommand cmd = Main.getCommandManager().getCommand(args[0]);
		if (cmd == null) {
			channel.sendMessage("❌ | Esse minigame não existe.").queue();
			return;
		}

		String order;
		if (cmd.getClass().getSimpleName().equals("FaceoffCommand")) {
			order = "ORDER BY score, id DESC";
		} else {
			order = "ORDER BY score DESC, id DESC";
		}

		List<Leaderboards> lb = Leaderboards.queryAllNative(Leaderboards.class, """
				SELECT MAX(l.id) AS id
					 , l.uid
					 , MAX(l.usr) AS usr
					 , l.minigame
					 , CASE l.minigame
					 	WHEN 'FaceoffCommand' THEN MIN(l.score)
					 	ELSE SUM(l.score)
					 END AS score
				FROM Leaderboards l
				WHERE l.minigame = :game
				GROUP BY l.uid, l.minigame
				""" + order, cmd.getCommand().getClass().getSimpleName());

		sendLeaderboards(cmd, lb, channel);
	}

	private void sendLeaderboards(PreparedCommand cmd, List<Leaderboards> lb, TextChannel channel) {
		if (lb == null) {
			channel.sendMessage("❌ | Esse minigame não possui placares.").queue();
			return;
		}

		XStringBuilder sb = new XStringBuilder();
		for (int i = 0; i < lb.size(); i++) {
			Leaderboards l = lb.get(i);
			String unit = switch (cmd.getCommand().getClass().getSimpleName()) {
				case "FaceoffCommand" -> "ms";
				case "SlotsCommand" -> "CR";
				case "GuessTheCardsCommand", "JankenponCommand", "GuessTheNumberCommand", "ColorNameCommand" -> "ponto" + (l.getScore() == 1 ? "" : "s");
				default -> "";
			};

			if (i == 0)
				sb.appendNewLine("**%dº - %s (%s %s)**".formatted(i + 1, l.getUsr(), StringHelper.separate(l.getScore()), unit));
			else
				sb.appendNewLine("%dº - %s (%s %s)".formatted(i + 1, l.getUsr(), StringHelper.separate(l.getScore()), unit));
		}

		EmbedBuilder eb = new ColorlessEmbedBuilder()
				.setTitle("10 melhores jogadores de " + switch (cmd.getCommand().getClass().getSimpleName()) {
					case "FaceoffCommand" -> "confronto";
					case "SlotsCommand" -> "slots";
					case "GuessTheCardsCommand" -> "adivinhe as cartas";
					case "GuessTheNumberCommand" -> "adivinhe o número";
					case "JankenponCommand" -> "pedra-papel-tesoura";
					case "ColorNameCommand" -> "acerte-a-cor";
					default -> "";
				})
				.setDescription(sb.toString());

		channel.sendMessageEmbeds(eb.build()).queue();
	}
}
