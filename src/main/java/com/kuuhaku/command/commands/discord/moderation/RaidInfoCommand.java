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

package com.kuuhaku.command.commands.discord.moderation;

import com.github.ygimenez.method.Pages;
import com.github.ygimenez.model.InteractPage;
import com.github.ygimenez.model.Page;
import com.kuuhaku.command.Category;
import com.kuuhaku.command.Executable;
import com.kuuhaku.controller.postgresql.RaidDAO;
import com.kuuhaku.model.annotations.Command;
import com.kuuhaku.model.common.ColorlessEmbedBuilder;
import com.kuuhaku.model.enums.I18n;
import com.kuuhaku.model.persistent.RaidInfo;
import com.kuuhaku.model.persistent.RaidMember;
import com.kuuhaku.utils.Helper;
import com.kuuhaku.utils.ShiroInfo;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.*;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Command(
		name = "raids",
		aliases = {"raidinfo"},
		usage = "req_id-opt",
		category = Category.MODERATION
)
public class RaidInfoCommand implements Executable {

	@Override
	public void execute(User author, Member member, String argsAsText, String[] args, Message message, TextChannel channel, Guild guild, String prefix) {
		if (args.length < 1) {
			List<List<RaidInfo>> chunks = Helper.chunkify(RaidDAO.getRaids(guild.getId()), 10);
			if (chunks.isEmpty()) {
				channel.sendMessage("❌ | Este servidor não sofreu nenhuma raid ainda.").queue();
				return;
			}

			Set<String> bans = new HashSet<>();
			if (guild.getSelfMember().hasPermission(Permission.BAN_MEMBERS)) {
				bans = guild.retrieveBanList().complete().stream()
						.map(Guild.Ban::getUser)
						.map(User::getId)
						.collect(Collectors.toSet());
			}

			List<Page> pages = new ArrayList<>();
			EmbedBuilder eb = new ColorlessEmbedBuilder()
					.setTitle(":octagonal_sign: | Raids bloqueadas pelo sistema R.A.ID neste servidor");
			for (List<RaidInfo> chunk : chunks) {
				eb.clearFields();

				for (RaidInfo r : chunk) {
					eb.addField(
							"`ID: " + r.getId() + "`",
							"""
									Ocorrido: %s
									Duração: %s
									Usuários detectados: %s
									Usuários perdoados: %s
									""".formatted(
									Helper.TIMESTAMP.formatted(r.getOccurrence().toEpochSecond()),
									Helper.toStringDuration(r.getDuration()),
									r.getMembers().size(),
									bans.stream()
											.filter(id -> r.getMembers().stream().noneMatch(rm -> rm.getUid().equals(id)))
											.count()
							),
							false
					);
				}

				pages.add(new InteractPage(eb.build()));
			}

			channel.sendMessageEmbeds((MessageEmbed) pages.get(0).getContent()).queue(s ->
					Pages.paginate(s, pages, ShiroInfo.USE_BUTTONS, 1, TimeUnit.MINUTES, u -> u.getId().equals(author.getId()))
			);
			return;
		}

		try {
			int i = Integer.parseInt(args[0]);
			RaidInfo r = RaidDAO.getRaid(i, guild.getId());
			if (r == null) {
				channel.sendMessage("❌ | Raid inexistente.").queue();
				return;
			}

			List<List<RaidMember>> chunks = Helper.chunkify(r.getMembers(), 10);

			Set<String> bans = new HashSet<>();
			if (guild.getSelfMember().hasPermission(Permission.BAN_MEMBERS)) {
				bans = guild.retrieveBanList().complete().stream()
						.map(Guild.Ban::getUser)
						.map(User::getId)
						.collect(Collectors.toSet());
			}

			List<Page> pages = new ArrayList<>();
			EmbedBuilder eb = new ColorlessEmbedBuilder()
					.setTitle(":octagonal_sign: | Raid ocorrida em " + Helper.FULL_DATE_FORMAT.format(r.getOccurrence()));
			for (List<RaidMember> chunk : chunks) {
				eb.clearFields();

				for (RaidMember m : chunk) {
					String status;
					if (bans.contains(m.getUid())) {
						status = ":red_circle: Banido";
					} else if (guild.getMemberById(m.getUid()) != null) {
						status = ":green_circle: No servidor";
					} else {
						status = ":orange_circle: Expulso/Ausente";
					}

					eb.addField(m.getName(), "ID: `" + m.getUid() + "`\nStatus: " + status, false);
				}

				pages.add(new InteractPage(eb.build()));
			}

			channel.sendMessageEmbeds((MessageEmbed) pages.get(0).getContent()).queue(s ->
					Pages.paginate(s, pages, ShiroInfo.USE_BUTTONS, 1, TimeUnit.MINUTES, u -> u.getId().equals(author.getId()))
			);
		} catch (NumberFormatException e) {
			channel.sendMessage(I18n.getString("err_invalid-index")).queue();
		}
	}
}
