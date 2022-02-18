/*
 * This file is part of Shiro J Bot.
 * Copyright (C) 2019-2021  Yago Gimenez (KuuHaKu)
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
import com.kuuhaku.model.enums.StorageUnit;
import com.kuuhaku.model.persistent.guild.GuildConfig;
import com.kuuhaku.utils.Helper;
import com.kuuhaku.utils.ShiroInfo;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.*;
import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
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
	public void execute(User author, Member member, String argsAsText, String[] args, Message message, TextChannel channel, Guild guild, String prefix) {
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
				AutoEmbedBuilder eb = null;
				if (message.getAttachments().size() > 0) {
					Message.Attachment att = message.getAttachments().get(0);
					if (Helper.getOr(att.getFileExtension(), "").equals("txt") && StorageUnit.KB.convert(att.getSize(), StorageUnit.B) <= 16) {
						try (InputStream is = att.retrieveInputStream().get()) {
							eb = new AutoEmbedBuilder(IOUtils.toString(is, StandardCharsets.UTF_8));
						}
					} else {
						m.editMessage("❌ | O arquivo deve set do tipo `.txt` e ser menor que 16KB.").queue();
						return;
					}
				}

				if (eb == null) eb = new AutoEmbedBuilder(argsAsText);

				if (Helper.hasPermission(member, PrivilegeLevel.MOD)) {
					AutoEmbedBuilder finalEb = eb;
					channel.sendMessage("✅ | Embed construído com sucesso, deseja configurá-lo para ser o formato das mensagens de boas-vindas/adeus?")
							.setEmbeds(eb.build())
							.queue(s ->
									Pages.buttonize(s, Map.of(Helper.parseEmoji(Helper.ACCEPT), wrapper -> {
												GuildConfig gc = GuildDAO.getGuildById(guild.getId());
												gc.setEmbedTemplate(finalEb.getEmbed());
												GuildDAO.updateGuildSettings(gc);

												channel.sendMessage("✅ | Embed de servidor definido com sucesso!")
														.flatMap(r -> m.delete())
														.flatMap(r -> s.delete())
														.queue();
											}), ShiroInfo.USE_BUTTONS, true, 1, TimeUnit.MINUTES
											, u -> u.getId().equals(author.getId())
											, ms -> channel.sendMessageEmbeds(finalEb.build())
													.flatMap(r -> m.delete())
													.flatMap(r -> ms.delete())
													.queue(null, Helper::doNothing)
									), Helper::doNothing
							);
				}
				else
					channel.sendMessageEmbeds(eb.build()).flatMap(s -> m.delete()).queue();
			} catch (IOException | IllegalStateException ex) {
				m.editMessage("❌ | JSON em formato inválido, você pode usar minha ferramenta oficial para criar seus embeds em https://shirojbot.site/EmbedBuilder").queue();
			} catch (Exception e) {
				m.editMessage("❌ | Erro ao construir embed, talvez você não tenha passado nenhum argumento.").queue();
			}
		});
	}
}
