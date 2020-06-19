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

package com.kuuhaku.command.commands.information;

import com.kuuhaku.command.Category;
import com.kuuhaku.command.Command;
import com.kuuhaku.controller.postgresql.KawaiponDAO;
import com.kuuhaku.model.common.KawaiponBook;
import com.kuuhaku.model.persistent.Kawaipon;
import com.kuuhaku.utils.Helper;
import com.kuuhaku.utils.I18n;
import com.kuuhaku.utils.ShiroInfo;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.*;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NonNls;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.List;

public class KawaiponsCommand extends Command {

	public KawaiponsCommand(String name, String description, Category category, boolean requiresMM) {
		super(name, description, category, requiresMM);
	}

	public KawaiponsCommand(@NonNls String name, @NonNls String[] aliases, String description, Category category, boolean requiresMM) {
		super(name, aliases, description, category, requiresMM);
	}

	public KawaiponsCommand(String name, String usage, String description, Category category, boolean requiresMM) {
		super(name, usage, description, category, requiresMM);
	}

	public KawaiponsCommand(@NonNls String name, @NonNls String[] aliases, String usage, String description, Category category, boolean requiresMM) {
		super(name, aliases, usage, description, category, requiresMM);
	}

	@Override
	public void execute(User author, Member member, String rawCmd, String[] args, Message message, MessageChannel channel, Guild guild, String prefix) {
		channel.sendMessage(ShiroInfo.getLocale(I18n.PT).getString("str_generating-collection")).queue(m -> {
			try {
				Kawaipon kp = KawaiponDAO.getKawaipon(author.getId());

				if (kp == null) {
					m.editMessage(":x: | Você ainda não coletou nenhum Kawaipon.").queue();
					return;
				}

				KawaiponBook kb = new KawaiponBook(kp.getCards());
				List<BufferedImage> cards = kb.view();
				int page;
				if (args.length < 1) page = 0;
				else {
					if (!StringUtils.isNumeric(args[0])) {
						channel.sendMessage(":x: | A página precisa ser um valor inteiro.").queue();
						return;
					}

					page = Integer.parseInt(args[0]) - 1;
				}

				if (page >= cards.size() || page < 0) {
					m.editMessage(":x: | A página precisa ser um valor entre 1 e " + cards.size() + ".").queue();
					return;
				}

				EmbedBuilder eb = new EmbedBuilder();

				eb.setTitle("\uD83C\uDFB4 | Kawaipons de " + author.getName() + " (página " + page + ")");
				eb.setImage("attachment://page.jpg");
				eb.setFooter("Total de Kawaipons: " + kp.getCards().size());

				m.delete().queue();
				channel.sendMessage(eb.build()).addFile(Helper.getBytes(cards.get(page)), "page.jpg").queue();
			} catch (IOException e) {
				m.editMessage(ShiroInfo.getLocale(I18n.PT).getString("err_collection-generation-error")).queue();
				Helper.logger(this.getClass()).error(e + " | " + e.getStackTrace()[0]);
			}
		});
	}
}
