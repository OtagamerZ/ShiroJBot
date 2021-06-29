/*
 * This file is part of Shiro J Bot.
 * Copyright (C) 2021  Yago Gimenez (KuuHaKu)
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
import com.kuuhaku.model.common.AutoEmbedBuilder;
import com.kuuhaku.model.enums.PrivilegeLevel;
import com.kuuhaku.model.persistent.guild.GuildConfig;
import com.kuuhaku.utils.Helper;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.*;

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
				gc.setEmbedTemplate(null);
				GuildDAO.updateGuildSettings(gc);

				m.delete().queue(null, Helper::doNothing);
				channel.sendMessage("✅ | Embed de servidor limpo com sucesso!").queue();
				return;
			}

			try {
				AutoEmbedBuilder eb = new AutoEmbedBuilder(argsAsText);

				if (Helper.hasPermission(member, PrivilegeLevel.MOD))
					channel.sendMessage("✅ | Embed construído com sucesso, deseja configurá-lo para ser o formato das mensagens de boas-vindas/adeus?")
							.embed(eb.build())
							.queue(s ->
									Pages.buttonize(s, Map.of(Helper.ACCEPT, (mb, ms) -> {
												GuildConfig gc = GuildDAO.getGuildById(guild.getId());
												gc.setEmbedTemplate(eb.getEmbed());
												GuildDAO.updateGuildSettings(gc);

												channel.sendMessage("✅ | Embed de servidor definido com sucesso!")
														.flatMap(r -> m.delete())
														.flatMap(r -> s.delete())
														.queue();
											}), true, 1, TimeUnit.MINUTES
											, u -> u.getId().equals(author.getId())
											, ms -> channel.sendMessage(eb.build())
													.flatMap(r -> m.delete())
													.flatMap(r -> ms.delete())
													.queue()
									), Helper::doNothing
							);
				else
					channel.sendMessage(eb.build()).flatMap(s -> m.delete()).queue();
			} catch (IllegalStateException ex) {
				m.editMessage("❌ | JSON em formato inválido, um exemplo da estrutura do embed pode ser encontrado em https://api.shirojbot.site/embedjson").queue();
			} catch (Exception e) {
				m.editMessage("❌ | Erro ao construir embed, talvez você não tenha passado nenhum argumento.").queue();
			}
		});
	}
}
