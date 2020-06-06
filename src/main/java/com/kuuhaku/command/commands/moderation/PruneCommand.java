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

package com.kuuhaku.command.commands.moderation;

import com.kuuhaku.command.Category;
import com.kuuhaku.command.Command;
import com.kuuhaku.utils.Helper;
import com.kuuhaku.utils.I18n;
import com.kuuhaku.utils.ShiroInfo;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.exceptions.InsufficientPermissionException;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NonNls;

import java.util.List;

public class PruneCommand extends Command {

	public PruneCommand(String name, String description, Category category, boolean requiresMM) {
		super(name, description, category, requiresMM);
	}

	public PruneCommand(String name, String[] aliases, String description, Category category, boolean requiresMM) {
		super(name, aliases, description, category, requiresMM);
	}

	public PruneCommand(String name, String usage, String description, Category category, boolean requiresMM) {
		super(name, usage, description, category, requiresMM);
	}

	public PruneCommand(@NonNls String name, @NonNls String[] aliases, String usage, String description, Category category, boolean requiresMM) {
		super(name, aliases, usage, description, category, requiresMM);
	}

	@Override
	public void execute(User author, Member member, String rawCmd, String[] args, Message message, MessageChannel channel, Guild guild, String prefix) {
		if (args.length == 0) {
			List<Message> msgs = channel.getHistory().retrievePast(100).complete();
			msgs.removeIf(m -> !m.getAuthor().isBot());
			channel.purgeMessages(msgs);
			channel.sendMessage(msgs.size() + " mensage" + (msgs.size() == 1 ? "m de bot limpa." : "ns de bots limpas.")).queue();
		} else if (StringUtils.isNumeric(args[0])) {
			List<Message> msgs = channel.getHistory().retrievePast(Integer.parseInt(args[0]) == 100 ? 100 : Integer.parseInt(args[0]) + 1).complete();
			channel.purgeMessages(msgs);
			channel.sendMessage(msgs.size() + " mensage" + (msgs.size() == 1 ? "m limpa." : "ns limpas.")).queue();
		} else if (Helper.containsAny(args[0], "user", "usuarios")) {
			List<Message> msgs = channel.getHistory().retrievePast(100).complete();
			msgs.removeIf(m -> m.getAuthor().isBot());
			channel.purgeMessages(msgs);
			channel.sendMessage(msgs.size() + " mensage" + (msgs.size() == 1 ? "m de usuário limpa." : "ns de usuários limpas.")).queue();
		} else if (Helper.containsAny(args[0], "all", "tudo")) {
			((TextChannel) channel).createCopy().queue(s -> {
				try {
					((GuildChannel) channel).delete().queue();
					s.sendMessage("Canal limpo com sucesso!").queue();
				} catch (InsufficientPermissionException e) {
					channel.sendMessage(ShiroInfo.getLocale(I18n.PT).getString("err_prune-permission-required")).queue();
				}
			});
		} else {
			channel.sendMessage(ShiroInfo.getLocale(I18n.PT).getString("err_amount-not-valid")).queue();
        }
	}
}
