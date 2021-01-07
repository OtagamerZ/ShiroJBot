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

package com.kuuhaku.command.commands.discord.misc;

import com.github.ygimenez.method.Pages;
import com.github.ygimenez.model.Page;
import com.github.ygimenez.type.PageType;
import com.kuuhaku.command.Category;
import com.kuuhaku.command.Command;
import com.kuuhaku.controller.postgresql.TrophyDAO;
import com.kuuhaku.controller.sqlite.MemberDAO;
import com.kuuhaku.model.common.ColorlessEmbedBuilder;
import com.kuuhaku.model.enums.TrophyType;
import com.kuuhaku.model.persistent.Trophy;
import com.kuuhaku.utils.Helper;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.*;
import org.jetbrains.annotations.NonNls;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class ProfileTrophyCommand extends Command {

	public ProfileTrophyCommand(String name, String description, Category category, boolean requiresMM) {
		super(name, description, category, requiresMM);
	}

	public ProfileTrophyCommand(String name, String[] aliases, String description, Category category, boolean requiresMM) {
		super(name, aliases, description, category, requiresMM);
	}

	public ProfileTrophyCommand(String name, String usage, String description, Category category, boolean requiresMM) {
		super(name, usage, description, category, requiresMM);
	}

	public ProfileTrophyCommand(@NonNls String name, @NonNls String[] aliases, String usage, String description, Category category, boolean requiresMM) {
		super(name, aliases, usage, description, category, requiresMM);
	}

	@Override
	public void execute(User author, Member member, String rawCmd, String[] args, Message message, MessageChannel channel, Guild guild, String prefix) {
		Trophy t = TrophyDAO.getTrophies(author.getId());

		if (args.length == 0) {
			if (t.getTrophies().size() == 0) {
				channel.sendMessage("❌ | Você não possui troféus ainda.").queue();
				return;
			}

			List<Page> pages = new ArrayList<>();

			List<List<TrophyType>> trophies = Helper.chunkify(t.getTrophies(), 10);

			EmbedBuilder eb = new ColorlessEmbedBuilder()
					.setTitle(":trophy: | Troféus de " + author.getName())
					.setFooter("Use `%strofeu ID` para definir um troféu para seu perfil.".formatted(prefix));
			for (List<TrophyType> chunk : trophies) {
				eb.clearFields();

				for (TrophyType tt : chunk)
					eb.addField("`ID: %s` | %s".formatted(tt.name(), tt.getName()), tt.getDescription(), false);

				pages.add(new Page(PageType.EMBED, eb.build()));
			}

			channel.sendMessage((MessageEmbed) pages.get(0).getContent()).queue(s ->
					Pages.paginate(s, pages, 1, TimeUnit.MINUTES, u -> u.getId().equals(author.getId()))
			);
			return;
		}

		if (Helper.equalsAny(args[0], "none", "reset", "resetar", "limpar")) {
			List<com.kuuhaku.model.persistent.Member> ms = MemberDAO.getMemberByMid(author.getId());
			for (com.kuuhaku.model.persistent.Member m : ms) {
				m.setTrophy(null);
				MemberDAO.updateMemberConfigs(m);
			}
			channel.sendMessage("✅ | Troféu removido com sucesso!").queue();
			return;
		}

		try {
			TrophyType tt = TrophyType.valueOf(args[0].toUpperCase());

			if (!t.getTrophies().contains(tt)) {
				channel.sendMessage("❌ | Você não possui esse troféu.").queue();
				return;
			}

			List<com.kuuhaku.model.persistent.Member> ms = MemberDAO.getMemberByMid(author.getId());
			for (com.kuuhaku.model.persistent.Member m : ms) {
				m.setTrophy(tt);
				MemberDAO.updateMemberConfigs(m);
			}
			channel.sendMessage("✅ | Troféu definido com sucesso!").queue();
		} catch (IllegalArgumentException e) {
			channel.sendMessage("❌ | Troféu inválido, verifique se você digitou o ID corretamente.").queue();
		}
	}
}
