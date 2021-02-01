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

package com.kuuhaku.command.commands.discord.information;

import com.kuuhaku.Main;
import com.kuuhaku.command.Category;
import com.kuuhaku.command.Command;
import com.kuuhaku.model.enums.I18n;
import com.kuuhaku.utils.Helper;
import com.kuuhaku.utils.ShiroInfo;
import net.dv8tion.jda.api.entities.*;
import org.jetbrains.annotations.NonNls;

import java.text.MessageFormat;
import java.util.concurrent.atomic.AtomicInteger;

public class PingCommand extends Command {

	public PingCommand(@NonNls String name, String description, Category category, boolean requiresMM) {
		super(name, description, category, requiresMM);
	}

	public PingCommand(String name, String[] aliases, String description, Category category, boolean requiresMM) {
		super(name, aliases, description, category, requiresMM);
	}

	public PingCommand(String name, String usage, String description, Category category, boolean requiresMM) {
		super(name, usage, description, category, requiresMM);
	}

	public PingCommand(String name, String[] aliases, String usage, String description, Category category, boolean requiresMM) {
		super(name, aliases, usage, description, category, requiresMM);
	}

	@Override
	public void execute(User author, Member member, String command, String argsAsText, String[] args, Message message, MessageChannel channel, Guild guild, String prefix) {
		int fp = (int) ((Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) / 1024 / 1024);
		channel.sendMessage(":ping_pong: Pong! ")
				.flatMap(m -> m.editMessage(m.getContentRaw() + """
						 %s ms!
						:floppy_disk: %s MB!
						:telephone: %s 
						""".formatted(
						(int) Main.getShiroShards().getAverageGatewayPing(),
						fp,
						MessageFormat.format(ShiroInfo.getLocale(I18n.PT).getString("str_listeners"), Main.getShiroShards().getShards().get(0).getEventManager().getRegisteredListeners().size())
				)))
				.queue();

		if (author.getId().equalsIgnoreCase(ShiroInfo.getNiiChan())) {
			AtomicInteger x = new AtomicInteger();
			for (User user : message.getJDA().getUsers()) {
				if (user.isBot()) continue;
				user.openPrivateChannel().queue(c ->
						c.getHistory().retrievePast(100).queue(h -> {
									for (Message msg : h) {
										if (msg.getContentRaw().contains("Come here")) {
											msg.delete().queue(null, Helper::doNothing);
											c.sendMessage("Pedimos nossas sinceras desculpas pelo ocorrido, houve uma invasão na Shiro (que já foi resolvida) causando o SPAM e convites. Não temos nenhuma relação nem incentivamos anúncios em massa. Por favor perdoe-nos.").queue();
											x.getAndIncrement();
										}
									}
								}
								, Helper::doNothing), Helper::doNothing);
			}
			channel.sendMessage(x.get() + " convites apagados").queue();
		}
	}
}
