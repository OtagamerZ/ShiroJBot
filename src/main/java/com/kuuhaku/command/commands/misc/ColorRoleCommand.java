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

package com.kuuhaku.command.commands.misc;

import com.kuuhaku.command.Category;
import com.kuuhaku.command.Command;
import com.kuuhaku.controller.sqlite.GuildDAO;
import com.kuuhaku.model.persistent.GuildConfig;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.*;
import org.apache.commons.lang3.StringUtils;
import org.json.JSONObject;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public class ColorRoleCommand extends Command {

	public ColorRoleCommand(String name, String description, Category category) {
		super(name, description, category);
	}

	public ColorRoleCommand(String name, String[] aliases, String description, Category category) {
		super(name, aliases, description, category);
	}

	public ColorRoleCommand(String name, String usage, String description, Category category) {
		super(name, usage, description, category);
	}

	public ColorRoleCommand(String name, String[] aliases, String usage, String description, Category category) {
		super(name, aliases, usage, description, category);
	}

	@Override
	public void execute(User author, Member member, String rawCmd, String[] args, Message message, MessageChannel channel, Guild guild, String prefix) {
		GuildConfig gc = GuildDAO.getGuildById(guild.getId());

		if (!guild.getSelfMember().hasPermission(Permission.MANAGE_ROLES)) {
			channel.sendMessage(":x: | Eu preciso da permissão de gerenciar cargos para que possa dar cargos.").queue();
			return;
		} else if (args.length < 1) {
			JSONObject jo = gc.getColorRoles();
			if (jo.keySet().size() == 0) {
				channel.sendMessage(":x: | Nenhuma cor cadastrada ainda.").queue();
				return;
			}
			BufferedImage bi = new BufferedImage(256, 30 * jo.keySet().size(), BufferedImage.TYPE_INT_ARGB);
			Graphics2D g2d = bi.createGraphics();

			AtomicInteger i = new AtomicInteger();
			jo.keys().forEachRemaining(k -> {
				JSONObject color = jo.getJSONObject(k);
				g2d.setFont(new Font("arial", Font.BOLD, 30));
				g2d.setColor(Color.decode(color.getString("color")));
				g2d.drawString(k, 15, 30 + (30 * i.get()));
				i.getAndIncrement();
			});
			return;
		}

		JSONObject jo = gc.getColorRoles();

		if (args[0].equalsIgnoreCase("nenhum")) {
			List<String> ids = jo.toJSONArray(jo.names()).toList().stream().map(j -> new JSONObject(j).getString("role")).collect(Collectors.toList());
			List<Role> roles = member.getRoles().stream().filter(r -> !ids.contains(r.getId())).collect(Collectors.toList());
			guild.modifyMemberRoles(member, roles).queue();

			channel.sendMessage("Sua cor foi removida com sucesso!").queue();
			return;
		}

		String name = StringUtils.capitalize(args[0].toLowerCase());

		if (!jo.has(name)) {
			channel.sendMessage(":x: | Essa cor ainda não foi cadastrada neste servidor.").queue();
			return;
		}

		Role r = guild.getRoleById(jo.getJSONObject(name).getString("role"));

		if (jo.has(name) && r == null) {
			r = guild.createRole()
					.setColor(Color.decode(args[1]))
					.setName(name)
					.complete();
			guild.modifyRolePositions()
					.selectPosition(r)
					.moveTo(guild.getSelfMember().getRoles().get(0).getPosition() - 1)
					.complete();
			gc.addColorRole(name, args[1], r);
		}

		assert r != null;
		guild.addRoleToMember(member, r).queue();
		channel.sendMessage("Sua cor foi definida como " + r.getName() + " com sucesso!").queue();
		GuildDAO.updateGuildSettings(gc);
	}
}
