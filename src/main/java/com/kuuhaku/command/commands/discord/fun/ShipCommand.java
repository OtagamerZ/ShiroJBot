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
import com.kuuhaku.model.common.ColorlessEmbedBuilder;
import com.kuuhaku.model.enums.I18n;
import com.kuuhaku.utils.Helper;
import com.kuuhaku.utils.ShiroInfo;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.MessageBuilder;
import net.dv8tion.jda.api.entities.*;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Random;

@Command(
		name = "ship",
		aliases = {"shippar"},
		usage = "req_two-mentions",
		category = Category.FUN
)
public class ShipCommand implements Executable {

	@Override
	public void execute(User author, Member member, String command, String argsAsText, String[] args, Message message, TextChannel channel, Guild guild, String prefix) {
		if (message.getMentionedUsers().size() < 2) {
			channel.sendMessage(ShiroInfo.getLocale(I18n.PT).getString("err_two-mention-required")).queue();
			return;
		}

		try {
			StringBuilder sb = new StringBuilder();
			String[] meter = {"-", "-", "-", "-", "-", "-", "-", "-", "-", "-", "-", "-", "-", "-", "-", "-", "-", "-", "-", "-"};
			String doneMeter;
			BufferedImage bi = new BufferedImage(257, 128, BufferedImage.TYPE_INT_RGB);
			Graphics2D g2d = bi.createGraphics();
			float love = 100 * new Random(message.getMentionedUsers().get(0).getIdLong() + message.getMentionedUsers().get(1).getIdLong()).nextFloat();

			for (int i = 0; i < Math.round(love / 5); i++) {
				meter[i] = "▉";
			}

			doneMeter = Arrays.toString(meter).replace(",", "").replace(" ", "");

			g2d.drawImage(ImageIO.read(Helper.getImage(message.getMentionedUsers().get(0).getEffectiveAvatarUrl())), null, 0, 0);
			g2d.drawImage(ImageIO.read(Helper.getImage(message.getMentionedUsers().get(1).getEffectiveAvatarUrl())), null, 129, 0);

			g2d.dispose();

			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			ImageIO.write(clipRoundEdges(bi), "png", baos);

			String n1 = message.getMentionedUsers().get(0).getName();
			String n2 = message.getMentionedUsers().get(1).getName();

			sb.append(":heartpulse: ***Nível de love entre ").append(message.getMentionedUsers().get(0).getName()).append(" e ").append(message.getMentionedUsers().get(1).getName()).append(":***");
			sb.append("\n\nNome de casal: `").append(n1, 0, n1.length() / 2 + (n1.length() % 2)).append(n2.substring(n2.length() / 2 - (n1.length() % 2))).append("`");
			if (love <= 30)
				sb.append("\n\nBem, esse casal jamais daria certo, hora de passar pra frente!\n**").append(Helper.round(love, 1)).append("%** `").append(doneMeter).append("`");
			else if (love <= 50)
				sb.append("\n\nPode ate dar certo esse casal, mas vai precisar insistir!\n**").append(Helper.round(love, 1)).append("%** `").append(doneMeter).append("`");
			else if (love <= 70)
				sb.append("\n\nOpa, ou eles já se conhecem, ou o destino sorriu pra eles!\n**").append(Helper.round(love, 1)).append("%** `").append(doneMeter).append("`");
			else
				sb.append("\n\nImpossível casal mais perfeito que esse, tem que casar JÁ!!\n**").append(Helper.round(love, 1)).append("%** `").append(doneMeter).append("`");

			EmbedBuilder eb = new ColorlessEmbedBuilder();
			eb.setImage("attachment://ship.png");

			MessageBuilder mb = new MessageBuilder();
			mb.append(sb.toString());
			mb.setEmbed(eb.build());

			channel.sendMessage(mb.build()).addFile(baos.toByteArray(), "ship.png").queue();
		} catch (IOException e) {
			Helper.logger(this.getClass()).error(e + " | " + e.getStackTrace()[0]);
		}
	}

	private static BufferedImage clipRoundEdges(BufferedImage image) {
		BufferedImage bi = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_ARGB);
		Graphics2D g2d = bi.createGraphics();
		g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

		g2d.setClip(new RoundRectangle2D.Float(0, 0, bi.getWidth(), bi.getHeight(), 20, 20));
		g2d.drawImage(image, null, 0, 0);
		g2d.dispose();

		return bi;
	}
}
