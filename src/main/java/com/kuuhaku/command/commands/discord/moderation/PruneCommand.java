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

import com.kuuhaku.command.Category;
import com.kuuhaku.command.Executable;
import com.kuuhaku.model.annotations.Command;
import com.kuuhaku.model.annotations.Requires;
import com.kuuhaku.model.enums.I18n;
import com.kuuhaku.utils.Helper;
import com.kuuhaku.utils.ShiroInfo;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.exceptions.InsufficientPermissionException;
import org.apache.commons.lang3.StringUtils;

import java.util.List;

@Command(
		name = "apagar",
		aliases = {"prune", "clean", "limpar"},
		usage = "req_qtd-all",
		category = Category.MODERATION
)
@Requires({Permission.MESSAGE_MANAGE})
public class PruneCommand implements Executable {

	@Override
	public void execute(User author, Member member, String command, String argsAsText, String[] args, Message message, TextChannel channel, Guild guild, String prefix) {
		if (args.length == 0) {
			List<Message> msgs = channel.getHistory().retrievePast(100).complete();
			msgs.removeIf(m -> !m.getAuthor().isBot());
			channel.purgeMessages(msgs);
			channel.sendMessage(msgs.size() + " mensage" + (msgs.size() == 1 ? "m de bot limpa." : "ns de bots limpas.")).queue();
		} else if (StringUtils.isNumeric(args[0]) && args[0].length() >= 10) {
			List<Message> msgs = channel.getHistory().retrievePast(100).complete();
			msgs.removeIf(m -> !m.getAuthor().getId().equals(args[0]));
			channel.purgeMessages(msgs);
			channel.sendMessage(msgs.size() + " mensage" + (msgs.size() == 1 ? "m de <@" + args[0] + "> limpa." : "ns de <@" + args[0] + "> limpas.")).queue(null, Helper::doNothing);
		} else if (StringUtils.isNumeric(args[0])) {
			if (!Helper.between(Integer.parseInt(args[0]), 1, 101)) {
				channel.sendMessage("❌ | Só é possível apagar entre 1 e 100 mensagens de uma vez").queue();
				return;
			}

			List<Message> msgs = channel.getHistory().retrievePast(Integer.parseInt(args[0]) == 100 ? 100 : Integer.parseInt(args[0]) + 1).complete();
			channel.purgeMessages(msgs);
			channel.sendMessage(msgs.size() + " mensage" + (msgs.size() == 1 ? "m limpa." : "ns limpas.")).queue(null, Helper::doNothing);
		} else if (message.getMentionedUsers().size() > 0) {
			User target = message.getMentionedUsers().get(0);
			List<Message> msgs = channel.getHistory().retrievePast(100).complete();
			msgs.removeIf(m -> !m.getAuthor().getId().equals(target.getId()));
			channel.purgeMessages(msgs);
			channel.sendMessage(msgs.size() + " mensage" + (msgs.size() == 1 ? "m de " + target.getAsMention() + " limpa." : "ns de " + target.getAsMention() + " limpas.")).queue(null, Helper::doNothing);
		} else if (Helper.equalsAny(args[0], "user", "usuarios")) {
			List<Message> msgs = channel.getHistory().retrievePast(100).complete();
			msgs.removeIf(m -> m.getAuthor().isBot());
			channel.purgeMessages(msgs);
			channel.sendMessage(msgs.size() + " mensage" + (msgs.size() == 1 ? "m de usuário limpa." : "ns de usuários limpas.")).queue(null, Helper::doNothing);
		} else if (Helper.equalsAny(args[0], "all", "tudo")) {
			((TextChannel) channel).createCopy().queue(s -> {
				try {
					((GuildChannel) channel).delete().queue();
					s.sendMessage("✅ | Canal limpo com sucesso!").queue(null, Helper::doNothing);
				} catch (InsufficientPermissionException e) {
					channel.sendMessage(ShiroInfo.getLocale(I18n.PT).getString("err_prune-permission-required")).queue(null, Helper::doNothing);
				}
			});
		} else {
			channel.sendMessage(ShiroInfo.getLocale(I18n.PT).getString("err_amount-not-valid")).queue();
		}
	}
}
