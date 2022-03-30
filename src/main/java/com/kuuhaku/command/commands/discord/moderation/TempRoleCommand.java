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

import com.kuuhaku.command.Category;
import com.kuuhaku.command.Executable;
import com.kuuhaku.controller.postgresql.TempRoleDAO;
import com.kuuhaku.model.annotations.Command;
import com.kuuhaku.model.annotations.Requires;
import com.kuuhaku.model.persistent.TempRole;
import com.kuuhaku.utils.helpers.StringHelper;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.*;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;

@Command(
		name = "cargotemp",
		aliases = {"temprole"},
		usage = "req_mention-role-time",
		category = Category.MODERATION
)
@Requires({Permission.MANAGE_ROLES})
public class TempRoleCommand implements Executable {

	@Override
	public void execute(User author, Member member, String argsAsText, String[] args, Message message, TextChannel channel, Guild guild, String prefix) {
		if (message.getMentionedMembers().isEmpty()) {
			channel.sendMessage("❌ | Você precisa mencionar um usuário.").queue();
			return;
		} else if (message.getMentionedRoles().isEmpty()) {
			channel.sendMessage("❌ | Você precisa mencionar um cargo.").queue();
			return;
		}

		Member mb = message.getMentionedMembers().get(0);
		Role r = message.getMentionedRoles().get(0);

		if (r.getPosition() > guild.getSelfMember().getRoles().get(0).getPosition()) {
			channel.sendMessage("❌ | Não posso manipular cargos que estejam acima de mim.").queue();
			return;
		}

		long time = StringHelper.stringToDurationMillis(argsAsText);
		if (time < 60000) {
			channel.sendMessage("❌ | O tempo deve ser ao menos 1 minuto.").queue();
		}

		ZonedDateTime zdt = ZonedDateTime.now(ZoneId.of("GMT-3")).plus(time, ChronoUnit.MILLIS);

		guild.addRoleToMember(mb, r)
				.flatMap(s -> channel.sendMessage("✅ | Cargo `" + r.getName() + "` atribuído à " + mb.getAsMention() + " com sucesso até <t:" + zdt.toEpochSecond() + ">!"))
				.queue(
						s -> TempRoleDAO.saveTempRole(new TempRole(mb, r, zdt)),
						t -> channel.sendMessage("❌ | Erro ao atribuir cargo.").queue()
				);
	}
}
