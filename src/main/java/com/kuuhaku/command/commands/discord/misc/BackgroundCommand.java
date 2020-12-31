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
import com.kuuhaku.controller.postgresql.AccountDAO;
import com.kuuhaku.controller.sqlite.MemberDAO;
import com.kuuhaku.model.enums.I18n;
import com.kuuhaku.model.persistent.Account;
import com.kuuhaku.utils.Helper;
import com.kuuhaku.utils.ShiroInfo;
import net.dv8tion.jda.api.entities.*;
import org.jetbrains.annotations.NonNls;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;

public class BackgroundCommand extends Command {

	public BackgroundCommand(String name, String description, Category category, boolean requiresMM) {
		super(name, description, category, requiresMM);
	}

	public BackgroundCommand(String name, String[] aliases, String description, Category category, boolean requiresMM) {
		super(name, aliases, description, category, requiresMM);
	}

	public BackgroundCommand(String name, String usage, String description, Category category, boolean requiresMM) {
		super(name, usage, description, category, requiresMM);
	}

	public BackgroundCommand(@NonNls String name, @NonNls String[] aliases, String usage, String description, Category category, boolean requiresMM) {
		super(name, aliases, usage, description, category, requiresMM);
	}

	@Override
	public void execute(User author, Member member, String rawCmd, String[] args, Message message, MessageChannel channel, Guild guild, String prefix) {
		if (args.length == 0) {
			channel.sendMessage(ShiroInfo.getLocale(I18n.PT).getString("err_no-image")).queue();
			return;
		}

		try {
			HttpURLConnection con = (HttpURLConnection) new URL(args[0]).openConnection();
			con.setRequestProperty("User-Agent", "Mozilla/5.0");
			BufferedImage bi = ImageIO.read(con.getInputStream());
			con.disconnect();
			bi.flush();

			if (Helper.getFileType(args[0]).contains("gif") && (bi.getWidth() < 400 || bi.getHeight() < 254)) {
				channel.sendMessage("❌ | Fundos de perfil animados devem ter no mínimo 400px de largura e 254px de altura!").queue();
				return;
			}

			Account acc = AccountDAO.getAccount(author.getId());
			List<com.kuuhaku.model.persistent.Member> ms = MemberDAO.getMemberByMid(author.getId());
			acc.setBackground(args[0]);
			AccountDAO.saveAccount(acc);
			if (args[0].contains("discordapp"))
				channel.sendMessage(":warning: | Imagens que utilizam o CDN do Discord (postadas no Discord) correm o risco de serem apagadas com o tempo, mas de todo modo: Imagem de fundo trocada com sucesso!").queue();
			else channel.sendMessage("Imagem de fundo trocada com sucesso!").queue();
		} catch (IOException | NullPointerException e) {
			if (args[0].contains("google"))
				channel.sendMessage("❌ | Você pegou o link da **pesquisa do Google** bobo!").queue();
			else channel.sendMessage(ShiroInfo.getLocale(I18n.PT).getString("err_invalid-image")).queue();
		}
    }
}
