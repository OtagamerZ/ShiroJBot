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

package com.kuuhaku.command.commands.discord.information;

import com.kuuhaku.command.Category;
import com.kuuhaku.command.Executable;
import com.kuuhaku.model.annotations.Command;
import com.kuuhaku.model.annotations.Requires;
import com.kuuhaku.model.enums.I18n;
import com.kuuhaku.utils.helpers.FileHelper;
import com.kuuhaku.utils.helpers.MiscHelper;
import com.kuuhaku.utils.helpers.MathHelper;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.*;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.Locale;

@Command(
		name = "quecor",
		aliases = {"tcolor", "testcolor"},
		usage = "req_color",
		category = Category.INFO
)
@Requires({Permission.MESSAGE_ATTACH_FILES, Permission.MESSAGE_EMBED_LINKS})
public class ColorTesterCommand implements Executable {

	@Override
	public void execute(User author, Member member, String argsAsText, String[] args, Message message, TextChannel channel, Guild guild, String prefix) {
		if (args.length < 1 || !args[0].contains("#") || !MathHelper.between(args[0].length(), 7, 8)) {
			channel.sendMessage(I18n.getString("err_invalid-color")).queue();
			return;
		}

		try {
			String tone = args[0].toUpperCase(Locale.ROOT);

			BufferedImage bi = new BufferedImage(128, 128, BufferedImage.TYPE_INT_RGB);
			Graphics2D g2d = bi.createGraphics();
			g2d.setColor(Color.decode(tone));

			g2d.fillRect(0, 0, 128, 128);
			g2d.dispose();

			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			ImageIO.write(bi, "png", baos);

			File colors = FileHelper.getResourceAsFile(this.getClass(), "colors.txt");
			int line = FileHelper.findStringInFile(colors, tone, s -> s.split(",")[0]);
			String name = line > -1 ? FileHelper.getLineFromFile(colors, line) : null;
			Color color = Color.decode(args[0]);

			EmbedBuilder eb = new EmbedBuilder()
					.setColor(color)
					.setTitle(I18n.getString("str_color", name != null ? name.split(",")[1] : tone))
					.addField("Vermelho (R)", (color.getRed() * 100 / 255) + "% (" + color.getRed() + ")", true)
					.addField("Verde (G)", (color.getGreen() * 100 / 255) + "% (" + color.getRed() + ")", true)
					.addField("Azul (B)", (color.getBlue() * 100 / 255) + "% (" + color.getRed() + ")", true)
					.setThumbnail("attachment://color.png");

			channel.sendMessageEmbeds(eb.build()).addFile(baos.toByteArray(), "color.png").queue();

		} catch (NumberFormatException e) {
			channel.sendMessage(I18n.getString("err_invalid-color")).queue();
		} catch (IOException e) {
			MiscHelper.logger(this.getClass()).error(e + " | " + e.getStackTrace()[0]);
		}
	}
}
