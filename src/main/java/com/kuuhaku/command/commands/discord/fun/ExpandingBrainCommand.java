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
import com.kuuhaku.model.common.Profile;
import com.kuuhaku.model.enums.I18n;
import com.kuuhaku.utils.Helper;
import com.kuuhaku.utils.ShiroInfo;
import net.dv8tion.jda.api.entities.*;
import org.jetbrains.annotations.NonNls;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Objects;

public class ExpandingBrainCommand implements Executable {

	public ExpandingBrainCommand(String name, String description, Category category, boolean requiresMM) {
		super(name, description, category, requiresMM);
	}

	public ExpandingBrainCommand(String name, String[] aliases, String description, Category category, boolean requiresMM) {
		super(name, aliases, description, category, requiresMM);
	}

	public ExpandingBrainCommand(String name, String usage, String description, Category category, boolean requiresMM) {
		super(name, usage, description, category, requiresMM);
	}

	public ExpandingBrainCommand(@NonNls String name, @NonNls String[] aliases, String usage, String description, Category category, boolean requiresMM) {
		super(name, aliases, usage, description, category, requiresMM);
	}

	@Override
	public void execute(User author, Member member, String command, String argsAsText, String[] args, Message message, TextChannel channel, Guild guild, String prefix) {

		if (args.length < 1) {
			channel.sendMessage(ShiroInfo.getLocale(I18n.PT).getString("err_meme-no-message")).queue();
			return;
		} else if (String.join(" ", args).split(";").length < 4) {
			channel.sendMessage(ShiroInfo.getLocale(I18n.PT).getString("err_meme-require-four")).queue();
			return;
		}

		try {
			BufferedImage bi = ImageIO.read(Objects.requireNonNull(this.getClass().getClassLoader().getResourceAsStream("memes/expandingbrain.png")));
			Graphics2D g2d = bi.createGraphics();
			g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

			g2d.setColor(Color.BLACK);
			g2d.setFont(new Font("Arial", Font.BOLD, 40));
			Profile.drawStringMultiLineNO(g2d, String.join(" ", args).split(";")[0], 390, 20, 40);
			Profile.drawStringMultiLineNO(g2d, String.join(" ", args).split(";")[1], 390, 20, 340);
			Profile.drawStringMultiLineNO(g2d, String.join(" ", args).split(";")[2], 390, 20, 650);
			Profile.drawStringMultiLineNO(g2d, String.join(" ", args).split(";")[3], 390, 20, 930);

			g2d.dispose();

			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			ImageIO.write(bi, "png", baos);

			channel.sendMessage("Aqui está seu meme " + author.getAsMention() + "!").addFile(baos.toByteArray(), "eb.jpg").queue();
		} catch (IOException e) {
			Helper.logger(this.getClass()).error(e + " | " + e.getStackTrace()[0]);
		}
	}

}
