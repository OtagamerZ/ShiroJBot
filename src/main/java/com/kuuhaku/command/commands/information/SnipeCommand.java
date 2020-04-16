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

package com.kuuhaku.command.commands.information;

import com.kuuhaku.command.Category;
import com.kuuhaku.command.Command;
import com.kuuhaku.utils.Helper;
import com.kuuhaku.utils.I18n;
import com.kuuhaku.utils.ShiroInfo;
import com.spaceprogram.kittycache.KittyCache;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.*;
import org.jetbrains.annotations.NonNls;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class SnipeCommand extends Command {

	public SnipeCommand(String name, String description, Category category, boolean requiresMM) {
		super(name, description, category, requiresMM);
	}

	public SnipeCommand(String name, String[] aliases, String description, Category category, boolean requiresMM) {
		super(name, aliases, description, category, requiresMM);
	}

	public SnipeCommand(String name, String usage, String description, Category category, boolean requiresMM) {
		super(name, usage, description, category, requiresMM);
	}

	public SnipeCommand(@NonNls String name, @NonNls String[] aliases, String usage, String description, Category category, boolean requiresMM) {
		super(name, aliases, usage, description, category, requiresMM);
	}

	@Override
	public void execute(User author, Member member, String rawCmd, String[] args, Message message, MessageChannel channel, Guild guild, String prefix) {
		if (message.getMentionedChannels().size() < 1 || message.getMentionedChannels().get(0).getType() != ChannelType.TEXT) {
			channel.sendMessage(ShiroInfo.getLocale(I18n.PT).getString("err_no-channel")).queue();
			return;
		}

		TextChannel chn = message.getMentionedChannels().get(0);
		EmbedBuilder eb = new EmbedBuilder();

		KittyCache<String, Message> cache = ShiroInfo.retrieveCache(guild);
		List<String> keys = new ArrayList<>();
		for (int i = 0; i < cache.size(); i++) {
			keys.add(String.valueOf(i));
		}

		try {
			eb.setTitle("Ultimas 5 mensagens deletadas no canal " + chn.getName());
			List<Message> msgs = cache.getAll(keys).values().stream().filter(m -> m.getChannel().getId().equals(chn.getId())).collect(Collectors.toList());
			msgs.sort(Comparator.comparing(Message::getTimeCreated).reversed());

			msgs.subList(0, Math.min(msgs.size(), 5)).forEach(m -> eb.addField("Enviada por " + m.getAuthor().getAsMention(), m.getContentRaw(), false));
			eb.setColor(Helper.getRandomColor());

			channel.sendMessage(eb.build()).queue();
		} catch (NullPointerException e) {
			channel.sendMessage(ShiroInfo.getLocale(I18n.PT).getString("err_no-cache")).queue();
		}
	}
}