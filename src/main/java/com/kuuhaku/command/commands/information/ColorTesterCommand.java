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
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.*;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class ColorTesterCommand extends Command {

	public ColorTesterCommand(String name, String description, Category category) {
		super(name, description, category);
	}

	public ColorTesterCommand(String name, String[] aliases, String description, Category category) {
		super(name, aliases, description, category);
	}

	public ColorTesterCommand(String name, String usage, String description, Category category) {
		super(name, usage, description, category);
	}

	public ColorTesterCommand(String name, String[] aliases, String usage, String description, Category category) {
		super(name, aliases, usage, description, category);
	}

	@Override
	public void execute(User author, Member member, String rawCmd, String[] args, Message message, MessageChannel channel, Guild guild, String prefix) {
		if (args.length < 1) {
			channel.sendMessage(":x: | Você tem que especificar uma cor no seguinte formato: `#RRGGBB`").queue();
			return;
		}

		try {
			BufferedImage bi = new BufferedImage(128, 128, BufferedImage.TYPE_INT_RGB);
			Graphics2D g2d = bi.createGraphics();
			g2d.setColor(Color.decode(args[0]));

			g2d.fillRect(0, 0, 128, 128);
			g2d.dispose();

			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			ImageIO.write(bi, "png", baos);

			EmbedBuilder eb = new EmbedBuilder();
			eb.setColor(Color.decode(args[0]));
			eb.setTitle("Cor " + args[0]);
			eb.setThumbnail("attachment://color.png");

			channel.sendMessage(eb.build()).addFile(baos.toByteArray(), "color.png").queue();

		} catch (NumberFormatException e) {
			channel.sendMessage(":x: | Cor no formato incorreto, ela deve seguir o padrão hexadecimal (#RRGGBB).").queue();
		} catch (IOException e) {
			Helper.logger(this.getClass()).error(e + " | " + e.getStackTrace()[0]);
		}
	}
}
