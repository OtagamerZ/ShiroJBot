/*
 * This file is part of Shiro J Bot.
 *
 *     Shiro J Bot is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     Shiro J Bot is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with Shiro J Bot.  If not, see <https://www.gnu.org/licenses/>
 */

package com.kuuhaku.command.commands.moderation;

import com.kuuhaku.Main;
import com.kuuhaku.command.Category;
import com.kuuhaku.command.Command;
import com.kuuhaku.controller.sqlite.GuildDAO;
import com.kuuhaku.utils.Helper;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.exceptions.ErrorResponseException;
import net.dv8tion.jda.api.exceptions.InsufficientPermissionException;

import java.util.Objects;

public class PunishMemberCommand extends Command {

	public PunishMemberCommand() {
		super("punish", new String[]{"punir", "mutar", "silenciar"}, "<membro>", "Pune o membro especificado.", Category.MODERACAO);
	}

	@Override
	public void execute(User author, Member member, String rawCmd, String[] args, Message message, MessageChannel channel, Guild guild, String prefix) {
		if (message.getMentionedUsers().size() == 0) {
			channel.sendMessage(":x: | Você precisa mencionar um membro.").queue();
			return;
		} else if (message.getMentionedUsers().size() > 1) {
			channel.sendMessage(":x: | Você mencionou membros demais.").queue();
			return;
		} else if (!member.hasPermission(Permission.MESSAGE_MANAGE)) {
			channel.sendMessage(":x: | Você não possui permissão para punir membros.").queue();
			return;
		} else if (Helper.hasRoleHigherThan(member, message.getMentionedMembers().get(0))) {
			channel.sendMessage(":x: | Você não pode punir membros que possuem o mesmo cargo ou maior.").queue();
			return;
		} else if (Main.getInfo().getDevelopers().contains(message.getMentionedUsers().get(0).getId())) {
			channel.sendMessage(":x: | Não posso punir meus desenvolvedores, faça isso manualmente.").queue();
			return;
		} else if (message.getMentionedMembers().get(0).getRoles().stream().anyMatch(r -> r.getId().equals(GuildDAO.getGuildById(guild.getId()).getCargoWarn()))) {
			channel.sendMessage(":x: | Este membro já está com uma punição ativa.").queue();
			return;
		}

		try {
			guild.addRoleToMember(message.getMentionedMembers().get(0), Objects.requireNonNull(guild.getRoleById(GuildDAO.getGuildById(guild.getId()).getCargoWarn()))).queue();
			channel.sendMessage("Punição aplicada com sucesso!").queue();
		} catch (NullPointerException | ErrorResponseException e) {
            channel.sendMessage(":x: | Cargo de punição não encontrado.").queue();
		} catch (InsufficientPermissionException e) {
			channel.sendMessage(":x: | Não possuo a permissão para punir membros.").queue();
		}
	}
}
