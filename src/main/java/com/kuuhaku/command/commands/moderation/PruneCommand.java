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

import com.kuuhaku.command.Category;
import com.kuuhaku.command.Command;
import net.dv8tion.jda.core.entities.*;
import net.dv8tion.jda.core.events.Event;
import org.apache.commons.lang3.StringUtils;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class PruneCommand extends Command {

	public PruneCommand() {
		super("prune", new String[]{"clean", "limpar"}, "Limpa X mensagens do canal atual.", Category.MODERACAO);
	}

	@Override
	public void execute(User author, Member member, String rawCmd, String[] args, Message message, MessageChannel channel, Guild guild, Event event, String prefix) {
		if (args.length == 0) {
			AtomicBoolean noBotMessages = new AtomicBoolean(false);
			int i = 0;
			AtomicInteger count = new AtomicInteger();
			while (!noBotMessages.get()) {
				channel.getHistory().retrievePast(i += 100).queue(msgs -> {
					msgs.removeIf(m -> !m.getAuthor().isBot());
					for (Message msg : msgs) {
						msg.delete().queue();
						count.getAndIncrement();
					}
					if (msgs.isEmpty()) noBotMessages.set(true);
				});
			}
			channel.sendMessage(count.get() + " mensagens de bots limpas.").queue();
		} else if (StringUtils.isNumeric(args[0])) {
			List<Message> msgs = channel.getHistory().retrievePast(Integer.parseInt(args[0])).complete();
			for (Message msg : msgs) {
				msg.delete().queue();
			}
			channel.sendMessage(msgs.size() + " mensagen" + (msgs.size() == 1 ? "" : "s") + " limpas.").queue();
		} else {
			channel.sendMessage(":x: | Valor inválido, a quantidade deve ser um valor inteiro.").queue();
		}
	}
}
