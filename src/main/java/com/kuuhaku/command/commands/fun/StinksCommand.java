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

package com.kuuhaku.command.commands.fun;

import com.kuuhaku.command.Category;
import com.kuuhaku.command.Command;
import com.kuuhaku.utils.Helper;
import net.dv8tion.jda.api.entities.*;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Objects;

public class StinksCommand extends Command {

	public StinksCommand() {
		super("stinks", new String[]{"notstks"}, "<texto>", "Gera um meme no formato \"Stinks\"", Category.FUN);
	}

	@Override
	public void execute(User author, Member member, String rawCmd, String[] args, Message message, MessageChannel channel, Guild guild, String prefix) {
		if (args.length < 1) {
			channel.sendMessage(":x: | Você tem que escrever a mensagem que deseja que apareca no meme.").queue();
			return;
		}

		try {
			String text = String.join(" ", args);
			BufferedImage bi = ImageIO.read(Objects.requireNonNull(this.getClass().getClassLoader().getResourceAsStream("stinks.jpg")));

			Graphics2D g2d = bi.createGraphics();
			g2d.setFont(new Font("Impact", Font.BOLD, 25));
			int h = text.contains("\n") ? text.split("\\r?\\n").length - 1 : g2d.getFontMetrics().stringWidth(text) / (bi.getWidth() - 50);

			BufferedImage canvas = new BufferedImage(bi.getWidth(), 32 * (h + 1) + bi.getHeight(), BufferedImage.TYPE_INT_RGB);
			g2d = canvas.createGraphics();
			g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

			g2d.setColor(Color.WHITE);
			g2d.fillRect(0, 0, canvas.getWidth(), canvas.getHeight());

			g2d.setColor(Color.BLACK);
			g2d.setFont(new Font("Impact", Font.BOLD, 25));
			Helper.drawString(g2d, text, 25, 30);
			g2d.drawImage(bi, 0, canvas.getHeight() - bi.getHeight(), null);

			g2d.dispose();

			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			ImageIO.write(canvas, "png", baos);

			channel.sendMessage("Aqui está seu meme " + author.getAsMention() + "!").addFile(baos.toByteArray(), "notstks.jpg").queue();
		} catch (IOException e) {
			Helper.logger(this.getClass()).error(e + " | " + e.getStackTrace()[0]);
		}
	}

}
