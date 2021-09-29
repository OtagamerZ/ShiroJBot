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

package com.kuuhaku.command.commands.discord.hero;

import com.kuuhaku.command.Category;
import com.kuuhaku.command.Executable;
import com.kuuhaku.controller.postgresql.CardDAO;
import com.kuuhaku.handlers.games.tabletop.games.shoukan.Hero;
import com.kuuhaku.model.annotations.Command;
import com.kuuhaku.utils.Helper;
import net.dv8tion.jda.api.entities.*;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.concurrent.ExecutionException;

@Command(
		name = "fotoheroi",
		aliases = {"heroimage"},
		usage = "req_file",
		category = Category.SUPPORT
)
public class HeroImageCommand implements Executable {

	@Override
	public void execute(User author, Member member, String argsAsText, String[] args, Message message, TextChannel channel, Guild guild, String prefix) {
		Hero h = CardDAO.getHero(author.getId());

		if (h == null) {
			channel.sendMessage("❌ | Você não possui um herói.").queue();
			return;
		} else if (message.getAttachments().isEmpty()) {
			channel.sendMessage("❌ | Você precisa enviar uma imagem.").queue();
			return;
		}

		try {
			Message.Attachment a = message.getAttachments().get(0);
			if (!a.isImage()) {
				channel.sendMessage("❌ | Você precisa enviar uma imagem.").queue();
				return;
			}

			BufferedImage bi = ImageIO.read(a.retrieveInputStream().get());
			if (Helper.getRatio(bi.getWidth(), bi.getHeight()) != Helper.getRatio(225, 350)) {
				channel.sendMessage("❌ | A proporção da imagem deve ser 225:350.").queue();
				return;
			}

			h.setImage(bi);
			CardDAO.saveHero(h);

			channel.sendMessage("✅ | Imagem alterada com sucesso.").queue();
		} catch (InterruptedException | ExecutionException | IOException e) {
			channel.sendMessage("❌ | Imagem inválida.").queue();
		}
	}
}
