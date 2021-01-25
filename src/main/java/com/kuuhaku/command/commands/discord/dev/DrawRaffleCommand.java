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

package com.kuuhaku.command.commands.discord.dev;

import com.kuuhaku.Main;
import com.kuuhaku.command.Category;
import com.kuuhaku.command.Command;
import com.kuuhaku.controller.postgresql.UpvoteDAO;
import com.kuuhaku.model.enums.I18n;
import com.kuuhaku.model.persistent.Upvote;
import com.kuuhaku.utils.Helper;
import com.kuuhaku.utils.ShiroInfo;
import net.dv8tion.jda.api.entities.*;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NonNls;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class DrawRaffleCommand extends Command {

	public DrawRaffleCommand(String name, String description, Category category, boolean requiresMM) {
		super(name, description, category, requiresMM);
	}

	public DrawRaffleCommand(@NonNls String name, @NonNls String[] aliases, String description, Category category, boolean requiresMM) {
		super(name, aliases, description, category, requiresMM);
	}

	public DrawRaffleCommand(String name, String usage, String description, Category category, boolean requiresMM) {
		super(name, usage, description, category, requiresMM);
	}

	public DrawRaffleCommand(@NonNls String name, @NonNls String[] aliases, String usage, String description, Category category, boolean requiresMM) {
		super(name, aliases, usage, description, category, requiresMM);
	}

	@Override
	public void execute(User author, Member member, String command, String argsAsText, String[] args, Message message, MessageChannel channel, Guild guild, String prefix) {
		if (args.length < 1) {
			channel.sendMessage(ShiroInfo.getLocale(I18n.PT).getString("err_no-period")).queue();
			return;
		} else if (!StringUtils.isNumeric(args[0])) {
			channel.sendMessage(ShiroInfo.getLocale(I18n.PT).getString("err_invalid-period")).queue();
			return;
		}

		int days = Integer.parseInt(args[0]);
		List<String> votes = UpvoteDAO.getVotes().stream().filter(u -> u.getVotedAt().isAfter(LocalDateTime.now().minusDays(days)) && Main.getInfo().getUserByID(u.getUid()) != null).map(Upvote::getUid).collect(Collectors.toList());
		Collections.shuffle(votes);

		if (votes.size() == 0) {
			channel.sendMessage(ShiroInfo.getLocale(I18n.PT).getString("err_raffle-no-votes")).queue();
            return;
        }

		channel.sendMessage("E o vencedor do sorteio é")
				.delay(2, TimeUnit.SECONDS)
				.flatMap(s -> s.editMessage(s.getContentRaw() + "."))
				.delay(2, TimeUnit.SECONDS)
				.flatMap(s -> s.editMessage(s.getContentRaw() + "."))
				.delay(2, TimeUnit.SECONDS)
				.flatMap(s -> s.editMessage(s.getContentRaw() + "."))
				.delay(2, TimeUnit.SECONDS)
				.flatMap(s -> s.editMessage(s.getContentRaw() + "."))
				.delay(2, TimeUnit.SECONDS)
				.flatMap(s -> s.editMessage(s.getContentRaw() + Main.getInfo().getUserByID(votes.get(Helper.rng(votes.size(), true))).getAsMention() + ", parabéns!\nUm desenvolvedor entrará em contato para discutir sobre a premiação."))
				.queue();
	}
}
