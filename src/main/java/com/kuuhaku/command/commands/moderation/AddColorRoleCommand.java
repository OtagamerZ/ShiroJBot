/*
 * This file is part of Shiro J Bot.
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

package com.kuuhaku.command.commands.moderation;

import com.kuuhaku.command.Category;
import com.kuuhaku.command.Command;
import com.kuuhaku.controller.sqlite.GuildDAO;
import com.kuuhaku.model.persistent.GuildConfig;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.*;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NonNls;
import org.json.JSONObject;

import java.awt.*;

public class AddColorRoleCommand extends Command {

	public AddColorRoleCommand(String name, String description, Category category, boolean requiresMM) {
		super(name, description, category, requiresMM);
	}

	public AddColorRoleCommand(String name, String[] aliases, String description, Category category, boolean requiresMM) {
		super(name, aliases, description, category, requiresMM);
	}

	public AddColorRoleCommand(String name, String usage, String description, Category category, boolean requiresMM) {
		super(name, usage, description, category, requiresMM);
	}

	public AddColorRoleCommand(@NonNls String name, @NonNls String[] aliases, String usage, String description, Category category, boolean requiresMM) {
		super(name, aliases, usage, description, category, requiresMM);
	}

	@Override
	public void execute(User author, Member member, String rawCmd, String[] args, Message message, MessageChannel channel, Guild guild, String prefix) {
		GuildConfig gc = GuildDAO.getGuildById(guild.getId());

		if (!guild.getSelfMember().hasPermission(Permission.MANAGE_ROLES)) {
			channel.sendMessage(":x: | Eu preciso da permissão de gerenciar cargos para que possa criar as cores.").queue();
			return;
		} else if (args.length < 1) {
			channel.sendMessage(":x: | É necessário informar um nome e uma cor em formato hexadecimal (`#RRGGBB`). Caso você informe apenas o nome da cor, ela será removida da lista.").queue();
			return;
		}

		try {
			String name = StringUtils.capitalize(args[0].toLowerCase());
			JSONObject jo = gc.getColorRoles();

			if (jo.has(name) && guild.getRoleById(jo.getJSONObject(name).getString("role")) != null) {
				Role r = guild.getRoleById(jo.getJSONObject(name).getString("role"));
				assert r != null;
				r.getManager()
						.setColor(Color.decode(args[1]))
						.complete();

				gc.addColorRole(name, args[1], r);
				channel.sendMessage("Cor modificada com sucesso!").queue();
				return;
			}

			Role r = guild.createRole()
					.setColor(Color.decode(args[1]))
					.setName(name)
					.complete();
			guild.modifyRolePositions()
					.selectPosition(r)
					.moveTo(guild.getSelfMember().getRoles().get(0).getPosition() - 1)
					.complete();
			gc.addColorRole(name, args[1], r);

			channel.sendMessage("Cor adicionada com sucesso!").queue();
		} catch (NumberFormatException e) {
			channel.sendMessage(":x: | Cor inválida, verifique se digitou no formato `#RRGGBB`.").queue();
		} catch (ArrayIndexOutOfBoundsException e) {
			String name = StringUtils.capitalize(args[0].toLowerCase());
			if (!gc.getColorRoles().has(name)) {
				channel.sendMessage("Essa cor não existe!").queue();
				return;
			}
			Role r = guild.getRoleById(gc.getColorRoles().getJSONObject(name).getString("role"));

			if (r != null) r.delete().queue();
			gc.removeColorRole(name);

			channel.sendMessage("Cor removida com sucesso!").queue();
		} finally {
			GuildDAO.updateGuildSettings(gc);
		}
	}
}
