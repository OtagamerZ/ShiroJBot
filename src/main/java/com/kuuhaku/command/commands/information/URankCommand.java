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

import com.kuuhaku.Main;
import com.kuuhaku.command.Category;
import com.kuuhaku.command.Command;
import com.kuuhaku.controller.SQLite;
import com.kuuhaku.model.Profile;
import net.dv8tion.jda.core.entities.*;
import net.dv8tion.jda.core.events.Event;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class URankCommand extends Command {

	public URankCommand() {
		super("rank", new String[]{"ranking", "top7"}, "<global>", "Mostra o ranking de usuários do servidor ou global.", Category.MISC);
	}

	@Override
	public void execute(User author, Member member, String rawCmd, String[] args, Message message, MessageChannel channel, Guild guild, Event event, String prefix) {
		List<com.kuuhaku.model.Member> mbs;

		if (args.length == 0) {
			mbs = SQLite.getMemberRank(guild.getId(), false);
			if (mbs.size() > 7) mbs.subList(7, mbs.size()).clear();
		} else if (args[0].equalsIgnoreCase("global")) {
			mbs = SQLite.getMemberRank(guild.getId(), true);
			if (mbs.size() > 7) mbs.subList(7, mbs.size()).clear();
		} else {
			channel.sendMessage(":x: | O único parâmetro permitido após o comando é `global`.").queue();
			return;
		}

		try {
			BufferedImage img = new BufferedImage(500, 600, BufferedImage.TYPE_INT_RGB);
			Graphics2D g2d = img.createGraphics();
			g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
			g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			g2d.setColor(Color.BLACK);

			HttpURLConnection con = (HttpURLConnection) new URL(Main.getInfo().getUserByID(mbs.get(0).getMid()).getAvatarUrl()).openConnection();
			con.setRequestProperty("User-Agent", "Mozilla/5.0");
			BufferedImage avatar = Profile.scaleImage(ImageIO.read(con.getInputStream()), 500, 150);
			g2d.drawImage(avatar.getSubimage(0, avatar.getHeight() / 4, avatar.getWidth(), 150), null, 0, 0);

			for (int i = 1; i < mbs.size(); i++) {
				con = (HttpURLConnection) new URL(Main.getInfo().getUserByID(mbs.get(i).getMid()).getAvatarUrl()).openConnection();
				con.setRequestProperty("User-Agent", "Mozilla/5.0");
				avatar = Profile.scaleImage(ImageIO.read(con.getInputStream()), 500, 75);
				g2d.drawImage(avatar.getSubimage(0, avatar.getHeight() / 4, avatar.getWidth(),  75), null, 0, 150 + 75 * (i - 1));
				g2d.fillRect(150 + 75 * i, 0, 500, 600);
			}

			g2d.dispose();
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			ImageIO.write(img, "png", baos);

			channel.sendFile(baos.toByteArray(), "rank.png").queue();
		} catch (IOException ignore) {
		}
	}
}
