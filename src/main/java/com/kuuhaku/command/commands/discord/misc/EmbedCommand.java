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
import com.kuuhaku.command.Executable;
import com.kuuhaku.controller.postgresql.GuildDAO;
import com.kuuhaku.model.annotations.Command;
import com.kuuhaku.model.annotations.Requires;
import com.kuuhaku.model.common.ColorlessEmbedBuilder;
import com.kuuhaku.model.enums.PrivilegeLevel;
import com.kuuhaku.model.persistent.guild.GuildConfig;
import com.kuuhaku.utils.Helper;
import com.kuuhaku.utils.JSONObject;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.*;
import org.json.JSONException;

import java.awt.*;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Command(
		name = "embed",
		usage = "req_json",
		category = Category.MISC
)
@Requires({Permission.MESSAGE_EMBED_LINKS})
public class EmbedCommand implements Executable {

	@Override
	public void execute(User author, Member member, String command, String argsAsText, String[] args, Message message, TextChannel channel, Guild guild, String prefix) {
		channel.sendMessage("<a:loading:697879726630502401> Construindo embed...").queue(m -> {
			if (Helper.hasPermission(member, PrivilegeLevel.MOD) && args.length > 0 && Helper.equalsAny(args[0], "reset", "resetar")) {
				GuildConfig gc = GuildDAO.getGuildById(guild.getId());
				gc.setEmbedTemplate(new JSONObject());
				GuildDAO.updateGuildSettings(gc);

				m.delete().queue(null, Helper::doNothing);
				channel.sendMessage("✅ | Embed de servidor limpo com sucesso!").queue();
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
				eb.setDescription(json.getString("body", Helper.VOID));

				if (json.has("fields")) {
					for (Object j : json.getJSONArray("fields")) {
						try {
							JSONObject jo = (JSONObject) j;
							eb.addField(jo.getString("name"), jo.getString("value"), true);
						} catch (Exception ignore) {
						}
					}
				}

				if (json.has("footer")) eb.setFooter(json.getString("footer"), null);

				m.delete().queue(null, Helper::doNothing);
				if (Helper.hasPermission(member, PrivilegeLevel.MOD))
					channel.sendMessage("✅ | Embed construído com sucesso, deseja configurá-lo para ser o formato das mensagens de boas-vindas/adeus?")
							.embed(eb.build())
							.queue(s ->
									Pages.buttonize(s, Map.of(Helper.ACCEPT, (mb, ms) -> {
												GuildConfig gc = GuildDAO.getGuildById(guild.getId());
												gc.setEmbedTemplate(json);
												GuildDAO.updateGuildSettings(gc);

												s.delete().queue(null, Helper::doNothing);
												channel.sendMessage("✅ | Embed de servidor definido com sucesso!").queue();
											}), true, 1, TimeUnit.MINUTES
											, u -> u.getId().equals(author.getId())
											, ms -> {
												channel.sendMessage(eb.build()).queue();
												ms.delete().queue();
											}
									), Helper::doNothing
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
