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
			mbs = SQLite.getMemberRank(guild.getId(), false).subList(0, 6);
		} else if (args[0].equalsIgnoreCase("global")) {
			mbs = SQLite.getMemberRank(guild.getId(), true).subList(0, 6);
		} else {
			channel.sendMessage(":x: | O único parâmetro permitido após o comando é `global`.").queue();
			return;
		}

		try {
			BufferedImage img = new BufferedImage(500, 600, BufferedImage.TYPE_INT_ARGB);
			Graphics2D g2d = img.createGraphics();

			HttpURLConnection con = (HttpURLConnection) new URL(Main.getInfo().getUserByID(mbs.get(0).getId().substring(mbs.get(0).getId().length() / 2 - 1)).getAvatarUrl()).openConnection();
			con.setRequestProperty("User-Agent", "Mozilla/5.0");
			g2d.drawImage(Profile.scaleImage(ImageIO.read(con.getInputStream()), 500, 150), null, 0, 0);

			for (int i = 1; i < mbs.size(); i++) {
				con = (HttpURLConnection) new URL(Main.getInfo().getUserByID(mbs.get(i).getId().substring(0, 17)).getAvatarUrl()).openConnection();
				con.setRequestProperty("User-Agent", "Mozilla/5.0");
				g2d.drawImage(Profile.scaleImage(ImageIO.read(con.getInputStream()), 500, 75), null, 0, 150 + 75 * (i - 1));
			}

			g2d.dispose();
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			ImageIO.write(img, "png", baos);

			channel.sendFile(baos.toByteArray(), "rank.png").queue();
		} catch (IOException ignore) {
		}
	}
}
