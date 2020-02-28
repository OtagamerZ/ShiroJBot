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

import java.awt.*;

public class AddColorRoleCommand extends Command {

	public AddColorRoleCommand(String name, String description, Category category) {
		super(name, description, category);
	}

	public AddColorRoleCommand(String name, String[] aliases, String description, Category category) {
		super(name, aliases, description, category);
	}

	public AddColorRoleCommand(String name, String usage, String description, Category category) {
		super(name, usage, description, category);
	}

	public AddColorRoleCommand(String name, String[] aliases, String usage, String description, Category category) {
		super(name, aliases, usage, description, category);
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
			Role r = guild.createRole()
					.setColor(Color.decode(args[1]))
					.setName(args[0])
					.complete();
			guild.modifyRolePositions()
					.selectPosition(r)
					.moveTo(guild.getSelfMember().getRoles().get(0).getPosition() - 1)
					.complete();
			gc.addColorRole(args[0], args[1], r);

			channel.sendMessage("Cor adicionada com sucesso!").queue();
		} catch (NumberFormatException e) {
			channel.sendMessage(":x: | Cor inválida, verifique se digitou no formato `#RRGGBB`.").queue();
		} catch (ArrayIndexOutOfBoundsException e) {
			if (!gc.getColorRoles().has(args[0])) {
				channel.sendMessage("Essa cor não existe!").queue();
				return;
			}
			Role r = guild.getRoleById(gc.getColorRoles().getJSONObject(args[0]).getString("role"));

			if (r != null) r.delete().queue();
			gc.removeColorRole(args[0]);

			channel.sendMessage("Cor removida com sucesso!").queue();
		} finally {
			GuildDAO.updateGuildSettings(gc);
		}
	}
}
