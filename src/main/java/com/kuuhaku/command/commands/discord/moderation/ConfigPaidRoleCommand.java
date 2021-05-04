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
import com.kuuhaku.model.persistent.guild.PaidRole;
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
		name = "cargopago",
		aliases = {"cargop", "paidrole", "prole"},
		usage = "req_value-role-time",
		category = Category.MODERATION
)
@Requires({Permission.MANAGE_ROLES})
public class ConfigPaidRoleCommand implements Executable {

	@Override
	public void execute(User author, Member member, String command, String argsAsText, String[] args, Message message, TextChannel channel, Guild guild, String prefix) {
		GuildConfig gc = GuildDAO.getGuildById(guild.getId());
		int highest = member.getRoles().stream()
				.map(Role::getPosition)
				.max(Integer::compareTo)
				.orElse(-1);

		if (args.length < 2 && !message.getMentionedRoles().isEmpty()) {
			Role r = message.getMentionedRoles().get(0);
			gc.removeLevelRole(r.getId());

			channel.sendMessage("✅ | Cargo `" + r.getName() + "` removido da listagem com sucesso!").queue();
			GuildDAO.updateGuildSettings(gc);
			return;
		} else if (args.length < 1) {
			List<Page> pages = new ArrayList<>();

			EmbedBuilder eb = new ColorlessEmbedBuilder()
					.setTitle(":bank: | Cargos pagos configurados no servidor");

			List<PaidRole> roles = List.copyOf(gc.getPaidRoles());
			if (roles.size() == 0) {
				channel.sendMessage("Não há nenhum cargo pago configurado neste servidor.").queue();
				return;
			}

			Map<Integer, String> fields = new TreeMap<>();
			for (PaidRole role : roles) {
				Role r = guild.getRoleById(role.getId());
				if (r == null) {
					gc.removeLevelRole(role.getId());
					continue;
				}

				if (role.getDuration() > -1)
					fields.merge(role.getPrice(), r.getAsMention() + " (" + Helper.toStringDuration(role.getDuration()) + ")", (p, n) -> String.join("\n", p, n));
				else
					fields.merge(role.getPrice(), r.getAsMention(), (p, n) -> String.join("\n", p, n));
			}
			GuildDAO.updateGuildSettings(gc);

			List<List<Integer>> chunks = Helper.chunkify(fields.keySet(), 10);
			for (List<Integer> chunk : chunks) {
				eb.clearFields();
				for (int value : chunk)
					eb.addField("Valor: " + Helper.separate(value) + " créditos", fields.get(value), false);

				pages.add(new Page(PageType.EMBED, eb.build()));
			}

			channel.sendMessage((MessageEmbed) pages.get(0).getContent()).queue(s ->
					Pages.paginate(s, pages, 1, TimeUnit.MINUTES, 5, u -> u.getId().equals(author.getId()))
			);
			return;
		}

		try {
			int value = Integer.parseInt(args[0]);
			if (value <= 0) {
				channel.sendMessage(ShiroInfo.getLocale(I18n.PT).getString("err_invalid-credit-amount")).queue();
				return;
			}

			Role r = message.getMentionedRoles().get(0);
			if (r.getPosition() > highest) {
				channel.sendMessage("❌ | Você não pode atribuir cargos maiores que os seus.").queue();
				return;
			}

			long time = args.length > 2 ? Math.max(1, Long.parseLong(args[3])) * Helper.MILLIS_IN_MINUTE : -1;
			gc.addPaidRole(r.getId(), value, time);

			if (time > -1)
				channel.sendMessage("✅ | O cargo `" + r.getName() + "` agora poderá ser comprado por **" + Helper.separate(value) + " créditos**! (" + Helper.toStringDuration(time) + ")").queue();
			else
				channel.sendMessage("✅ | O cargo `" + r.getName() + "` agora poderá ser comprado por **" + Helper.separate(value) + " créditos**!").queue();
			GuildDAO.updateGuildSettings(gc);
		} catch (NumberFormatException e) {
			channel.sendMessage(ShiroInfo.getLocale(I18n.PT).getString("err_invalid-price-time")).queue();
		} catch (IndexOutOfBoundsException e) {
			channel.sendMessage("❌ | Você precisa mencionar um cargo.").queue();
		}
	}
}
