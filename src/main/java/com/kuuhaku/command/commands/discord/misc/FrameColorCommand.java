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

package com.kuuhaku.command.commands.discord.misc;

import com.github.ygimenez.method.Pages;
import com.github.ygimenez.model.InteractPage;
import com.github.ygimenez.model.Page;
import com.kuuhaku.command.Category;
import com.kuuhaku.command.Executable;
import com.kuuhaku.handlers.games.tabletop.games.shoukan.enums.FrameColor;
import com.kuuhaku.model.annotations.Command;
import com.kuuhaku.model.annotations.Requires;
import com.kuuhaku.model.persistent.Account;
import com.kuuhaku.utils.Constants;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

@Command(
		name = "cordaborda",
		aliases = {"borda", "framecolor", "frame"},
		usage = "req_frame-color",
		category = Category.MISC
)
@Requires({
		Permission.MESSAGE_EMBED_LINKS,
		Permission.MESSAGE_ADD_REACTION,
		Permission.MESSAGE_ATTACH_FILES
})
public class FrameColorCommand implements Executable {

	@Override
	public void execute(User author, Member member, String argsAsText, String[] args, Message message, TextChannel channel, Guild guild, String prefix) {
		Account acc = Account.find(Account.class, author.getId());

		if (args.length == 0) {
			List<Page> pages = new ArrayList<>();

			EmbedBuilder eb = new EmbedBuilder();
			for (FrameColor fc : FrameColor.values()) {
				if (fc.canUse(acc)) {
					eb.clear()
							.setTitle(":flower_playing_cards: | Cor " + fc.toString().toLowerCase(Locale.ROOT))
							.setDescription(fc.getDescription())
							.setThumbnail(Constants.RESOURCES_URL + "/shoukan/frames/back/" + fc.name().toLowerCase(Locale.ROOT) + ".png")
							.setImage(Constants.RESOURCES_URL + "/shoukan/frames/front/" + fc.name().toLowerCase(Locale.ROOT) + ".png")
							.setColor(fc.getThemeColor());

					pages.add(new InteractPage(eb.build()));
				}
			}

			channel.sendMessageEmbeds((MessageEmbed) pages.get(0).getContent()).queue(s -> Pages.paginate(s, pages, Constants.USE_BUTTONS, 1, TimeUnit.MINUTES, true, u -> u.getId().equals(author.getId())));
			return;
		}

		FrameColor fc = FrameColor.getByName(args[0]);

		if (fc == null) {
			channel.sendMessage("❌ | Nenhuma cor encontrada com esse nome.").queue();
			return;
		} else if (!fc.canUse(acc)) {
			channel.sendMessage("❌ | Você ainda não desbloqueou essa cor.").queue();
			return;
		}

		acc.setFrame(fc);
		acc.save();
		channel.sendMessage("✅ | Cor definida com sucesso!").queue();
	}
}
