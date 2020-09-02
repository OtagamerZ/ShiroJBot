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
import com.kuuhaku.model.common.ColorlessEmbedBuilder;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.*;
import org.jetbrains.annotations.NonNls;
import org.json.JSONException;
import org.json.JSONObject;

import java.awt.*;

public class EmbedCommand extends Command {

	public EmbedCommand(String name, String description, Category category, boolean requiresMM) {
		super(name, description, category, requiresMM);
	}

	public EmbedCommand(String name, String[] aliases, String description, Category category, boolean requiresMM) {
		super(name, aliases, description, category, requiresMM);
	}

	public EmbedCommand(@NonNls String name, String usage, String description, Category category, boolean requiresMM) {
		super(name, usage, description, category, requiresMM);
	}

	public EmbedCommand(String name, String[] aliases, String usage, String description, Category category, boolean requiresMM) {
		super(name, aliases, usage, description, category, requiresMM);
	}

	@Override
	public void execute(User author, Member member, String rawCmd, String[] args, Message message, MessageChannel channel, Guild guild, String prefix) {
		channel.sendMessage("<a:loading:697879726630502401> Construindo embed...").queue(m -> {
			try {
				JSONObject json = new JSONObject(String.join(" ", args));

				EmbedBuilder eb = new ColorlessEmbedBuilder();

				if (json.has("title")) eb.setTitle(json.getString("title"));
				if (json.has("color")) eb.setColor(Color.decode(json.getString("color")));
				if (json.has("thumbnail")) eb.setThumbnail(json.getString("thumbnail"));
				if (json.has("body")) eb.setDescription(json.getString("body"));

				if (json.has("fields")) json.getJSONArray("fields").forEach(j -> {
					try {
						JSONObject jo = (JSONObject) j;
						eb.addField(jo.getString("name"), jo.getString("value"), true);
					} catch (Exception ignore) {
					}
				});

				if (json.has("footer")) eb.setFooter(json.getString("footer"), null);

				m.delete().queue();
				channel.sendMessage(eb.build()).queue();
			} catch (JSONException ex) {
				m.editMessage("❌ | JSON em formato inválido, recomendo utilizar este site para checar se está tudo correto: https://jsonlint.com/.").queue();
			} catch (Exception e) {
				m.editMessage("❌ | Erro ao construir embed, talvez você não tenha passado nenhum argumento.").queue();
			}
		});
	}
}
