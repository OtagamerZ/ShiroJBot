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

import com.github.ygimenez.method.Pages;
import com.kuuhaku.command.Category;
import com.kuuhaku.command.Command;
import com.kuuhaku.controller.sqlite.GuildDAO;
import com.kuuhaku.model.common.ColorlessEmbedBuilder;
import com.kuuhaku.model.enums.PrivilegeLevel;
import com.kuuhaku.model.persistent.GuildConfig;
import com.kuuhaku.utils.Helper;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.*;
import org.jetbrains.annotations.NonNls;
import org.json.JSONException;
import org.json.JSONObject;

import java.awt.*;
import java.util.Map;
import java.util.concurrent.TimeUnit;

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
			if (Helper.hasPermission(member, PrivilegeLevel.MOD) && args.length > 0 && Helper.equalsAny(args[0], "reset", "resetar")) {
				GuildConfig gc = GuildDAO.getGuildById(guild.getId());
				gc.setEmbedTemplate(null);
				GuildDAO.updateGuildSettings(gc);

				m.delete().queue(null, Helper::doNothing);
				channel.sendMessage(":white_check_mark: | Embed de servidor limpo com sucesso!").queue();
				return;
			}

			try {
				JSONObject json = new JSONObject(String.join(" ", args));

				EmbedBuilder eb;
				if (json.has("color")) eb = new EmbedBuilder();
				else eb = new ColorlessEmbedBuilder();

				if (json.has("title")) eb.setTitle(json.getString("title"));
				if (json.has("color")) eb.setColor(Color.decode(json.getString("color")));
				if (json.has("thumbnail")) eb.setThumbnail(json.getString("thumbnail"));
				if (json.has("image")) eb.setImage(json.getString("image"));
				if (json.has("body")) eb.setDescription(json.getString("body"));

				if (json.has("fields")) json.getJSONArray("fields").forEach(j -> {
					try {
						JSONObject jo = (JSONObject) j;
						eb.addField(jo.getString("name"), jo.getString("value"), true);
					} catch (Exception ignore) {
					}
				});

				if (json.has("footer")) eb.setFooter(json.getString("footer"), null);

				m.delete().queue(null, Helper::doNothing);
				if (Helper.hasPermission(member, PrivilegeLevel.MOD))
					channel.sendMessage("Embed construído com sucesso, deseja configurá-lo para ser o formato das mensagens de boas-vindas/adeus?")
							.embed(eb.build())
							.queue(s ->
									Pages.buttonize(s, Map.of(Helper.ACCEPT, (mb, ms) -> {
												GuildConfig gc = GuildDAO.getGuildById(guild.getId());
												gc.setEmbedTemplate(json);
												GuildDAO.updateGuildSettings(gc);

												s.delete().queue(null, Helper::doNothing);
												channel.sendMessage(":white_check_mark: | Embed de servidor definido com sucesso!").queue();
											}), true, 1, TimeUnit.MINUTES
											, u -> u.getId().equals(author.getId())
											, ms -> {
												ms.editMessage(eb.build()).queue();
											}
									)
							);
				else
					channel.sendMessage(eb.build()).queue();
			} catch (JSONException ex) {
				m.editMessage("❌ | JSON em formato inválido, recomendo utilizar este site para checar se está tudo correto: https://jsonlint.com/.").queue();
			} catch (Exception e) {
				m.editMessage("❌ | Erro ao construir embed, talvez você não tenha passado nenhum argumento.").queue();
			}
		});
	}
}
