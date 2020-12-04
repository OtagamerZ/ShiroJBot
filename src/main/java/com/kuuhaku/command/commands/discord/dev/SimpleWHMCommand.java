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

package com.kuuhaku.command.commands.discord.dev;

import club.minnced.discord.webhook.WebhookClient;
import club.minnced.discord.webhook.WebhookClientBuilder;
import club.minnced.discord.webhook.send.WebhookMessageBuilder;
import com.kuuhaku.Main;
import com.kuuhaku.command.Category;
import com.kuuhaku.command.Command;
import com.kuuhaku.utils.Helper;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.exceptions.InsufficientPermissionException;
import org.jetbrains.annotations.NonNls;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;

public class SimpleWHMCommand extends Command {

	public SimpleWHMCommand(String name, String description, Category category, boolean requiresMM) {
		super(name, description, category, requiresMM);
	}

	public SimpleWHMCommand(String name, String[] aliases, String description, Category category, boolean requiresMM) {
		super(name, aliases, description, category, requiresMM);
	}

	public SimpleWHMCommand(@NonNls String name, String usage, String description, Category category, boolean requiresMM) {
		super(name, usage, description, category, requiresMM);
	}

	public SimpleWHMCommand(String name, String[] aliases, String usage, String description, Category category, boolean requiresMM) {
		super(name, aliases, usage, description, category, requiresMM);
	}

	@Override
	public void execute(User author, Member member, String rawCmd, String[] args, Message message, MessageChannel channel, Guild guild, String prefix) {
		try {
			Webhook wh = Helper.getOrCreateWebhook((TextChannel) channel, "Webhook Test", Main.getInfo().getAPI());
			Map<String, Consumer<Void>> s = Helper.sendEmotifiedString(guild, String.join(" ", args));

			WebhookMessageBuilder wmb = new WebhookMessageBuilder();
			wmb.setContent(String.valueOf(s.keySet().toArray()[0]));
			wmb.setAvatarUrl(author.getAvatarUrl());
			wmb.setUsername(author.getName());

			assert wh != null;
			WebhookClient wc = new WebhookClientBuilder(wh.getUrl()).build();
			try {
				wc.send(wmb.build()).thenAccept(rm -> s.get(String.valueOf(s.keySet().toArray()[0])).accept(null)).get();
			} catch (InterruptedException | ExecutionException e) {
				e.printStackTrace();
			}
		} catch (InterruptedException | ExecutionException | InsufficientPermissionException e) {
			Helper.sendPM(Objects.requireNonNull(message.getTextChannel().getGuild().getOwner()).getUser(), "❌ | " + Main.getInfo().getAPI().getSelfUser().getName() + " não possui permissão para criar um webhook em seu servidor no canal " + message.getTextChannel().getAsMention());
		}
	}
}
