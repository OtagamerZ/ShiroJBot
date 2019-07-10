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

package com.kuuhaku.command.commands.information;

import com.kuuhaku.command.Category;
import com.kuuhaku.command.Command;
import com.kuuhaku.controller.SQLite;
import net.dv8tion.jda.core.entities.*;
import net.dv8tion.jda.core.events.Event;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

public class ValidateGIFCommand extends Command {

	public ValidateGIFCommand() {
		super("validate", new String[]{"testgif", "tgif"}, "<link>", "Testa se as dimensões da GIF são recomendadas para o uso em reações.", Category.MISC);
	}

	@Override
	public void execute(User author, Member member, String rawCmd, String[] args, Message message, MessageChannel channel, Guild guild, Event event, String prefix) {
		if (args.length == 0) {
			channel.sendMessage(":x: | Você precisa definir uma imagem.").queue();
			return;
		}

		try {
			HttpURLConnection con = (HttpURLConnection) new URL(args[0]).openConnection();
			con.setRequestProperty("User-Agent", "Mozilla/5.0");
			BufferedImage bi = ImageIO.read(con.getInputStream());
			con.disconnect();
			bi.flush();

			String q;
			if (bi.getHeight() >= 280 && bi.getWidth() >= 500) q = "EXCELENTE";
			else if (bi.getHeight() >= 230 && bi.getWidth() >= 400) q = "BOA";
			else if (bi.getHeight() >= 180 && bi.getWidth() >= 300) q = "RUIM";
			else q = "HORRÍVEL";
			String s = "Propoções: " + bi.getWidth() + "x" + bi.getHeight() + "\nEssa GIF possui uma qualidade **" + q + "**!";

			channel.sendMessage(s).queue();
		} catch (IOException e) {
			channel.sendMessage(":x: | O link da imagem não me parece correto.").queue();
		}
	}
}
