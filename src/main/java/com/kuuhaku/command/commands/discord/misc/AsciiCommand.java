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

import com.kuuhaku.command.Category;
import com.kuuhaku.command.Executable;
import com.kuuhaku.model.annotations.Command;
import com.kuuhaku.model.enums.I18n;
import com.kuuhaku.utils.Helper;
import net.dv8tion.jda.api.entities.*;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.Map;

@Command(
		name = "ascii",
		usage = "req_text-image",
		category = Category.MISC
)
public class AsciiCommand implements Executable {
	private final Map<Integer, Character> tones = Map.of(
			0, '⠂',
			20, '⠌',
			40, '⠕',
			60, '⠞',
			80, '⠟',
			100, '⠿'
	);

	@Override
	public void execute(User author, Member member, String argsAsText, String[] args, Message message, TextChannel channel, Guild guild, String prefix) {
		if (args.length == 0) {
			if (message.getAttachments().isEmpty() || !message.getAttachments().get(0).isImage()) {
				channel.sendMessage(I18n.getString("err_ascii-no-text-or-image")).queue();
				return;
			}

			try {
				channel.sendMessage(":warning: | O texto ASCII pode parecer deformado devido ao tamanho do seu ecrã!\n```\n" + asciify(ImageIO.read(Helper.getImage(message.getAttachments().get(0).getUrl()))) + "\n```").queue();
			} catch (IOException e) {
				Helper.logger(this.getClass()).error(e + " | " + e.getStackTrace()[0]);
				channel.sendMessage(I18n.getString("err_image-error")).queue();
			}

			return;
		}

		StringBuilder query = new StringBuilder();
		for (String arg : args) {
			query.append(arg).append("+ ");
			query = new StringBuilder(query.substring(0, query.length() - 1));
		}

		OkHttpClient caller = new OkHttpClient();
		Request request = new Request.Builder().url("https://artii.herokuapp.com/make?text=" + query).build();
		try {
			Response response = caller.newCall(request).execute();
			assert response.body() != null;
			channel.sendMessage(":warning: | O texto ASCII pode parecer deformado devido ao tamanho do seu ecrã!\n```\n" + response.body().string() + "\n```").queue();
		} catch (IOException | IllegalArgumentException e) {
			channel.sendMessage(I18n.getString("err_api-error")).queue();
		}
	}

	private String asciify(BufferedImage bi) {
		BufferedImage in = new BufferedImage(50, 25, BufferedImage.TYPE_INT_RGB);
		Graphics2D g2d = in.createGraphics();
		g2d.drawImage(bi, 0, 0, 50, 25, null);
		g2d.dispose();

		StringBuilder sb = new StringBuilder();
		for (int y = 0; y < in.getHeight(); y++) {
			for (int x = 0; x < in.getWidth(); x++) {
				sb.append(tones.get(Helper.roundTrunc(Helper.toLuma(in.getRGB(x, y)) / 255f * 100, 20)));
			}
			sb.append('\n');
		}

		return sb.toString();
	}
}