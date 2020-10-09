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

import com.github.ygimenez.method.Pages;
import com.kuuhaku.Main;
import com.kuuhaku.command.Category;
import com.kuuhaku.command.Command;
import com.kuuhaku.controller.Sweeper;
import com.kuuhaku.controller.sqlite.GuildDAO;
import com.kuuhaku.controller.sqlite.MemberDAO;
import com.kuuhaku.model.persistent.GuildConfig;
import com.kuuhaku.utils.Helper;
import com.kuuhaku.utils.ShiroInfo;
import net.dv8tion.jda.api.entities.*;
import org.jetbrains.annotations.NonNls;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

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
		if (Main.getInfo().getConfirmationPending().getIfPresent(author.getId()) != null) {
			channel.sendMessage("❌ | Você possui um comando com confirmação pendente, por favor resolva-o antes de usar este comando novamente.").queue();
			return;
		}

		channel.sendMessage("<a:loading:697879726630502401> | Comparando índices...").queue(s -> {
			Set<GuildConfig> gds = new HashSet<>(GuildDAO.getAllGuilds());
			Set<com.kuuhaku.model.persistent.Member> mbs = new HashSet<>(MemberDAO.getAllMembers());

			Set<String> guildTrashBin = new HashSet<>();
			Set<String> memberTrashBin = new HashSet<>();

			s.editMessage("<a:loading:697879726630502401> | Comparando índices... (" + gds.size() + " guilds)").queue();

			gds.forEach(gd -> {
				if (Main.getInfo().getGuildByID(gd.getGuildID()) == null)
					guildTrashBin.add(gd.getGuildID());
			});

			s.editMessage("<a:loading:697879726630502401> | Comparando índices... (" + mbs.size() + " membros)").queue();

			Map<String, List<String>> members = new HashMap<>();
			mbs.forEach(mb -> {
				members.putIfAbsent(mb.getSid(), new ArrayList<>());
				members.get(mb.getSid()).add(mb.getMid());
			});

			Set<String> foundIds = new HashSet<>();
			int total = mbs.size();
			int processed = 0;
			int percent;
			int lastPercent = 0;
			for (Map.Entry<String, List<String>> e : members.entrySet()) {
				if (guildTrashBin.contains(e.getKey())) {
					memberTrashBin.addAll(e.getValue());
					total -= e.getValue().size();
					continue;
				}

				Guild g = Main.getInfo().getGuildByID(e.getKey());
				foundIds.addAll(
						g.loadMembers().get()
								.stream()
								.map(Member::getId)
								.collect(Collectors.toList())
				);

				processed += e.getValue().size();
				percent = Helper.prcntToInt(processed, total);
				if (percent > lastPercent) {
					s.editMessage("<a:loading:697879726630502401> | Comparando índices... (" + total + " membros | " + percent + "%)").queue();
					lastPercent = percent;
				}
			}

			memberTrashBin.addAll(
					mbs.stream()
							.map(com.kuuhaku.model.persistent.Member::getSid)
							.filter(id -> !foundIds.contains(id))
							.collect(Collectors.toList())
			);

			if (guildTrashBin.size() + memberTrashBin.size() > 0) {
				String hash = Helper.generateHash(guild, author);
				ShiroInfo.getHashes().add(hash);
				Main.getInfo().getConfirmationPending().put(author.getId(), true);
				s.editMessage(":warning: | Foram encontrados " + guildTrashBin.size() + " índices de servidores e " + memberTrashBin + " membros inexistentes, deseja executar a limpeza?").queue(m ->
						Pages.buttonize(m, Map.of(Helper.ACCEPT, (mb, ms) -> {
							if (!ShiroInfo.getHashes().remove(hash)) return;
							Main.getInfo().getConfirmationPending().invalidate(author.getId());
							Sweeper.sweep(guildTrashBin, memberTrashBin);
							ms.editMessage(Helper.ACCEPT + " | Entradas limpas com sucesso!").queue();
						}), true, 1, TimeUnit.MINUTES, (u) -> u.getId().equals(author.getId()), ms -> {
							ShiroInfo.getHashes().remove(hash);
							Main.getInfo().getConfirmationPending().invalidate(author.getId());
						})
				);
			} else s.editMessage(Helper.ACCEPT + " | Não há entradas para serem limpas.").queue();
		});
	}
}
