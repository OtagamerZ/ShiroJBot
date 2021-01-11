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

package com.kuuhaku.command.commands.discord.misc;

import com.github.ygimenez.method.Pages;
import com.github.ygimenez.model.Page;
import com.github.ygimenez.type.PageType;
import com.kuuhaku.command.Category;
import com.kuuhaku.command.Command;
import com.kuuhaku.controller.postgresql.AccountDAO;
import com.kuuhaku.handlers.games.tabletop.games.shoukan.enums.FrameColor;
import com.kuuhaku.model.common.ColorlessEmbedBuilder;
import com.kuuhaku.model.persistent.Account;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.*;
import org.jetbrains.annotations.NonNls;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class FrameColorCommand extends Command {

	public FrameColorCommand(String name, String description, Category category, boolean requiresMM) {
		super(name, description, category, requiresMM);
	}

	public FrameColorCommand(String name, String[] aliases, String description, Category category, boolean requiresMM) {
		super(name, aliases, description, category, requiresMM);
	}

	public FrameColorCommand(String name, String usage, String description, Category category, boolean requiresMM) {
		super(name, usage, description, category, requiresMM);
	}

	public FrameColorCommand(@NonNls String name, @NonNls String[] aliases, String usage, String description, Category category, boolean requiresMM) {
		super(name, aliases, usage, description, category, requiresMM);
	}

	@Override
	public void execute(User author, Member member, String rawCmd, String[] args, Message message, MessageChannel channel, Guild guild, String prefix) {
		Account acc = AccountDAO.getAccount(author.getId());

		if (args.length == 0) {
			List<Page> pages = new ArrayList<>();

			EmbedBuilder eb = new ColorlessEmbedBuilder();
			for (FrameColor fc : FrameColor.values()) {
				eb.clear();
				eb.setTitle(":flower_playing_cards: | Cor " + fc.toString().toLowerCase());
				eb.setDescription(fc.getDescription());
				eb.setThumbnail("https://raw.githubusercontent.com/OtagamerZ/ShiroJBot/master/src/main/resources/shoukan/frames/card_back_" + fc.name().toLowerCase() + ".png");
				eb.setImage("https://raw.githubusercontent.com/OtagamerZ/ShiroJBot/master/src/main/resources/shoukan/frames/card_front_" + fc.name().toLowerCase() + ".png");
				pages.add(new Page(PageType.EMBED, eb.build()));
			}

			channel.sendMessage((MessageEmbed) pages.get(0).getContent()).queue(s -> Pages.paginate(s, pages, 1, TimeUnit.MINUTES, 1, u -> u.getId().equals(author.getId())));
			return;
		}

		FrameColor fc = FrameColor.getByName(args[0]);

		if (fc == null) {
			channel.sendMessage("❌ | Nenhuma cor encontrada com esse nome.").queue();
			return;
		}

		acc.setFrame(fc);
		AccountDAO.saveAccount(acc);
		channel.sendMessage("✅ | Cor definida com sucesso!").queue();
	}
}
