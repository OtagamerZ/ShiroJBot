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

package com.kuuhaku.command.commands.discord.dev;

import com.github.ygimenez.method.Pages;
import com.kuuhaku.Main;
import com.kuuhaku.command.Category;
import com.kuuhaku.command.Executable;
import com.kuuhaku.controller.Sweeper;
import com.kuuhaku.controller.postgresql.GuildDAO;
import com.kuuhaku.controller.postgresql.MemberDAO;
import com.kuuhaku.model.annotations.Command;
import com.kuuhaku.model.annotations.Requires;
import com.kuuhaku.model.persistent.guild.GuildConfig;
import com.kuuhaku.utils.Constants;
import com.kuuhaku.utils.helpers.MiscHelper;
import com.kuuhaku.utils.helpers.StringHelper;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.*;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

@Command(
		name = "sweep",
		category = Category.DEV
)
@Requires({Permission.MESSAGE_EXT_EMOJI})
public class SweepCommand implements Executable {

	@Override
	public void execute(User author, Member member, String argsAsText, String[] args, Message message, TextChannel channel, Guild guild, String prefix) {
		channel.sendMessage("<a:loading:697879726630502401> | Comparando índices...").queue(s -> {
			Set<GuildConfig> gds = new HashSet<>(GuildDAO.getAllGuilds());
			Set<com.kuuhaku.model.persistent.Member> mbs = new HashSet<>(MemberDAO.getAllMembers());

			Set<String> guildTrashBin = new HashSet<>();
			Set<String> memberTrashBin = new HashSet<>();

			s.editMessage("<a:loading:697879726630502401> | Comparando índices... (" + gds.size() + " guilds)").queue();

			for (GuildConfig gd : gds) {
				if (Main.getInfo().getGuildByID(gd.getGuildId()) == null) {
					guildTrashBin.add(gd.getGuildId());
					MiscHelper.logger(this.getClass()).debug(gd.getName() + " is null, added to trash bin");
				}
			}

			s.editMessage("<a:loading:697879726630502401> | Comparando índices... (" + mbs.size() + " membros)").queue();

			Map<String, List<String>> members = new HashMap<>();
			for (com.kuuhaku.model.persistent.Member mb1 : mbs) {
				members.putIfAbsent(mb1.getSid(), new ArrayList<>());
				members.get(mb1.getSid()).add(mb1.getUid());
			}

			Set<String> foundIds = new HashSet<>();
			Map<String, Set<String>> missingIds = new HashMap<>();
			for (Map.Entry<String, List<String>> e : members.entrySet()) {
				if (guildTrashBin.contains(e.getKey())) {
					missingIds.putIfAbsent(e.getKey(), new HashSet<>());
					missingIds.get(e.getKey()).addAll(e.getValue());
					continue;
				}

				Guild g = Main.getInfo().getGuildByID(e.getKey());
				if (g == null) {
					guildTrashBin.add(e.getKey());

					missingIds.computeIfAbsent(e.getKey(), t -> new HashSet<>())
							.addAll(e.getValue().stream().map(id -> id + e.getKey()).toList());
					MiscHelper.logger(this.getClass()).debug("GID " + e.getKey() + " is null, added to trash bin");
				} else {
					try {
						CompletableFuture<Void> loaded = new CompletableFuture<>();
						g.findMembers(m -> !m.getUser().isBot()).onSuccess(res -> {
							foundIds.addAll(
									res.stream()
											.map(m -> m.getId() + e.getKey()).toList()
							);

							loaded.complete(null);
							MiscHelper.logger(this.getClass()).debug(g.getName() + ": Loaded " + res.size() + " members");
						});
						loaded.get();
					} catch (ExecutionException | InterruptedException err) {
						foundIds.addAll(mbs.stream().map(m -> m.getUid() + m.getSid()).toList());
					}
				}
			}

			for (com.kuuhaku.model.persistent.Member mb1 : mbs) {
				if (!foundIds.contains(mb1.getUid() + mb1.getSid())) {
					missingIds.putIfAbsent(mb1.getSid(), new HashSet<>());
					missingIds.get(mb1.getSid()).add(mb1.getUid());
				}
			}

			for (Map.Entry<String, Set<String>> entry : missingIds.entrySet()) {
				String k = entry.getKey();
				Set<String> v = entry.getValue();
				for (String id : v) {
					memberTrashBin.add(id + k);
				}
			}

			if (guildTrashBin.size() + memberTrashBin.size() > 0) {
				Main.getInfo().getConfirmationPending().put(author.getId(), true);
				String gText = guildTrashBin.size() > 0 ? guildTrashBin.size() == 1 ? guildTrashBin.size() + " índice de servidor" : guildTrashBin.size() + " índices de servidores" : "";
				String mText = memberTrashBin.size() > 0 ? memberTrashBin.size() == 1 ? memberTrashBin.size() + " membro" : memberTrashBin.size() + " membros" : "";

				s.editMessage(":warning: | " + (guildTrashBin.size() + memberTrashBin.size() > 1 ? "Foram encontrados " : "Foi encontrado ") + gText + (!gText.isBlank() && !mText.isBlank() ? " e " : "") + mText + (guildTrashBin.size() + memberTrashBin.size() > 1 ? " inexistentes" : " inexistente") + ", deseja executar a limpeza?")
						.queue(m -> Pages.buttonize(m, Map.of(StringHelper.parseEmoji(Constants.ACCEPT), wrapper -> {
									Main.getInfo().getConfirmationPending().remove(author.getId());
									Sweeper.sweep(guildTrashBin, memberTrashBin);

									m.delete().queue(null, MiscHelper::doNothing);
									channel.sendMessage("✅ | Entradas limpas com sucesso!").queue();
								}), Constants.USE_BUTTONS, true, 1, TimeUnit.MINUTES,
								u -> u.getId().equals(author.getId()),
								ms -> Main.getInfo().getConfirmationPending().remove(author.getId())
						));
			} else s.editMessage("✅ | Não há entradas para serem limpas.").queue();
		});
	}
}
