/*
 * This file is part of Shiro J Bot.
 * Copyright (C) 2019-2023  Yago Gimenez (KuuHaKu)
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

package com.kuuhaku.command.moderation;

import com.kuuhaku.interfaces.Executable;
import com.kuuhaku.interfaces.annotations.Command;
import com.kuuhaku.interfaces.annotations.Requires;
import com.kuuhaku.interfaces.annotations.Syntax;
import com.kuuhaku.model.enums.Category;
import com.kuuhaku.model.enums.I18N;
import com.kuuhaku.model.records.EventData;
import com.kuuhaku.model.records.MessageData;
import com.ygimenez.json.JSONObject;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Icon;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.emoji.CustomEmoji;
import net.dv8tion.jda.api.entities.emoji.RichCustomEmoji;
import net.dv8tion.jda.api.requests.RestAction;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Command(
		name = "emote",
		path = "add",
		category = Category.MODERATION
)
@Syntax({
		"<name:word:r>",
		"<emotes:text:r>"
})
@Requires(Permission.MANAGE_GUILD_EXPRESSIONS)
public class EmoteAddCommand implements Executable {
	@Override
	public void execute(JDA bot, I18N locale, EventData data, MessageData.Guild event, JSONObject args) {
		List<CustomEmoji> emotes = new ArrayList<>(event.message().getMentions().getCustomEmojis());

		List<RestAction<RichCustomEmoji>> acts = new ArrayList<>();
		if (args.has("name")) {
			List<RichCustomEmoji> current = event.guild().getEmojis();
			int animated = (int) current.stream().filter(CustomEmoji::isAnimated).count();
			int normal = current.size() - animated;

			if (animated + 1 > event.guild().getMaxEmojis() || normal + 1 > event.guild().getMaxEmojis()) {
				event.channel().sendMessage(locale.get("error/no_emote_space")).queue();
				return;
			}

			if (!event.message().getAttachments().isEmpty()) {
				for (Message.Attachment att : event.message().getAttachments()) {
					if (att.isImage()) {
						try (InputStream is = att.getProxy().download().join()) {
							acts.add(event.guild().createEmoji(args.getString("name"), Icon.from(is)));
						} catch (IOException e) {
							throw new RuntimeException(e);
						}
						break;
					}
				}
			} else {
				event.channel().sendMessage(locale.get("error/image_required")).queue();
				return;
			}
		} else {
			if (emotes.isEmpty()) {
				event.channel().sendMessage(locale.get("error/no_emotes")).queue();
				return;
			}

			List<RichCustomEmoji> current = event.guild().getEmojis();
			int animated = (int) current.stream().filter(CustomEmoji::isAnimated).count();
			int normal = current.size() - animated;

			for (CustomEmoji e : emotes) {
				if (e.isAnimated()) animated++;
				else normal++;
			}

			if (animated > event.guild().getMaxEmojis() || normal > event.guild().getMaxEmojis()) {
				event.channel().sendMessage(locale.get("error/no_emote_space")).queue();
				return;
			}

			Map<CustomEmoji, CompletableFuture<InputStream>> mapped = new HashMap<>();
			for (CustomEmoji e : emotes) {
				mapped.put(e, e.getImage().download());
			}

			CompletableFuture.allOf(mapped.values().toArray(CompletableFuture[]::new)).join();

			for (Map.Entry<CustomEmoji, CompletableFuture<InputStream>> e : mapped.entrySet()) {
				InputStream is = e.getValue().getNow(null);
				if (is == null) continue;

				try (is) {
					acts.add(event.guild().createEmoji(e.getKey().getName(), Icon.from(is)));
				} catch (IOException ex) {
					throw new RuntimeException(ex);
				}
			}
		}

		RestAction.allOf(acts)
				.flatMap(s -> event.channel().sendMessage(locale.get("success/emotes_added", s.size())))
				.queue();
	}
}
