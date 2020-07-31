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

package com.kuuhaku.command.commands.dev;

import com.github.ygimenez.method.Pages;
import com.kuuhaku.Main;
import com.kuuhaku.command.Category;
import com.kuuhaku.command.Command;
import com.kuuhaku.controller.Sweeper;
import com.kuuhaku.controller.sqlite.GuildDAO;
import com.kuuhaku.controller.sqlite.MemberDAO;
import com.kuuhaku.model.persistent.GuildConfig;
import com.kuuhaku.utils.Helper;
import net.dv8tion.jda.api.entities.*;
import org.jetbrains.annotations.NonNls;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

public class SweepCommand extends Command {

	public SweepCommand(String name, String description, Category category, boolean requiresMM) {
		super(name, description, category, requiresMM);
	}

	public SweepCommand(@NonNls String name, @NonNls String[] aliases, String description, Category category, boolean requiresMM) {
		super(name, aliases, description, category, requiresMM);
	}

	public SweepCommand(String name, String usage, String description, Category category, boolean requiresMM) {
		super(name, usage, description, category, requiresMM);
	}

	public SweepCommand(String name, String[] aliases, String usage, String description, Category category, boolean requiresMM) {
		super(name, aliases, usage, description, category, requiresMM);
	}

	@Override
	public void execute(User author, Member member, String rawCmd, String[] args, Message message, MessageChannel channel, Guild guild, String prefix) {
		channel.sendMessage("<a:loading:697879726630502401> | Comparando índices...").queue(s -> {
			Set<GuildConfig> gds = new HashSet<>(GuildDAO.getAllGuilds());
			Set<com.kuuhaku.model.persistent.Member> mbs = new HashSet<>(MemberDAO.getAllMembers());

			Set<String> guildTrashBin = new HashSet<>();
			Set<String> memberTrashBin = new HashSet<>();

			gds.forEach(gd -> {
				try {
					assert Main.getInfo().getGuildByID(gd.getGuildID()) != null;
				} catch (AssertionError e) {
					guildTrashBin.add(gd.getGuildID());
				}
			});

			mbs.forEach(mb -> {
				if (guildTrashBin.contains(mb.getSid())) memberTrashBin.add(mb.getId());
				else try {
					assert Main.getInfo().getGuildByID(mb.getSid()).getMemberById(mb.getMid()) != null;
				} catch (AssertionError e) {
					memberTrashBin.add(mb.getId());
				}
			});

			if (guildTrashBin.size() + memberTrashBin.size() > 0)
				s.editMessage(":warning: | Foram encontrados " + guildTrashBin.size() + " índices de servidores e " + memberTrashBin + " membros inexistentes, deseja executar a limpeza?").queue(m ->
						Pages.buttonize(m, Map.of(Helper.ACCEPT, (mb, ms) -> {
							Sweeper.sweep(guildTrashBin, memberTrashBin);
							ms.editMessage(Helper.ACCEPT + " | Entradas limpas com sucesso!").queue();
						}), true, 1, TimeUnit.MINUTES, (u) -> u.getId().equals(author.getId()))
				);
			else s.editMessage(Helper.ACCEPT + " | Não há entradas para serem limpas.").queue();
		});
	}
}
