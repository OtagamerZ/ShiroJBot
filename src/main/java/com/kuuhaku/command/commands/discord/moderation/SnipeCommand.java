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

import com.github.ygimenez.method.Pages;
import com.github.ygimenez.model.InteractPage;
import com.github.ygimenez.model.Page;
import com.kuuhaku.Main;
import com.kuuhaku.command.Category;
import com.kuuhaku.command.Executable;
import com.kuuhaku.model.annotations.Command;
import com.kuuhaku.model.annotations.Requires;
import com.kuuhaku.model.common.ColorlessEmbedBuilder;
import com.kuuhaku.utils.Helper;
import com.kuuhaku.utils.ShiroInfo;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.*;
import org.apache.commons.lang.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Command(
		name = "snipe",
		aliases = {"undelete", "desdeletar"},
		category = Category.MODERATION
)
@Requires({
		Permission.MESSAGE_HISTORY,
		Permission.MESSAGE_EMBED_LINKS,
		Permission.MESSAGE_ADD_REACTION,
		Permission.MESSAGE_EXT_EMOJI
})
public class SnipeCommand implements Executable {

	@Override
	public void execute(User author, Member member, String argsAsText, String[] args, Message message, TextChannel channel, Guild guild, String prefix) {
		List<Message> c = List.copyOf(Main.getInfo().retrieveCache(guild).values());
		List<String> hist = channel.getHistory()
				.retrievePast(100)
				.complete().stream()
				.map(Message::getId)
				.toList();

		List<List<Message>> chunks = Helper.chunkify(
				c.stream()
						.filter(m -> m.getChannel().getId().equals(channel.getId()))
						.filter(m -> !hist.contains(m.getId()))
						.toList(),
				10);
		if (chunks.isEmpty()) {
			channel.sendMessage("❌ | Não há nenhuma mensagem deletada recentemente neste canal.").queue();
			return;
		}

		List<Page> pages = new ArrayList<>();
		EmbedBuilder eb = new ColorlessEmbedBuilder()
				.setTitle(":detective: | Mensagens deletadas recentemente");
		for (List<Message> chunk : chunks) {
			eb.clearFields();

			for (Message msg : chunk) {
				eb.addField("(" + Helper.TIMESTAMP.formatted(msg.getTimeCreated().toEpochSecond()) + ") " + msg.getAuthor().getAsTag() + " disse:", StringUtils.abbreviate(msg.getContentRaw(), 100), false);
			}

			pages.add(new InteractPage(eb.build()));
		}

		channel.sendMessageEmbeds((MessageEmbed) pages.get(0).getContent()).queue(s ->
				Pages.paginate(s, pages, ShiroInfo.USE_BUTTONS, 1, TimeUnit.MINUTES, u -> u.getId().equals(author.getId()))
		);
	}
}
