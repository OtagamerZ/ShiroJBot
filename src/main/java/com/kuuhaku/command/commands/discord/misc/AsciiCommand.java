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
import com.kuuhaku.command.Command;
import com.kuuhaku.model.enums.I18n;
import com.kuuhaku.utils.Helper;
import com.kuuhaku.utils.ShiroInfo;
import net.dv8tion.jda.api.entities.*;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.jetbrains.annotations.NonNls;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;

public class AsciiCommand extends Command {

	public AsciiCommand(String name, String description, Category category, boolean requiresMM) {
		super(name, description, category, requiresMM);
	}

	public AsciiCommand(String name, String[] aliases, String description, Category category, boolean requiresMM) {
		super(name, aliases, description, category, requiresMM);
	}

	public AsciiCommand(@NonNls String name, String usage, String description, Category category, boolean requiresMM) {
		super(name, usage, description, category, requiresMM);
	}

	public AsciiCommand(String name, String[] aliases, String usage, String description, Category category, boolean requiresMM) {
		super(name, aliases, usage, description, category, requiresMM);
	}

	@Override
	public void execute(User author, Member member, String command, String argsAsText, String[] args, Message message, MessageChannel channel, Guild guild, String prefix) {
		if (args.length == 0) {
			if (message.getAttachments().size() == 0 || !message.getAttachments().get(0).isImage()) {
				channel.sendMessage(ShiroInfo.getLocale(I18n.PT).getString("err_ascii-no-text-or-image")).queue();
				return;
			}

			try {
				channel.sendMessage(":warning: | O texto ASCII pode parecer deformado devido ao tamanho do seu ecrã!\n\n" + asciify(ImageIO.read(Helper.getImage(message.getAttachments().get(0).getUrl()))) + "").queue();
			} catch (IOException e) {
				Helper.logger(this.getClass()).error(e + " | " + e.getStackTrace()[0]);
				channel.sendMessage(ShiroInfo.getLocale(I18n.PT).getString("err_image-error")).queue();
			}

			return;
		}

		StringBuilder query = new StringBuilder();
		for (String arg : args) {
			query.append(arg).append("+ ");
			query = new StringBuilder(query.substring(0, query.length() - 1));
		}

		OkHttpClient caller = new OkHttpClient();
		Request request = new Request.Builder().url("http://artii.herokuapp.com/make?text=" + query).build();
		try {
			Response response = caller.newCall(request).execute();
			assert response.body() != null;
			channel.sendMessage(":warning: | O texto ASCII pode parecer deformado devido ao tamanho do seu ecrã!\n```\n" + response.body().string() + "\n```").queue();
		} catch (IOException | IllegalArgumentException e) {
			channel.sendMessage(ShiroInfo.getLocale(I18n.PT).getString("err_api-error")).queue();
		}
	}

	private String asciify(BufferedImage bi) {
		final char base = '\u2800';

		BufferedImage in = new BufferedImage(100, 100, BufferedImage.TYPE_INT_RGB);
		Graphics2D g2d = in.createGraphics();
		g2d.drawImage(bi, 0, 0, 100, 100, null);
		g2d.dispose();

		int threshold = getGrayScale(in.getRGB(0, 0));
		for (int y = 0; y < in.getHeight(); y++) {
			for (int x = 0; x < in.getWidth(); x++) {
				if (in.getRGB(x, y) == Color.BLACK.getRGB() || in.getRGB(x, y) == Color.WHITE.getRGB()) continue;
				threshold = (threshold + getGrayScale(in.getRGB(x, y))) / 2;
			}
		}

		StringBuilder sb = new StringBuilder();
		for (int y = 0; y < in.getHeight(); y += 4) {
			for (int x = 0; x < in.getWidth(); x += 2) {
				int bytes = 0;

				if (getGrayScale(in.getRGB(x, y)) >= threshold) bytes += 1;
				if (getGrayScale(in.getRGB(x, y + 1)) >= threshold) bytes += 2;
				if (getGrayScale(in.getRGB(x, y + 2)) >= threshold) bytes += 4;

				if (getGrayScale(in.getRGB(x + 1, y)) >= threshold) bytes += 8;
				if (getGrayScale(in.getRGB(x + 1, y + 1)) >= threshold) bytes += 16;
				if (getGrayScale(in.getRGB(x + 1, y + 2)) >= threshold) bytes += 32;

				if (getGrayScale(in.getRGB(x, y + 3)) >= threshold) bytes += 64;
				if (getGrayScale(in.getRGB(x + 1, y + 3)) >= threshold) bytes += 128;

				if (bytes > 0) sb.append(Character.toString(base + bytes));
				else sb.append("⡀");
			}
			sb.append('\n');
		}

		return sb.toString();
	}

	public int getGrayScale(int rgb) {
		int r = (rgb >> 16) & 0xff;
		int g = (rgb >> 8) & 0xff;
		int b = (rgb) & 0xff;

		return (int) (0.2126 * r + 0.7152 * g + 0.0722 * b);
	}
}