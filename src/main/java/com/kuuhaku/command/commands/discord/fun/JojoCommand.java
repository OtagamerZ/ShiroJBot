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
import com.kuuhaku.model.common.Profile;
import com.kuuhaku.model.enums.I18n;
import com.kuuhaku.utils.Helper;
import com.kuuhaku.utils.ShiroInfo;
import net.dv8tion.jda.api.entities.*;
import org.apache.commons.lang3.StringUtils;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Command(
		name = "jojo",
		usage = "req_two-mentions-image",
		category = Category.FUN
)
public class JojoCommand implements Executable {

	@Override
	public void execute(User author, Member member, String command, String argsAsText, String[] args, Message message, TextChannel channel, Guild guild, String prefix) {
		if (args.length < 3) {
			channel.sendMessage(ShiroInfo.getLocale(I18n.PT).getString("err_meme-no-message")).queue();
			return;
		} else if (message.getMentionedUsers().size() < 2) {
			channel.sendMessage("❌ | Você precisa mencionar 2 usuários antes da mensagem.").queue();
			return;
		}

		List<User> usrs = new ArrayList<>(message.getMentionedUsers());
		String mention = "<@!{0}>";
		String oldMention = "<@{0}>";

		String msg = StringUtils.normalizeSpace(String.join(" ", args)
				.replace(MessageFormat.format(mention, usrs.get(0).getId()), "")
				.replace(MessageFormat.format(oldMention, usrs.get(0).getId()), "")
				.replace(MessageFormat.format(mention, usrs.get(1).getId()), "")
				.replace(MessageFormat.format(oldMention, usrs.get(1).getId()), "")
		);

		try {
			BufferedImage bi = ImageIO.read(Objects.requireNonNull(this.getClass().getClassLoader().getResourceAsStream("memes/jojo.jpg")));
			Graphics2D g2d = bi.createGraphics();
			g2d.setBackground(Color.black);
			g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

			g2d.setFont(new Font("Arial", Font.BOLD, 25));
			g2d.setColor(Color.WHITE);

			Profile.printCenteredString(usrs.get(0).getName(), 99, 128, 81, g2d);
			Profile.printCenteredString(usrs.get(0).getName(), 99, 331, 865, g2d);

			Profile.printCenteredString(usrs.get(1).getName(), 99, 331, 102, g2d);
			Profile.printCenteredString(usrs.get(1).getName(), 99, 516, 841, g2d);

			Profile.printCenteredString(author.getName(), 99, 496, 126, g2d);
			Profile.printCenteredString(author.getName(), 99, 312, 439, g2d);
			Profile.printCenteredString(author.getName(), 99, 91, 789, g2d);

			g2d.dispose();

			ByteArrayOutputStream baos = Helper.renderMeme(msg, bi);

			channel.sendMessage("Aqui está seu meme " + author.getAsMention() + "!").addFile(baos.toByteArray(), "tb.jpg").queue();
		} catch (IOException e) {
			Helper.logger(this.getClass()).error(e + " | " + e.getStackTrace()[0]);
		}
	}

}
