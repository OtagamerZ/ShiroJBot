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

package com.kuuhaku.command.commands.discord.exceed;

import com.kuuhaku.command.Category;
import com.kuuhaku.command.Executable;
import com.kuuhaku.model.annotations.Command;
import com.kuuhaku.model.annotations.Requires;
import com.kuuhaku.model.common.ColorlessEmbedBuilder;
import com.kuuhaku.model.enums.ExceedEnum;
import com.kuuhaku.model.enums.TagIcons;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.*;

@Command(
		name = "exceedpalette",
		aliases = {"expalette", "paletaex"},
		category = Category.EXCEED
)
@Requires({
		Permission.MESSAGE_EMBED_LINKS,
		Permission.MESSAGE_MANAGE,
		Permission.MESSAGE_ADD_REACTION,
		Permission.MESSAGE_EXT_EMOJI
})
public class ExceedPaletteCommand implements Executable {

	@Override
	public void execute(User author, Member member, String command, String argsAsText, String[] args, Message message, TextChannel channel, Guild guild, String prefix) {
		EmbedBuilder eb = new ColorlessEmbedBuilder();
		eb.setTitle("Cores de cada Exceed");
		for (ExceedEnum ex : ExceedEnum.values()) {
			eb.addField(TagIcons.getExceed(ex) + " | " + ex.getName() + ":", String.format("#%02x%02x%02x", ex.getPalette().getRed(), ex.getPalette().getGreen(), ex.getPalette().getBlue()), true);
		}
		eb.setThumbnail("https://image.flaticon.com/icons/png/512/564/564232.png");
		channel.sendMessage(eb.build()).queue();
	}
}
