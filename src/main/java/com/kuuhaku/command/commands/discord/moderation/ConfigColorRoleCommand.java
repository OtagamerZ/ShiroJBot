/*
 * This file is part of Shiro J Bot.
 * Copyright (C) 2021  Yago Gimenez (KuuHaKu)
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

import com.kuuhaku.command.Category;
import com.kuuhaku.command.Executable;
import com.kuuhaku.controller.postgresql.GuildDAO;
import com.kuuhaku.model.annotations.Command;
import com.kuuhaku.model.annotations.Requires;
import com.kuuhaku.model.enums.I18n;
import com.kuuhaku.model.persistent.guild.ColorRole;
import com.kuuhaku.model.persistent.guild.GuildConfig;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.exceptions.HierarchyException;
import org.apache.commons.lang3.StringUtils;

import java.awt.*;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Command(
		name = "cargocor",
		aliases = {"colorrole"},
		usage = "req_name-color",
		category = Category.MODERATION
)
@Requires({Permission.MANAGE_ROLES})
public class ConfigColorRoleCommand implements Executable {

	@Override
	public void execute(User author, Member member, String command, String argsAsText, String[] args, Message message, TextChannel channel, Guild guild, String prefix) {
		GuildConfig gc = GuildDAO.getGuildById(guild.getId());

		if (args.length < 1) {
			channel.sendMessage(I18n.getString("err_color-not-enough-args")).queue();
			return;
		}

		Map<String, ColorRole> roles = gc.getColorRoles().stream()
				.collect(Collectors.toMap(cr -> StringUtils.capitalize(cr.getName().toLowerCase(Locale.ROOT)), Function.identity()));
		try {
			String name = StringUtils.capitalize(args[0].toLowerCase(Locale.ROOT));

			if (name.length() > 15) {
				channel.sendMessage(I18n.getString("err_color-name-too-long")).queue();
				return;
			}

			if (roles.containsKey(name) && guild.getRoleById(roles.get(name).getId()) != null) {
				try {
					Role r = guild.getRoleById(roles.get(name).getId());
					assert r != null;
					r.getManager()
							.setColor(Color.decode(args[1]))
							.complete();

					gc.addColorRole(r.getId(), args[1], name);
					channel.sendMessage("✅ | Cor modificada com sucesso!").queue();
					GuildDAO.updateGuildSettings(gc);
					return;
				} catch (HierarchyException e) {
					channel.sendMessage("❌ | Não posso modificar um cargo de cor que está acima de mim.").queue();
					return;
				}
			}

			Role r = guild.createRole()
					.setColor(Color.decode(args[1]))
					.setName(name)
					.setPermissions(Permission.EMPTY_PERMISSIONS)
					.complete();
			guild.modifyRolePositions()
					.selectPosition(r)
					.moveTo(guild.getSelfMember().getRoles().get(0).getPosition() - 1)
					.complete();
			gc.addColorRole(r.getId(), args[1], name);

			channel.sendMessage("✅ | Cor adicionada com sucesso!").queue();
		} catch (NumberFormatException e) {
			channel.sendMessage(I18n.getString("err_invalid-color")).queue();
        } catch (ArrayIndexOutOfBoundsException e) {
			String name = StringUtils.capitalize(args[0].toLowerCase(Locale.ROOT));

			if (!roles.containsKey(name)) {
				channel.sendMessage("❌ | Essa cor não existe!").queue();
				return;
			}
			Role r = guild.getRoleById(roles.get(name).getId());

			if (r != null) r.delete().queue();
			gc.removeColorRole(name);

			channel.sendMessage("✅ | Cor removida com sucesso!").queue();
		} finally {
			GuildDAO.updateGuildSettings(gc);
		}
	}
}
