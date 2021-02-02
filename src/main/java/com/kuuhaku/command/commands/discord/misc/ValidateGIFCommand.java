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

import com.kuuhaku.command.Category;
import com.kuuhaku.command.Executable;
import com.kuuhaku.model.annotations.Command;
import com.kuuhaku.utils.Helper;
import net.dv8tion.jda.api.entities.*;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;

@Command(
		name = "validate",
		aliases = {"testgif", "tgif"},
		usage = "req_link",
		category = Category.MISC
)
public class ValidateGIFCommand implements Executable {

	@Override
	public void execute(User author, Member member, String command, String argsAsText, String[] args, Message message, TextChannel channel, Guild guild, String prefix) {
		if (args.length == 0) {
			channel.sendMessage("❌ | Você precisa definir uma imagem.").queue();
			return;
		}

		try {
			BufferedImage bi = ImageIO.read(Helper.getImage(args[0]));

			String w;
			if (bi.getWidth() >= 500) w = "EXCELENTE";
			else if (bi.getWidth() >= 400) w = "BOA";
			else if (bi.getWidth() >= 300) w = "RUIM";
			else w = "HORRÍVEL";

			String h;
			if (bi.getHeight() >= 220) h = "EXCELENTE";
			else if (bi.getHeight() >= 200) h = "BOA";
			else if (bi.getHeight() >= 180) h = "RUIM";
			else h = "HORRÍVEL";
			String s = "Propoções: " + bi.getWidth() + "x" + bi.getHeight() + "\nEssa GIF possui uma qualidade `" + w + "`x`" + h + "`!";

			channel.sendMessage(s).queue();
		} catch (IOException e) {
			channel.sendMessage("❌ | O link da imagem não me parece correto.").queue();
		} catch (NullPointerException npe) {
			channel.sendMessage("❌ | Houve um erro ao recuperar dados da imagem (O site Tenor costuma retornar este erro).").queue();
		}
	}
}
