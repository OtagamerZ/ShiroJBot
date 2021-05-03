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

package com.kuuhaku.command.commands.discord.misc;

import com.kuuhaku.command.Category;
import com.kuuhaku.command.Executable;
import com.kuuhaku.controller.postgresql.GuildDAO;
import com.kuuhaku.model.annotations.Command;
import com.kuuhaku.model.annotations.Requires;
import com.kuuhaku.model.enums.I18n;
import com.kuuhaku.model.persistent.guild.ColorRole;
import com.kuuhaku.model.persistent.guild.GuildConfig;
import com.kuuhaku.utils.Helper;
import com.kuuhaku.utils.ShiroInfo;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.*;
import org.apache.commons.lang3.StringUtils;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Command(
		name = "cor",
		aliases = {"color"},
		usage = "req_name",
		category = Category.MISC
)
@Requires({Permission.MANAGE_ROLES, Permission.MESSAGE_ATTACH_FILES})
public class ColorRoleCommand implements Executable {

	@Override
	public void execute(User author, Member member, String command, String argsAsText, String[] args, Message message, TextChannel channel, Guild guild, String prefix) {
		GuildConfig gc = GuildDAO.getGuildById(guild.getId());

		if (!guild.getSelfMember().hasPermission(Permission.MANAGE_ROLES)) {
			channel.sendMessage(ShiroInfo.getLocale(I18n.PT).getString("err_manage-roles")).queue();
			return;
		} else if (args.length < 1) {
			Set<ColorRole> roles = new TreeSet<>(Comparator.comparing(ColorRole::getName));
			roles.addAll(gc.getColorRoles());
			if (roles.isEmpty()) {
				channel.sendMessage(ShiroInfo.getLocale(I18n.PT).getString("err_color-not-found")).queue();
				return;
			}

			BufferedImage bi = new BufferedImage(960, 60 + 30 * (roles.size() / 3), BufferedImage.TYPE_INT_ARGB);
			Graphics2D g2d = bi.createGraphics();
			g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			g2d.setFont(new Font("arial", Font.BOLD, 30));

			int i = 0;
			for (ColorRole role : roles) {
				g2d.setColor(role.getColor());
				g2d.drawString(role.getName(), 30 + (i % 3) * 300, 45 + (30 * (i / 3)));
				i++;
			}

			g2d.dispose();

			try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
				ImageIO.write(bi, "png", baos);
				channel.sendMessage("**Cores disponíveis neste servidor:**").addFile(baos.toByteArray(), "colors.png").queue();
			} catch (IOException e) {
				Helper.logger(this.getClass()).error(e + " | " + e.getStackTrace()[0]);
			}
			return;
		}

		Map<String, ColorRole> roles = gc.getColorRoles().stream()
				.collect(Collectors.toMap(cr -> StringUtils.capitalize(cr.getName().toLowerCase(Locale.ROOT)), Function.identity()));
		List<Role> rols = member.getRoles().stream()
				.filter(r -> roles.values().stream().noneMatch(cr -> cr.getId().equals(r.getId())))
				.collect(Collectors.toList());

		if (args[0].equalsIgnoreCase("nenhum")) {
			guild.modifyMemberRoles(member, rols).queue();

			channel.sendMessage("✅ | Sua cor foi removida com sucesso!").queue();
			return;
		}

		String name = StringUtils.capitalize(args[0].toLowerCase(Locale.ROOT));

		if (!roles.containsKey(name)) {
			channel.sendMessage(ShiroInfo.getLocale(I18n.PT).getString("err_color-not-registered")).queue();
			return;
		}

		Role r = guild.getRoleById(roles.get(name).getId());

		if (roles.containsKey(name) && r == null) {
			Color c = roles.get(name).getColor();
			r = guild.createRole()
					.setColor(c)
					.setName(name)
					.setPermissions(Permission.EMPTY_PERMISSIONS)
					.complete();

			guild.modifyRolePositions()
					.selectPosition(r)
					.moveTo(guild.getSelfMember().getRoles().get(0).getPosition() - 1)
					.complete();

			gc.addColorRole(r.getId(), c, name);
		}

		assert r != null;
		if (r.getPosition() > guild.getSelfMember().getRoles().get(0).getPosition()) {
			channel.sendMessage("❌ | O cargo dessa cor está acima de mim. Por favor peça a um moderador para colocar-me acima dele.").queue();
			return;
		}

		guild.modifyMemberRoles(member, rols).queue();
		guild.addRoleToMember(member, r).queue();
		channel.sendMessage("✅ | Sua cor foi definida como " + r.getName() + " com sucesso!").queue();
		GuildDAO.updateGuildSettings(gc);
	}
}
