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

package com.kuuhaku.command.commands.misc;

import com.kuuhaku.command.Category;
import com.kuuhaku.command.Command;
import net.dv8tion.jda.api.entities.*;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.jetbrains.annotations.NonNls;

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
	public void execute(User author, Member member, String rawCmd, String[] args, Message message, MessageChannel channel, Guild guild, String prefix) {
		if (args.length == 0) {
			channel.sendMessage(":x: | Você precisa informar um texto para converter em ASCII.").queue();
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
		} catch (IOException e) {
			channel.sendMessage(":x: | Ocorreu um erro ao contactar à API.").queue();
		}
	}
}