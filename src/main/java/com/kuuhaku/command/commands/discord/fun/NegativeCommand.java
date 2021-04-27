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

package com.kuuhaku.command.commands.discord.fun;

import com.kuuhaku.command.Category;
import com.kuuhaku.command.Executable;
import com.kuuhaku.model.annotations.Command;
import com.kuuhaku.model.annotations.Requires;
import com.kuuhaku.utils.Helper;
import com.kuuhaku.utils.ImageFilters;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.*;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.Comparator;
import java.util.concurrent.atomic.AtomicReference;

@Command(
		name = "negativo",
		aliases = {"negative"},
		category = Category.FUN
)
@Requires({Permission.MESSAGE_ATTACH_FILES, Permission.MESSAGE_HISTORY})
public class NegativeCommand implements Executable {

	@Override
	public void execute(User author, Member member, String command, String argsAsText, String[] args, Message message, TextChannel channel, Guild guild, String prefix) {
		AtomicReference<Message> ms = new AtomicReference<>();
		channel.sendMessage("<a:loading:697879726630502401> Negativando a imagem...")
				.flatMap(s -> {
					ms.set(s);
					return channel.getHistory().retrievePast(25);
				})
				.queue(s -> {
					Message msg;
					if (Helper.getImageFrom(message) != null)
						msg = message;
					else
						msg = s.stream()
								.filter(m -> Helper.getImageFrom(m) != null)
								.max(Comparator.comparing(ISnowflake::getTimeCreated))
								.orElse(null);

					if (msg == null) {
						ms.get().delete().queue(null, Helper::doNothing);
						channel.sendMessage("❌ | Não encontrei nenhum imagem nas últimas 25 mensagens, por favor envie uma.").queue();
						return;
					}

					try {
						BufferedImage bi = ImageIO.read(Helper.getImage(Helper.getImageFrom(msg)));

						ms.get().delete().queue(null, Helper::doNothing);
						channel.sendMessage("Aqui está sua imagem!")
								.addFile(Helper.writeAndGet(ImageFilters.invert(bi), "inverted", "png"))
								.queue();
					} catch (IOException e) {
						ms.get().delete().queue(null, Helper::doNothing);
						channel.sendMessage("❌ | Deu erro ao baixar a imagem, tente com outra.").queue();
					}
				});
	}
}
