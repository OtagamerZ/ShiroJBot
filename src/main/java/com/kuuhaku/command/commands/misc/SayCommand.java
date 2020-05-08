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

import club.minnced.discord.webhook.WebhookClient;
import club.minnced.discord.webhook.WebhookClientBuilder;
import club.minnced.discord.webhook.send.WebhookMessageBuilder;
import com.kuuhaku.Main;
import com.kuuhaku.command.Category;
import com.kuuhaku.command.Command;
import com.kuuhaku.utils.Helper;
import net.dv8tion.jda.api.MessageBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.exceptions.ErrorResponseException;
import net.dv8tion.jda.api.exceptions.InsufficientPermissionException;
import org.jetbrains.annotations.NonNls;

import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;

public class SayCommand extends Command {

	public SayCommand(String name, String description, Category category, boolean requiresMM) {
		super(name, description, category, requiresMM);
	}

	public SayCommand(String name, String[] aliases, String description, Category category, boolean requiresMM) {
		super(name, aliases, description, category, requiresMM);
	}

	public SayCommand(String name, String usage, String description, Category category, boolean requiresMM) {
		super(name, usage, description, category, requiresMM);
	}

	public SayCommand(@NonNls String name, @NonNls String[] aliases, String usage, String description, Category category, boolean requiresMM) {
		super(name, aliases, usage, description, category, requiresMM);
	}

	@Override
	public void execute(User author, Member member, String rawCmd, String[] args, Message message, MessageChannel channel, Guild guild, String prefix) {

		if (args.length == 0) {
			channel.sendMessage(":x: | Você precisa definir uma mensagem.").queue();
			return;
		}

		MessageBuilder mb = new MessageBuilder();
		mb.append(Helper.makeEmoteFromMention(args)).stripMentions(guild);

		try {
			Webhook wh = Helper.getOrCreateWebhook((TextChannel) channel, "Shiro", Main.getInfo().getAPI());
			Map<String, Consumer<Void>> s = Helper.sendEmotifiedString(guild, mb.getStringBuilder().toString());

			WebhookMessageBuilder wmb = new WebhookMessageBuilder();
			wmb.setContent(String.valueOf(s.keySet().toArray()[0]));
			wmb.setAvatarUrl(author.getAvatarUrl());
			wmb.setUsername(author.getName());

			assert wh != null;
			WebhookClient wc = new WebhookClientBuilder(wh.getUrl()).build();
			try {
				message.delete().queue();
				wc.send(wmb.build()).thenAccept(rm -> s.get(String.valueOf(s.keySet().toArray()[0])).accept(null)).get();
			} catch (InterruptedException | ExecutionException e) {
				Helper.logger(this.getClass()).error(e + " | " + e.getStackTrace()[0]);
			}
		} catch (IndexOutOfBoundsException | InsufficientPermissionException | ErrorResponseException | NullPointerException e) {
			channel.sendMessage(mb.build()).queue();
			if (guild.getSelfMember().hasPermission(Permission.MESSAGE_MANAGE))
				message.delete().queue(null, Helper::doNothing);
		}
	}
}
