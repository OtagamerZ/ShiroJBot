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

package com.kuuhaku.command.commands.discord.exceed;

import com.kuuhaku.command.Category;
import com.kuuhaku.command.Command;
import com.kuuhaku.controller.postgresql.ExceedDAO;
import com.kuuhaku.model.common.Exceed;
import com.kuuhaku.model.common.Profile;
import com.kuuhaku.utils.ExceedEnums;
import com.kuuhaku.utils.Helper;
import com.kuuhaku.utils.I18n;
import com.kuuhaku.utils.ShiroInfo;
import net.dv8tion.jda.api.entities.*;
import org.jetbrains.annotations.NonNls;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static com.kuuhaku.model.common.Profile.*;

public class ExceedRankCommand extends Command {

	public ExceedRankCommand(String name, String description, Category category, boolean requiresMM) {
		super(name, description, category, requiresMM);
	}

	public ExceedRankCommand(@NonNls String name, @NonNls String[] aliases, String description, Category category, boolean requiresMM) {
		super(name, aliases, description, category, requiresMM);
	}

	public ExceedRankCommand(String name, String usage, String description, Category category, boolean requiresMM) {
		super(name, usage, description, category, requiresMM);
	}

	public ExceedRankCommand(String name, String[] aliases, String usage, String description, Category category, boolean requiresMM) {
		super(name, aliases, usage, description, category, requiresMM);
	}

	@Override
	public void execute(User author, Member member, String rawCmd, String[] args, Message message, MessageChannel channel, Guild guild, String prefix) {
		channel.sendMessage("<a:loading:697879726630502401> Gerando placares...").queue(m -> {
			if (ExceedDAO.getExceed(author.getId()).isBlank()) {
				m.editMessage(ShiroInfo.getLocale(I18n.PT).getString("err_exceed-rank-no-exceed")).queue();
				return;
			}

			try {
				BufferedImage bi = new BufferedImage(WIDTH, HEIGTH, BufferedImage.TYPE_INT_ARGB);
				List<Exceed> exceeds = new ArrayList<>();
				for (ExceedEnums ex : ExceedEnums.values()) {
					exceeds.add(ExceedDAO.getExceed(ex));
				}

				BufferedImage bg = Helper.scaleImage(ImageIO.read(Helper.getImage("http://snagfilms-a.akamaihd.net/08/bd/a9131d1c48089e81990bdeafc0c4/1426-lec3-1536x865.jpg")), WIDTH, HEIGTH);
				BufferedImage fg = ImageIO.read(Helper.getImage("https://i.imgur.com/eGLuRMb.png"));

				List<BufferedImage> bars = new ArrayList<>() {{
					add(Helper.scaleImage(ImageIO.read(Helper.getImage("https://i.imgur.com/1USPoLD.jpg")), 68, 350));
					add(Helper.scaleImage(ImageIO.read(Helper.getImage("https://i.imgur.com/HNX66NB.jpg")), 68, 350));
					add(Helper.scaleImage(ImageIO.read(Helper.getImage("https://i.imgur.com/uCtc2Jr.jpg")), 68, 350));
					add(Helper.scaleImage(ImageIO.read(Helper.getImage("https://i.imgur.com/otuZup3.jpg")), 68, 350));
					add(Helper.scaleImage(ImageIO.read(Helper.getImage("https://i.imgur.com/nDIQ4ln.jpg")), 68, 350));
					add(Helper.scaleImage(ImageIO.read(Helper.getImage("https://i.imgur.com/H725kN3.jpg")), 68, 350));
				}};

				List<String> names = new ArrayList<>() {{
					add(ExceedEnums.IMANITY.getName());
					add(ExceedEnums.SEIREN.getName());
					add(ExceedEnums.WEREBEAST.getName());
					add(ExceedEnums.ELF.getName());
					add(ExceedEnums.EXMACHINA.getName());
					add(ExceedEnums.FLUGEL.getName());
				}};

				Graphics2D g2d = (Graphics2D) bi.getGraphics();
				g2d.setFont(FONT.deriveFont(Font.PLAIN, 30));
				g2d.drawImage(bg, null, 0, 0);
				g2d.drawImage(fg, null, 0, 0);

				long total = exceeds.stream().mapToLong(Exceed::getExp).sum();

				for (int i = 5; i >= 0; i--) {
					int h = (int) (10 + ((exceeds.get(i).getExp() * 100 / total) * 90 / 100)) * 358 / 100 * 2;
					g2d.setBackground(Color.black);
					Profile.drawRotate(g2d, 186 + (113 * i), 577 - h, -45, names.get(i));
					g2d.setClip(new Rectangle2D.Float(152 + (113 * i), 230 + (358 - h), 68, h));
					g2d.drawImage(bars.get(i), null, 152 + (113 * i), 230);
					g2d.setClip(null);
				}

				g2d.dispose();

				ByteArrayOutputStream baos = new ByteArrayOutputStream();
				ImageIO.write(Profile.clipRoundEdges(bi), "png", baos);
				channel.sendFile(baos.toByteArray(), "ranking.png").queue(s -> s.delete().queueAfter(1, TimeUnit.MINUTES));
				m.delete().queue();
			} catch (Exception e) {
				m.editMessage(ShiroInfo.getLocale(I18n.PT).getString("err_exceed-rank")).queue();
				Helper.logger(this.getClass()).error(e + " | " + e.getStackTrace()[0]);
			}
		});
	}
}
