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

package com.kuuhaku.command.commands.discord.moderation;

import com.kuuhaku.command.Category;
import com.kuuhaku.command.Executable;
import com.kuuhaku.model.annotations.Command;
import com.kuuhaku.model.annotations.Requires;
import com.kuuhaku.utils.Helper;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.requests.RestAction;
import net.dv8tion.jda.api.requests.restaction.AuditableRestAction;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

@Command(
		name = "adicionaremote",
		aliases = {"adicionaremoji", "addemote", "addemoji"},
		usage = "req_emotes-name",
		category = Category.MODERATION
)
@Requires({Permission.MANAGE_EMOTES})
public class AddEmoteCommand implements Executable {

	@Override
	public void execute(User author, Member member, String command, String argsAsText, String[] args, Message message, TextChannel channel, Guild guild, String prefix) {
		Message.Attachment att = message.getAttachments().stream().filter(Message.Attachment::isImage).findFirst().orElse(null);

		if (message.getEmotes().isEmpty() && (args.length < 1 || att == null)) {
			channel.sendMessage("❌ | Você precisa informar ao menos 1 emote para adicionar.").queue();
			return;
		} else if ((guild.getEmotes().size() + message.getEmotes().size() > guild.getMaxEmotes() || guild.getEmotes().size() + 1 > guild.getMaxEmotes())) {
			channel.sendMessage("❌ | O servidor não tem espaço suficiente para emotes.").queue();
			return;
		}

		if (message.getEmotes().isEmpty()) {
			if (args[0].length() < 2) {
				channel.sendMessage("❌ | Emotes devem ter no mínimo 2 caracteres no nome.").queue();
				return;
			} else if (att == null) {
				channel.sendMessage("❌ | Você deve enviar uma imagem para o Emote.").queue();
				return;
			}

			String ext = att.getFileExtension();
			if (ext == null) {
				channel.sendMessage("❌ | Arquivo com extensão inválida.").queue();
				return;
			}

			try (InputStream is = Helper.getImage(att.getUrl())) {
				byte[] bytes = is.readAllBytes();
				if (bytes.length > 256000) {
					channel.sendMessage("❌ | O Discord só permite arquivos de até 256kb.").queue();
					return;
				}

				Icon.IconType type = Icon.IconType.fromExtension(ext);

				guild.createEmote(args[0], Icon.from(bytes, type))
						.flatMap(s -> channel.sendMessage("✅ | Emote adicionado com sucesso!"))
						.queue(null, Helper::doNothing);
			} catch (IOException e) {
				channel.sendMessage("❌ | Não foi possível obter a imagem.").queue();
			}
		} else {
			List<AuditableRestAction<Emote>> acts = new ArrayList<>();
			int added = 0;
			for (Emote emote : message.getEmotes()) {
				try {
					if (guild.getEmotes().size() + added >= guild.getMaxEmotes()) break;
					acts.add(guild.createEmote(emote.getName(), Icon.from(Helper.getImage(emote.getImageUrl()))));
					added++;
				} catch (IOException ignore) {
				}
			}

			int finalAdded = added;
			RestAction.allOf(acts)
					.mapToResult()
					.flatMap(s -> channel.sendMessage("✅ | " + finalAdded + " emotes adicionados com sucesso!"))
					.queue(null, Helper::doNothing);
		}
	}
}
