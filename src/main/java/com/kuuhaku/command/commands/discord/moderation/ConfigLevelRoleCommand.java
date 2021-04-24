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

package com.kuuhaku.command.commands.discord.moderation;

import com.github.ygimenez.method.Pages;
import com.github.ygimenez.model.Page;
import com.github.ygimenez.type.PageType;
import com.kuuhaku.command.Category;
import com.kuuhaku.command.Executable;
import com.kuuhaku.controller.postgresql.GuildDAO;
import com.kuuhaku.model.annotations.Command;
import com.kuuhaku.model.annotations.Requires;
import com.kuuhaku.model.common.ColorlessEmbedBuilder;
import com.kuuhaku.model.enums.I18n;
import com.kuuhaku.model.persistent.guild.GuildConfig;
import com.kuuhaku.model.persistent.guild.LevelRole;
import com.kuuhaku.utils.Helper;
import com.kuuhaku.utils.ShiroInfo;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;

@Command(
		name = "cargolevel",
		aliases = {"levelrole"},
		usage = "req_level-role",
		category = Category.MODERATION
)
@Requires({Permission.MANAGE_ROLES})
public class ConfigLevelRoleCommand implements Executable {

	@Override
	public void execute(User author, Member member, String command, String argsAsText, String[] args, Message message, TextChannel channel, Guild guild, String prefix) {
		GuildConfig gc = GuildDAO.getGuildById(guild.getId());

		if (args.length < 2 && !message.getMentionedRoles().isEmpty()) {
			Role r = message.getMentionedRoles().get(0);
			gc.removeLevelRole(r.getId());

			channel.sendMessage("✅ | Cargo `" + r.getName() + "` removido da listagem com sucesso!").queue();
			GuildDAO.updateGuildSettings(gc);
			return;
		} else if (args.length < 1) {
			List<Page> pages = new ArrayList<>();

			EmbedBuilder eb = new ColorlessEmbedBuilder()
					.setTitle(":dna: | Cargos de level configurados no servidor");

			List<LevelRole> roles = new ArrayList<>(gc.getLevelRoles());
			if (roles.size() == 0) {
				channel.sendMessage("Não há nenhum cargo de nível configurado neste servidor.").queue();
				return;
			}

			Map<Integer, String> fields = new TreeMap<>();
			for (LevelRole role : roles) {
				Role r = guild.getRoleById(role.getId());
				if (r == null) {
					gc.removeLevelRole(role.getId());
					continue;
				}

				fields.merge(role.getLevel(), r.getAsMention(), (p, n) -> String.join("\n", p, n));
			}
			GuildDAO.updateGuildSettings(gc);

			List<List<Integer>> chunks = Helper.chunkify(fields.keySet(), 10);
			for (List<Integer> chunk : chunks) {
				eb.clearFields();
				for (int level : chunk)
					eb.addField("Nível " + level, fields.get(level), true);

				pages.add(new Page(PageType.EMBED, eb.build()));
			}

			channel.sendMessage((MessageEmbed) pages.get(0).getContent()).queue(s ->
					Pages.paginate(s, pages, 1, TimeUnit.MINUTES, 5, u -> u.getId().equals(author.getId()))
			);
			return;
		}

		try {
			int level = Integer.parseInt(args[0]);
			if (level <= 0) {
				channel.sendMessage("❌ | O nível deve ser um valor maior que 0.").queue();
				return;
			}

			Role r = message.getMentionedRoles().get(0);
			gc.addLevelRole(r.getId(), level);

			channel.sendMessage("✅ | Membros com nível " + level + " receberão o cargo `" + r.getName() + "`!").queue();
			GuildDAO.updateGuildSettings(gc);
		} catch (NumberFormatException e) {
			channel.sendMessage(ShiroInfo.getLocale(I18n.PT).getString("err_invalid-level")).queue();
		}
	}
}
