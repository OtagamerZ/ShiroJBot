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

package com.kuuhaku.command.commands.misc;

import com.kuuhaku.command.Category;
import com.kuuhaku.command.Command;
import com.kuuhaku.controller.sqlite.GuildDAO;
import com.kuuhaku.model.persistent.GuildConfig;
import com.kuuhaku.utils.Helper;
import com.kuuhaku.utils.I18n;
import com.kuuhaku.utils.ShiroInfo;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.*;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NonNls;
import org.json.JSONObject;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public class ColorRoleCommand extends Command {

	public ColorRoleCommand(String name, String description, Category category, boolean requiresMM) {
		super(name, description, category, requiresMM);
	}

	public ColorRoleCommand(String name, String[] aliases, String description, Category category, boolean requiresMM) {
		super(name, aliases, description, category, requiresMM);
	}

	public ColorRoleCommand(String name, String usage, String description, Category category, boolean requiresMM) {
		super(name, usage, description, category, requiresMM);
	}

	public ColorRoleCommand(@NonNls String name, @NonNls String[] aliases, String usage, String description, Category category, boolean requiresMM) {
		super(name, aliases, usage, description, category, requiresMM);
	}

	@Override
	public void execute(User author, Member member, String rawCmd, String[] args, Message message, MessageChannel channel, Guild guild, String prefix) {
		GuildConfig gc = GuildDAO.getGuildById(guild.getId());

		if (!guild.getSelfMember().hasPermission(Permission.MANAGE_ROLES)) {
            channel.sendMessage(ShiroInfo.getLocale(I18n.PT).getString("err_manage-roles")).queue();
            return;
        } else if (args.length < 1) {
			JSONObject jo = gc.getColorRoles();
			if (jo.keySet().size() == 0) {
                channel.sendMessage(ShiroInfo.getLocale(I18n.PT).getString("err_color-not-found")).queue();
                return;
            }
			BufferedImage bi = new BufferedImage(900, 30 + 30 * (jo.keySet().size() / 3), BufferedImage.TYPE_INT_ARGB);
			Graphics2D g2d = bi.createGraphics();
			g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

			AtomicInteger i = new AtomicInteger();
			jo.keys().forEachRemaining(k -> {
				JSONObject color = jo.getJSONObject(k);
				g2d.setFont(new Font("arial", Font.BOLD, 30));
				g2d.setColor(Color.decode(color.getString("color")));
				g2d.drawString(k, (i.get() % 3) * 300, 30 + (30 * (i.get() / 3)));
				i.getAndIncrement();
			});

			g2d.dispose();

			try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
				ImageIO.write(bi, "png", baos);
				channel.sendMessage("**Cores disponíveis neste servidor:**").addFile(baos.toByteArray(), "colors.png").queue();
			} catch (IOException e) {
				Helper.logger(this.getClass()).error(e + " | " + e.getStackTrace()[0]);
			}
			return;
		}

		JSONObject jo = gc.getColorRoles();
		List<String> ids = new ArrayList<>();
		jo.keys().forEachRemaining(k -> ids.add(jo.getJSONObject(k).getString("role")));
		List<Role> roles = member.getRoles().stream().filter(r -> !ids.contains(r.getId())).collect(Collectors.toList());

		if (args[0].equalsIgnoreCase("nenhum")) {
			guild.modifyMemberRoles(member, roles).queue();

			channel.sendMessage("Sua cor foi removida com sucesso!").queue();
			return;
		}

		String name = StringUtils.capitalize(args[0].toLowerCase());

		if (!jo.has(name)) {
            channel.sendMessage(ShiroInfo.getLocale(I18n.PT).getString("err_color-not-registered")).queue();
            return;
        }

		Role r = guild.getRoleById(jo.getJSONObject(name).getString("role"));

		if (jo.has(name) && r == null) {
			String c = jo.getJSONObject(name).getString("color");
			r = guild.createRole()
					.setColor(Color.decode(c))
					.setName(name)
					.complete();
			guild.modifyRolePositions()
					.selectPosition(r)
					.moveTo(guild.getSelfMember().getRoles().get(0).getPosition() - 1)
					.complete();
			gc.addColorRole(name, c, r);
		}

		guild.modifyMemberRoles(member, roles).queue();
		assert r != null;
		guild.addRoleToMember(member, r).queue();
		channel.sendMessage("Sua cor foi definida como " + r.getName() + " com sucesso!").queue();
		GuildDAO.updateGuildSettings(gc);
	}
}
