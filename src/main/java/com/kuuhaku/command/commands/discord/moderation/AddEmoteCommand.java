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

import com.kuuhaku.Main;
import com.kuuhaku.command.Category;
import com.kuuhaku.command.Executable;
import com.kuuhaku.model.annotations.Command;
import com.kuuhaku.model.annotations.Requires;
import com.kuuhaku.model.enums.StorageUnit;
import com.kuuhaku.utils.Helper;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.requests.RestAction;
import net.dv8tion.jda.api.requests.restaction.AuditableRestAction;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Command(
		name = "adicionaremote",
		aliases = {"adicionaremoji", "addemote", "addemoji"},
		usage = "req_emotes-name-image-roles",
		category = Category.MODERATION
)
@Requires({Permission.MANAGE_EMOTES})
public class AddEmoteCommand implements Executable {

	@Override
	public void execute(User author, Member member, String command, String argsAsText, String[] args, Message message, TextChannel channel, Guild guild, String prefix) {
		Message.Attachment att = message.getAttachments().stream().filter(Message.Attachment::isImage).findFirst().orElse(null);
		long currAnim = guild.getEmotes().stream().filter(Emote::isAnimated).count();
		long currNormal = guild.getEmotes().size() - currAnim;

		if (message.getEmotes().isEmpty() && args.length < 1) {
			channel.sendMessage("❌ | Você precisa informar ao menos 1 emote para adicionar.").queue();
			return;
		}

		Set<Emote> emts = new HashSet<>();
		for (String arg : args) {
			if (Helper.regex(arg, "\\{a?&\\w+&\\d{10,}}")) {
				Emote e = Main.getShiroShards().getEmoteById(Helper.getOr(Helper.extract(arg, "\\{a?&\\w+&(\\d+)}", 1), "1"));
				if (e == null) {
					channel.sendMessage("❌ | Emote `" + arg + "` não encontrado, verifique se digitou a menção correta no formato usado por mim no `" + prefix + "semotes`.").queue();
					return;
				}
				emts.add(e);
			}
		}

		if (message.getEmotes().isEmpty() && emts.isEmpty()) {
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
				if (bytes.length > StorageUnit.B.convert(256, StorageUnit.KB)) {
					channel.sendMessage("❌ | O Discord só permite emotes de até 256kb.").queue();
					return;
				}

				Icon.IconType type = Icon.IconType.fromExtension(ext);

				switch (type) {
					case JPEG, PNG, WEBP -> {
						if (currNormal + 1 > guild.getMaxEmotes()) {
							channel.sendMessage("❌ | O servidor não tem espaço suficiente para emotes.").queue();
							return;
						}
					}
					case GIF -> {
						if (currAnim + 1 > guild.getMaxEmotes()) {
							channel.sendMessage("❌ | O servidor não tem espaço suficiente para emotes.").queue();
							return;
						}
					}
					case UNKNOWN -> {
						channel.sendMessage("❌ | Arquivo com extensão desconhecida.").queue();
						return;
					}
				}

				String msg;
				if (message.getMentionedRoles().size() > 0) {
					msg = "✅ | Emote adicionado com sucesso para os cargos " + message.getMentionedRoles().stream().map(r -> "`" + r.getName() + "`").collect(Collectors.collectingAndThen(Collectors.toList(), Helper.properlyJoin())) + "!";
				} else {
					msg = "✅ | Emote adicionado com sucesso!";
				}

				guild.createEmote(args[0], Icon.from(bytes, type), message.getMentionedRoles().toArray(new Role[0]))
						.flatMap(s -> channel.sendMessage(msg))
						.queue(null, Helper::doNothing);
			} catch (IOException ex) {
				channel.sendMessage("❌ | Não foi possível obter a imagem.").queue();
			}
		} else {
			List<AuditableRestAction<Emote>> acts = new ArrayList<>();

			Set<Emote> toadd = new HashSet<>() {{
				addAll(emts);
				addAll(message.getEmotes());
			}};

			long anim = toadd.stream().filter(Emote::isAnimated).count();
			long normal = toadd.size() - anim;

			if (currAnim + anim > guild.getMaxEmotes() || currNormal + normal > guild.getMaxEmotes()) {
				channel.sendMessage("❌ | O servidor não tem espaço suficiente para emotes.").queue();
				return;
			}

			int added = 0;
			for (Emote emote : toadd) {
				try {
					acts.add(guild.createEmote(emote.getName(), Icon.from(Helper.getImage(emote.getImageUrl())), message.getMentionedRoles().toArray(new Role[0])));
					added++;
				} catch (IOException ignore) {
				}
			}

			String msg;
			if (message.getMentionedRoles().size() > 0) {
				msg = "✅ | " + added + " emotes adicionado com sucesso para os cargos " + message.getMentionedRoles().stream().map(r -> "`" + r.getName() + "`").collect(Collectors.joining(", ")) + "!";
			} else {
				msg = "✅ | " + added + " emotes adicionado com sucesso!";
			}

			RestAction.allOf(acts)
					.mapToResult()
					.flatMap(s -> channel.sendMessage(msg))
					.queue(null, Helper::doNothing);
		}
	}
}
