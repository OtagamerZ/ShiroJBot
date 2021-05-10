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
import com.kuuhaku.model.persistent.Leaderboards;
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

		sendLeaderboards(
				switch (cmd.getCommand().getClass().getSimpleName()) {
					case "FaceoffCommand" -> LeaderboardsDAO.getFaceoffLeaderboards();
					case "SlotsCommand" -> LeaderboardsDAO.getSlotsLeaderboards();
					case "GuessTheCardsCommand" -> LeaderboardsDAO.getCommonLeaderboards(GuessTheCardsCommand.class);
					case "GuessTheNumberCommand" -> LeaderboardsDAO.getCommonLeaderboards(GuessTheNumberCommand.class);
					case "JankenponCommand" -> LeaderboardsDAO.getCommonLeaderboards(JankenponCommand.class);
					default -> null;
				}, channel);
	}

	private void sendLeaderboards(List<Leaderboards> lb, TextChannel channel) {
		channel.sendMessage("❌ | Esse comando não é um minigame ou não possui placares.").queue();
	}
}
