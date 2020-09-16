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

import com.kuuhaku.command.Category;
import com.kuuhaku.command.Command;
import com.kuuhaku.controller.postgresql.ExceedDAO;
import com.kuuhaku.controller.sqlite.MemberDAO;
import com.kuuhaku.model.common.ColorlessEmbedBuilder;
import com.kuuhaku.model.enums.ExceedEnum;
import com.kuuhaku.model.enums.Tag;
import com.kuuhaku.model.enums.TagIcons;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.*;
import org.jetbrains.annotations.NonNls;

import java.util.Objects;
import java.util.Set;

public class MyTagsCommand extends Command {

	public MyTagsCommand(String name, String description, Category category, boolean requiresMM) {
		super(name, description, category, requiresMM);
	}

	public MyTagsCommand(@NonNls String name, @NonNls String[] aliases, String description, Category category, boolean requiresMM) {
		super(name, aliases, description, category, requiresMM);
	}

	public MyTagsCommand(String name, String usage, String description, Category category, boolean requiresMM) {
		super(name, usage, description, category, requiresMM);
	}

	public MyTagsCommand(String name, String[] aliases, String usage, String description, Category category, boolean requiresMM) {
		super(name, aliases, usage, description, category, requiresMM);
	}

	@Override
	public void execute(User author, Member member, String rawCmd, String[] args, Message message, MessageChannel channel, Guild guild, String prefix) {
		EmbedBuilder eb = new ColorlessEmbedBuilder();
		com.kuuhaku.model.persistent.Member mb = MemberDAO.getMemberById(author.getId() + guild.getId());
		String exceed = ExceedDAO.getExceed(author.getId());
		Set<Tag> tags = Tag.getTags(author, member);

		eb.setTitle(":label: Seus emblemas");

		StringBuilder badges = new StringBuilder();

		if (!exceed.isEmpty()) {
			badges.append(TagIcons.getExceed(ExceedEnum.getByName(exceed)));
		}

		tags.forEach(t -> badges.append(t.getEmote(mb) == null ? "" : Objects.requireNonNull(t.getEmote(mb)).getTag(mb.getLevel())));

		eb.addField("Emblemas:", badges.toString(), false);

		channel.sendMessage(eb.build()).queue();
	}
}
