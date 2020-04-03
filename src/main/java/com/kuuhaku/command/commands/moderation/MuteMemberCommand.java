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

import com.kuuhaku.Main;
import com.kuuhaku.command.Category;
import com.kuuhaku.command.Command;
import com.kuuhaku.controller.postgresql.MemberDAO;
import com.kuuhaku.controller.sqlite.GuildDAO;
import com.kuuhaku.model.persistent.GuildConfig;
import com.kuuhaku.model.persistent.MutedMember;
import com.kuuhaku.utils.Helper;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.exceptions.InsufficientPermissionException;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NonNls;
import org.json.JSONArray;

import java.util.Arrays;

public class MuteMemberCommand extends Command {

	public MuteMemberCommand(String name, String description, Category category, boolean requiresMM) {
		super(name, description, category, requiresMM);
	}

	public MuteMemberCommand(String name, String[] aliases, String description, Category category, boolean requiresMM) {
		super(name, aliases, description, category, requiresMM);
	}

	public MuteMemberCommand(String name, String usage, String description, Category category, boolean requiresMM) {
		super(name, usage, description, category, requiresMM);
	}

	public MuteMemberCommand(@NonNls String name, @NonNls String[] aliases, String usage, String description, Category category, boolean requiresMM) {
		super(name, aliases, usage, description, category, requiresMM);
	}

	@Override
	public void execute(User author, Member member, String rawCmd, String[] args, Message message, MessageChannel channel, Guild guild, String prefix) {
		GuildConfig gc = GuildDAO.getGuildById(guild.getId());

		if (message.getMentionedUsers().size() == 0) {
			channel.sendMessage(":x: | Você precisa mencionar um membro.").queue();
			return;
		} else if (args.length < 2) {
			channel.sendMessage(":x: | Você precisa informar um tempo (em minutos).").queue();
			return;
		} else if (!StringUtils.isNumeric(args[1])) {
			channel.sendMessage(":x: | O tempo de punição deve um valor inteiro.").queue();
			return;
		} else if (args.length < 3) {
			channel.sendMessage(":x: | Você precisa informar um motivo.").queue();
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
		} else if (gc.getCargoWarn() == null || gc.getCargoWarn().isEmpty()) {
			channel.sendMessage(":x: | Nenhum cargo de punição configurado neste servidor.").queue();
			return;
		} else if (MemberDAO.getMutedMemberById(message.getMentionedMembers().get(0).getId()) != null && MemberDAO.getMutedMemberById(message.getMentionedUsers().get(0).getId()).isMuted()) {
			channel.sendMessage(":x: | Este membro já está silenciado.").queue();
			return;
		}

		String reason = String.join(" ", Arrays.copyOfRange(args, 2, args.length));

		try {
			Member mb = message.getMentionedMembers().get(0);
			MutedMember m = Helper.getOr(MemberDAO.getMutedMemberById(mb.getId()), new MutedMember(mb.getId(), guild.getId()));

			m.setReason(reason);
			m.setRoles(new JSONArray(mb.getRoles().stream().map(Role::getId).toArray(String[]::new)));
			m.mute(Integer.parseInt(args[1]));

			MemberDAO.saveMutedMember(m);

			guild.modifyMemberRoles(mb, guild.getRoleById(gc.getCargoWarn())).complete();
			Helper.logToChannel(author, false, null, mb.getAsMention() + " foi silenciado com a seguinte razão: `" + reason + "`", guild);
			channel.sendMessage("Usuário silenciado com sucesso!\nMotivo: `" + reason + "`").queue();
		} catch (InsufficientPermissionException e) {
			channel.sendMessage(":x: | Não possuo a permissão para silenciar membros.").queue();
		}
	}
}
